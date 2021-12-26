package cn.edu.bistu.music.util;

import java.util.ArrayList;
import java.util.List;

import cn.edu.bistu.music.empty.Music;

/**
 * 全局变量
 */
public class GlobalNumber {

    public static List<Music> musicPlayList = new ArrayList<>();
    // 播放队列，所有的播放操作，如上一首、下一首、添加队列，都是基于此进行的 动态修改组件

}
