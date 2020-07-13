package com.example.wywebrtc.webrtcsource;

import android.util.Log;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.example.wywebrtc.bean.Event;
import com.example.wywebrtc.bean.Message;
import com.example.wywebrtc.type.MessageType;
import com.example.wywebrtc.webrtcinderface.SocketInterface;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * P2P通信中，信令交换的驱动者，直接与信令服务器打交道，包括信令的收取和发送，
 * 同时还可以用于信令外的信息交换，如发起聊天请求，聊天文字的收发等
 */
public class WebSocket implements SocketInterface {

    private static final String TAG = "WebSocket";

    private WebRtcManager manager;
    private WebSocketClient webSocketClient;

    public WebSocket(){
        this.manager = WebRtcManager.getInstance();
    }

    //将服务器应答消息进行分发处理
    //数据格式：{"eventName":"_peers","data":{"connections":["0edd0098-d5cc-48ec-870d-52d8e9de27b0"],"you":"2ebc378d-c99c-49c3-9183-33c31e62ea12"}}
    //数据的格式好像都是固定死的，但是任然可以定义自己的数据装到如data标签下进行传输
    private void handleMessage(String message){
        Map map = JSON.parseObject(message,Map.class);
        String eventType = (String) map.get("eventName");
        //当请求加入房间时的响应，返回房间内所有人的ID
        if ("_peers".equals(eventType)){
            handleJoinRoom(map);
        }
        //向对方发送ice_candidate offer 后对方也会响应他的ice_candidate
        else if ("_ice_candidate".equals(eventType)){
            handleRemoteCandidate(map);
        }
        //向对方发送sdp offer 后对方也会响应他的sdp
        else if("_answer".equals(eventType)){
            handlerAnswer(map);
        }
        //当自己已经在房间，有新人加入房间，返回其ID
        else if("_new_peer".equals(eventType)){
            handlerRemoteInRoom(map);
        }
        //有人离开房间
        else if("_remove_peer".equals(eventType)){
            handlerRemoteOutRoom(map);
        }
        //有人发起了offer，即发送他的SDP信息
        else if("_offer".equals(eventType)){
            handlerOffer(map);
        }
    }

    /**==================================处理webSocket消息========================*/
    //有人发起了offer,将对方的SDP设置到Peer中，并返回自己的SDP
    private void handlerOffer(Map map){
        Map data = (Map) map.get("data");
        Map sdpMap;
        if (data != null){
            sdpMap = (Map) data.get("sdp");
            String socketId = (String) data.get("socketId");
            String sdp = (String) sdpMap.get("sdp");
            manager.onReceiveOffer(socketId,sdp);
        }
    }

    //有人离开房间
    private void handlerRemoteOutRoom(Map map){
        Map data = (Map) map.get("data");
        String socketId;
        if (data != null){
            socketId = (String) data.get("socketId");
            manager.remoteOutRoom(socketId);

        }
    }

    //当自己已经在房间，有新人加入房间，则创建一个Peer
    private void handlerRemoteInRoom(Map map){
        Map data = (Map) map.get("data");
        String socketId;
        if (data != null){
            socketId = (String) data.get("socketId");
            manager.remoteJoinToRoom(socketId);
        }
    }

    //拿到对方的sdp后设置到连接对象Peer中
    private void handlerAnswer(Map map){
        Map data = (Map) map.get("data");
        Map sdpMap;
        if (data != null){
            sdpMap = (Map) data.get("sdp");
            String socketId = (String) data.get("socketId");
            String sdp = (String) sdpMap.get("sdp");
            manager.onReceiveAnswer(socketId,sdp);
        }
    }

    //拿到对方的ice_candidate后设置到连接对象Peer中
    private void handleRemoteCandidate(Map map){
        Map data = (Map) map.get("data");
        String socketId;
        if (data != null){
            socketId = (String) data.get("socketId");
            String sdpMid = (String) data.get("id");
            sdpMid = (sdpMid == null) ? "video" : sdpMid;
            int sdpMLineIndex = (int)Double.parseDouble(String.valueOf(data.get("label")));
            String candidate = (String) data.get("candidate");
            IceCandidate iceCandidate = new IceCandidate(sdpMid,sdpMLineIndex,candidate);
            manager.onRemoteCandidate(socketId,iceCandidate);
        }
    }

    //申请加入房间后，服务器返回房间所有人的id和自己的id
    //根据返回的id分别建立P2P通信
    private void handleJoinRoom(Map map){
        Map data = (Map) map.get("data");
        JSONArray jsonArray;
        if (data != null){
            jsonArray = (JSONArray) data.get("connections");
            String json = JSONObject.toJSONString(jsonArray, SerializerFeature.WriteClassName);
            ArrayList<String> connections = (ArrayList<String>) JSONObject.parseArray(json,String.class);
            String selfId = (String) data.get("you");
            //通过PeerConnectionManager建立p2p
            manager.createConnection(connections,selfId);
        }
    }
    /**==================================处理webSocket消息========================*/


    /**============================通过webSocket发送消息========================*/
    //发起加入房间请求
    @Override
    public void joinRoom(String roomId){
        HashMap<String,Object> map = new HashMap<>();
        HashMap<String,String> child = new HashMap<>();
        child.put("room",roomId);     //房间号
        map.put("eventName","__join");//事件类型为加入房间
        map.put("data",child);
        JSONObject jsonObject = new JSONObject(map);
        String message = jsonObject.toString();
        webSocketClient.send(message);
    }

    //向房间的其他成员发送自己的SDP信息
    @Override
    public void sendOffer(String socketId, SessionDescription localDescription) {
        HashMap<String,Object> map = new HashMap<>();
        HashMap<String,Object> childA = new HashMap<>();
        HashMap<String,Object> childB = new HashMap<>();

        childA.put("type","offer");
        childA.put("sdp",localDescription.description);

        childB.put("socketId",socketId);
        childB.put("sdp",childA);

        map.put("eventName","__offer");
        map.put("data",childB);

        JSONObject jsonObject = new JSONObject(map);
        String message = jsonObject.toJSONString();
        webSocketClient.send(message);
    }
    //发送应答,告诉对方自己的SDP
    @Override
    public void sendAnswer(String socketId, SessionDescription localDescription) {
        HashMap<String,Object> map = new HashMap<>();
        HashMap<String,Object> childA = new HashMap<>();
        HashMap<String,Object> childB = new HashMap<>();

        childA.put("type","answer");
        childA.put("sdp",localDescription.description);

        childB.put("socketId",socketId);
        childB.put("sdp",childA);

        map.put("eventName","__answer");
        map.put("data",childB);

        JSONObject jsonObject = new JSONObject(map);
        String message = jsonObject.toJSONString();
        webSocketClient.send(message);
    }
    //向房间的其他成员发送自己的iceCandidate信息
    @Override
    public void sendIceCandidate(String socketId, IceCandidate iceCandidate) {
        HashMap<String,Object> map = new HashMap<>();
        HashMap<String,Object> childA = new HashMap<>();

        childA.put("id",iceCandidate.sdpMid);
        childA.put("label",iceCandidate.sdpMLineIndex);
        childA.put("candidate",iceCandidate.sdp);
        childA.put("socketId",socketId);

        map.put("eventName","__ice_candidate");
        map.put("data",childA);

        JSONObject jsonObject = new JSONObject(map);
        String message = jsonObject.toString();

        webSocketClient.send(message);
    }
    /**============================通过webSocket发送消息========================*/

    //调用webSocketClient.close()后onClose方法会被回调
    //而远端的onMessage方法被回调，并且类型是_remove_peer
    //测试中发现，p2p没有建立的时候调用webSocketClient.close()远端无任何回调响应
    @Override
    public void close() {
        if (webSocketClient != null){
            webSocketClient.close();
            webSocketClient = null;
        }
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
        }
    }

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
        manager.socketCallback(message);
    }

    //自己加入房间后得到的服务器响应，服务器会返回房间信息
    private void comingSelf(Event event){

    }

    //有人加入了房间
    private void someoneJoin(Event event){

    }

    //有人离开房间
    private void someoneLeave(Event event){

    }

    //有人发起了媒体协商,即收到了别人主动给自己发送的媒体协商数据
    private void someoneSendOffer(Event event){

    }

    //有人响应了媒体协商，即自己主动给别人发送媒体协商数据后得到了对方的响应
    private void someoneAnswerOffer(Event event){

    }

    //网络协商交换，即收别人的网络协商数据
    //和Offer不同的是candidate不需要Answer,双方把自己重ice服务器获取的网络数据发送给对方就行了
    //他们之间的数据发送是异步的，即不需要等到A收到B的数据才给B响应，并且网络协商数据会交换多次
    //而媒体协商数据Offer交换一次就够了
    private void candidateSwitch(Event event){

    }
}
