package com.example.wywebrtc.webrtcsource;

import com.example.wywebrtc.type.DeviceType;

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
    public static final String USER_ID = "0";                    //userId设置为"0",默认小于6位长度由服务器自动产生
    public static final int DEVICE = DeviceType.PHONE.getCode(); //设备类型为手机
    public static final String SOCKET_URI = "ws://114.55.252.175:8080/webRtcSignalingService/webRtcSignaling";//webSocket地址
    //摄像头配置
    public static final int CAPTURE_WIDTH = 320;  //宽
    public static final int CAPTURE_HEIGHT = 240; //高
    public static final int CAPTURE_FPS = 10;     //帧率
}
