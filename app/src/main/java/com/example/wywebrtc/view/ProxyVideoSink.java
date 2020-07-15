package com.example.wywebrtc.view;

import org.webrtc.Logging;
import org.webrtc.VideoFrame;
import org.webrtc.VideoSink;

/**
 * @author wonderful
 * @date 2020-7-?
 * @version 1.0
 * @description ?
 * @license  BSD-2-Clause License
 */
public class ProxyVideoSink implements VideoSink {
    private static final String TAG = "ProxyVideoSink";
    private VideoSink target;

    @Override
    synchronized public void onFrame(VideoFrame videoFrame) {
        if (target == null){
            Logging.d(TAG,"Dropping frame in proxy because target is null!");
            return;
        }
        target.onFrame(videoFrame);
    }

    synchronized void setTarget(VideoSink videoSink){
        this.target = videoSink;
    }
}
