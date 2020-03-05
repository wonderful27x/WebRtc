package com.example.wywebrtc.webrtcinderface;

import androidx.annotation.IntDef;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 信息类型
 */
@IntDef({
        MessageType.SOCKET_OPEN,
        MessageType.SOCKET_CLOSE,
        MessageType.SOCKET_ERROR,
        MessageType.ROOM_FULL,
        MessageType.DEFAULT
})
@Retention(RetentionPolicy.SOURCE)
@Target({
        ElementType.PARAMETER,
        ElementType.METHOD,
        ElementType.FIELD
})
public @interface MessageType {
    int SOCKET_OPEN = 0;    //socket连接成功
    int SOCKET_CLOSE = 1;   //socket关闭
    int SOCKET_ERROR = 2;   //socket连接失败
    int ROOM_FULL = 3;      //房间容量已满
    int DEFAULT = -1;
}
