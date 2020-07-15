package com.example.wywebrtc.webrtcsource;

import android.util.Log;
import com.example.wywebrtc.bean.BaseMessage;
import com.example.wywebrtc.bean.Event;
import com.example.wywebrtc.bean.Message;
import com.example.wywebrtc.bean.NegotiationMessage;
import com.example.wywebrtc.bean.Room;
import com.example.wywebrtc.bean.User;
import com.example.wywebrtc.type.MessageType;
import com.example.wywebrtc.webrtcinderface.SocketInterface;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

/**
 * @author wonderful
 * @date 2020-7-?
 * @version 1.0
 * @description P2P通信中，信令交换的驱动者，直接与信令服务器打交道，包括信令的收取和发送，
 * 同时还可以用于信令外的信息交换，如发起聊天请求，聊天文字的收发等
 * @license  BSD-2-Clause License
 */
public class WebSocket implements SocketInterface {

    private static final String TAG = "WebSocket";

    private WebRtcManager manager;
    private WebSocketClient webSocketClient;

    public WebSocket(){
        this.manager = WebRtcManager.getInstance();
    }

    //请求服务器建立socket连接
    @Override
    public void connect(String socketUri) {
        URI uri = null;
        try {
            uri = new URI(socketUri);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        webSocketClient = new WebSocketClient(uri) {
            //连接成功
            @Override
            public void onOpen(ServerHandshake handshakedata) {
                handleMessage(MessageType.SOCKET_OPEN,null);
                Log.d(TAG,"onOpen");
            }
            //消息推送
            @Override
            public void onMessage(String message) {
                Message messageObject = new Message(message);
                Event event = new Event();
                event.objA = event;
                event.objB = message;
                handleMessage(messageObject.getMessageType(),event);
                Log.d(TAG,message);
            }
            //关闭
            @Override
            public void onClose(int code, String reason, boolean remote) {
                Event event = new Event();
                event.code = code;
                event.message = reason;
                handleMessage(MessageType.SOCKET_CLOSE,event);
                Log.d(TAG,"onClose");
            }
            //连接失败
            @Override
            public void onError(Exception ex) {
                Event event = new Event();
                event.message = ex.getMessage();
                handleMessage(MessageType.SOCKET_ERROR,event);
                Log.e(TAG,ex.getMessage());
            }
        };
        //TODO 使用加密连接
        if (socketUri.startsWith("wss")){
            throw new RuntimeException("暂不支持安全的webSocket连接！");
        }
        webSocketClient.connect();//建立连接
    }

    //关闭webSocket连接，这个关闭动作会被服务器监听到，
    //服务器会给处理关闭者的其他人发送消息，告诉大家有人离开了房间
    @Override
    public void close() {
        if (webSocketClient != null){
            webSocketClient.close();
            webSocketClient = null;
        }
    }

    //消息分发
    private void handleMessage(MessageType messageType, Event event){
        switch (messageType){
            case SOCKET_OPEN:
                socketOpen();
                break;
            case SOCKET_CLOSE:
                socketClose(event);
                break;
            case SOCKET_ERROR:
                socketError(event);
                break;
            case CONNECT_OK:
                socketConnectOk(event);
                break;
            case COME:
                comingSelf(event);
                break;
            case JOIN:
                someoneJoin(event);
                break;
            case LEAVE:
                someoneLeave(event);
                break;
            case OFFER:
                someoneSendOffer(event);
                break;
            case ANSWER:
                someoneAnswerOffer(event);
                break;
            case CANDIDATE:
                candidateSwitch(event);
                break;
            case ROOM_FULL:
                roomFull(event);
                break;
        }
    }

    /**==================================处理webSocket消息========================*/

    //socket打开，也就是连接成功了，将信息回掉出去
    private void socketOpen(){
        Message message = new Message();
        message.setMessageType(MessageType.SOCKET_OPEN);
        manager.socketCallback(message);
    }

    //socket关闭，也就是断开连接了，将信息回掉出去
    private void socketClose(Event event){
        Message message = new Message();
        message.setMessageType(MessageType.SOCKET_CLOSE);
        message.setMessage(event.message);
        manager.socketCallback(message);
    }

    //socket发生错误
    private void socketError(Event event){
        Message message = new Message();
        message.setMessageType(MessageType.SOCKET_ERROR);
        message.setMessage(event.message);
        manager.socketCallback(message);
    }

    //socket 连接成功，注意他和SOCKET_OPEN是有区别的，SOCKET_OPEN仅仅是代表连接成功而已，这是webSocket的回调api，不携带信息
    //而CONNECT_OK是服务器通过发送消息告诉客户端连接成功了，是携带了信息的，从这里开始才真正的进入信令交换
    private void socketConnectOk(Event event){
        Message message = (Message) event.objA;
        BaseMessage<User,Object> baseMessage = message.transForm(new BaseMessage<User, Object>() {});
        manager.connectSuccess(baseMessage.getMessage());
    }

    //自己加入房间后得到的服务器响应，服务器会返回房间信息,根据房间成员id分别建立Connection
    private void comingSelf(Event event){
        Message message = (Message) event.objA;
        BaseMessage<Room,Object> baseMessage = message.transForm(new BaseMessage<Room, Object>() {});
        List<String> membersId = baseMessage.getMessage().getMembers();
        manager.createConnection(membersId);
    }

    //有人加入了房间
    private void someoneJoin(Event event){
        Message message = (Message) event.objA;
        BaseMessage<User,Object> baseMessage = message.transForm(new BaseMessage<User, Object>() {});
        manager.remoteJoinToRoom(baseMessage.getMessage());
    }

    //有人离开房间
    private void someoneLeave(Event event){
        Message message = (Message) event.objA;
        BaseMessage<User,Object> baseMessage = message.transForm(new BaseMessage<User, Object>() {});
        manager.remoteOutRoom(baseMessage.getMessage());
    }

    //有人发起了媒体协商,即收到了别人主动给自己发送的媒体协商数据
    private void someoneSendOffer(Event event){
        Message message = (Message) event.objA;
        BaseMessage<NegotiationMessage,Object> baseMessage = message.transForm(new BaseMessage<NegotiationMessage, Object>() {});
        NegotiationMessage negotiationMessage = baseMessage.getMessage();
        manager.onReceiveOffer(negotiationMessage.userId,negotiationMessage.sdp);
    }

    //有人响应了媒体协商，即自己主动给别人发送媒体协商数据后得到了对方的响应
    private void someoneAnswerOffer(Event event){
        Message message = (Message) event.objA;
        BaseMessage<NegotiationMessage,Object> baseMessage = message.transForm(new BaseMessage<NegotiationMessage, Object>() {});
        NegotiationMessage negotiationMessage = baseMessage.getMessage();
        manager.onReceiveAnswer(negotiationMessage.userId,negotiationMessage.sdp);
    }

    //网络协商交换，即收别人的网络协商数据
    //和Offer不同的是candidate不需要Answer,双方把自己重ice服务器获取的网络数据发送给对方就行了
    //他们之间的数据发送是异步的，即不需要等到A收到B的数据才给B响应，并且网络协商数据会交换多次
    //而媒体协商数据Offer交换一次就够了
    private void candidateSwitch(Event event){
        Message message = (Message) event.objA;
        BaseMessage<NegotiationMessage,Object> baseMessage = message.transForm(new BaseMessage<NegotiationMessage, Object>() {});
        NegotiationMessage negotiationMessage = baseMessage.getMessage();
        IceCandidate iceCandidate = new IceCandidate(negotiationMessage.sdpMid,negotiationMessage.sdpMLineIndex,negotiationMessage.sdp);
        manager.onRemoteCandidate(negotiationMessage.userId,iceCandidate);
    }

    //房间已满
    private void roomFull(Event event){
        Message message = (Message) event.objA;
        manager.socketCallback(message);
    }

    /**==================================处理webSocket消息========================*/


    /**============================通过webSocket发送消息========================*/
    //发起加入房间请求
    @Override
    public void joinRoom(NegotiationMessage negotiationMessage){
        BaseMessage<NegotiationMessage,Object> baseMessage = new BaseMessage<NegotiationMessage, Object>() {};
        baseMessage.setMessage(negotiationMessage);
        webSocketClient.send(baseMessage.toJson());
    }

    //向房间的其他成员发送自己的SDP信息
    @Override
    public void sendOffer(String socketId, SessionDescription localDescription) {
        BaseMessage<NegotiationMessage,Object> baseMessage = new BaseMessage<NegotiationMessage, Object>() {};
        baseMessage.getMessage().userId = socketId;
        baseMessage.getMessage().sdp = localDescription.description;
        String jsonData = baseMessage.toJson();
        webSocketClient.send(jsonData);
    }

    //发送应答,告诉对方自己的SDP
    @Override
    public void sendAnswer(String socketId, SessionDescription localDescription) {
        BaseMessage<NegotiationMessage,Object> baseMessage = new BaseMessage<NegotiationMessage, Object>() {};
        baseMessage.getMessage().userId = socketId;
        baseMessage.getMessage().sdp = localDescription.description;
        String jsonData = baseMessage.toJson();
        webSocketClient.send(jsonData);
    }

    //向房间的其他成员发送自己的iceCandidate信息
    @Override
    public void sendIceCandidate(String socketId, IceCandidate iceCandidate) {
        BaseMessage<NegotiationMessage,Object> baseMessage = new BaseMessage<NegotiationMessage, Object>() {};
        baseMessage.getMessage().userId = socketId;
        baseMessage.getMessage().sdpMid = iceCandidate.sdpMid;
        baseMessage.getMessage().sdpMLineIndex = iceCandidate.sdpMLineIndex;
        baseMessage.getMessage().sdp = iceCandidate.sdp;
        String jsonData = baseMessage.toJson();
        webSocketClient.send(jsonData);
    }
    /**============================通过webSocket发送消息========================*/
}
