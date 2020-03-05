package com.example.wywebrtc.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;
import com.example.wywebrtc.R;
import com.example.wywebrtc.bean.Message;
import com.example.wywebrtc.utils.PositionUtils;
import com.example.wywebrtc.webrtcinderface.MessageType;
import com.example.wywebrtc.webrtcinderface.ViewCallback;
import com.example.wywebrtc.webrtcinderface.WebRtcInterface;
import com.example.wywebrtc.webrtcsource.WebRtcManager;
import org.webrtc.EglBase;
import org.webrtc.MediaStream;
import org.webrtc.RendererCommon;
import org.webrtc.SurfaceViewRenderer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MeetRoomActivity extends AppCompatActivity implements WebRtcInterface, ViewCallback {

    private FrameLayout videoFrameLayout;
    private WebRtcInterface manager;
    private EglBase eglBase;
    private Map<String, SurfaceViewRenderer> videoViews = new HashMap<>();//保存用户信息
    private Map<String,ProxyVideoSink> videoSinkMap = new HashMap<>();
    private List<String> personIds = new ArrayList<>();                   //保存用户id

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chat_room_layout);
        placeFragment();
        videoFrameLayout = findViewById(R.id.videoFrameLayout);
        eglBase = EglBase.create();
        manager = WebRtcManager.getInstance(this,eglBase);
        manager.joinRoom();
    }

    /**===============================WebRtcInterface========================*/
    @Override
    public void switchMute(boolean mute) {
        manager.switchMute(mute);
    }

    @Override
    public void switchCamera() {
        manager.switchCamera();
    }

    @Override
    public void switchHandsfree(boolean handsfree) {
        manager.switchHandsfree(handsfree);
    }

    @Override
    public void powerCamera(boolean enable) {
        manager.powerCamera(enable);
    }

    //挂断时释放自己的资源，并关闭webSocket,
    //webSocket的关闭将会得到远端的响应
    @Override
    public void hangUp() {
        exitRoom();
        finish();
    }

    @Override
    public void chatRequest(int mediaType, String roomId) {

    }

    @Override
    public void joinRoom() {

    }
    /**===============================WebRtcInterface========================*/


    /**================================ViewCallback==========================*/

    @Override
    public void socketCallback(Message message) {
        switch (message.getMessageType()){
            case MessageType.SOCKET_OPEN:
                break;
            case MessageType.SOCKET_CLOSE:
                break;
            case MessageType.SOCKET_ERROR:
                break;
            case MessageType.ROOM_FULL:
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MeetRoomActivity.this,message.getMessage(),Toast.LENGTH_LONG).show();
                    }
                });
                break;
            default:
                break;
        }
    }

    /**
     * 调用这个方法，创建surfaceView并渲染刷新界面，即将自己摄像头信息显示在屏幕上
     * @param localStream 本地流
     * @param selfId 用户id,请求建立连接后服务器返回的id
     */
    @Override
    public void setLocalStream(MediaStream localStream, String selfId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                addView(localStream,selfId);
            }
        });
    }

    /**
     * 调用这个方法，创建surfaceView并渲染刷新界面，即将远端摄像头信息显示在屏幕上
     * @param remoteStream
     * @param socketId
     */
    @Override
    public void addRemoteStream(MediaStream remoteStream, String socketId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                addView(remoteStream,socketId);
            }
        });
    }

    //当远端有人退出房间是这个方法会被回调，则将窗口关闭
    @Override
    public void closeWindow(String socketId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                removeView(socketId);
            }
        });
    }
    /**================================ViewCallback==========================*/

    /**
     * 当有人离开退出房间是则移除对应的窗口
     * @param socketId
     */
    private void removeView(String socketId){
        SurfaceViewRenderer renderer = videoViews.get(socketId);
        ProxyVideoSink videoSink = videoSinkMap.get(socketId);
        if (renderer != null){
            renderer.release();
        }
        if (videoSink != null){
            videoSink.setTarget(null);
        }
        videoViews.remove(socketId);
        videoSinkMap.remove(socketId);
        personIds.remove(socketId);
        videoFrameLayout.removeView(renderer);
        rePlaceView();
    }

    /**
     * 每当有一个人加入房间这个方法就会被调用一次，
     * 创建surfaceView，添加到窗口中，并重新计算宽高设置摆放位置
     * @param stream 媒体流（本地或远端）
     * @param userId 用户id
     */
    public void addView(MediaStream stream,String userId){

        //创建surfaceView并初始化
        SurfaceViewRenderer surfaceViewRenderer = new SurfaceViewRenderer(this);//采用webrtc中的surfaceView
        surfaceViewRenderer.init(eglBase.getEglBaseContext(),null);       //初始化surfaceView
        surfaceViewRenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT); //设置缩放模式 SCALE_ASPECT_FIT:按照view宽高设置，SCALE_ASPECT_FILL：按照摄像头预览画面设置
        surfaceViewRenderer.setMirror(true);                                             //镜像翻转

        ProxyVideoSink videoSink = new ProxyVideoSink();
        videoSink.setTarget(surfaceViewRenderer);

        //将摄像头数据渲染到surfaceView
        if (stream.videoTracks.size()>0){
            stream.videoTracks.get(0).addSink(videoSink);
        }

        //将surfaceView添加到窗口中
        videoFrameLayout.addView(surfaceViewRenderer);

        //保存数据
        videoViews.put(userId,surfaceViewRenderer);
        videoSinkMap.put(userId,videoSink);
        personIds.add(userId);

        rePlaceView();
    }

    //重新计算宽高，设置摆放位置
    private void rePlaceView(){
        int size = videoViews.size();
        for (int i=0; i<size; i++){
            String id = personIds.get(i);
            SurfaceViewRenderer renderer = videoViews.get(id);
            if (renderer != null){
                FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                layoutParams.width = PositionUtils.getWith(this,size);
                layoutParams.height = PositionUtils.getWith(this,size);
                layoutParams.leftMargin = PositionUtils.getX(this,size,i);
                layoutParams.topMargin = PositionUtils.getY(this,size,i);
                renderer.setLayoutParams(layoutParams);
            }
        }
    }

    //退出房间
    private void exitRoom(){
        manager.hangUp();
        for (SurfaceViewRenderer renderer:videoViews.values()){
            renderer.release();
        }
        for (ProxyVideoSink sink:videoSinkMap.values()){
            sink.setTarget(null);
        }
        videoViews.clear();
        videoSinkMap.clear();
        personIds.clear();
        eglBase = null;
    }

    private void placeFragment(){
        MenuFragment fragment = new MenuFragment(this);
        FragmentManager manager = getSupportFragmentManager();
        manager.beginTransaction()
                .replace(R.id.menuFrameLayout,fragment)
                .commit();
    }

    public static void startSelf(Context context){
        Intent intent = new Intent(context,MeetRoomActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        exitRoom();
        super.onDestroy();
    }
}
