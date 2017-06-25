package cn.javayuan.diary.activity;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.mzlion.core.lang.StringUtils;
import com.mzlion.easyokhttp.HttpClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cn.bingoogolapple.androidcommon.adapter.BGARecyclerViewAdapter;
import cn.bingoogolapple.androidcommon.adapter.BGAViewHolderHelper;
import cn.bingoogolapple.photopicker.activity.BGAPhotoPreviewActivity;
import cn.bingoogolapple.photopicker.imageloader.BGARVOnScrollListener;
import cn.bingoogolapple.photopicker.widget.BGANinePhotoLayout;
import cn.javayuan.diary.R;
import cn.javayuan.diary.bean.DiaryBean;
import cn.javayuan.diary.utils.AppUtil;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class ImagesActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks, BGANinePhotoLayout.Delegate{
    private static final int REQUEST_CODE_PERMISSION_PHOTO_PREVIEW = 1;
    private static final String IMAGE_URL = AppUtil.URL+"/Daily/imagesList";
    private static final int REQUEST_CODE_ADD_MOMENT = 1;
    private RecyclerView mImagesRv;
    private ImagesAdapter mImagesAdapter;
    //加载弹窗
    private ProgressDialog mProgressDialog;
    private BGANinePhotoLayout mCurrentClickNpl;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_images);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_images);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        setTitle("图库");
        mProgressDialog = ProgressDialog.show(this, null, "正在加载，请稍候...", true, false);
        mProgressDialog.show();
        mImagesRv = (RecyclerView) findViewById(R.id.list_images);
        mImagesAdapter = new ImagesAdapter(mImagesRv);
        mImagesRv.addOnScrollListener(new BGARVOnScrollListener(this));
        mImagesRv.setLayoutManager(new LinearLayoutManager(this));
        mImagesRv.setAdapter(mImagesAdapter);
        new ImagesAsyncTask().execute();
    }
    @Override
    public void setTitle(CharSequence title) {
        TextView textView= (TextView) findViewById(R.id.toolbar_images_title);
        if(title!=null&&!title.equals("")){
            textView.setText(title);
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == REQUEST_CODE_ADD_MOMENT) {
            mImagesRv.smoothScrollToPosition(0);
        }
    }
    /**
     * 图片预览，兼容6.0动态权限
     */
    @AfterPermissionGranted(REQUEST_CODE_PERMISSION_PHOTO_PREVIEW)
    private void photoPreviewWrapper() {
        if (mCurrentClickNpl == null) {
            return;
        }

        // 保存图片的目录，改成你自己要保存图片的目录。如果不传递该参数的话就不会显示右上角的保存按钮
        File downloadDir = new File(Environment.getExternalStorageDirectory(), "Diary");

        String[] perms = {Manifest.permission.WRITE_EXTERNAL_STORAGE};
        if (EasyPermissions.hasPermissions(this, perms)) {
            if (mCurrentClickNpl.getItemCount() == 1) {
                // 预览单张图片

                startActivity(BGAPhotoPreviewActivity.newIntent(this, downloadDir, mCurrentClickNpl.getCurrentClickItem()));
            } else if (mCurrentClickNpl.getItemCount() > 1) {
                // 预览多张图片

                startActivity(BGAPhotoPreviewActivity.newIntent(this,downloadDir, mCurrentClickNpl.getData(), mCurrentClickNpl.getCurrentClickItemPosition()));
            }
        } else {
            EasyPermissions.requestPermissions(this, "图片预览需要以下权限:\n\n1.访问设备上的照片", REQUEST_CODE_PERMISSION_PHOTO_PREVIEW, perms);
        }
    }
    @Override
    public void onClickNinePhotoItem(BGANinePhotoLayout ninePhotoLayout, View view, int position, String model, List<String> models) {
        mCurrentClickNpl = ninePhotoLayout;
        photoPreviewWrapper();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
        if (requestCode == REQUEST_CODE_PERMISSION_PHOTO_PREVIEW) {
            Toast.makeText(this, "您拒绝了「图片预览」所需要的相关权限!", Toast.LENGTH_SHORT).show();
        }
    }

    private class ImagesAdapter extends BGARecyclerViewAdapter<DiaryBean> {

        public ImagesAdapter(RecyclerView recyclerView) {
            super(recyclerView, R.layout.list_images);
        }

        @Override
        protected void fillData(BGAViewHolderHelper helper, int position, DiaryBean model) {
            if(model.getImages()!=null){
                helper.setText(R.id.list_images_date,model.getCreateDate());
                BGANinePhotoLayout ninePhotoLayout = helper.getView(R.id.list_images_photos);
                ninePhotoLayout.setDelegate(ImagesActivity.this);
                ninePhotoLayout.setData(new ArrayList<String>(Arrays.asList(model.getImages())));
            }
        }
    }

    private class ImagesAsyncTask extends AsyncTask<Void,Void,List<DiaryBean>>{
        @Override
        protected List<DiaryBean> doInBackground(Void... params) {
            String jsonString = HttpClient.post(IMAGE_URL).execute().asString();
            if(!StringUtils.isEmpty(jsonString)){
                try {
                    JSONObject jsonObject=new JSONObject(jsonString);
                    if(jsonObject.getInt("state")==0){
                        List<DiaryBean> diaryBeanList = new ArrayList<>();
                        JSONArray jsonArray=jsonObject.getJSONArray("data");
                        for (int i = 0; i <jsonArray.length() ; i++) {
                            jsonObject=jsonArray.getJSONObject(i);
                            if(!StringUtils.isEmpty(jsonObject.getString("images"))){
                                DiaryBean diaryBean=new DiaryBean();
                                diaryBean.setCreateDate(jsonObject.getString("time"));
                                diaryBean.setImages(jsonObject.getString("images").split(","));
                                diaryBeanList.add(diaryBean);
                            }
                        }
                        return diaryBeanList;

                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                
            }
            return null;
        }

        @Override
        protected void onPostExecute(List<DiaryBean> diaryBeen) {
            super.onPostExecute(diaryBeen);
            mProgressDialog.dismiss();
            if(diaryBeen!=null){
                for (DiaryBean bean : diaryBeen) {
                    if(bean.getImages()!=null){
                        String [] images=new String[bean.getImages().length];
                        for (int i = 0; i <bean.getImages().length ; i++) {
                            images[i]=AppUtil.IMAGE_URL+bean.getImages()[i];
                        }
                        bean.setImages(images);
                    }
                }
                mImagesAdapter.setData(diaryBeen);
            }
        }
    }

}
