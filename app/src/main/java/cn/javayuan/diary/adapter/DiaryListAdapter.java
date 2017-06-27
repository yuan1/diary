package cn.javayuan.diary.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.mzlion.core.lang.StringUtils;

import java.util.Arrays;
import java.util.List;

import cn.javayuan.diary.bean.DiaryBean;
import cn.javayuan.diary.R;
import cn.javayuan.diary.utils.AppUtil;

/**
 * Created by lmy on 2017/6/18.
 */

public class DiaryListAdapter extends BaseAdapter {

    private List<DiaryBean> list;
    private LayoutInflater inflater;

    public DiaryListAdapter(Context context,List<DiaryBean> data){
        list=data;
        inflater=LayoutInflater.from(context);

    }
    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int position) {
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder=null;
        if(convertView==null){
            viewHolder = new ViewHolder();
            convertView=inflater.inflate(R.layout.list_main,null);
            viewHolder.image= (ImageView) convertView.findViewById(R.id.main_list_image);
            viewHolder.date= (TextView) convertView.findViewById(R.id.main_list_date);
            viewHolder.content= (TextView) convertView.findViewById(R.id.main_list_content);
            viewHolder.time= (TextView) convertView.findViewById(R.id.main_list_time);
            convertView.setTag(viewHolder);
        }else {
            viewHolder= (ViewHolder) convertView.getTag();
        }
        viewHolder.image.setImageResource(R.mipmap.bga_pp_ic_holder_dark);
        viewHolder.image.setVisibility(View.GONE);
        if(list.get(position).getImages()!=null&&list.get(position).getImages().length>0&&!list.get(position).getImages().equals("null")){
            String url=list.get(position).getImages()[0];
            if(!StringUtils.isEmpty(url)) {
                viewHolder.image.setVisibility(View.VISIBLE);
                Glide.with(convertView.getContext())
                        .load(AppUtil.IMAGE_URL+url)
                        .into(viewHolder.image);
            }
        }
        viewHolder.time.setText(list.get(position).getCreateTime());
        viewHolder.content.setText(list.get(position).getContent());
        viewHolder.date.setText(list.get(position).getCreateDate());
        return convertView;
    }

    private class ViewHolder{
        public ImageView image;
        public TextView date,content,time;

    }
}
