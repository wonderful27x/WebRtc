package com.example.wywebrtc.webrtcinderface;

import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;

/**
 * socket层的接口，在交换信令中与服务器的直接对话
 */
public interface SocketInterface {
    public void connect(String uri);//发起加入房间请求,成功后服务器返回应答onMessage被调用，带有房间里的人的id和自己的id
    public void sendOffer(String socketId, SessionDescription localDescription); //向房间的其他成员发送自己的SDP信息
    public void sendAnswer(String socketId, SessionDescription localDescription);//发送应答
    public void sendIceCandidate(String socketId, IceCandidate iceCandidate);    //向房间的其他成员发送自己的iceCandidate信息
    public void close();                                                         //关闭socket
    public void joinRoom(String roomId);                                         //请求加入房间
}
