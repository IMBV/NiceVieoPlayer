package com.xiao.nicevideoplayer;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.Surface;
import android.view.TextureView;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.io.IOException;
import java.util.Map;

import tv.danmaku.ijk.media.player.AndroidMediaPlayer;
import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;

/**
 * Created by XiaoJianjun on 2017/4/28.
 * 播放器
 */
public class VideoPlayerRender extends FrameLayout
        implements VideoPlayerContract.Presenter,
        TextureView.SurfaceTextureListener {
    private final String TAG = this.getClass().getSimpleName();
    private final boolean mEnableLog = true;

    private int mPlayerType = PlayerConstants.PLAYER_TYPE_IJK;
    private PlayerState mCurrentPlayState = PlayerState.IDLE;
    private PlayerState mTargetPlayState = PlayerState.IDLE;
    private int mTargetPosition = -1;

    private ScreenMode mPlayerScreenMode = ScreenMode.NORMAL;

    private Context mContext;
    private FrameLayout mContainer;
    private TextureView mTextureView;
    private VideoPlayerContract.View mViewer;

    private SurfaceTexture mSurfaceTexture;
    private String mPlayingUrl;
    private Map<String, String> mHeaders;
    private IMediaPlayer mMediaPlayer;

    private int mBufferPercentage;

    public VideoPlayerRender(Context context) {
        this(context, null);
    }

    public VideoPlayerRender(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        init();
    }

    private void init() {
        mContainer = new FrameLayout(mContext);
        mContainer.setBackgroundColor(Color.BLACK);
        LayoutParams params = new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        this.addView(mContainer, params);
    }

    @Override
    public void setUp(String url, Map<String, String> headers) {
        mPlayingUrl = url;
        mHeaders = headers;
        stop();
    }

    private boolean isPathValid(){
        return !TextUtils.isDigitsOnly(mPlayingUrl);
    }

    @Override
    public void setPlayerType(int playerType) {
        mPlayerType = playerType;
    }

    @Override
    public void start() {
        if (mCurrentPlayState == PlayerState.IDLE
                || mCurrentPlayState == PlayerState.ERROR
                || mCurrentPlayState == PlayerState.COMPLETED) {
            mTargetPlayState = PlayerState.PLAYING;
            initMediaPlayer();
            initTextureView();
            addTextureView();
        }
    }

    @Override
    public void resume() {
        log("resume");
        mTargetPlayState = PlayerState.PLAYING;
        log("targe state:"+mTargetPlayState);
        if (mCurrentPlayState == PlayerState.PAUSED || mCurrentPlayState == PlayerState.PREPARED) {
            mMediaPlayer.start();
            mCurrentPlayState = PlayerState.PLAYING;
            mViewer.onPlayerStateChanged(mPlayerScreenMode, mCurrentPlayState);
            log("STATE_PLAYING");
        }else if (mCurrentPlayState == PlayerState.BUFFERING_PAUSED) {
            mMediaPlayer.start();
            mCurrentPlayState = PlayerState.BUFFERING_PLAYING;
            mViewer.onPlayerStateChanged(mPlayerScreenMode, mCurrentPlayState);
            log("STATE_BUFFERING_PLAYING");
        }else if(mCurrentPlayState == PlayerState.IDLE){
            openMediaPlayer();
        }
    }

    @Override
    public void pause() {
        log("pause");
        mTargetPlayState = PlayerState.PAUSED;
        log("targe state:"+mTargetPlayState);
        if (mCurrentPlayState == PlayerState.PLAYING) {
            mMediaPlayer.pause();
            mCurrentPlayState = PlayerState.PAUSED;
            mViewer.onPlayerStateChanged(mPlayerScreenMode, mCurrentPlayState);
        }
        if (mCurrentPlayState == PlayerState.BUFFERING_PLAYING) {
            mMediaPlayer.pause();
            mCurrentPlayState = PlayerState.BUFFERING_PAUSED;
            mViewer.onPlayerStateChanged(mPlayerScreenMode, mCurrentPlayState);
        }

    }

    @Override
    public void seekTo(int pos) {
        log("seekTo:"+pos);
        if (mMediaPlayer != null) {
            if(pos >= 0){
                mTargetPosition = pos;
                log("pos:"+pos+",state:"+ mPlayerScreenMode);
                if(mCurrentPlayState != PlayerState.PREPARING && mCurrentPlayState != PlayerState.IDLE && mCurrentPlayState != PlayerState.ERROR){
                    mMediaPlayer.seekTo(pos);
                }
            }
        }
    }

    @Override
    public String getPlayingUrl() {
        return mPlayingUrl;
    }

    @Override
    public boolean isIdle() {
        return mCurrentPlayState == PlayerState.IDLE;
    }

    @Override
    public boolean isPreparing() {
        return mCurrentPlayState == PlayerState.PREPARING;
    }

    @Override
    public boolean isPrepared() {
        return mCurrentPlayState == PlayerState.PREPARED;
    }

    @Override
    public boolean isBufferingPlaying() {
        return mCurrentPlayState == PlayerState.BUFFERING_PLAYING;
    }

    @Override
    public boolean isBufferingPaused() {
        return mCurrentPlayState == PlayerState.BUFFERING_PAUSED;
    }

    @Override
    public boolean isPlaying() {
        return mCurrentPlayState == PlayerState.PLAYING;
    }

    @Override
    public boolean isPaused() {
        return mCurrentPlayState == PlayerState.PAUSED;
    }

    @Override
    public boolean isError() {
        return mCurrentPlayState == PlayerState.ERROR;
    }

    @Override
    public boolean isCompleted() {
        return mCurrentPlayState == PlayerState.COMPLETED;
    }

    @Override
    public boolean isFullScreenMode() {
        return mPlayerScreenMode == ScreenMode.FULL_SCREEN;
    }

    @Override
    public boolean isTinyWindowMode() {
        return mPlayerScreenMode == ScreenMode.TINY_WINDOW;
    }

    @Override
    public boolean isNormalMode() {
        return mPlayerScreenMode == ScreenMode.NORMAL;
    }

    @Override
    public long getDuration() {
        return mMediaPlayer != null ? mMediaPlayer.getDuration() : 0;
    }

    @Override
    public long getCurrentPosition() {
        return mMediaPlayer != null ? mMediaPlayer.getCurrentPosition() : 0;
    }

    @Override
    public int getBufferPercentage() {
        return mBufferPercentage;
    }

    private void initMediaPlayer() {
        if (mMediaPlayer == null) {
            switch (mPlayerType) {
                case PlayerConstants.PLAYER_TYPE_NATIVE:
                    mMediaPlayer = new AndroidMediaPlayer();
                    break;
                case PlayerConstants.PLAYER_TYPE_IJK:
                default:
                    mMediaPlayer = new IjkMediaPlayer();
                    break;
            }
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mMediaPlayer.setScreenOnWhilePlaying(false);

            mMediaPlayer.setOnPreparedListener(mOnPreparedListener);
            mMediaPlayer.setOnVideoSizeChangedListener(mOnVideoSizeChangedListener);
            mMediaPlayer.setOnCompletionListener(mOnCompletionListener);
            mMediaPlayer.setOnErrorListener(mOnErrorListener);
            mMediaPlayer.setOnInfoListener(mOnInfoListener);
            mMediaPlayer.setOnBufferingUpdateListener(mOnBufferingUpdateListener);
            mMediaPlayer.setOnSeekCompleteListener(mOnSeekToCompletedListener);
        }
    }

    private void initTextureView() {
        if (mTextureView == null) {
            mTextureView = new TextureView(mContext);
            mTextureView.setSurfaceTextureListener(this);
        }
    }

    private void addTextureView() {
        mContainer.removeView(mTextureView);
        LayoutParams params = new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        mContainer.addView(mTextureView, 0, params);
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
        log("onSurfaceTextureAvailable", null);
        if (mSurfaceTexture == null) {
            log("onSurfaceTextureAvailable, it not exists opening---------");
            mSurfaceTexture = surfaceTexture;
            openMediaPlayer();
        } else {
            log("onSurfaceTextureAvailable, it exists updating-------");
//            mTextureView.setSurfaceTexture(mSurfaceTexture);
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN){
                mTextureView.setSurfaceTexture(mSurfaceTexture);
            }else{
                //todo: 考虑当前帧是否保留
                mSurfaceTexture.release();
                mSurfaceTexture = surfaceTexture;
                if(mMediaPlayer != null){
                    mMediaPlayer.setSurface(new Surface(mSurfaceTexture));
                }
            }

        }
    }

    private void openMediaPlayer() {
        if(!isPathValid() || mSurfaceTexture == null){
            return;
        }

        try {
            mMediaPlayer.setDataSource(mContext.getApplicationContext(), Uri.parse(mPlayingUrl), mHeaders);
            mMediaPlayer.setSurface(new Surface(mSurfaceTexture));
            mMediaPlayer.prepareAsync();
            mCurrentPlayState = PlayerState.PREPARING;
            mViewer.onPlayerStateChanged(mPlayerScreenMode, mCurrentPlayState);
            log("STATE_PREPARING");
        } catch (IOException e) {
            e.printStackTrace();
            log("打开播放器发生错误"+e.toString(), e);
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        log("onSurfaceTextureSizeChanged", null);
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        log("onSurfaceTextureDestroyed", null);
        return mSurfaceTexture == null;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
//        log("onSurfaceTextureUpdated", null);
    }

    private IMediaPlayer.OnPreparedListener mOnPreparedListener
            = new IMediaPlayer.OnPreparedListener() {
        @Override
        public void onPrepared(IMediaPlayer mp) {
            log("onPrepared ——> STATE_PREPARED");
            mCurrentPlayState = PlayerState.PREPARED;
            mViewer.onPlayerStateChanged(mPlayerScreenMode, mCurrentPlayState);
            if(mTargetPlayState == PlayerState.PLAYING){
                if(mTargetPosition > 0){
                    seekTo(mTargetPosition);
                    mTargetPosition = -1;
                }else{
                    mp.start();
                }
            }else if(mTargetPlayState == PlayerState.PAUSED){
                if(mTargetPosition > 0){
                    seekTo(mTargetPosition);
                    mTargetPosition = -1;
                }
            }
        }
    };

    private IMediaPlayer.OnVideoSizeChangedListener mOnVideoSizeChangedListener
            = new IMediaPlayer.OnVideoSizeChangedListener() {
        @Override
        public void onVideoSizeChanged(IMediaPlayer mp, int width, int height, int sar_num, int sar_den) {
            log("onVideoSizeChanged ——> width：" + width + "，height：" + height);
        }
    };

    private IMediaPlayer.OnCompletionListener mOnCompletionListener
            = new IMediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(IMediaPlayer mp) {
            mCurrentPlayState = PlayerState.COMPLETED;
            mTargetPlayState = PlayerState.IDLE;
            mTargetPosition = -1;
            mViewer.onPlayerStateChanged(mPlayerScreenMode, mCurrentPlayState);
            log("onCompletion ——> STATE_COMPLETED");
        }
    };

    private IMediaPlayer.OnErrorListener mOnErrorListener
            = new IMediaPlayer.OnErrorListener() {
        @Override
        public boolean onError(IMediaPlayer mp, int what, int extra) {
            mCurrentPlayState = PlayerState.ERROR;
            mTargetPlayState = PlayerState.ERROR;
            mViewer.onPlayerStateChanged(mPlayerScreenMode, mCurrentPlayState);
            log("onError ——> STATE_ERROR ———— what：" + what);
            return false;
        }
    };

    private IMediaPlayer.OnInfoListener mOnInfoListener
            = new IMediaPlayer.OnInfoListener() {
        @Override
        public boolean onInfo(IMediaPlayer mp, int what, int extra) {
            if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
                log("onInfo ——> MEDIA_INFO_VIDEO_RENDERING_START");
                // 播放器开始渲染
                if(mTargetPlayState == PlayerState.PLAYING){
                    mCurrentPlayState = PlayerState.PLAYING;
                }else if(mTargetPlayState == PlayerState.PAUSED){
                    mCurrentPlayState = PlayerState.PAUSED;
                }
                mViewer.onPlayerStateChanged(mPlayerScreenMode, mCurrentPlayState);
                log("onInfo ——> MEDIA_INFO_VIDEO_RENDERING_START：STATE_PLAYING");
            } else if (what == IMediaPlayer.MEDIA_INFO_BUFFERING_START) {
                log("onInfo ——> MEDIA_INFO_BUFFERING_START");
                // MediaPlayer暂时不播放，以缓冲更多的数据
                if (mTargetPlayState == PlayerState.PAUSED) {
                    mCurrentPlayState = PlayerState.BUFFERING_PAUSED;
                    log("onInfo ——> MEDIA_INFO_BUFFERING_START：STATE_BUFFERING_PAUSED");
                } else if(mTargetPlayState == PlayerState.PLAYING) {
                    mCurrentPlayState = PlayerState.BUFFERING_PLAYING;
                    log("onInfo ——> MEDIA_INFO_BUFFERING_START：STATE_BUFFERING_PLAYING");
                }
                mViewer.onPlayerStateChanged(mPlayerScreenMode, mCurrentPlayState);
            } else if (what == IMediaPlayer.MEDIA_INFO_BUFFERING_END) {
                log("onInfo ——> MEDIA_INFO_BUFFERING_END");
                // 填充缓冲区后，MediaPlayer恢复播放/暂停
                if (mCurrentPlayState == PlayerState.BUFFERING_PLAYING) {
                    mCurrentPlayState = PlayerState.PLAYING;
                    mViewer.onPlayerStateChanged(mPlayerScreenMode, mCurrentPlayState);
                    log("onInfo ——> MEDIA_INFO_BUFFERING_END： STATE_PLAYING");
                }
                if (mCurrentPlayState == PlayerState.BUFFERING_PAUSED) {
                    mCurrentPlayState = PlayerState.PAUSED;
                    mViewer.onPlayerStateChanged(mPlayerScreenMode, mCurrentPlayState);
                    log("onInfo ——> MEDIA_INFO_BUFFERING_END： STATE_PAUSED");
                }
            } else {
                log("onInfo ——> what：" + what);
            }
            return true;
        }
    };

    private IMediaPlayer.OnBufferingUpdateListener mOnBufferingUpdateListener
            = new IMediaPlayer.OnBufferingUpdateListener() {
        @Override
        public void onBufferingUpdate(IMediaPlayer mp, int percent) {
            mBufferPercentage = percent;
        }
    };

    private IMediaPlayer.OnSeekCompleteListener mOnSeekToCompletedListener = new IMediaPlayer.OnSeekCompleteListener() {
        @Override
        public void onSeekComplete(IMediaPlayer mediaPlayer) {
            log("onSeekToCompleted");
            if(mTargetPlayState == PlayerState.PLAYING){
                if(mCurrentPlayState == PlayerState.PREPARED){
                    if(mOnPreparedListener != null){
                        mOnPreparedListener.onPrepared(mediaPlayer);
                    }
                }

            }
        }
    };

    /**
     * 全屏，将mContainer(内部包含mTextureView和mController)从当前容器中移除，并添加到android.R.content中.
     * 切换横屏时需要在manifest的activity标签下添加android:configChanges="orientation|keyboardHidden|screenSize"配置，
     * 以避免Activity重新走生命周期
     */
    @Override
    public void enterFullScreen() {
        if (mPlayerScreenMode == ScreenMode.FULL_SCREEN) return;

        // 隐藏ActionBar、状态栏，并横屏
        NiceUtil.hideActionBar(mContext);
        NiceUtil.scanForActivity(mContext)
                .setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        this.removeView(mContainer);
        ViewGroup contentView = (ViewGroup) NiceUtil.scanForActivity(mContext)
                .findViewById(android.R.id.content);
        LayoutParams params = new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        contentView.addView(mContainer, params);

        mPlayerScreenMode = ScreenMode.FULL_SCREEN;
        mViewer.onPlayerStateChanged(mPlayerScreenMode, mCurrentPlayState);
        log("SCREEN_MODE_FULL_SCREEN");
    }

    /**
     * 退出全屏，移除mTextureView和mController，并添加到非全屏的容器中。
     * 切换竖屏时需要在manifest的activity标签下添加android:configChanges="orientation|keyboardHidden|screenSize"配置，
     * 以避免Activity重新走生命周期.
     *
     * @return true退出全屏.
     */
    @Override
    public boolean exitFullScreen() {
        if (mPlayerScreenMode == ScreenMode.FULL_SCREEN) {
            NiceUtil.showActionBar(mContext);
            NiceUtil.scanForActivity(mContext)
                    .setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

            ViewGroup contentView = (ViewGroup) NiceUtil.scanForActivity(mContext)
                    .findViewById(android.R.id.content);
            contentView.removeView(mContainer);
            LayoutParams params = new LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);
            this.addView(mContainer, params);

            mPlayerScreenMode = ScreenMode.NORMAL;
            mViewer.onPlayerStateChanged(mPlayerScreenMode, mCurrentPlayState);
            log("SCREEN_MODE_NORMAL");
            return true;
        }
        return false;
    }

    /**
     * 进入小窗口播放，小窗口播放的实现原理与全屏播放类似。
     */
    @Override
    public void enterTinyWindow() {
        if (mPlayerScreenMode == ScreenMode.TINY_WINDOW) return;
        this.removeView(mContainer);

        ViewGroup contentView = (ViewGroup) NiceUtil.scanForActivity(mContext)
                .findViewById(android.R.id.content);
        // 小窗口的宽度为屏幕宽度的60%，长宽比默认为16:9，右边距、下边距为8dp。
        LayoutParams params = new LayoutParams(
                (int) (NiceUtil.getScreenWidth(mContext) * 0.6f),
                (int) (NiceUtil.getScreenWidth(mContext) * 0.6f * 9f / 16f));
        params.gravity = Gravity.BOTTOM | Gravity.END;
        params.rightMargin = NiceUtil.dp2px(mContext, 8f);
        params.bottomMargin = NiceUtil.dp2px(mContext, 8f);

        contentView.addView(mContainer, params);

        mPlayerScreenMode = ScreenMode.TINY_WINDOW;
        mViewer.onPlayerStateChanged(mPlayerScreenMode, mCurrentPlayState);
        log("SCREEN_MODE_TINY_WINDOW");
    }

    /**
     * 退出小窗口播放
     */
    @Override
    public boolean exitTinyWindow() {
        if (mPlayerScreenMode == ScreenMode.TINY_WINDOW) {
            ViewGroup contentView = (ViewGroup) NiceUtil.scanForActivity(mContext)
                    .findViewById(android.R.id.content);
            contentView.removeView(mContainer);
            LayoutParams params = new LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);
            this.addView(mContainer, params);

            mPlayerScreenMode = ScreenMode.NORMAL;
            mViewer.onPlayerStateChanged(mPlayerScreenMode, mCurrentPlayState);
            log("SCREEN_MODE_NORMAL");
            return true;
        }
        return false;
    }

    @Override
    public void stop() {
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
        mContainer.removeView(mTextureView);
        if (mSurfaceTexture != null) {
            mSurfaceTexture.release();
            mSurfaceTexture = null;
        }
        if (mViewer != null) {
            mViewer.reset();
        }
        mCurrentPlayState = PlayerState.IDLE;
        mPlayerScreenMode = ScreenMode.NORMAL;
        mTargetPlayState = PlayerState.IDLE;
    }

    public void setViewer(VideoPlayerContract.View viewer) {
        mViewer = viewer;
        mViewer.setPresenter(this);
        mContainer.removeView(mViewer.asView());
        LayoutParams params = new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        mContainer.addView(mViewer.asView(), params);
    }

    private void log(String info){
        if(mEnableLog){
            Log.e(TAG, info);
        }
    }

    private void log(String info, Throwable throwable){
        if(mEnableLog){
            Log.e(TAG, info, throwable);
        }
    }
}
