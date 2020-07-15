package com.example.wywebrtc.view;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.example.wywebrtc.R;
import com.example.wywebrtc.bean.Message;
import com.example.wywebrtc.type.RoomType;
import com.example.wywebrtc.webrtcinderface.ViewCallback;
import com.example.wywebrtc.webrtcinderface.WebRtcInterface;
import com.example.wywebrtc.webrtcsource.WebRtcManager;
import org.webrtc.MediaStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @author wonderful
 * @date 2020-7-?
 * @version 1.0
 * @description 主activity
 * @license  BSD-2-Clause License
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener, ViewCallback {

    private EditText roomNumber;
    private Button addRoom;
    private Button p2pVideo;
    private Button p2pAudio;
    private Button live;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
        permissionCheck(RoomType.DEFAULT);
    }

    private void init(){
        roomNumber = findViewById(R.id.roomNumber);
        addRoom = findViewById(R.id.addRoom);
        p2pVideo = findViewById(R.id.p2pVideo);
        p2pAudio = findViewById(R.id.p2pAudio);
        live = findViewById(R.id.live);

        addRoom.setOnClickListener(this);
        p2pVideo.setOnClickListener(this);
        p2pAudio.setOnClickListener(this);
        live.setOnClickListener(this);
    }

    private void permissionCheck(RoomType roomType){
        List<String> permissions = permissionCheck();
        if (!permissions.isEmpty()){
            permissionRequest(permissions,roomType.getCode());
        }else if (roomType != RoomType.DEFAULT){
            sendRequest(roomType.getCode());
        }
    }

    //判断是否授权所有权限
    private List<String> permissionCheck(){
        List<String> permissions = new ArrayList<>();
        if (!checkPermission(Manifest.permission.CAMERA)){
            permissions.add(Manifest.permission.CAMERA);
        }
        if (!checkPermission(Manifest.permission.RECORD_AUDIO)){
            permissions.add(Manifest.permission.RECORD_AUDIO);
        }
        if (!checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)){
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        return permissions;
    }

    //发起权限申请
    private void permissionRequest(List<String> permissions,int requestCode){
        String[] permissionArray = permissions.toArray(new String[permissions.size()]);
        ActivityCompat.requestPermissions(this,permissionArray,requestCode);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == RoomType.DEFAULT.getCode()){
            if (grantResults.length >0){
                for (int result:grantResults){
                    if (result != PackageManager.PERMISSION_GRANTED){
                        Toast.makeText(MainActivity.this,"您拒绝了权限相关功能将无法使用！",Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
            }else {
                Toast.makeText(MainActivity.this,"发生未知错误！",Toast.LENGTH_SHORT).show();
            }
        }else{
            if (grantResults.length >0){
                for (int result:grantResults){
                    if (result != PackageManager.PERMISSION_GRANTED){
                        Toast.makeText(MainActivity.this,"对不起，您拒绝了权限无法使用此功能！",Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
                sendRequest(requestCode);
            }else {
                Toast.makeText(MainActivity.this,"发生未知错误！",Toast.LENGTH_SHORT).show();
            }
        }
    }

    //判断是否有权限
    private boolean checkPermission(String permission){
        return ContextCompat.checkSelfPermission(this,permission) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.addRoom:
                permissionCheck(RoomType.NORMAL);
                break;
            case R.id.p2pVideo:
                permissionCheck(RoomType.VIDEO_ONLY);
                break;
            case R.id.p2pAudio:
                permissionCheck(RoomType.AUDIO_ONLY);
                break;
            case R.id.live:
                permissionCheck(RoomType.LIVE);
                break;
            default:
                break;
        }
    }


    @Override
    public void socketCallback(Message message) {

    }

    @Override
    public void setLocalStream(MediaStream localStream, String selfId) {

    }

    @Override
    public void addRemoteStream(MediaStream localStream, String selfId) {

    }

    @Override
    public void closeWindow(String socketId) {

    }

    //发起请求
    private void sendRequest(int roomTypeCode){
        WebRtcInterface webRtcInterface = WebRtcManager.getInstance(this,null);
        webRtcInterface.chatRequest(RoomType.getRooType(roomTypeCode),roomNumber.getText().toString());
    }
}
