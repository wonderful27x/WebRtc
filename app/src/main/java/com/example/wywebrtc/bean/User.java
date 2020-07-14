package com.example.wywebrtc.bean;


import com.example.wywebrtc.type.DeviceType;

/**
 * 用户实体
 */
public class User {

    private String userId;         //用户id
    private String userName;       //用户名
    private String roomId;         //用户所在房间号
    private DeviceType deviceType; //使用的终端类型

    public User(){}

    public User(String userId, String userName) {
        this.userId = userId;
        this.userName = userName;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public DeviceType getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(DeviceType deviceType) {
        this.deviceType = deviceType;
    }
}
