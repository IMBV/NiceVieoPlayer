# NiceVieoPlayer
## 用IjkPlayer/MediaPlayer + TextureView封装，完美切换全屏、小窗口播放。
### Usage
下载niceviewoplayer库，在AndroidSutio中作为Mudule添加依赖。

1.在Activity中使用：
```
 private void init() {
      mNiceVideoPlayer = (NiceVideoPlayer) findViewById(R.id.nice_video_player);
      mNiceVideoPlayer.setPlayerType(NiceVideoPlayer.PLAYER_TYPE_IJK); // or NiceVideoPlayer.PLAYER_NATIVE
      mNiceVideoPlayer.setUp(mVideoUrl, null);
      NiceVideoPlayerController controller = new NiceVideoPlayerController(this);
      controller.setTitle(mTitle);
      controller.setImage(mImageUrl);
      mNiceVideoPlayer.setController(controller);
  }
  
  // 按返回键
  // 当前是全屏或小窗口，需要先退出全屏或小窗口。
  @Override
  public void onBackPressed() {
      if (NiceVideoPlayerManager.instance().onBackPressd()) {
          return;
      }
      super.onBackPressed();
  }
  ```
2.在RecyclerView列表中使用需要监听itemView detach：
```
mRecyclerView.addOnChildAttachStateChangeListener(new RecyclerView.OnChildAttachStateChangeListener() {
    @Override
    public void onChildViewAttachedToWindow(View view) {

    }

    @Override
    public void onChildViewDetachedFromWindow(View view) {
        NiceVideoPlayer niceVideoPlayer = (NiceVideoPlayer) view.findViewById(R.id.nice_video_player);
        if (niceVideoPlayer != null) {
            niceVideoPlayer.release();
        }
    }
});
```
### Proguard
```
-keep class tv.danmaku.ijk.media.player.**{*;}
```
### Demo
![](https://github.com/xiaoyanger0825/NiceVieoPlayer/raw/master/images/aa.jpg)
![](https://github.com/xiaoyanger0825/NiceVieoPlayer/raw/master/images/bb.jpg)
![](https://github.com/xiaoyanger0825/NiceVieoPlayer/raw/master/images/cc.jpg)
![](https://github.com/xiaoyanger0825/NiceVieoPlayer/raw/master/images/dd.jpg)

# 个人工程fix的bug

1. Activity在缓冲中，不能正确的pause
2. Activity由于后台进程占用过多，在onsaveState后，不能保存状态
3. seekTo（），onInfoListener（）调用时，状态没有正确的判断，导致部分情况下，seekTo（）无效，以及onInfoListenrer（）的部分处理无效


# 个人优化
1. 采用MVP的方式，书写VideoPlayerView和VideoPlayerRender，防止了双向调用，数据操作流向不清晰
2. 删除NiceVideoPlayerController之前CountDownTimer和Timer的方式，避免采用不必要的开销，采用Handler的处理即可
3. 优化了之前变量的定义，PlayerState和PlayState，难以理解，采用了ScreenMode和PlayerState的Enum方式，方便理解


#TODO:

1. 滑动列表中的改造
