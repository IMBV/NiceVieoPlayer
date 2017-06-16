package com.xiao.nicevideoplayer;

/**
 * 播放器的状态.
 *
 * @author panda
 */

public enum PlayerState {
    /**播放错误*/
    ERROR(-1),
    /**播放未开始*/
    IDLE(0),
    /**播放准备中*/
    PREPARING(1),
    /**播放准备就绪*/
    PREPARED(2),
    /**正在播放*/
    PLAYING(3),
    /**暂停播放*/
    PAUSED(4),
    /**正在缓冲(播放器正在播放时，缓冲区数据不足，进行缓冲，缓冲区数据足够后恢复播放)*/
    BUFFERING_PLAYING(5),
    /**正在缓冲(播放器正在播放时，缓冲区数据不足，进行缓冲，此时暂停播放器，继续缓冲，缓冲区数据足够后恢复暂停)*/
    BUFFERING_PAUSED(6),
    /**播放完成*/
    COMPLETED(7);

    private int mValue;

    private PlayerState(int value){
        mValue = value;
    }

    public int value(){
        return mValue;
    }
}
