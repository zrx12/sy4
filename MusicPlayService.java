package cn.edu.bistu.music.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cn.edu.bistu.music.MainActivity;
import cn.edu.bistu.music.R;
import cn.edu.bistu.music.empty.Music;
import cn.edu.bistu.music.util.GlobalNumber;

/***
 *  MusicPlayService 音乐播放服务
 *  提供一个 音乐播放的服务
 * @version 1.0
 * @apiNote 1
 */
public class MusicPlayService extends Service {
    private MediaPlayer player;
    // MediaPlayer 用于播放 音乐，可播放在线音乐，
    // 播放在线音乐的时候相当于先缓存在播放。
    private String TAG = "MusicPlayService";
    // TAG 用于输出日志，做标识
    private int position = -1;
    // 目前播放音乐在播放队列中的位置
    // -1 表示队列为空

    private Notification.Builder builder;
    private NotificationManager notificationManager;
    String CHANNEL_ID = "cn.edu.bistu.music";
    private IntentFilter intentFilter;
    // 这四行代码与通知有关，在播放的时候会在通知显示

    /***
     * 创建一个通知，在服务建立的时候
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public  void  start(){
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        //获取一个Notification构造器
        builder = new Notification.Builder(this);
        Intent nfIntent = new Intent(this, MainActivity.class);
        builder.setContentIntent(PendingIntent.getActivity(this, 0, nfIntent, 0)) // 设置PendingIntent
                .setLargeIcon(BitmapFactory.decodeResource(this.getResources(), R.drawable.ic_launcher_foreground)) // 设置下拉列表中的图标(大图标)
                .setContentTitle("当前暂未播放歌曲") // 设置下拉列表里的标题
                .setChannelId(CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground) // 设置状态栏内的小图标
                .setContentText("当前暂未播放歌曲"); // 设置上下文内容
        // 获取构建好的Notification
        Notification stepNotification = builder.build();
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
            NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID, "Music play", NotificationManager.IMPORTANCE_HIGH);
            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(notificationChannel);
        }
        notificationManager.notify(110,stepNotification);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onCreate() {
        super.onCreate();
        player = new MediaPlayer();  // 初始化一个 MediaPlayer
        start();  // 注册，并显示通知
        Log.d(TAG, "READY TO PLAY MUSIC");
    }
    @Override
    public IBinder onBind(Intent intent) {
        return  new MusicControl();
    }

    /***
     * 修改当前通知
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public  void  changeNotification(Music music){
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        //获取一个Notification构造器
        builder = new Notification.Builder(this);
        Intent nfIntent = new Intent(this, MainActivity.class);
        builder.setContentIntent(PendingIntent.getActivity(this, 0, nfIntent, 0)) // 设置PendingIntent
                .setLargeIcon(BitmapFactory.decodeResource(this.getResources(), R.drawable.ic_launcher_foreground)) // 设置下拉列表中的图标(大图标)
                .setContentTitle("当前播放歌曲"+music.getTitle()) // 设置下拉列表里的标题
                .setChannelId(CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground) // 设置状态栏内的小图标
                .setContentText("当前播放歌曲" + music.getArt() +" "+music.getTitle()); // 设置上下文内容
        // 获取构建好的Notification
        Notification stepNotification = builder.build();
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
            NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID, "Music play", NotificationManager.IMPORTANCE_HIGH);
            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(notificationChannel);
        }
        notificationManager.notify(110,stepNotification);
    }

    //该方法包含关于歌曲的操作，
    //在主活动中通过Binder，对服务中的MediaPlayer进行操作
    public class MusicControl extends Binder {

        //判断是否处于播放状态
        public boolean isPlaying(){
            return player.isPlaying();
        }
        // 对GlobalNumber中的播放队列执行添加操作
        @RequiresApi(api = Build.VERSION_CODES.O)
        public boolean addMusic(Music music) throws IOException {
            // postion == -1 ， 队列为空 ， 添加队列的时候，直接播放
            if (position == -1) {
                position++;
                // position + 1 ，当前播放的位置 为第一个
                GlobalNumber.musicPlayList.add(music);
                // 添加到队列
                Log.d(TAG,Arrays.toString(GlobalNumber.musicPlayList.toArray()));
                // Log 输出 当前播放的播放队列
                player.reset();
                // MediaPlayer 状态重置
                player.setDataSource("https://music.163.com/song/media/outer/url?id="+ GlobalNumber.musicPlayList.get(position).getId()+".mp3");
                // MediaPlayer 加载音频资源
                player.prepare();
                // MediaPlayer 准备
                player.start();
                // MediaPlayer 播放
                changeNotification(GlobalNumber.musicPlayList.get(position));
                // 改变通知显示

            }else{
                if (GlobalNumber.musicPlayList.contains(music)) {
                    Toast.makeText(getApplicationContext(),"添加失败，已经存在",Toast.LENGTH_SHORT).show();
                    return false;
                }else{
                    GlobalNumber.musicPlayList.add(music);
                    Toast.makeText(getApplicationContext(),"已添加到播放列表",Toast.LENGTH_SHORT).show();
                    return true;

                }
                //                GlobalNumber.musicPlayList.add(music);  // 添加到队列
            }
            return false;


        }

        /***
         * 根据目前音乐在队列中的位置，播放音乐。
         * 用于在播放队列中点击任意音乐播放
         * @param p 音乐位置
         * @throws IOException IO异常
         */
        @RequiresApi(api = Build.VERSION_CODES.O)
        public void playbyID(int p) throws IOException {
            try{
                player.reset();
                // MediaPlayer 状态重置
                position = p;
                // position 改变为播放的位置
                player.setDataSource("https://music.163.com/song/media/outer/url?id="+ GlobalNumber.musicPlayList.get(position).getId()+".mp3");
                player.prepare();
                player.start();
                // MediaPlayer 加载音频资源，准备并播放
                changeNotification(GlobalNumber.musicPlayList.get(position));            // 改变通知显示
            }catch (IOException e){
                Toast.makeText(getApplicationContext(),"播放失败，好像资源不存在",Toast.LENGTH_SHORT).show();

            }
        }
        //播放
        public void play()   {
            if ( GlobalNumber.musicPlayList.size()!=0){ // 判断列表是不是为空
                if (!player.isPlaying()) { // 如果目前音乐未在播放
                    player.start(); // 播放开始
                    Log.d(TAG, "播放音乐");
                }else{
                    player.pause(); // 暂停播放
                    Log.d(TAG, "暂停音乐");
                }
            }
        }

        /***
         * 播放上一首歌曲
         * @throws IOException
         */
        @RequiresApi(api = Build.VERSION_CODES.O)
        public void preMusic() throws IOException {
            if (position > 0 ) { // position > 0 防止下标越界错误 造成程序闪退
                try{
                    player.reset();
                    // MediaPlayer 状态重置
                    position--;
                    // position 位置减一
                    player.setDataSource("https://music.163.com/song/media/outer/url?id="+ GlobalNumber.musicPlayList.get(position).getId()+".mp3");
                    player.prepare();
                    player.start();
                    // MediaPlayer 加载音频资源，准备并播放
                    changeNotification(GlobalNumber.musicPlayList.get(position));
                    // 改变通知显示

                }catch (IOException e){
                    Toast.makeText(getApplicationContext(),"播放失败，好像资源不存在",Toast.LENGTH_SHORT).show();

                }

            }else{
                Toast.makeText(getApplicationContext(),"已经是第一首了",Toast.LENGTH_SHORT).show();
            }
        }

        /***
         * 播放下一首
         * @throws IOException
         */
        @RequiresApi(api = Build.VERSION_CODES.O)
        public void nextMusic() throws IOException {
            if ( position < GlobalNumber.musicPlayList.size() - 1) { // position < 最后一个的下标  防止下标越界错误 造成程序闪退
                try{
                    player.reset();
                    // MediaPlayer 状态重置
                    position++;
                    // position 位置加一
                    player.setDataSource("https://music.163.com/song/media/outer/url?id="+ GlobalNumber.musicPlayList.get(position).getId()+".mp3");
                    player.prepare();
                    player.start();
                    // MediaPlayer 加载音频资源，准备并播放
                    changeNotification(GlobalNumber.musicPlayList.get(position));
                    // 改变通知显示
                }catch (IOException e){
                    Toast.makeText(getApplicationContext(),"播放失败，好像资源不存在",Toast.LENGTH_SHORT).show();

                }

            }else{
                Toast.makeText(getApplicationContext(),"已经是最后一首了",Toast.LENGTH_SHORT).show();
            }
        }


        //返回歌曲的长度，单位为毫秒
        public int getDuration(){
            return player.getDuration();
        }

        //返回歌曲目前的进度，单位为毫秒
        public int getCurrenPostion(){
            return player.getCurrentPosition();
        }
        //返回歌曲目前的歌名
        public String getCurrenTitle(){
            return GlobalNumber.musicPlayList.get(position).getTitle();
        }
        //返回歌曲目前的作者
        public String getCurrenArt(){
            return GlobalNumber.musicPlayList.get(position).getArt();
        }
        //设置歌曲播放的进度，单位为毫秒
        public void seekTo(int mesc){
            player.seekTo(mesc);
        }
    }

}