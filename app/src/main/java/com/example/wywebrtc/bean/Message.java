package com.example.wywebrtc.bean;

import com.example.wywebrtc.webrtcinderface.MessageType;

/**
 * 消息封装类
 */
public class Message {
    private @MessageType int messageType;
    private String message;

    public Message(){}

    public Message(@MessageType int messageType, String message) {
        this.messageType = messageType;
        this.message = message;
    }

    public @MessageType int getMessageType() {
        return messageType;
    }

    public void setMessageType(@MessageType int messageType) {
        this.messageType = messageType;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
