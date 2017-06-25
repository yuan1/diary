package cn.javayuan.diary.activity;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.mzlion.core.lang.StringUtils;
import com.mzlion.easyokhttp.HttpClient;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import cn.bingoogolapple.photopicker.activity.BGAPhotoPickerActivity;
import cn.bingoogolapple.photopicker.activity.BGAPhotoPickerPreviewActivity;
import cn.bingoogolapple.photopicker.widget.BGASortableNinePhotoLayout;
import cn.javayuan.diary.R;
import cn.javayuan.diary.bean.DiaryBean;
import cn.javayuan.diary.utils.AppUtil;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class AddDiaryActivity extends AppCompatActivity  implements EasyPermissions.PermissionCallbacks, BGASortableNinePhotoLayout.Delegate{
    private static final int REQUEST_CODE_PERMISSION_PHOTO_PICKER = 1;
    private static final int REQUEST_CODE_PHOTO_PREVIEW = 2;
    private static final int REQUEST_CODE_CHOOSE_PHOTO = 1;

    private final String[] mWtItems = {"晴","阴","多云","雨","雪","霾","雾","未知"};
    private static int WEATHER_SEL =0;
    //设置日历控件格式
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yy/MM/dd EE", Locale.CHINESE);
    private SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm", Locale.CHINESE);

    private TextView mTvWt, mTvDateTitle,mTvTimeTitle;
    //加载弹窗
    private ProgressDialog mProgressDialog;
    private final String IMAGE_UPLOAD_URL=AppUtil.URL+"/Daily/upload";
    private final String GET_DIARY_URL=AppUtil.URL+"/Daily/selectDaily";
    private final String DEAL_DIARY_URL=AppUtil.URL+"/Daily/dealDaily";
    private EditText mEdtDiaryContent;
    private static int EDIT_ID=0;
    public static final MediaType MEDIA_TYPE_IMAGE
            = MediaType.parse("image/png; charset=utf-8");

    private ArrayList<String> LIST_NOW_IMAGE_URL =new ArrayList<>();

    /**
     * 拖拽排序九宫格控件
     */
    private BGASortableNinePhotoLayout mPhotosSnpl;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_diary);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_add_diary);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog();
            }
        });
        mProgressDialog = ProgressDialog.show(this, null, "正在加载，请稍候...", true, false);
        mProgressDialog.dismiss();
        mTvWt= (TextView) findViewById(R.id.add_diary_wt);
        mTvWt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(AddDiaryActivity.this).setTitle("选择天气").setSingleChoiceItems(
                        mWtItems, WEATHER_SEL,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                WEATHER_SEL =which;
                                mTvWt.setText(mWtItems[which]);
                                dialog.dismiss();
                            }
                        }).setNegativeButton("取消", null).show();

            }
        });
        mEdtDiaryContent= (EditText) findViewById(R.id.add_diary_text);
        mTvDateTitle= (TextView) findViewById(R.id.add_diary_date_title);
        mTvDateTitle.setText(dateFormat.format(new Date()));
        mTvTimeTitle= (TextView) findViewById(R.id.add_diary_date_sub_title);
        mTvTimeTitle.setText(timeFormat.format(new Date()));
        mPhotosSnpl = (BGASortableNinePhotoLayout) findViewById(R.id.snpl_moment_add_photos);
        mPhotosSnpl.setDelegate(this);
        Intent intent=getIntent();
        Bundle bundle=intent.getExtras();
        if(bundle!=null){
            EDIT_ID=bundle.getInt("diaryId");
        }else {
            EDIT_ID=0;
        }
        //编辑状态
        if(EDIT_ID!=0){
            mProgressDialog.show();
            new GetDiaryAsyncTask().execute(EDIT_ID);
        }

    }



    /**
     * 显示提示框
     */
    private void alertDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(AddDiaryActivity.this);
        builder.setTitle("提示");
        builder.setMessage("确认放弃编辑？");
        builder.setPositiveButton("保存", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                saveDiary();
            }
        });

        builder.setNeutralButton("取消", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.dismiss();
            }
        });

        builder.setNegativeButton("放弃", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                setResult(RESULT_OK);
                finish();
            }
        });
        builder.create().show();
    }

    /**
     * 保存操作
     */
    private void saveDiary() {
        if(TextUtils.isEmpty(mEdtDiaryContent.getText())){
            Toast.makeText(AddDiaryActivity.this,"请输入内容",Toast.LENGTH_LONG).show();
        }else {
            mProgressDialog.show();
            new SaveDiaryAsyncTask().execute();
        }

    }

    @Override
    public void onBackPressed() {
        alertDialog();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_add_diary_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.add_diary_action_dis) {
            finish();
        }else if(id==R.id.add_diary_action_save){
            saveDiary();
        }else if(id==R.id.add_diary_action_classify){
            alertClassifyDialog();
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * 分类提示框
     */
    private void alertClassifyDialog() {
        new AlertDialog.Builder(AddDiaryActivity.this).setTitle("选择分类").setSingleChoiceItems(
                MainActivity.mClassifyItems, MainActivity.CLASSIFY_SEL,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        MainActivity.CLASSIFY_SEL =which;
                        dialog.dismiss();
                    }
                }).setNegativeButton("取消", null).show();
    }


    @Override
    public void onClickAddNinePhotoItem(BGASortableNinePhotoLayout sortableNinePhotoLayout, View view, int position, ArrayList<String> models) {
        choicePhotoWrapper();
    }

    @Override
    public void onClickDeleteNinePhotoItem(BGASortableNinePhotoLayout sortableNinePhotoLayout, View view, int position, String model, ArrayList<String> models) {
        LIST_NOW_IMAGE_URL.remove(position);
        mPhotosSnpl.removeItem(position);
    }

    @Override
    public void onClickNinePhotoItem(BGASortableNinePhotoLayout sortableNinePhotoLayout, View view, int position, String model, ArrayList<String> models) {
        startActivityForResult(BGAPhotoPickerPreviewActivity.newIntent(this, mPhotosSnpl.getMaxItemCount(), models, models, position, false), REQUEST_CODE_PHOTO_PREVIEW);
    }
    @AfterPermissionGranted(REQUEST_CODE_PERMISSION_PHOTO_PICKER)
    private void choicePhotoWrapper() {
        String[] perms = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA};
        if (EasyPermissions.hasPermissions(this, perms)) {
            // 拍照后照片的存放目录，改成你自己拍照后要存放照片的目录。如果不传递该参数的话就没有拍照功能
            File takePhotoDir = new File(Environment.getExternalStorageDirectory(), "Diary");
            startActivityForResult(BGAPhotoPickerActivity.newIntent(this, takePhotoDir, mPhotosSnpl.getMaxItemCount() - mPhotosSnpl.getItemCount(), null, false), REQUEST_CODE_CHOOSE_PHOTO);
        } else {
            EasyPermissions.requestPermissions(this, "图片选择需要以下权限:\n\n1.访问设备上的照片\n\n2.拍照", REQUEST_CODE_PERMISSION_PHOTO_PICKER, perms);
        }
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
        if (requestCode == REQUEST_CODE_PERMISSION_PHOTO_PICKER) {
            Toast.makeText(this, "您拒绝了「图片选择」所需要的相关权限!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == REQUEST_CODE_CHOOSE_PHOTO) {
            uploadImages(BGAPhotoPickerActivity.getSelectedImages(data));
            mPhotosSnpl.addMoreData(BGAPhotoPickerActivity.getSelectedImages(data));
        } else if (requestCode == REQUEST_CODE_PHOTO_PREVIEW) {
            mPhotosSnpl.setData(BGAPhotoPickerPreviewActivity.getSelectedImages(data));
        }
    }

    private void uploadImages(ArrayList<String> photos){
        mProgressDialog.show();
        for(String str:photos){
           new UploadImageAsyncTask().execute(str);
        }
    }

    private DiaryBean getDiaryJsonDataById(int id){
        String jsonString = HttpClient
                .post(GET_DIARY_URL)
                .param("id",String.valueOf(id))
                .execute()
                .asString();
        if(!StringUtils.isEmpty(jsonString)){
            try {
                JSONObject jsonObject=new JSONObject(jsonString);
                if(jsonObject.getInt("state")==0){
                    jsonObject=jsonObject.getJSONObject("data");
                    DiaryBean diaryBean=new DiaryBean();
                    diaryBean=AppUtil.convertJsonToDiaryBean(jsonObject);
                    return diaryBean;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
    private class GetDiaryAsyncTask extends AsyncTask<Integer,Void,DiaryBean>{
        @Override
        protected DiaryBean doInBackground(Integer... params) {
            return getDiaryJsonDataById(params[0]);
        }

        @Override
        protected void onPostExecute(DiaryBean diaryBean) {
            super.onPostExecute(diaryBean);
            if(diaryBean!=null){
                mEdtDiaryContent.setText(diaryBean.getContent());
                mEdtDiaryContent.setSelection(diaryBean.getContent().length());
                mTvDateTitle.setText(diaryBean.getCreateDate());
                mTvTimeTitle.setText(diaryBean.getCreateTime());
                if(diaryBean.getImages()!=null&&diaryBean.getImages().length>0){
                    for (int i = 0; i < diaryBean.getImages().length; i++) {
                        LIST_NOW_IMAGE_URL.add(diaryBean.getImages()[i]);
                        mPhotosSnpl.addLastItem(AppUtil.IMAGE_URL+diaryBean.getImages()[i]);
                    }
                }
                WEATHER_SEL =diaryBean.getWeather();
                mTvWt.setText(mWtItems[diaryBean.getWeather()]);
            }
            mProgressDialog.dismiss();

        }
    }
    
    private class UploadImageAsyncTask extends AsyncTask<String,Void,String>{
        @Override
        protected String doInBackground(String... params) {
            File file= new File(params[0]);
            RequestBody requestBody = new MultipartBody.Builder().setType(MultipartBody.FORM)
                    .addFormDataPart("image", file.getName(), RequestBody.create(MEDIA_TYPE_IMAGE, file))
                    .build();
            //创建Request
            final Request request = new Request.Builder().url(IMAGE_UPLOAD_URL).post(requestBody).build();
            OkHttpClient client = new OkHttpClient();
            Response response = null;
            try {
                response = client.newCall(request).execute();
                if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
                String jsonString =response.body().string();
                if(!StringUtils.isEmpty(jsonString)){
                    try {
                        JSONObject jsonObj=new JSONObject(jsonString);
                        if(jsonObj.getInt("state")==0){
                            return jsonObj.getString("data");
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            if(s!=null){
                LIST_NOW_IMAGE_URL.add(s);
            }
            mProgressDialog.dismiss();
        }
    }

    private class SaveDiaryAsyncTask extends AsyncTask<Void,Void,Boolean>{
        @Override
        protected Boolean doInBackground(Void... params) {
            String images="";
            for (int i = 0; i < LIST_NOW_IMAGE_URL.size(); i++) {
                images+=LIST_NOW_IMAGE_URL.get(i)+",";
            }
            String jsonString=HttpClient.post(DEAL_DIARY_URL)
                    .param("id",String.valueOf(EDIT_ID))
                    .param("content",mEdtDiaryContent.getText().toString())
                    .param("images",images.length()>0?images.substring(0,images.length()-1):images)
                    .param("createDate",mTvDateTitle.getText().toString())
                    .param("createTime",mTvTimeTitle.getText().toString())
                    .param("weather",String.valueOf(WEATHER_SEL))
                    .param("tag", MainActivity.mClassifyItems[MainActivity.CLASSIFY_SEL])
                    .execute().asString();
            if(!StringUtils.isEmpty(jsonString)){
                try {
                    JSONObject jsonObject = new JSONObject(jsonString);
                    if(jsonObject.getInt("state")==0){
                        return true;
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
            mProgressDialog.dismiss();
            if(aBoolean){
                Toast.makeText(AddDiaryActivity.this,EDIT_ID==0?"添加成功！":"修改成功",Toast.LENGTH_LONG).show();
                finish();
            }else {
                Toast.makeText(AddDiaryActivity.this,EDIT_ID==0?"添加失败！":"修改失败",Toast.LENGTH_LONG).show();
            }
        }
    }


}
