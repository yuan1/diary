package cn.javayuan.diary.utils;

import com.mzlion.core.lang.StringUtils;

import org.json.JSONException;
import org.json.JSONObject;

import cn.javayuan.diary.bean.DiaryBean;

/**
 * 主要用配置服务器信息
 * Created by lmy on 2017/6/19.
 */

public class AppUtil {
    public static String URL="http://123.206.84.232/daily/index.php/Home";
    public static String IMAGE_URL="http://123.206.84.232/daily/Uploads/";

    public static DiaryBean convertJsonToDiaryBean(JSONObject jsonObject){
        DiaryBean diaryBean=new DiaryBean();
        try {
            diaryBean.setId(jsonObject.getInt("id"));
            diaryBean.setCreateTime(jsonObject.getString("createtime"));
            diaryBean.setCreateDate(jsonObject.getString("createdate"));
            diaryBean.setCreateYear(jsonObject.getString("createyear"));
            diaryBean.setContent(jsonObject.getString("content"));
            if(!StringUtils.isEmpty(jsonObject.getString("images"))){
                diaryBean.setImages(jsonObject.getString("images").split(","));
            }
            diaryBean.setType(jsonObject.getInt("type"));
            diaryBean.setWeather(jsonObject.getInt("weather"));
            return diaryBean;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
}
