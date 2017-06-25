package cn.javayuan.diary.bean;

/**
 * 日记
 * Created by lmy on 2017/6/18.
 */

public class DiaryBean {
    private int id;
    private String content;
    private String[] images;
    private String createTime;
    private String createDate;
    private String createYear;

    public DiaryBean() {
    }

    public DiaryBean(String createYear, String createDate, String createTime, String[] images) {
        this.images = images;
        this.createTime = createTime;
        this.createDate = createDate;
        this.createYear = createYear;
    }

    public String getCreateYear() {
        return createYear;
    }

    public void setCreateYear(String createYear) {
        this.createYear = createYear;
    }

    private int type;
    private int weather;

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getWeather() {
        return weather;
    }

    public void setWeather(int weather) {
        this.weather = weather;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }


    public String[] getImages() {
        return images;
    }

    public void setImages(String[] images) {
        this.images = images;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public String getCreateDate() {
        return createDate;
    }

    public void setCreateDate(String createDate) {
        this.createDate = createDate;
    }
}
