package com.example.wywebrtc.webrtcsource;

/**
 * @author wonderful
 * @date 2020-7-?
 * @version 1.0
 * @description webRtc配置文件
 * 特别注意：在这里stun和turn分别使用了3478和3479两个不同的端口，
 * 而然谁使用哪个端口并没有映射关系，可以颠倒。
 * 并且他们还可以使用相同的端口，如都使用3478也是没问题的，不管是使用相同或不同的端口，
 * 需要注意的是这些端口都必须是有效的，因为这些端口都是服务端配置的，
 * 而配置时很有可能只配置了3478端口，详细参见服务端的配置
 * @license  BSD-2-Clause License
 */
public class WebRtcConfig {
    //stun服务器配置
    public static final String STUN_URI = "stun:114.55.252.175:3478?transport=udp";
    public static final String STUN_USER_NAME = "";
    public static final String STUN_PASSWORD = "";
    //turn服务器配置
    public static final String TURN_URI = "turn:114.55.252.175:3479?transport=udp";
    public static final String TURN_USER_NAME = "wonderful";
    public static final String TURN_PASSWORD = "wonderful@123";
    //socket配置
    public static final String SOCKET_URI = "wss://114.55.252.175/wss";
    //摄像头配置
    public static final int CAPTURE_WIDTH = 320;  //宽
    public static final int CAPTURE_HEIGHT = 240; //高
    public static final int CAPTURE_FPS = 10;     //帧率
}
