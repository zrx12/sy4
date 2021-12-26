package cn.edu.bistu.music.empty;


import java.util.Objects;

/***
 * 音乐实体类
 * 记录了 音乐的id，用于在 网易云中取得mp3文件
 * 音乐的标题
 * 音乐的作者
 * 音乐的专辑
 */
public class Music {
    private String id;
    private String title;
    private String art;
    private String album;
    public Music(){}

    public String getAlbum() {
        return album;
    }

    public String getArt() {
        return art;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public void setArt(String art) {
        this.art = art;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public String toString() {
        return "Music {" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", art='" + art + '\'' +
                ", album='" + album + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Music music = (Music) o;
        System.out.println(music.toString());
        return id.equals(music.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
