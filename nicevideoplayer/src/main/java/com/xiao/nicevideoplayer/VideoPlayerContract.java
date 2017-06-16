package com.xiao.nicevideoplayer;

import java.util.Map;

/**
 * Description.
 *
 * @author panda
 */

public class VideoPlayerContract {

    public interface View{

        void onPlayerStateChanged(ScreenMode screenMode, PlayerState playState);

        void reset();

        void setPresenter(Presenter presenter);

        android.view.View asView();
    }

    public interface Presenter{
        void setUp(String url, Map<String, String> headers);

        /**
         * 设置播放器类型
         *
         * @param playerType IjkPlayer or MediaPlayer.
         */
        void setPlayerType(int playerType);
        void start();
        void resume();
        void pause();
        void seekTo(int pos);

        String getPlayingUrl();

        boolean isIdle();
        boolean isPreparing();
        boolean isPrepared();
        boolean isBufferingPlaying();
        boolean isBufferingPaused();
        boolean isPlaying();
        boolean isPaused();
        boolean isError();
        boolean isCompleted();

        boolean isFullScreenMode();
        boolean isTinyWindowMode();
        boolean isNormalMode();

        long getDuration();
        long getCurrentPosition();
        int getBufferPercentage();

        void enterFullScreen();
        boolean exitFullScreen();
        void enterTinyWindow();
        boolean exitTinyWindow();

        void stop();

        void setViewer(View viewer);
    }


}
