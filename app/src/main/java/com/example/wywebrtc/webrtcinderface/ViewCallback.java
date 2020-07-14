package com.example.wywebrtc.webrtcinderface;

import com.example.wywebrtc.bean.Message;
import org.webrtc.MediaStream;

/**
 * UI层的回调接口，P2P通信过程中对UI层的控制，如界面刷新等,
 * WebRtcManager会持有这个接口实现者Activity的引用,
 * 整个P2P通信过程中对UI的操作都由WebRtcManager中转完成，
 * PeerConnection、WebSocket与UI层不存在耦合
 */
public interface ViewCallback {
    public void socketCallback(Message message);                      //socket状态回调
    public void setLocalStream(MediaStream localStream,String selfId); //添加本地流进行预览
    public void addRemoteStream(MediaStream remoteStream,String socketId);//添加远端的流进行显示
    public void closeWindow(String socketId);                          //关闭窗口
}
