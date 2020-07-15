package com.example.wywebrtc.webrtcinderface;

import com.example.wywebrtc.bean.User;
import org.webrtc.IceCandidate;
import java.util.List;

/**
 * PeerConnection层的接口，处理信令信息以及人员进出房间的动作
 */
public interface ConnectionInterface extends WebRtcInterface{
    public void connectSuccess(User user);                                       //webSocket连接成功
    public void remoteJoinToRoom(User user);                                     //有人加入房间
    public void remoteOutRoom(User user);                                        //有人退出房间
    public void onReceiveOffer(String socketId, String sdp);                     //远端发起offer
    public void onReceiveAnswer(String socketId, String sdp);                    //远端响应了offer
    public void onRemoteCandidate(String socketId, IceCandidate iceCandidate);   //远端响应了Candidate

    public void createConnection(List<String> membersId);                        //创建P2P连接
    public int getConnectNum();                                                  //获取链接数
}
