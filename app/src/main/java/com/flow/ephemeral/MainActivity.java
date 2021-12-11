package com.flow.ephemeral;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends Activity implements
        SeekBar.OnSeekBarChangeListener,SurfaceHolder.Callback {
    private SurfaceHolder holder;
    private MediaPlayer mediaplayer;
    private Timer timer;
    private TimerTask task;
    private SurfaceView sv;
    private RelativeLayout rl;
    private RelativeLayout rl_top;
    private SeekBar sbar;
    private ImageView play;
    private ImageButton fullScreen;
    private int proceed;
    private static TextView tv_progress, tv_total;
    private BatteryReceiver castReceiver;
    private SurfaceView sv_video;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);// 去掉标题栏
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);// 去掉信息栏
        setContentView(R.layout.activity_main);
        sv_video = (SurfaceView) findViewById(R.id.sv_video);
        rl_top = (RelativeLayout) findViewById(R.id.rl_top);
        sv = (SurfaceView) findViewById(R.id.sv_video);
        tv_progress = (TextView) findViewById(R.id.tv_progress);
        tv_total = (TextView) findViewById(R.id.tv_total);
        // 得到SurfaceView的容器,界面内容是显示在容器里面的
        holder = sv.getHolder();
        holder.addCallback(this);
        rl = (RelativeLayout) findViewById(R.id.rl);
        play = (ImageView) findViewById(R.id.play);
        sbar = (SeekBar) findViewById(R.id.sbar);
        sbar.setOnSeekBarChangeListener(this);
        // 获得控件实例
        fullScreen = (ImageButton) findViewById(R.id.btn_one);
        //实现按钮的点击
        fullScreen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view){
                if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                    // 当前是竖屏，切换成横屏
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                } else {
                    // 当前是横屏，切换成竖屏
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                }
            }
        });
        // 初始化计时器
        timer = new Timer();
        task = new TimerTask() {
            @Override
            public void run() {
                if (mediaplayer != null && mediaplayer.isPlaying()) {
                    int total = mediaplayer.getDuration();
                    sbar.setMax(total);
                    int progress = mediaplayer.getCurrentPosition();
                    sbar.setProgress(progress);

                    int duration =mediaplayer.getDuration();                //获取视频总时长
                    int currentPosition =mediaplayer.getCurrentPosition();//获取播放进度
                    Message msg = handler.obtainMessage();//创建消息对象
                    //将视频的总时长和播放进度封装至消息对象中
                    Bundle bundle = new Bundle();
                    bundle.putInt("duration", duration);
                    bundle.putInt("currentPosition", currentPosition);
                    msg.setData(bundle);
//                    Log.i("时长", String.valueOf(duration));
//                    Log.i("当前",String.valueOf(currentPosition));
                    //将消息发送到主线程的消息队列
                    handler.sendMessage(msg);
                }else{
                    // play.setImageResource(android.R.drawable.ic_media_play);
                }
            }
        };
        // 设置TimerTask延迟100ms,每隔100ms执行一次
        timer.schedule(task, 100, 100);
    }
    //判断当前是否横屏
    private boolean isLand() {
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            return false;
        } else {
            return true;
        }
    }
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.i("调用","onConfigurationChanged()");
        if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            Log.e("pid", "竖屏了");
            setParam(mediaplayer, false);
        } else if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Log.e("pid", "横屏了");
            setParam(mediaplayer, true);
        }
    }
    private void setParam(MediaPlayer mp, boolean isLand) {
        float screenWidth = getWindowManager().getDefaultDisplay().getWidth();
        float screenHeight = screenWidth / 16f * 9f;
        if (isLand) {
            screenHeight = getWindowManager().getDefaultDisplay().getHeight();
        }
        float videoWdith = mp.getVideoWidth();
        float videoHeight = mp.getVideoHeight();

        float screenPor = screenWidth / screenHeight;// 16:9
        float videoPor = videoWdith / videoHeight;// 9:16

        ViewGroup.LayoutParams pa = sv_video.getLayoutParams();
        if (videoPor <= screenPor) {
            pa.height = (int) screenHeight;
            pa.width = (int) (screenHeight * videoPor);
        } else {
            pa.width = (int) screenWidth;
            pa.height = (int) (screenWidth / videoPor);
        }
        ViewGroup.LayoutParams rl_pa = rl_top.getLayoutParams();
        rl_pa.width = pa.width;
        rl_pa.height = pa.height;
        rl_top.setLayoutParams(rl_pa);
        sv_video.setLayoutParams(pa);
    }
    @Override
    protected void onResume() {
        super.onResume();
        castReceiver = new BatteryReceiver();
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(castReceiver, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mediaplayer.isPlaying()) {
            // 保存当前的播放位置
            proceed= mediaplayer.getCurrentPosition();
            //Log.i("退出当前时长", String.valueOf(proceed));
            mediaplayer.stop();
        }
        Log.i("MainActivity","调用onPause()");
        unregisterReceiver(castReceiver);
    }


    public static Handler handler = new Handler() {//创建消息处理器对象
        // 在主线程中处理从子线程发送过来的消息
        @Override
        public void handleMessage(Message msg) {
            Bundle bundle = msg.getData(); //获取从子线程发送过来的音乐播放进度
            int duration = bundle.getInt("duration");                  //视频的总时长
            int currentPostition = bundle.getInt("currentPosition");//视频当前进度
//            Log.i("时长", String.valueOf(duration));
//            Log.i("当前",String.valueOf(currentPostition));
            //视频的总时长
            int minute = duration / 1000 / 60;
            int second = duration / 1000 % 60;
            String strMinute = null;
            String strSecond = null;
            if (minute < 10) {              //如果视频的时间中的分钟小于10
                strMinute = "0" + minute; //在分钟的前面加一个0
            } else {
                strMinute = minute + "";
            }
            if (second < 10) {             //如果视频的时间中的秒钟小于10
                strSecond = "0" + second;//在秒钟前面加一个0
            } else {
                strSecond = second + "";
            }
            tv_total.setText(strMinute + ":" + strSecond);
            //视频当前播放时长
            minute = currentPostition / 1000 / 60;
            second = currentPostition / 1000 % 60;
            if (minute < 10) {             //如果视频的时间中的分钟小于10
                strMinute = "0" + minute;//在分钟的前面加一个0
            } else {
                strMinute = minute + "";
            }
            if (second < 10) {               //如果视频的时间中的秒钟小于10
                strSecond = "0" + second;  //在秒钟前面加一个0
            } else {
                strSecond = second + "";
            }
            tv_progress.setText(strMinute + ":" + strSecond);
        }
    };

    //SurfaceHolder创建完成时触发
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            mediaplayer = new MediaPlayer();
            mediaplayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            Uri uri = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE+"://"+
                    getPackageName()+"/"+R.raw.video);
            try{
                mediaplayer.setDataSource(MainActivity.this,uri);
            } catch (IOException e) {
                Toast.makeText(MainActivity.this,"播放失败",
                        Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
            mediaplayer.setDisplay(holder);
            mediaplayer.prepareAsync();
            mediaplayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener()
            {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    if(proceed>0){
                        setParam(mp, isLand());
                        mediaplayer.seekTo(proceed);
                        mediaplayer.start();
                    }else{
                        setParam(mp, isLand());
                        mediaplayer.start();
                    }
                }
            });
        } catch (Exception e) {
            Toast.makeText(MainActivity.this, "播放失败",
                    Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    //SurfaceHolder大小变化时触发
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }
    //SurfaceHolder注销时触发
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if(mediaplayer. isPlaying()){
            mediaplayer.stop();
        }
    }
    //播放暂停按钮的点击事件
    public void click(View view) {
        if (mediaplayer != null && mediaplayer.isPlaying()) {
            mediaplayer.pause();
            play.setImageResource(android.R.drawable.ic_media_play);
        } else {
            mediaplayer.start();
            play.setImageResource(android.R.drawable.ic_media_pause);
        }
    }
    //进度发生变化时触发
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

    }
    //进度条开始拖动时触发
    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }
    //进度条拖动停止时触发
    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        int position = seekBar.getProgress();
//        Log.i("拖动当前时长", String.valueOf(position));
        if (mediaplayer != null) {
            mediaplayer.seekTo(position);
        }
    }
    //屏幕触摸事件
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (rl.getVisibility() == View.INVISIBLE) {
                    rl.setVisibility(View.VISIBLE);
                    // 倒计时3秒
                    CountDownTimer cdt = new CountDownTimer(3000, 1000) {
                        @Override
                        public void onTick(long millisUntilFinished) {
                            System.out.println(millisUntilFinished);
                        }
                        @Override
                        public void onFinish() {
                            rl.setVisibility(View.INVISIBLE);
                        }
                    };
                    cdt.start();
                } else if (rl.getVisibility() == View.VISIBLE) {
                    rl.setVisibility(View.INVISIBLE);
                }
                break;
        }
        return super.onTouchEvent(event);
    }
    //Activity注销时把Timer和TimerTask对象置为空
    @Override
    protected void onDestroy() {
        timer.cancel();
        task.cancel();
        timer = null;
        task = null;
        mediaplayer.release();
        mediaplayer = null;
        super.onDestroy();
    }
}