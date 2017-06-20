package com.xiao.nicevideoplayer;

import com.bumptech.glide.Glide;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.DrawableRes;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

/**
 * 播放器按钮控制器
 */
public class VideoPlayerView extends FrameLayout
        implements View.OnClickListener,VideoPlayerContract.View,
        SeekBar.OnSeekBarChangeListener {

    private static final String TAG = "VideoPlayerView";
    private final int MSG_TYPE_DISMISS_DELAY = 1000;
    private final int MSG_TYPE_UPDATE_PROGRESS = 1001;
    private final long DISMISS_DELAY = 8000;
    private final long UPDATE_PROGRESS_DELAY = 300;

    private Handler mUiHandler;
    private VideoPlayerContract.Presenter mPresenter;
    private ImageView mImage;
    private ImageView mCenterStart;
    private LinearLayout mTop;
    private ImageView mBack;
    private TextView mTitle;
    private LinearLayout mBottom;
    private ImageView mRestartPause;
    private TextView mPosition;
    private TextView mDuration;
    private SeekBar mSeek;
    private ImageView mFullScreen;
    private LinearLayout mLoading;
    private TextView mLoadText;
    private LinearLayout mError;
    private TextView mRetry;
    private LinearLayout mCompleted;
    private TextView mReplay;
    private TextView mShare;

    private boolean mTopBottomVisible;

    public VideoPlayerView(Context context) {
        super(context);
        init();
    }

    private void init() {
        mUiHandler = new Handler(mCallback);
        LayoutInflater.from(getContext()).inflate(R.layout.nice_video_palyer_controller, this, true);
        mCenterStart = (ImageView) findViewById(R.id.center_start);
        mImage = (ImageView) findViewById(R.id.image);

        mTop = (LinearLayout) findViewById(R.id.top);
        mBack = (ImageView) findViewById(R.id.back);
        mTitle = (TextView) findViewById(R.id.title);

        mBottom = (LinearLayout) findViewById(R.id.bottom);
        mRestartPause = (ImageView) findViewById(R.id.restart_or_pause);
        mPosition = (TextView) findViewById(R.id.position);
        mDuration = (TextView) findViewById(R.id.duration);
        mSeek = (SeekBar) findViewById(R.id.seek);
        mFullScreen = (ImageView) findViewById(R.id.full_screen);

        mLoading = (LinearLayout) findViewById(R.id.loading);
        mLoadText = (TextView) findViewById(R.id.load_text);

        mError = (LinearLayout) findViewById(R.id.error);
        mRetry = (TextView) findViewById(R.id.retry);

        mCompleted = (LinearLayout) findViewById(R.id.completed);
        mReplay = (TextView) findViewById(R.id.replay);
        mShare = (TextView) findViewById(R.id.share);

        mCenterStart.setOnClickListener(this);
        mBack.setOnClickListener(this);
        mRestartPause.setOnClickListener(this);
        mFullScreen.setOnClickListener(this);
        mRetry.setOnClickListener(this);
        mReplay.setOnClickListener(this);
        mShare.setOnClickListener(this);
        mSeek.setOnSeekBarChangeListener(this);
        this.setOnClickListener(this);
    }


    public void setTitle(String title) {
        mTitle.setText(title);
    }

    public void setImage(String imageUrl) {
        Glide.with(getContext())
                .load(imageUrl)
                .placeholder(R.drawable.img_default)
                .crossFade()
                .into(mImage);
    }

    public void setImage(@DrawableRes int resId) {
        mImage.setImageResource(resId);
    }

    @Override
    public void onClick(View v) {
        if (v == mCenterStart) {
            if (mPresenter.isIdle()) {
                mPresenter.start();
            }
        } else if (v == mBack) {
            if (mPresenter.isFullScreenMode()) {
                mPresenter.exitFullScreen();
            } else if (mPresenter.isTinyWindowMode()) {
                mPresenter.exitTinyWindow();
            }
        } else if (v == mRestartPause) {
            if (mPresenter.isPlaying() || mPresenter.isBufferingPlaying()) {
                mPresenter.pause();
            } else if (mPresenter.isPaused() || mPresenter.isBufferingPaused()) {
                mPresenter.resume();
            }
        } else if (v == mFullScreen) {
            if (mPresenter.isNormalMode()) {
                mPresenter.enterFullScreen();
            } else if (mPresenter.isFullScreenMode()) {
                mPresenter.exitFullScreen();
            }
        } else if (v == mRetry) {
            mPresenter.stop();
            mPresenter.start();
        } else if (v == mReplay) {
            mRetry.performClick();
        } else if (v == mShare) {
            Toast.makeText(getContext(), "分享", Toast.LENGTH_SHORT).show();
        } else if (v == this) {
            if (mPresenter.isPlaying()
                    || mPresenter.isPaused()
                    || mPresenter.isBufferingPlaying()
                    || mPresenter.isBufferingPaused()) {
                setTopBottomVisible(!mTopBottomVisible);
            }
        }
    }

    private void setTopBottomVisible(boolean visible) {
        mTop.setVisibility(visible ? View.VISIBLE : View.GONE);
        mBottom.setVisibility(visible ? View.VISIBLE : View.GONE);
        mTopBottomVisible = visible;
        if (visible) {
            if (!mPresenter.isPaused() && !mPresenter.isBufferingPaused()) {
                startDismissTopBottomTimer();
            }
        } else {
            cancelDismissTopBottomTimer();
        }
    }

    @Override
    public void onPlayerStateChanged(ScreenMode screenMode, PlayerState playState) {
        if(screenMode == ScreenMode.NORMAL){
            mBack.setVisibility(View.GONE);
            mFullScreen.setVisibility(View.VISIBLE);
            mFullScreen.setImageResource(R.drawable.ic_player_enlarge);
        }else if(screenMode == ScreenMode.FULL_SCREEN){
            mBack.setVisibility(View.VISIBLE);
            mFullScreen.setVisibility(View.VISIBLE);
            mFullScreen.setImageResource(R.drawable.ic_player_shrink);
        }else if(screenMode == ScreenMode.TINY_WINDOW){
            mFullScreen.setVisibility(View.GONE);
        }else{
            //todo: error
        }


        if(playState == PlayerState.IDLE){

        }else if(playState == PlayerState.PREPARING){
            // 只显示准备中动画，其他不显示
            mImage.setVisibility(View.GONE);
            mLoading.setVisibility(View.VISIBLE);
            mLoadText.setText("正在准备...");
            mError.setVisibility(View.GONE);
            mCompleted.setVisibility(View.GONE);
            mTop.setVisibility(View.GONE);
            mCenterStart.setVisibility(View.GONE);
        }else if(playState == PlayerState.PREPARED){
            startUpdateProgressTimer();
        }else if(playState == PlayerState.PLAYING){
            mLoading.setVisibility(View.GONE);
            mRestartPause.setImageResource(R.drawable.ic_player_pause);
            startDismissTopBottomTimer();
        }else if(playState == PlayerState.PAUSED){
            mLoading.setVisibility(View.GONE);
            mRestartPause.setImageResource(R.drawable.ic_player_start);
            cancelDismissTopBottomTimer();
        }else if(playState == PlayerState.BUFFERING_PLAYING){
            mLoading.setVisibility(View.VISIBLE);
            mRestartPause.setImageResource(R.drawable.ic_player_pause);
            mLoadText.setText("正在缓冲...");
            startDismissTopBottomTimer();
        }else if(playState == PlayerState.BUFFERING_PAUSED){
            mLoading.setVisibility(View.VISIBLE);
            mRestartPause.setImageResource(R.drawable.ic_player_start);
            mLoadText.setText("正在缓冲...");
            cancelDismissTopBottomTimer();
        }else if(playState == PlayerState.COMPLETED){
            cancelUpdateProgressTimer();
            setTopBottomVisible(false);
            mImage.setVisibility(View.VISIBLE);
            mCompleted.setVisibility(View.VISIBLE);
            if (mPresenter.isFullScreenMode()) {
                mPresenter.exitFullScreen();
            }
            if (mPresenter.isTinyWindowMode()) {
                mPresenter.exitTinyWindow();
            }
//                mPresenter.stop();
        }else if(playState == PlayerState.ERROR){
            cancelUpdateProgressTimer();
            setTopBottomVisible(false);
            mTop.setVisibility(View.VISIBLE);
            mError.setVisibility(View.VISIBLE);
        }else{
            //todo: log error
        }
    }

    private void startUpdateProgressTimer() {
        Log.e(TAG, "startUpdateProgressTimer");
        cancelUpdateProgressTimer();
        mUiHandler.sendEmptyMessageDelayed(MSG_TYPE_UPDATE_PROGRESS, UPDATE_PROGRESS_DELAY);
    }

    private void updateProgress() {
        Log.e(TAG, "updateProgress");
        long position = mPresenter.getCurrentPosition();
        long duration = mPresenter.getDuration();
        int bufferPercentage = mPresenter.getBufferPercentage();
        mSeek.setSecondaryProgress(bufferPercentage);
        int progress = (int) (100f * position / duration);
        mSeek.setProgress(progress);
        mPosition.setText(NiceUtil.formatTime(position));
        mDuration.setText(NiceUtil.formatTime(duration));
    }

    private void cancelUpdateProgressTimer() {
        mUiHandler.removeMessages(MSG_TYPE_UPDATE_PROGRESS);
        Log.e(TAG, "cancelUpdateProgressTimer");
    }

    private void startDismissTopBottomTimer() {
        cancelDismissTopBottomTimer();
        mUiHandler.sendEmptyMessageDelayed(MSG_TYPE_DISMISS_DELAY, DISMISS_DELAY);
    }


    private void cancelDismissTopBottomTimer() {
        mUiHandler.removeMessages(MSG_TYPE_DISMISS_DELAY);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        cancelDismissTopBottomTimer();
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        if (mPresenter.isBufferingPaused() || mPresenter.isPaused()) {
            mPresenter.resume();
        }
        int position = (int) (mPresenter.getDuration() * seekBar.getProgress() / 100f);
        mPresenter.seekTo(position);
        startDismissTopBottomTimer();
    }

    /**
     * 控制器恢复到初始状态
     */
    @Override
    public void reset() {
        mTopBottomVisible = false;
        cancelUpdateProgressTimer();
        cancelDismissTopBottomTimer();
        mSeek.setProgress(0);
        mSeek.setSecondaryProgress(0);

        mCenterStart.setVisibility(View.VISIBLE);
        mImage.setVisibility(View.VISIBLE);

        mBottom.setVisibility(View.GONE);
        mFullScreen.setImageResource(R.drawable.ic_player_enlarge);

        mTop.setVisibility(View.VISIBLE);
        mBack.setVisibility(View.GONE);

        mLoading.setVisibility(View.GONE);
        mError.setVisibility(View.GONE);
        mCompleted.setVisibility(View.GONE);
    }

    @Override
    public void setPresenter(VideoPlayerContract.Presenter presenter) {
        mPresenter = presenter;
        if (mPresenter.isIdle()) {
            mBack.setVisibility(View.GONE);
            mTop.setVisibility(View.VISIBLE);
            mBottom.setVisibility(View.GONE);
        }
    }

    @Override
    public View asView() {
        return this;
    }

    private Handler.Callback mCallback = new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what){
                case MSG_TYPE_DISMISS_DELAY:
                    setTopBottomVisible(false);
                    break;
                case MSG_TYPE_UPDATE_PROGRESS:
                    updateProgress();
                    startUpdateProgressTimer();
                    break;
            }
            return true;
        }
    };
}
