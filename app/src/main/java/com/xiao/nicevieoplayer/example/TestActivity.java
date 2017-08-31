package com.xiao.nicevieoplayer.example;

import com.xiao.nicevideoplayer.NiceVideoPlayer;
import com.xiao.nicevideoplayer.VideoPlayerContract;
import com.xiao.nicevideoplayer.VideoPlayerView;
import com.xiao.nicevieoplayer.R;

import android.content.Intent;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class TestActivity extends AppCompatActivity {

    private final String TAG = this.getClass().getSimpleName();

    private static final String SAVED_KEY_POSITION = "position";
    private static final String SAVED_KEY_POSITION_IS_PLAYING = "is_playing";
    private static final String SAVED_KEY_URL = "url";

    private VideoPlayerContract.Presenter mPresenter;
    private boolean mPausedBySystem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        init();
        restorePlayerInfoIfExists(savedInstanceState);
    }

    private void restorePlayerInfoIfExists(Bundle savedInstanceState) {
        Log.e(TAG, "onCreate, hasBundle ? "+(savedInstanceState != null));
        if(savedInstanceState != null){
            String url = savedInstanceState.getString(SAVED_KEY_URL);
            long position = savedInstanceState.getLong(SAVED_KEY_POSITION, -1);
            boolean isPlaying = savedInstanceState.getBoolean(SAVED_KEY_POSITION_IS_PLAYING, false);
            if(!TextUtils.isEmpty(url)){
                mPresenter.setUp(url, null);
                if(position > 0){
                    mPresenter.start();
                    if(!isPlaying){
                        mPresenter.pause();
                    }
                    mPresenter.seekTo((int) position);

                }
            }
        }
    }

    private void init() {
        mPresenter = (VideoPlayerContract.Presenter) findViewById(R.id.nice_video_player);
        mPresenter.setPlayerType(NiceVideoPlayer.PLAYER_TYPE_NATIVE);
        mPresenter.setUp("http://tanzi27niu.cdsb.mobi/wps/wp-content/uploads/2017/05/2017-05-17_17-33-30.mp4", null);
        VideoPlayerView controller = new VideoPlayerView(this);
        controller.setTitle("办公室小野开番外了，居然在办公室开澡堂！老板还点赞？");
        controller.setImage("http://tanzi27niu.cdsb.mobi/wps/wp-content/uploads/2017/05/2017-05-17_17-30-43.jpg");
        mPresenter.setViewer(controller);
    }


    public void enterTinyWindow(View view) {
        if (mPresenter.isPlaying()
                || mPresenter.isBufferingPlaying()
                || mPresenter.isPaused()
                || mPresenter.isBufferingPaused()) {
            mPresenter.enterTinyWindow();
        } else {
            Toast.makeText(this, "要播放后才能进入小窗口", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        if(mPausedBySystem && (mPresenter.isPaused() || mPresenter.isBufferingPaused())){
            mPresenter.resume();
        }
        super.onResume();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.e(TAG, "onSaveInstanceState");
        if(mPresenter.isBufferingPaused() || mPresenter.isBufferingPlaying() || mPresenter.isPlaying() || mPresenter.isPaused()){
            outState.putBoolean(SAVED_KEY_POSITION_IS_PLAYING, (mPresenter.isBufferingPlaying() || mPresenter.isPlaying()));
            outState.putLong(SAVED_KEY_POSITION, mPresenter.getCurrentPosition());
            outState.putString(SAVED_KEY_URL, mPresenter.getPlayingUrl());
        }
    }

    @Override
    protected void onPause() {
        if(mPresenter.isBufferingPlaying() || mPresenter.isPlaying()){
            mPresenter.pause();
            mPausedBySystem = true;
        }else{
            mPausedBySystem = false;
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        mPresenter.stop();
        super.onDestroy();
    }

    public void showVideoList(View view) {
        startActivity(new Intent(this, RecyclerViewActivity.class));
    }
}
