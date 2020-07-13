package com.example.wywebrtc.webrtcsource;

import android.content.Context;
import android.media.AudioManager;

import com.example.wywebrtc.type.RoomType;
import com.example.wywebrtc.webrtcinderface.ConnectionInterface;
import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.Camera1Enumerator;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.CameraVideoCapturer;
import org.webrtc.DataChannel;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RtpReceiver;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoDecoderFactory;
import org.webrtc.VideoEncoderFactory;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;
import org.webrtc.audio.JavaAudioDeviceModule;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * WebRTC应用可以使用ICE框架来克服实际应用中复杂的网络问题。
 * 要使用ICE的话，你的应用必须如下所述的在RTCPeerConnection中传递ICE服务器的URL。
 *
 * ICE试图找到连接端点的最佳路径。它并行的查找所有可能性，然后选择最有效率的一项。
 * ICE首先用从设备操作系统和网卡上获取的主机地址来尝试连接，如果失败了(比如设备处于NAT之后)，
 * ICE会使用从STUN 服务器获取到的外部地址，如果仍然失败，则交由TURN中继服务器来连接。
 *
 * 换句话说:
 * STUN服务器用于获取设备的外部网络地址
 * TURN服务器是在点对点失败后用于通信中继。
 *
 * 每一个TURN服务器都支持STUN，因为TURN就是在STUN服务器中内建了一个中继功能。
 */

/**
 * 本来想写一篇博客的，但是因为懒就把我的一些理解和收获写进注释里面，请结合代码理解
 * 在P2P通信中遇到了很多困难，通过网上的资料和实际测试得出一些结论，并将连接建立的过程描述如下：
 * 首先我们要介绍两个很重要的概念，ICE服务器和信令服务器
 * ICE服务器：
 * stun其实是一种内外穿透协议，允许位于NAT路由后的设备拿到其通往互联网的公网地址，所谓的stun服务器我觉得就是stun协议的具体实现的应用程序，
 * turn是一个用于数据转发的中继服务器，在stun穿透失败时使用
 * ICE是一个框架，整合了stun和tun，ICE服务器应该就是实现这个框架的应用程序
 * 信令服务器：
 * 信令服务器主要在P2P建立连接前作信息交换，当然连接成功后也可以继续用作其他信息交流
 * 信令主要有两类：
 * （1）媒体协商SDP
 *  SDP是双方预定的媒体信息，如Peer-A端可支持VP8、H264多种编码格式，而Peer-B端支持VP9、H264，要保证二端都正确的编解码，最简单的办法就是取它们的交集H264，
 * （2）网络协商candidate
 *  两个专网想通信就必须知道对方通往互联网的公网IP，这个信息通过ICE服务器进行内网穿透实现，candidate大概描述的就是这个信息
 *  注意：在课堂上老师说SDP是一个总的网络路径，candidate是路径的一段，将SDP和candidate混为一谈，这种解释是错误的
 *  在测试中分别打印了SDP和candidate，发现SDP中确实是与媒体相关的信息，并没有包含具体的ip地址
 *  然而即便是官方对SDP和candidate的描述也容易让人产生误解，实际上SDP作为一种会话描述，是信息交换双方约定的统一格式，
 *  无论是媒体协商还是网络协商他们都将信息封装成了SDP会话描述，然后再进行传输
 *
 * 将连接的建立分两种情况：
 * 情况一：房间有人，自己加入房间，我们把这种情况叫做situation A,简称SA
 * 情况二：自己已在房间，有新人加入房间，我们把这种情况叫做situation B,简称SB
 *
 * 下面我们将这两种情况分开来讨论，但是请注意，他们是有很密切的联系的，因为SA的另一端就是SB，所以在理解的时候应当统一起来
 * SA：
 * （1）向服务器发起加入房间的请求，peers被响应并返回房间内所有人的ID
 * （2）做一些必要的数据初始化，即音视频流MediaStream的创建和PeerConnectionFactory这个工厂的创建
 * （3）为房间内每个ID创建PeerConnection封装类Peer，并创建PeerConnection（对于单方面每个P2P连接对应一个PeerConnection），
 *  特别注意：在PeerConnection的创建中设置了ICE服务器stun和turn的地址
 * （4）将音视频流MediaStream设置到PeerConnection中
 * （5）调用createOffer创建SDP会话描述
 * （6）createOffer设置成功后onCreateSuccess被调用，调用setLocalDescription设置SDP内容，
 *  在实测中发现setLocalDescription调用后会触发内网穿透，即请求ICE服务器开始打洞，并且这个过程和接下来的SDP信息交换是异步执行的，
 *  因此在下面我们用相同的序号来表示这个异步
 * （7）setLocalDescription成功后onSetSuccess被调用，通过webSocket将自己的SDP信息发送给对等方
 * （8）对方响应_answer,即通过webSocket将自己的SDP回传
 *  (9)调用setRemoteDescription，将对方SDP设置到PeerConnection中，至此一个SDP交换完成
 * （7）onIceCandidate被调用，得到了自己到ICE服务器的一段路径，并通过webSocket将自己的candidate发送给对等方
 * （8）对方响应_ice_candidate，即通过webSocket将自己的candidate回传，
 *  (9)调用addIceCandidate，将对方candidate设置到PeerConnection中
 *  和SDP不同的是，candidate的交换，即（7）（8）（9）会被重复多次直到完整的candidate交换完成，这取决于到ICE服务器的路径
 *  课上说的是onIceCandidate的调用次数等于路由节点数
 *  当上述过程都完成后P2P通信连接就建立成功了，就可以进行音视频会话了
 *
 *  SB：
 * （1）一个新人加入房间，webSocket端_new_peer被响应并返回新人ID，根据ID创建PeerConnection封装类Peer
 * （2）接着webSocket端_offer被响应
 * （4）调用setRemoteDescription将对方SDP设置到PeerConnection中
 * （5）setRemoteDescription设置成功后onSetSuccess被调用，调用createAnswer创建SDP会话描述
 * （6）createAnswer设置成功后onCreateSuccess被调用，调用setLocalDescription设置SDP内容，setLocalDescription触发内网穿透
 * （7）setLocalDescription设置成功后onSetSuccess再次被调用，响应对方的SDP offer，即通过webSocket将自己的SDP信息发送给对方
 *  一个SDP交换完成
 * （5）webSocket端_ice_candidate被响应
 * （6）调用addIceCandidate将对方candidate设置到PeerConnection
 * （7）onIceCandidate被调用，得到了自己到ICE服务器的一段路径，并通过webSocket将自己的candidate发送给对等方
 *  和上面相似，（5）（6）（7）重复多次直到完整的candidate交换完成，当上述过程都完成后P2P通信连接就建立成功了，就可以进行音视频会话了
 *
 *  有趣的发现：
 *  如果通信双方在同一个网络内，他们可以直接通信，测试发现即使将stun和turn的地址故意写错他们仍然能够正常通信，
 *  猜测这时并没有走stun和turn
 */

/**
 * P2P通信中最重要的连接管理类，他将与WebSocket共同完成连接的建立
 */
public class PeerConnectionManager implements ConnectionInterface{
    private static PeerConnectionManager perConnectionManager = null;
    private WebRtcManager manager;                       //中转类
    private RoomType roomType;                           //房间类型
    private String selfId;                               //自己的ID
    private ArrayList<String> socketIds;                 //房间内其他人的id
    private Map<String,Peer> peerConnectionMap;          //会议室所有的P2P连接
    private ExecutorService executorService;             //线程池
    private PeerConnectionFactory peerConnectionFactory; //peerConnection工长
    private List<PeerConnection.IceServer> iceServers;   //ICE服务器结合，服务器有可能有多个
    private MediaStream localStream;                     //本地音视频流
    private AudioSource audioSource;                     //音频源
    private VideoSource videoSource;                     //视频源
    private AudioTrack audioTrack;                       //音轨
    private VideoTrack videoTrack;                       //视频轨
    private VideoCapturer videoCapturer;                 //视频捕获器
    private SurfaceTextureHelper surfaceTextureHelper;   //Surface帮助类，用于渲染
    private enum Role{caller,receiver}                   //角色定义，如果是请求加入房间则是caller，如果别人加入房间则是receiver
    private Role role;
    private AudioManager audioManager;

    private static final String AUDIO_ECHO_CANCELLATION_CONSTRAINT = "googEchoCancellation";//回音消除
    private static final String AUDIO_NOISE_SUPPRESSION_CONSTRAINT = "googNoiseSuppression";//噪声抑制
    private static final String AUDIO_AUTO_GAIN_CONTROL_CONSTRAINT = "googAutoGainControl"; //自动增益
    private static final String AUDIO_HIGH_PASS_FILTER_CONSTRAINT = "googHighpassFilter";   //高通滤波

    private static final String LOCAL_STREAM_ID = "ARDAMS";                      //本地流ID
    private static final String AUDIO_TRACK_ID = "ARDAMS-AUDIO";                 //音频轨ID
    private static final String VIDEO_TRACK_ID = "ARDAMS-VIDEO";                 //视频轨ID
    private static final String SURFACE_THREAD_NAME = "surfaceCaptureThread";    //线程名

    private PeerConnectionManager(RoomType roomType){
        init(roomType);
    }

    private void init(RoomType roomType){
        this.manager = WebRtcManager.getInstance();
        this.roomType = roomType;
        socketIds = new ArrayList<>();
        executorService = Executors.newSingleThreadExecutor();
        peerConnectionMap = new HashMap<>();
        iceServers = new ArrayList<>();

        //创建stun服务器信息
        PeerConnection.IceServer stun = PeerConnection.IceServer.
                builder(WebRtcConfig.STUN_URI)
                .setUsername(WebRtcConfig.STUN_USER_NAME)
                .setPassword(WebRtcConfig.STUN_PASSWORD)
                .createIceServer();
        //创建turn服务器信息
        PeerConnection.IceServer turn = PeerConnection.IceServer.
                builder(WebRtcConfig.TURN_URI)
                .setUsername(WebRtcConfig.TURN_USER_NAME)
                .setPassword(WebRtcConfig.TURN_PASSWORD)
                .createIceServer();
        iceServers.add(stun);
        iceServers.add(turn);
    }

    public static PeerConnectionManager getInstance(RoomType roomType){
        if (perConnectionManager == null){
            synchronized (PeerConnectionManager.class){
                if (perConnectionManager == null){
                    perConnectionManager = new PeerConnectionManager(roomType);
                }
            }
        }else {
            synchronized (PeerConnectionManager.class){
                perConnectionManager.init(roomType);
            }
        }
        return perConnectionManager;
    }

    public static PeerConnectionManager getInstance(){
        if (perConnectionManager == null || perConnectionManager.manager == null){
            throw new RuntimeException("please use getInstance which has parameter first before use");
        }
        return perConnectionManager;
    }

    /**==================================ConnectionInterface===========================*/
    //远端有人加入房间
    @Override
    public void remoteJoinToRoom(String socketId) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                if (localStream == null){
                    createLocalStream();
                }
                Peer peer = new Peer(socketId);
                peer.peerConnection.addStream(localStream);
                socketIds.add(socketId);
                peerConnectionMap.put(socketId,peer);
            }
        });
    }
    //有人离开房间
    @Override
    public void remoteOutRoom(String socketId) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                closePeerConnection(socketId);
            }
        });
    }
    //有人发起了SDP Offer
    @Override
    public void onReceiveOffer(String socketId, String sdp) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                role = Role.receiver;
                Peer peer = peerConnectionMap.get(socketId);
                if (peer != null){
                    SessionDescription sessionDescription = new SessionDescription(SessionDescription.Type.OFFER,sdp);
                    peer.peerConnection.setRemoteDescription(peer,sessionDescription);
                }
            }
        });
    }
    //有人回应了SDP
    @Override
    public void onReceiveAnswer(String socketId, String sdp) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                Peer peer = peerConnectionMap.get(socketId);
                SessionDescription sessionDescription = new SessionDescription(SessionDescription.Type.ANSWER,sdp);
                if (peer != null){
                    peer.peerConnection.setRemoteDescription(peer,sessionDescription);
                }
            }
        });
    }
    //有人回应了Candidate
    @Override
    public void onRemoteCandidate(String socketId, IceCandidate iceCandidate) {
        Peer peer = peerConnectionMap.get(socketId);
        if (peer != null){
            peer.peerConnection.addIceCandidate(iceCandidate);
        }
    }

    //开始初始化数据并创建P2P连接
    @Override
    public void createConnection(ArrayList<String> connections, String socketId) {
        socketIds.addAll(connections);
        this.selfId = socketId;
        //开启一个线程来建立PeerConnection，由于建立过程复杂耗时，所以需要开线程
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                if (peerConnectionFactory == null){
                    peerConnectionFactory = createPeerConnectionFactory();//创建工厂
                }
                if (localStream == null){
                    createLocalStream();     //创建本地流
                }
                createPeerConnections();     //创建p2p连接
                addStream();                 //为每个连接添加自己的音视频流，准备推流
                createOffers();              //创建SDP会话描述
            }
        });
    }

    @Override
    public void switchMute(boolean mute) {
        if (audioTrack != null){
            audioTrack.setEnabled(mute);
        }
    }

    @Override
    public void switchCamera() {
        if (videoCapturer == null)return;
        if (videoCapturer instanceof CameraVideoCapturer){
            ((CameraVideoCapturer)videoCapturer).switchCamera(null);
        }
    }

    @Override
    public void switchHandsfree(boolean handsfree) {
        if (audioManager == null){
            audioManager = (AudioManager) manager.getContext().getSystemService(Context.AUDIO_SERVICE);
        }
        audioManager.setSpeakerphoneOn(handsfree);
    }

    @Override
    public void powerCamera(boolean enable) {
        if (videoTrack != null){
            videoTrack.setEnabled(enable);
        }
    }
    //挂断,这会请求网络，是个耗时操作
    @Override
    public void hangUp() {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                ArrayList<String> ids = (ArrayList<String>)socketIds.clone();
                for (String id:ids){
                    closePeerConnection(id);
                }
                //关闭音频
                if (audioSource != null){
                    audioSource.dispose();
                    audioSource = null;
                }
                //关闭视频
                if (videoSource != null){
                    videoSource.dispose();
                    videoSource = null;
                }
                //关闭摄像头预览
                if (videoCapturer != null){
                    try {
                        videoCapturer.stopCapture();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    videoCapturer.dispose();
                    videoCapturer = null;
                }
                //关闭surface帮助类
                if(surfaceTextureHelper != null){
                    surfaceTextureHelper.dispose();
                    surfaceTextureHelper = null;
                }
                //关闭工厂
                if (peerConnectionFactory != null){
                    peerConnectionFactory.dispose();
                    peerConnectionFactory = null;
                }
                //清空列表
                if (iceServers != null){
                    iceServers.clear();
                    iceServers = null;
                }
                localStream = null;
                audioManager = null;
                //关闭socket
                manager.close();
            }
        });
    }

    private void closePeerConnection(String socketId){
        Peer peer = peerConnectionMap.get(socketId);
        if (peer != null){
            peer.peerConnection.close();
            peerConnectionMap.remove(socketId);
            socketIds.remove(socketId);
            manager.closeWindow(socketId);
        }
    }

    @Override
    public void chatRequest(int mediaType, String roomId) {

    }

    @Override
    public void joinRoom() {

    }

    //获取连接数，这里应该处理线程同步问题
    public int getConnectNum(){
        return peerConnectionMap.size();
    }
    /**==================================ConnectionInterface===========================*/

    //先创建PeerConnection工厂，用于创建PeerConnection
    private PeerConnectionFactory createPeerConnectionFactory(){
        VideoEncoderFactory videoEncoderFactory = null;
        VideoDecoderFactory videoDecoderFactory = null;
        if (mediaType != MediaType.SINGLE_AUDIO){
            //创建视频编码器工厂并开启v8和h264编码，webrtc会自动选择最优的，当然也可以只开启其中一个
            videoEncoderFactory = new DefaultVideoEncoderFactory(manager.getEglBase().getEglBaseContext(),true,true);
            //创建视频解密器工厂
            videoDecoderFactory = new DefaultVideoDecoderFactory(manager.getEglBase().getEglBaseContext());
        }
        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
        //将其他参数设置成默认值
        PeerConnectionFactory.initialize(PeerConnectionFactory.InitializationOptions.builder(manager.getContext()).createInitializationOptions());
        return PeerConnectionFactory.builder()
                .setOptions(options)          //设置网络类型，这是使用options自动判断
                .setAudioDeviceModule(JavaAudioDeviceModule.builder(manager.getContext()).createAudioDeviceModule()) //设置音频类型
                .setVideoEncoderFactory(videoEncoderFactory)//设置视频编码工厂
                .setVideoDecoderFactory(videoDecoderFactory)//设置视频解码工厂
                .createPeerConnectionFactory();
    }

    //创建本地音视频流
    private void createLocalStream(){
        //调用工厂方法创建流，其中label标签必须以ARDAMS开头，可以有后缀
        //localStream是音视频的承载，后面需要将音视频轨设置到其中
        localStream = peerConnectionFactory.createLocalMediaStream(LOCAL_STREAM_ID);

        //音频
        audioSource = peerConnectionFactory.createAudioSource(createMediaConstraints());    //创建音频源,并设置约束属性
        audioTrack = peerConnectionFactory.createAudioTrack(AUDIO_TRACK_ID,audioSource);    //采集音频
        localStream.addTrack(audioTrack);                                                   //将音轨设置到localStream里

        //视频
        if (mediaType == MediaType.SINGLE_AUDIO)return;
        videoCapturer = createVideoCapturer();                                              //创建videoCapturer
        videoSource = peerConnectionFactory.createVideoSource(videoCapturer.isScreencast());//创建视频源

        //进行摄像头预览的设置，因为聊天室也需要显示自己的图像，这是借助了SurfaceTextureHelper通过openGL进行渲染，聊天室内每一个窗口的渲染会单独开一个线程
        surfaceTextureHelper = SurfaceTextureHelper.create(SURFACE_THREAD_NAME,manager.getEglBase().getEglBaseContext());
        videoCapturer.initialize(surfaceTextureHelper,manager.getContext(),videoSource.getCapturerObserver());//初始化videoCapturer
        videoCapturer.startCapture(WebRtcConfig.CAPTURE_WIDTH,WebRtcConfig.CAPTURE_HEIGHT,WebRtcConfig.CAPTURE_FPS);//开始采集 i:宽，i1:高，i2:帧率

        videoTrack = peerConnectionFactory.createVideoTrack(VIDEO_TRACK_ID,videoSource);    //创建视频轨
        localStream.addTrack(videoTrack);//将视频轨设置到localStream中

        manager.setLocalStream(localStream,selfId);//刷新显示自己的窗口
    }

    //创建p2p连接，注意这里的逻辑：
    //在开始加入房间的时候服务器会返回房间内所有人的id，
    //因为自己需要和每个人都要通信，因此必须和每个人都建立一个P2P连接，
    //通过id循环与房间内的每个人建立P2P连接
    private void createPeerConnections(){
        for (String id:socketIds){
            Peer peer = new Peer(id);
            peerConnectionMap.put(id,peer);
        }
    }

    //将本地流加入peerConnection中
    private void addStream(){
        for (Map.Entry<String,Peer> entry:peerConnectionMap.entrySet()){
            if (localStream == null){
                createLocalStream();
            }
            entry.getValue().peerConnection.addStream(localStream);
        }
    }

    //给每一个人发送邀请,附带自己流媒体信息，这里其实只是创建了一个SDP会话描述而已，
    //创建成功后onCreateSuccess被调用，接着设置SDP，最后才将这个SDP信息发送给对等方
    private void createOffers(){
        for (Map.Entry<String,Peer> entry:peerConnectionMap.entrySet()){
            role = Role.caller;
            Peer peer = entry.getValue();
            MediaConstraints mediaConstraints;
            if (mediaType == MediaType.SINGLE_AUDIO){
                mediaConstraints = createMediaConstraintsForOfferAnswer(true,false);
            }else {
                mediaConstraints = createMediaConstraintsForOfferAnswer(true,true);
            }
            peer.peerConnection.createOffer(peer,mediaConstraints);
        }
    }

    //在发起请求之前设置一些约束信息
    //可以控制是否输入音频及视频
    //MediaConstraints在设置到offer或answer中时的意思是“我需不需要音频或视频”
    private MediaConstraints createMediaConstraintsForOfferAnswer(boolean enableAudio,boolean enableVideo){
        MediaConstraints mediaConstraints = new MediaConstraints();
        List<MediaConstraints.KeyValuePair> keyValuePairList = new ArrayList<>();
        //控制音频传输
        keyValuePairList.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio",String.valueOf(enableAudio)));
        //控制视频传输
        keyValuePairList.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo",String.valueOf(enableVideo)));
        mediaConstraints.mandatory.addAll(keyValuePairList);
        return mediaConstraints;
    }

    //创建MediaConstraints，用于设置噪声抑制、回声消除、自动增益、高通滤波等各种约束
    private MediaConstraints createMediaConstraints(){
        MediaConstraints audioConstraints = new MediaConstraints();
        audioConstraints.mandatory.add(
                new MediaConstraints.KeyValuePair(AUDIO_ECHO_CANCELLATION_CONSTRAINT,"true"));
        audioConstraints.mandatory.add(
                new MediaConstraints.KeyValuePair(AUDIO_NOISE_SUPPRESSION_CONSTRAINT,"true"));
        audioConstraints.mandatory.add(
                new MediaConstraints.KeyValuePair(AUDIO_AUTO_GAIN_CONTROL_CONSTRAINT,"false"));
        audioConstraints.mandatory.add(
                new MediaConstraints.KeyValuePair(AUDIO_HIGH_PASS_FILTER_CONSTRAINT,"true"));
        return audioConstraints;
    }

    //创建videoCapturer，设备有可能有camera1和camera2,每个camera有可能有前置和后置摄像头
    private VideoCapturer createVideoCapturer(){
        VideoCapturer videoCapturer;
        CameraEnumerator cameraEnumerator;
        //如果支持则默认使用camera2
        if (camera2Support()){
            cameraEnumerator = new Camera2Enumerator(manager.getContext());
            videoCapturer = createCameraCapturer(cameraEnumerator);
        }else {
            cameraEnumerator = new Camera1Enumerator(true);
            videoCapturer = createCameraCapturer(cameraEnumerator);
        }
        return videoCapturer;
    }

    //根据CameraEnumerator类型创建相应VideoCapturer
    private VideoCapturer createCameraCapturer(CameraEnumerator cameraEnumerator){
        VideoCapturer capturer = null;
        String[] deviceNames = cameraEnumerator.getDeviceNames();

        for (String name:deviceNames){
            //默认优先使用前置摄像头
            if (cameraEnumerator.isFrontFacing(name)){
                capturer = cameraEnumerator.createCapturer(name,null);
                if (capturer != null){
                    return capturer;
                }
            }
        }

        for (String name:deviceNames){
            //否则使用后置摄像头
            if (!cameraEnumerator.isFrontFacing(name)){
                capturer = cameraEnumerator.createCapturer(name,null);
                if (capturer != null){
                    return capturer;
                }
            }
        }

        return null;
    }

    //判断是否支持Camera2
    private boolean camera2Support(){
        return Camera2Enumerator.isSupported(manager.getContext());
    }

    //P2P连接封装类
    private class Peer implements PeerConnection.Observer, SdpObserver {
        private PeerConnection peerConnection;//跟远端用户的一个连接
        private String socketId;              //远端用户的id

        public Peer(String socketId){
            this.socketId = socketId;
            //创建ice服务器信息配置,即stun和turn，从这里我们可以看出，p2p连接的建立先进行了内网穿透
            PeerConnection.RTCConfiguration configuration = new PeerConnection.RTCConfiguration(iceServers);
            //通过工厂创建连接,并设置回调
            peerConnection = peerConnectionFactory.createPeerConnection(configuration,this);
        }

        /**==================================PeerConnection.Observer==================================*/
        /**
         * 实现PeerConnectionObserver接口，这是内网穿透中重要的信息交换，其中就有网络地址信息Candidate
         */
        //网络发生改变是调用，如4切到wifi
        @Override
        public void onSignalingChange(PeerConnection.SignalingState signalingState) {

        }
        //连接上ICE服务器
        @Override
        public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {

        }

        @Override
        public void onIceConnectionReceivingChange(boolean b) {

        }

        @Override
        public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {

        }

        //重要的网络地址回调，调用时机有两种情况：
        //(1).第一次连接到ICE服务器的时候，调用次数=路由节点数
        //(2).（其他人加入房间）连接ICE服务器时，调用次数=路由节点数
        //iceCandidate对象封装了网络节点的信息
        /**
         *  在实测中发现setLocalDescription调用后会触发内网穿透，得到关键网络节点信息后回调这个方法，
         *  iceCandidate封装了路由ip地址，这个方法会被多次调用，调用次数等于路由节点数
         *  在得到自己的iceCandidate后通过socket告知对方，而对方收到信息后同样会返回自己的iceCandidate，
         *  多次重复最后完成iceCandidate交换
         * @param iceCandidate
         */
        @Override
        public void onIceCandidate(IceCandidate iceCandidate) {
            manager.sendIceCandidate(socketId,iceCandidate);
        }

        @Override
        public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {

        }

        //p2p连接建立成功后回调，mediaStream封装了音视频流，这时就可以回到ui层进行对方音视频的播放和显示了
        @Override
        public void onAddStream(MediaStream mediaStream) {
            manager.addRemoteStream(mediaStream,socketId);
        }

        @Override
        public void onRemoveStream(MediaStream mediaStream) {
            //TODO
            manager.closeWindow(socketId);
        }

        @Override
        public void onDataChannel(DataChannel dataChannel) {

        }

        @Override
        public void onRenegotiationNeeded() {

        }

        @Override
        public void onAddTrack(RtpReceiver rtpReceiver, MediaStream[] mediaStreams) {

        }

        /**==================================SdpObserver==================================*/

        /**
         * 新的理解：
         * SA/SB:createOffer设置成功后onCreateSuccess被调用，调用setLocalDescription设置SDP内容，
         *  在实测中发现setLocalDescription调用后会触发内网穿透，即请求ICE服务器开始打洞，
         *  并且这个过程和接下来的SDP信息交换是异步执行的，
         *  在setLocalDescription()方法被调用前RTCPeerConnection都不会开始收集candidates,这是JSEP IRTF draft中要求的
         * @param sessionDescription 其中的description描述了媒体协商数据
         */
        @Override
        public void onCreateSuccess(SessionDescription sessionDescription) {
            //设置本地sdp，如果设置成功则回调onSetSuccess
            peerConnection.setLocalDescription(this,sessionDescription);
        }

        /**
         * SA:
         * （7）setLocalDescription成功后onSetSuccess被调用，通过webSocket将自己的SDP信息发送给对等方
         * SB:
         * （5）setRemoteDescription设置成功后onSetSuccess被调用，调用createAnswer创建SDP会话描述
         * （6）createAnswer设置成功后onCreateSuccess被调用，调用setLocalDescription设置SDP内容，setLocalDescription触发内网穿透
         * （7）setLocalDescription设置成功后onSetSuccess再次被调用，响应对方的SDP offer，即通过webSocket将自己的SDP信息发送给对方
         *  一个SDP交换完成
         */
        @Override
        public void onSetSuccess() {
            //TODO 通过socket交换sdp,这里目前并没有很好的理解，先实现功能，后期再研究并注释
            if (peerConnection.signalingState() == PeerConnection.SignalingState.HAVE_REMOTE_OFFER){
                MediaConstraints mediaConstraints;
                if (mediaType == MediaType.SINGLE_AUDIO){
                    mediaConstraints = createMediaConstraintsForOfferAnswer(true,false);
                }else {
                    mediaConstraints = createMediaConstraintsForOfferAnswer(true,true);
                }
                peerConnection.createAnswer(this,mediaConstraints);
            }else if (peerConnection.signalingState() == PeerConnection.SignalingState.HAVE_LOCAL_OFFER){
                //接收者，这个方法在实测时一直没有被触发，不知道为什么，注释后也能正常运行
                if (role == Role.receiver){
                    manager.sendAnswer(socketId,peerConnection.getLocalDescription());
                }
                //发送者
                else if (role == Role.caller){
                    manager.sendOffer(socketId,peerConnection.getLocalDescription());
                }
            }
            else if (peerConnection.signalingState() == PeerConnection.SignalingState.STABLE){
                //这一句非常重要，在有人加入房间后，收到offer -> setRemoteDescription -> onCreateSuccess -> setLocalDescription,然后被调用
                //实测在注释掉之后通信无法建立
                if (role == Role.receiver){
                    manager.sendAnswer(socketId,peerConnection.getLocalDescription());
                }
            }
        }

        @Override
        public void onCreateFailure(String s) {

        }

        @Override
        public void onSetFailure(String s) {

        }
    }
}
