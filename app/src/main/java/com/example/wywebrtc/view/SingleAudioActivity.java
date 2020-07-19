package com.example.wywebrtc.view;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.example.wywebrtc.R;
import com.example.wywebrtc.bean.BaseMessage;
import com.example.wywebrtc.bean.Message;
import com.example.wywebrtc.bean.Room;
import com.example.wywebrtc.type.RoomType;
import com.example.wywebrtc.webrtcinderface.ViewCallback;
import com.example.wywebrtc.webrtcinderface.WebRtcInterface;
import com.example.wywebrtc.webrtcsource.WebRtcManager;
import org.webrtc.EglBase;
import org.webrtc.MediaStream;

/**
 * @author wonderful
 * @date 2020-7-?
 * @version 1.0
 * @description 一对一语言聊天
 * @license  BSD-2-Clause License
 */
public class SingleAudioActivity extends AppCompatActivity implements WebRtcInterface, ViewCallback {

    private WebRtcInterface manager;
    private EglBase eglBase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.single_audio_layout);
        placeFragment();
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

    @Override
    public void hangUp() {
        manager.hangUp();
        eglBase = null;
        finish();
    }

    @Override
    public void chatRequest(RoomType roomType, String roomId) {

    }

    @Override
    public void joinRoom() {

    }
    /**===============================WebRtcInterface========================*/

    /**================================ViewCallback==========================*/
    @Override
    public void socketCallback(Message message) {
        switch (message.getMessageType()){
            case SOCKET_OPEN:
                break;
            case SOCKET_CLOSE:
                break;
            case SOCKET_ERROR:
                break;
            case ROOM_FULL:
                showFullMessage(message);
                break;
            default:
                break;
        }
    }

    private void showFullMessage(Message message){
        BaseMessage<Room,Object> baseMessage = message.transForm(new BaseMessage<Room, Object>() {});
        String msg = "房间已满，房间号： " + baseMessage.getMessage().getRoomId() + " maxSize: " + baseMessage.getMessage().maxSize() + " currentSize: " + baseMessage.getMessage().currentSize();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(SingleAudioActivity.this,msg,Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void setLocalStream(MediaStream localStream, String selfId) {

    }

    @Override
    public void addRemoteStream(MediaStream localStream, String selfId) {

    }

    @Override
    public void closeWindow(String socketId) {
        finish();
    }
    /**================================ViewCallback==========================*/

    private void placeFragment(){
        MenuFragment fragment = new MenuFragment(this);
        FragmentManager manager = getSupportFragmentManager();
        manager.beginTransaction()
                .replace(R.id.menuFrameLayout,fragment)
                .commit();
    }

    public static void startSelf(Context context){
        Intent intent = new Intent(context,SingleAudioActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        manager.hangUp();
        eglBase = null;
        super.onDestroy();
    }
}
