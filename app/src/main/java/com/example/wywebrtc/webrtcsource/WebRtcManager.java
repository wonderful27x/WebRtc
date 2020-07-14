package com.example.wywebrtc.webrtcsource;

import android.content.Context;

import com.example.wywebrtc.type.MessageType;
import com.example.wywebrtc.type.RoomType;
import com.example.wywebrtc.view.MeetRoomActivity;
import com.example.wywebrtc.view.SingleAudioActivity;
import com.example.wywebrtc.view.SingleVideoActivity;
import com.example.wywebrtc.bean.Message;
import com.example.wywebrtc.webrtcinderface.ConnectionInterface;
import com.example.wywebrtc.webrtcinderface.SocketInterface;
import com.example.wywebrtc.webrtcinderface.ViewCallback;
import com.example.wywebrtc.webrtcinderface.WebRtcInterface;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaStream;
import org.webrtc.SessionDescription;
import java.util.ArrayList;

/**
 * P2P通信中PeerConnectionManager、WebSocket和UI交互的中间纽带，做大量的信息交换，实现各层之间的解耦
 * PeerConnectionManager在建立连接的时候需要通过WebSocket进行信令交换，这项工作将由WebRtcManager转发，
 * 而WebSocket端响应后需要通知PeerConnectionManager，这项工作也由WebRtcManager转发
 */
public class WebRtcManager implements WebRtcInterface,ConnectionInterface,SocketInterface,ViewCallback{
    private static WebRtcManager webRtcManager = null;
    private SocketInterface socketInterface;          //Socket层接口
    private ConnectionInterface connectionInterface;  //Connection层接口
    private ViewCallback viewCallback;                //view层回掉接口
    private Context context;                          //上下文
    private EglBase eglBase;                          //EglBase可以获取openGL上下文，利用这个上下文可以直接进行渲染
    private String roomId;                            //房间号
    private RoomType roomType;                        //房间类型

    private WebRtcManager(ViewCallback viewCallback,EglBase eglBase){
        init(viewCallback,eglBase);
    }

    private void init(ViewCallback viewCallback,EglBase eglBase){
        this.viewCallback = viewCallback;
        this.context = (Context) viewCallback;
        this.eglBase = eglBase;
    }

    public static WebRtcManager getInstance(ViewCallback viewCallback,EglBase eglBase){
        if (webRtcManager == null){
            synchronized (WebRtcManager.class){
                if (webRtcManager == null){
                    webRtcManager = new WebRtcManager(viewCallback,eglBase);
                }
            }
        }else {
            synchronized (WebRtcManager.class){
                webRtcManager.init(viewCallback,eglBase);
            }
        }
        return webRtcManager;
    }

    public static WebRtcManager getInstance(){
        if (webRtcManager == null || webRtcManager.viewCallback == null){
            throw new RuntimeException("please use getInstance which has parameter first before use");
        }
        return webRtcManager;
    }

    //TODO 这个context将会在PeerConnectionManager创建必要资源是使用
    //TODO 转换成ApplicationContext非常重要，不转换会出现各种bug，根本无法定位到！！！
    public Context getContext(){
        return context.getApplicationContext();
    }

    public EglBase getEglBase(){
        return eglBase;
    }

    //向服务器发起建立WebSocket连接的请求
    private void connect(){
        if (socketInterface == null){
            socketInterface = new WebSocket();
            connectionInterface = PeerConnectionManager.getInstance(roomType);
        }
        socketInterface.connect(WebRtcConfig.SOCKET_URI);
    }

    //socket建立成功
    public void connectSuccess() {
        switch (mediaType) {
            case MediaType.MEET_ROOM:
                MeetRoomActivity.startSelf(context);
                break;
            case MediaType.SINGLE_VIDEO:
                SingleVideoActivity.startSelf(context);
                break;
            case MediaType.SINGLE_AUDIO:
                SingleAudioActivity.startSelf(context);
                break;
            default:
                break;
        }
    }

    /**==================================WebRtcInterface===========================*/
    //静音切换
    @Override
    public void switchMute(boolean mute) {
        connectionInterface.switchMute(mute);
    }
    //前置摄像头切换
    @Override
    public void switchCamera() {
        connectionInterface.switchCamera();
    }
    //免提
    @Override
    public void switchHandsfree(boolean handsfree) {
        connectionInterface.switchHandsfree(handsfree);
    }
    //摄像头开关
    @Override
    public void powerCamera(boolean enableCamera) {
        connectionInterface.powerCamera(enableCamera);
    }
    //主动挂断，这时应该解除对象的引用
    //顺序不能变，因为viewCallback不需要再回调了
    @Override
    public void hangUp() {
        viewCallback = null;
        context = null;
        eglBase = null;
        if (connectionInterface != null){
            connectionInterface.hangUp();
            connectionInterface = null;
        }
    }

    //发起聊天请求
    @Override
    public void chatRequest(RoomType roomType, String roomId) {
        this.roomType = roomType;
        this.roomId = roomId;
        connect();
    }

    //socket连接建立后申请加入房间
    @Override
    public void joinRoom() {
        socketInterface.joinRoom(roomId);
    }

    /**==================================WebRtcInterface===========================*/

    /**====================================ConnectionInterface============================*/
    @Override
    public void remoteJoinToRoom(String socketId) {
        if(mediaType == MediaType.SINGLE_AUDIO || mediaType == MediaType.SINGLE_VIDEO){
            if (getConnectNum() >= 1){
                return;
            }
        }
        connectionInterface.remoteJoinToRoom(socketId);
    }

    @Override
    public void remoteOutRoom(String socketId) {
        connectionInterface.remoteOutRoom(socketId);
    }

    @Override
    public void onReceiveOffer(String socketId, String sdp) {
        connectionInterface.onReceiveOffer(socketId,sdp);
    }

    @Override
    public void onReceiveAnswer(String socketId, String sdp) {
        connectionInterface.onReceiveAnswer(socketId,sdp);
    }

    @Override
    public void onRemoteCandidate(String socketId, IceCandidate iceCandidate) {
        connectionInterface.onRemoteCandidate(socketId,iceCandidate);
    }

    //创建P2P连接
    @Override
    public void createConnection(ArrayList<String> connections, String socketId) {
        if(mediaType == MediaType.SINGLE_AUDIO || mediaType == MediaType.SINGLE_VIDEO){
            if (connections.size()>1){
                Message message = new Message(MessageType.ROOM_FULL,"房间已满加入失败！");
                viewCallback.socketCallback(message);
                return;
            }
        }
        connectionInterface.createConnection(connections,socketId);
    }

    //获取链接数
    @Override
    public int getConnectNum() {
        return connectionInterface.getConnectNum();
    }

    /**====================================ConnectionInterface============================*/

    /**====================================SocketInterface============================*/
    @Override
    public void connect(String uri) {

    }

    @Override
    public void sendOffer(String socketId, SessionDescription localDescription) {
        socketInterface.sendOffer(socketId,localDescription);
    }

    @Override
    public void sendAnswer(String socketId, SessionDescription localDescription) {
        socketInterface.sendAnswer(socketId,localDescription);
    }

    @Override
    public void sendIceCandidate(String socketId, IceCandidate iceCandidate) {
        socketInterface.sendIceCandidate(socketId,iceCandidate);
    }

    @Override
    public void close() {
        if (socketInterface != null){
            socketInterface.close();
            socketInterface = null;
        }
    }

    @Override
    public void joinRoom(String roomId) {

    }
    /**====================================SocketInterface============================*/


    /**====================================ViewCallback============================*/
    @Override
    public void socketCallback(Message message) {
        viewCallback.socketCallback(message);
    }

    @Override
    public void setLocalStream(MediaStream localStream, String selfId) {
        viewCallback.setLocalStream(localStream,selfId);
    }

    @Override
    public void addRemoteStream(MediaStream remoteStream, String socketId) {
        viewCallback.addRemoteStream(remoteStream,socketId);
    }

    @Override
    public void closeWindow(String socketId) {
        if (viewCallback != null){
            viewCallback.closeWindow(socketId);
        }
    }
    /**====================================ViewCallback============================*/
}
