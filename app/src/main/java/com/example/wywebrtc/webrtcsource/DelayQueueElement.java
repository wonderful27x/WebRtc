package com.example.wywebrtc.webrtcsource;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * @author wonderful
 * @date 2020-7-21
 * @version 1.0
 * @description 延迟队列元素，必须实现Delayed接口
 * @license  BSD-2-Clause License
 */
public class DelayQueueElement<T> implements Delayed {

    private WonderfulDelayQueue<T> wonderfulDelayQueue;//延迟队列引用
    private long intervalTimeOfFirst;                  //距离队列中第一个元素的时间间隔
    private T data;                                    //队列中的关键数据

    /**
     * 构造函数
     * @param intervalTimeOfFirst 距离队列中第一个元素的时间间隔
     * @param data                队列中的关键数据
     * @param wonderfulDelayQueue 延迟队列引用
     */
    public DelayQueueElement(long intervalTimeOfFirst, T data, WonderfulDelayQueue<T> wonderfulDelayQueue) {
        this.data = data;
        this.wonderfulDelayQueue = wonderfulDelayQueue;
        this.intervalTimeOfFirst = intervalTimeOfFirst;
    }

    //获取剩余时间
    @Override
    public long getDelay(TimeUnit unit) {
        //intervalTimeOfFirst + candidateQueue.startTime得到的是当前元素的过期时间
        //再减去当前的时间就等于剩余的时间，如果大于0说明没有过期，则无法从队列中取得
        return unit.convert(this.intervalTimeOfFirst + wonderfulDelayQueue.startTime - System.currentTimeMillis(),TimeUnit.MILLISECONDS);
    }

    //优先级排序
    @Override
    public int compareTo(Delayed o) {
        return (int) (this.getDelay(TimeUnit.MILLISECONDS) - o.getDelay(TimeUnit.MILLISECONDS));
    }

    public T getData() {
        return data;
    }
}
