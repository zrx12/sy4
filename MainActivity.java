package cn.edu.bistu.music;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import cn.edu.bistu.music.bean.Artist;
import cn.edu.bistu.music.bean.Artists;
import cn.edu.bistu.music.bean.CheckBean;
import cn.edu.bistu.music.bean.JsonRootBean;
import cn.edu.bistu.music.bean.Songs;
import cn.edu.bistu.music.empty.Music;
import cn.edu.bistu.music.services.MusicPlayService;
import cn.edu.bistu.music.util.GlobalNumber;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    private LayoutInflater inflater; // 用于动态 生成组件 添加到 LinearLayout 中
    private EditText searchText; // search EditText
    private LinearLayout searchResult; // 搜索结果的展示  LinearLayout
    private Button searchButton;
    private Button nextButton;
    private Button preButtom;
    private Button playListBButton;
    private Button playButton;
    private TextView playInformation;
    private ScrollView searchResultScroll; // 显示搜索结果的ScrollView
    private ScrollView playListScroll;// 显示播放队列的ScrollView
    private LinearLayout playList;// 播放队列的展示  LinearLayout

    private SeekBar seekBar; // 进度条

    private int limit = 20,offset = 0;
    // 这个 画的饼，在这个程序上没啥实意，用于搜索结果的分页
    private ArrayList<Music> musics ;
    // 用于搜索结果富人Music 列表

    private MyConnection conn;
    // ServiceConnection 用于连接 MusicPlayService

    private MusicPlayService.MusicControl musicControl;
    // MusicPlayService 中的 MusicControl 的Binder ， 用于控制音乐的播放

    private static final int UPDATE_PROGRESS = 0;
    // 更新进度条

    //使用handler定时更新进度条
    @SuppressLint("HandlerLeak")
    private final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == UPDATE_PROGRESS) {
                updateProgress();
            }
        }
    };

    // 更新播放下面显示的提示字
    @SuppressLint("SetTextI18n")
    private void updatePlayerInformation() {
        playInformation.setText("当前播放:"+musicControl.getCurrenTitle()+": "+musicControl.getCurrenArt());
    }

    //更新进度条
    private void updateProgress() {
        int currenPostion = musicControl.getCurrenPostion();
        seekBar.setProgress(currenPostion);
        //使用Handler每500毫秒更新一次进度条
        handler.sendEmptyMessageDelayed(UPDATE_PROGRESS, 500);
    }

    /***
     * 检查音乐是否可用
     * @param id 歌曲id
     * @throws IOException
     */
    private void checkMusic(String id) throws IOException {
        String url = "http://api.we-chat.cn/check/music?id="+id;
        // API url
        OkHttpClient okHttpClient = new OkHttpClient();
        final Request request = new Request.Builder()
                .url(url)
                .build();
        final Call call = okHttpClient.newCall(request);
        // OkHttp 对象,这里我采用同步的方法，由于主线程不能网络访问，当然要开个子线程啦，这和异步写法作用上一样的

        new Thread(new Runnable() {
            CheckBean checkBean2 = null;
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void run() {
                try {
                    Response response = call.execute(); // 发起请求
                    String text = response.body().string(); // 获取 服务器返回的结果
                    checkBean2 = JSONObject.parseObject(text, CheckBean.class);
                    if (!checkBean2.getSuccess()) {
                        runOnUiThread(()->{
                            Toast.makeText(getApplicationContext(),checkBean2.getMessage(),Toast.LENGTH_SHORT).show();
                        });
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    @Override
    protected void onStop() {
        super.onStop();
        handler.removeCallbacksAndMessages(null);
        // 停止更新进度条
    }

    private class MyConnection implements ServiceConnection {
        //服务启动完成后会进入到这个方法
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            //获得service中的MyBinder
            musicControl = (MusicPlayService.MusicControl) service;
            //设置进度条的最大值
            seekBar.setMax(musicControl.getDuration());
            //设置进度条的进度
            seekBar.setProgress(musicControl.getCurrenPostion());
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    }

    /***
     * 在搜索的线性布局上显示搜索结果
     * @param linearLayout 需要插入的布局
     * @param musics 音乐列表数据
     */
    private void initSearchResult(LinearLayout linearLayout,ArrayList<Music> musics){
        for (Music m: musics) { // 遍历
            // 通过 inflater 用名叫 musicitems 的布局文件 创建一个view 用于添加到 LinearLayout
            View view = inflater.inflate(
                    R.layout.musicitems,
                    linearLayout, false
            );
            // 布局文件中的三个组件。
            TextView title = view.findViewById(R.id.title);
            TextView art = view.findViewById(R.id.art);
            TextView album = view.findViewById(R.id.album);
            // 把布局文件中的三个组件赋值上音乐信息
            title.setText(m.getTitle());
            art.setText(m.getArt());
            album.setText(m.getAlbum());

            // 这个自定义组件(搜索结果)的点击响应事件
            view.setOnClickListener(
                    new View.OnClickListener() {
                        @RequiresApi(api = Build.VERSION_CODES.O)
                        @Override
                        public void onClick(View v) {
                            // 打印当前点击的音乐数据
                            System.out.println(m.toString());

                            System.out.println(GlobalNumber.musicPlayList.contains(m));
                            if (!GlobalNumber.musicPlayList.contains(m)) {
                                // 同时再创建一个自定义组件view2，并添加到播放队列的视图中
                                View view2 = inflater.inflate(
                                        R.layout.musicitems,
                                        playList, false
                                );
                                // 和上面一样
                                TextView title = view2.findViewById(R.id.title);
                                TextView art = view2.findViewById(R.id.art);
                                TextView album = view2.findViewById(R.id.album);
                                title.setText(m.getTitle());
                                art.setText(m.getArt());
                                album.setText(m.getAlbum());
                                System.out.println(Arrays.toString(GlobalNumber.musicPlayList.toArray()));

                                playList.addView(view2);
                                //自定义组件view2的响应事件
                                view2.setOnClickListener(
                                        new View.OnClickListener() {
                                            @RequiresApi(api = Build.VERSION_CODES.O)
                                            @Override
                                            public void onClick(View v) {
                                                try {
                                                    checkMusic(m.getId());
                                                    // GlobalNumber.musicPlayList.indexOf(m) 找到这首歌在播放队列中的位置
                                                    // 并用 playbyID  根据目前音乐在队列中的位置，播放音乐。 在播放队列中点击任意音乐播放
                                                    musicControl.playbyID(GlobalNumber.musicPlayList.indexOf(m));
                                                    seekBar.setMax(musicControl.getDuration());
                                                    updateProgress(); // 更新进度
                                                    updatePlayerInformation(); // 更新 播放信息
                                                } catch (IOException ioException) {
                                                    ioException.printStackTrace();
                                                }

                                            }
                                        }
                                );
                            }

                            // 添加到播放列表
                            try {
                                  musicControl.addMusic(m);

                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                            // 表明这是播放列表第一个
                            if (GlobalNumber.musicPlayList.size() == 1) {
                                //设置进度条的最大值
                                seekBar.setMax(musicControl.getDuration());
                                updateProgress(); // 更新进度
                                updatePlayerInformation(); // 更新 播放信息
                                playButton.setText("暂停");
                            }


                        }
                    }
            );

            // 最后把这个自定义组件添加到线性布局（搜索结果）中
            linearLayout.addView(view);



        }

    }

    /**
     * 搜索,通过网络请求，获取搜索结果
     * @throws IOException
     */
    private  void search() throws IOException {
        String url = "http://api.we-chat.cn/search?keywords="+searchText.getText().toString()+"&limit="+limit+"&offset="+offset;
        // API url
        musics = new ArrayList<>();
        // 初始化搜索结果列表
        OkHttpClient okHttpClient = new OkHttpClient();
        final Request request = new Request.Builder()
                .url(url)
                .build();
        final Call call = okHttpClient.newCall(request);
        // OkHttp 对象,这里我采用同步的方法，由于主线程不能网络访问，当然要开个子线程啦，这和异步写法作用上一样的
        new Thread(new Runnable() {
            @Override
            public void run() {
                final JsonRootBean  jsonRootBean;
                try {
                    Response response = call.execute(); // 发起请求
                    String text = response.body().string(); // 获取 服务器返回的结果
                    jsonRootBean = JSONObject.parseObject(text, JsonRootBean.class);
                    // 通过 fastjson 将 字符串 解析成 JsonRootBean 的实体类
                    // 遍历 返回结果 JsonRootBean 里面 的列表
                    if(jsonRootBean.getResult().getSongCount()==0){

                        runOnUiThread(
                                () -> {
                                    Toast.makeText(getApplicationContext(),"未找到歌曲",Toast.LENGTH_SHORT).show();
                                    searchResultScroll.setVisibility(View.GONE);
                                    playListScroll.setVisibility(View.VISIBLE);
                                }
                        );
                    }else {
                        for (Songs song : jsonRootBean.getResult().getSongs()) {
                            Music music = new Music();
                            // 初始化一个音乐对象
                            music.setId(String.valueOf(song.getId()));
                            // 设置music id
                            music.setTitle(song.getName());
                            // 设置music 歌名
                            String author = "";
                            // 音乐作者可能是多作者，
                            // 通过遍历把 所有作者添加到 author 字符串中
                            // 并且添加到 music对象中
                            for (Artists artist : song.getArtists()) {
                                author += " " + artist.getName();
                            }
                            music.setArt(author);
                            // 设置music 专辑
                            music.setAlbum(song.getAlbum().getName());
                            // 添加到搜索结果的列表
                            musics.add(music);

                        }
                        // 在UI线程中执行的操作
                        runOnUiThread(
                                () -> {
                                    // Log 打印搜索结果的列表
                                    System.out.println(Arrays.toString(musics.toArray()));
                                    // 防止重复添加，清空一下搜索结果 的线性布局组件
                                    searchResult.removeAllViews();
                                    // 调用上面那个initSearchResult
                                    initSearchResult(searchResult, musics);
                                }
                        );
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();

    }


    @Override
    protected void onResume() {
        super.onResume();
        //进入到界面后开始更新进度条
        if (musicControl != null){
            handler.sendEmptyMessage(UPDATE_PROGRESS);
        }
    }

    /***
     * Init 初始化组件
     */
    private  void initViews(){
        searchText = findViewById(R.id.search_editText);
        searchButton = findViewById(R.id.search_buttons);
        playButton = findViewById(R.id.playMusic);
        playListBButton = findViewById(R.id.playlist);
        preButtom = findViewById(R.id.preMusic);
        nextButton = findViewById(R.id.nextMusic);

        playList = findViewById(R.id.play_list);
        playListScroll = findViewById(R.id.play_list_scroll);
        searchResultScroll   = findViewById(R.id.search_result_scroll);

        searchResult = findViewById(R.id.search_result);
        inflater = LayoutInflater.from(this);
        seekBar = findViewById(R.id.seekbar);
        playInformation = findViewById(R.id.nowMusic);

        // 初始化播放队列
        GlobalNumber.musicPlayList = new ArrayList<>();

        //使用混合的方法开启服务，
        Intent intent3 = new Intent(this, MusicPlayService.class);
        conn = new MyConnection();
        startService(intent3);
        bindService(intent3, conn, BIND_AUTO_CREATE);

    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(conn);
        // 销毁（关闭） 应用的时候 结束播放服务
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews(); // 初始化组件

        // 搜索按钮的响应
        searchButton.setOnClickListener(e->{
            playListScroll.setVisibility(View.GONE);
            // 播放队列的ScrollView 设置为隐藏，搜索结果的ScrollView 设置为显示
            searchResultScroll.setVisibility(View.VISIBLE);
            try {
                search(); // 执行 上面的search 方法，开始搜索
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        });

        playListBButton.setOnClickListener(e->{
            // 播放队列的ScrollView 设置为显示，搜索结果的ScrollView 设置为隐藏
            searchResultScroll.setVisibility(View.GONE);
            playListScroll.setVisibility(View.VISIBLE);
        });
        // 上一首
        preButtom.setOnClickListener(e->{
            try {
                musicControl.preMusic();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
            seekBar.setMax(musicControl.getDuration());
            updateProgress(); // 更新进度
            updatePlayerInformation(); // 更新 播放信息
        });
        // 下一首
        nextButton.setOnClickListener(e->{

            try {
                musicControl.nextMusic();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
            seekBar.setMax(musicControl.getDuration());
            updateProgress(); // 更新进度
            updatePlayerInformation(); // 更新 播放信息
        });

        // 控制播放暂停
        playButton.setOnClickListener(e->{
            boolean isPlay = musicControl.isPlaying();
            if (isPlay) {
                playButton.setText("播放");
                musicControl.play();
            }else{
                playButton.setText("暂停");
                musicControl.play();
                seekBar.setMax(musicControl.getDuration());
                seekBar.setProgress(musicControl.getCurrenPostion());
            }

        });

        // 这里是音乐拖动 进度的实现
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                //进度条改变
                if (fromUser){
                    musicControl.seekTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                //开始触摸进度条
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                //停止触摸进度条
            }
        });




    }
}