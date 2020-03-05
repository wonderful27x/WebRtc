package com.example.wywebrtc.webrtcinderface;

import androidx.annotation.IntDef;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 聊天类型
 */
@IntDef({
        MediaType.MEET_ROOM,
        MediaType.SINGLE_VIDEO,
        MediaType.SINGLE_AUDIO,
        MediaType.LIVE,
        MediaType.DEFAULT
})
@Retention(RetentionPolicy.SOURCE)
@Target({
        ElementType.PARAMETER,
        ElementType.FIELD
})
public @interface MediaType {
    int MEET_ROOM = 1;   //会议室
    int SINGLE_VIDEO = 2;//一对一视频
    int SINGLE_AUDIO = 3;//一对一语言
    int LIVE = 4;        //直播
    int DEFAULT = 0;
}
