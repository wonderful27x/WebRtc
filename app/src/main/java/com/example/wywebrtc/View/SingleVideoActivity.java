package com.example.wywebrtc.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.widget.Toast;
import com.example.wywebrtc.R;
import com.example.wywebrtc.bean.Message;
import com.example.wywebrtc.webrtcinderface.MessageType;
import com.example.wywebrtc.webrtcinderface.ViewCallback;
import com.example.wywebrtc.webrtcinderface.WebRtcInterface;
import com.example.wywebrtc.webrtcsource.WebRtcManager;
import org.webrtc.EglBase;
import org.webrtc.MediaStream;
import org.webrtc.RendererCommon;
import org.webrtc.SurfaceViewRenderer;

//一对一视频聊天
public class SingleVideoActivity extends AppCompatActivity implements WebRtcInterface, ViewCallback {

    private static final String TAG = "SingleVideoActivity";

    private SurfaceViewRenderer localView;
    private SurfaceViewRenderer remoteView;
    private WebRtcInterface manager;
    private EglBase eglBase;
    private ProxyVideoSink localViewSink;
    private ProxyVideoSink removeViewSink;
    private boolean swapWindow;
    private float lastX = 0;
    private float lastY = 0;
    private float positionX = 0;
    private float positionY = 0;
    private boolean longClick = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.single_video_layout);
        initView();
        initListener();
        startCall();
    }

    //开始请求加入房间
    private void startCall(){
        manager = WebRtcManager.getInstance(this,eglBase);
        manager.joinRoom();
    }

    //初始化SurfaceViewRenderer
    private void initView(){
        placeFragment();

        localView = findViewById(R.id.localView);
        remoteView = findViewById(R.id.remoteView);
        eglBase = EglBase.create();

        //本地图像初始化
        localView.init(eglBase.getEglBaseContext(),null);
        localView.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
        localView.setZOrderMediaOverlay(true);
        localView.setMirror(true);
        localViewSink = new ProxyVideoSink();
        //远端图像初始化
        remoteView.init(eglBase.getEglBaseContext(),null);
        remoteView.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_BALANCED);
        remoteView.setMirror(true);
        removeViewSink = new ProxyVideoSink();

        swapWindow(true);
        localView.setOnClickListener(v -> swapWindow(!swapWindow));
        localView.setOnLongClickListener( view -> {
            longClick = true;
            return true;
        });
    }

    //是否互换窗口位置
    private void swapWindow(boolean swapWindow){
        this.swapWindow = swapWindow;
        localViewSink.setTarget(swapWindow ? remoteView : localView);
        removeViewSink.setTarget(swapWindow ? localView : remoteView);
    }

    //初始化滑动监听
    @SuppressLint("ClickableViewAccessibility")
    private void initListener(){
        localView.setOnTouchListener((view,motionEvent) -> {
            if (!longClick)return false;
            switch (motionEvent.getAction()){
                case MotionEvent.ACTION_DOWN:
                    lastX = positionX = motionEvent.getRawX();
                    lastY = positionY = motionEvent.getRawY();
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (lastX == 0 && lastY == 0){
                        lastX = motionEvent.getRawX();
                        lastY = motionEvent.getRawY();
                        return true;
                    }
                    positionX = motionEvent.getRawX();
                    positionY = motionEvent.getRawY();
                    localView.setTranslationX(localView.getTranslationX() + positionX-lastX);
                    localView.setTranslationY(localView.getTranslationY() + positionY-lastY);
                    lastX = positionX;
                    lastY = positionY;
                    break;
                case MotionEvent.ACTION_UP:
                    lastX = positionX = 0;
                    lastY = positionY = 0;
                    longClick = false;
                    break;
                default:
                    break;
            }
            return true;
        });
    }

    private void placeFragment(){
        MenuFragment fragment = new MenuFragment(this);
        FragmentManager manager = getSupportFragmentManager();
        manager.beginTransaction()
                .replace(R.id.menuFrameLayout,fragment)
                .commit();
    }

    public static void startSelf(Context context){
        Intent intent = new Intent(context,SingleVideoActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        exitRoom();
        super.onDestroy();
    }

    private void release(){
        if (localViewSink != null){
            localViewSink.setTarget(null);
            localViewSink = null;
        }
        if (removeViewSink != null){
            removeViewSink.setTarget(null);
            removeViewSink = null;
        }
        if (localView != null){
            localView.release();
            localView = null;
        }
        if (remoteView != null){
            remoteView.release();
            remoteView = null;
        }
        eglBase = null;
    }

    //退出房间
    private void exitRoom(){
        manager.hangUp();
        release();
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
                        Toast.makeText(SingleVideoActivity.this,message.getMessage(),Toast.LENGTH_LONG).show();
                    }
                });
                break;
            default:
                break;
        }
    }

    @Override
    public void setLocalStream(MediaStream localStream, String selfId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //将摄像头数据渲染到surfaceView
                if (localStream.videoTracks.size()>0){
                    localStream.videoTracks.get(0).addSink(localViewSink);
                }
            }
        });
    }

    @Override
    public void addRemoteStream(MediaStream remoteStream, String selfId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //将摄像头数据渲染到surfaceView
                if (remoteStream.videoTracks.size()>0){
                    remoteStream.videoTracks.get(0).addSink(removeViewSink);
                    swapWindow(false);
                }
            }
        });
    }

    @Override
    public void closeWindow(String socketId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                release();
                finish();
            }
        });
    }
    /**================================ViewCallback==========================*/

}
