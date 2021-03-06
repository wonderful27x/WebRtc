package com.example.wywebrtc.webrtcinderface;

/**
 * WebRtcManager 与UI层交互的接口
 * UI层会持有这个接口实现者WebRtcManager的引用，
 * 在整个P2P通信中对通信的控制都由WebRtcManager中转完成
 * PeerConnection、WebSocket与UI层不存在耦合
 */
public interface WebRtcInterface{
    public void switchMute(boolean mute);           //静音切换
    public void switchCamera();                     //前后摄像头切换
    public void switchHandsfree(boolean handsfree); //免提切换
    public void powerCamera(boolean enable);        //摄像头开关
    public void hangUp();
    public void chatRequest(@MediaType int mediaType, String roomId);//发起聊天请求，建立socket连接
    public void joinRoom();                                          //请求加入房间
}
