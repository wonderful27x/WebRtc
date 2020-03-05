package com.example.wywebrtc.webrtcsource;

import android.util.Log;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.example.wywebrtc.bean.Message;
import com.example.wywebrtc.webrtcinderface.MessageType;
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
                Message message = new Message();
                message.setMessageType(MessageType.SOCKET_OPEN);
                manager.socketCallback(message);
                Log.d(TAG,"onOpen");
            }
            //消息推送
            @Override
            public void onMessage(String message) {
                handleMessage(message);
                Log.d(TAG,message);
            }
            //关闭
            @Override
            public void onClose(int code, String reason, boolean remote) {
                Message message = new Message();
                message.setMessageType(MessageType.SOCKET_CLOSE);
                message.setMessage(reason);
                manager.socketCallback(message);
                Log.d(TAG,"onClose");
            }
            //连接失败
            @Override
            public void onError(Exception ex) {
                Message message = new Message();
                message.setMessageType(MessageType.SOCKET_ERROR);
                message.setMessage(ex.getMessage());
                manager.socketCallback(message);
                Log.e(TAG,ex.getMessage());
            }
        };
        if (socketUri.startsWith("wss")){
            try {
                SSLContext context = SSLContext.getInstance("TLS");
                TrustManager[] trustManager = new TrustManager[]{new TrustManagerTest()};
                context.init(null,trustManager,new SecureRandom());
                SSLSocketFactory factory = context.getSocketFactory();
                if (factory != null){
                    webSocketClient.setSocket(factory.createSocket());//创建一个具有加密证书的socket
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        webSocketClient.connect();//建立连接
    }

    //实现一个接口什么都不做，相当于忽略整数
    class TrustManagerTest implements X509TrustManager{

        @Override
        public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {

        }

        @Override
        public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {

        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }
}
