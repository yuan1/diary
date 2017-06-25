package cn.javayuan.diary.activity;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.mzlion.core.lang.StringUtils;
import com.mzlion.easyokhttp.HttpClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.javayuan.diary.R;
import cn.javayuan.diary.utils.AppUtil;

public class ClassifyActivity extends AppCompatActivity {
    private static final String CLASSIFY_LIST_URL = AppUtil.URL+"/Tag/tagList";
    private static final String CLASSIFY_ADD_URL = AppUtil.URL+"/Tag/addTag";
    private static final String CLASSIFY_DELETE_URL = AppUtil.URL+"/Tag/deleteTag";
    private ListView mClassifyListView;
    private List<Map<String, Object>> classifyList;
    //加载弹窗
    private ProgressDialog mProgressDialog;
    private TextView mTvAddClassify;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_classify);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_classify);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        mProgressDialog = ProgressDialog.show(this, null, "正在加载，请稍候...", true, false);
        mProgressDialog.show();
        mClassifyListView= (ListView) findViewById(R.id.classify_list);
        mClassifyListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                final String name=classifyList.get(position).get("name").toString();
                new AlertDialog.Builder(ClassifyActivity.this)
                        .setTitle("提示")
                        .setMessage("确认删除？")
                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mProgressDialog.show();
                                new DeleteClassifyAsyncTask().execute(name);
                            }
                        })
                        .setNegativeButton("取消", null)
                        .show();
                return true;
            }
        });
        mTvAddClassify= (TextView) findViewById(R.id.classify_add);
        mTvAddClassify.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final EditText mClassifyText = new EditText(ClassifyActivity.this);
                new AlertDialog.Builder(ClassifyActivity.this)
                        .setTitle("添加分类")
                        .setView(mClassifyText,80,80,80,0)
                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String input = mClassifyText.getText().toString();
                                if(!StringUtils.isEmpty(input)){
                                    mProgressDialog.show();
                                    new AddClassifyAsyncTask().execute(input);
                                }
                            }
                        })
                        .setNegativeButton("取消", null)
                        .show();
            }
        });
        new ClassifyAsyncTask().execute();
    }

    private class ClassifyAsyncTask extends AsyncTask<Void,Void,Boolean> {
        @Override
        protected Boolean  doInBackground(Void... params) {
            classifyList =new ArrayList<>();
            String jsonString=HttpClient.post(CLASSIFY_LIST_URL)
                    .execute().asString();
            if(!StringUtils.isEmpty(jsonString)){
                try {
                    JSONObject jsonObj=new JSONObject(jsonString);
                    if(jsonObj.getInt("state")==0){
                        JSONArray jsonArray=jsonObj.getJSONArray("data");
                        for (int i = 0; i <jsonArray.length(); i++) {
                            Map<String, Object> map = new HashMap<>();
                            jsonObj=jsonArray.getJSONObject(i);
                            map.put("name", jsonObj.getString("tag"));
                            map.put("count", jsonObj.getString("count")+"篇");
                            classifyList.add(map);
                        }
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
            mProgressDialog.dismiss();
            super.onPostExecute(aBoolean);
            if(aBoolean){
                String[] strings = {"name","count"};//Map的key集合数组
                int[] ids = {R.id.classify_list_name,R.id.classify_list_count};//对应布局文件的id
                SimpleAdapter simpleAdapter = new SimpleAdapter(ClassifyActivity.this,
                        classifyList, R.layout.list_classify, strings, ids);
                mClassifyListView.setAdapter(simpleAdapter);//绑定适配器
            }
        }
    }
    private class AddClassifyAsyncTask extends AsyncTask<String,Void,Boolean> {
        @Override
        protected Boolean  doInBackground(String... params) {
           String jsonString =HttpClient.post(CLASSIFY_ADD_URL)
                    .param("tag",params[0])
                    .execute().asString();
            if(!StringUtils.isEmpty(jsonString)){
                try {
                    JSONObject jsonObj=new JSONObject(jsonString);
                    if(jsonObj.getInt("state")==0){
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
            mProgressDialog.dismiss();
            super.onPostExecute(aBoolean);
            Toast.makeText(ClassifyActivity.this,aBoolean?"添加分类成功！":"添加失败，分类名称已存在？",Toast.LENGTH_LONG).show();
            if(aBoolean){
                mProgressDialog.show();
                new ClassifyAsyncTask().execute();
            }

        }
    }

    private class DeleteClassifyAsyncTask extends AsyncTask<String,Void,Boolean>{

        @Override
        protected Boolean doInBackground(String... params) {
            String jsonString =HttpClient.post(CLASSIFY_DELETE_URL)
                    .param("tag",params[0])
                    .execute().asString();
            if(!StringUtils.isEmpty(jsonString)){
                try {
                    JSONObject jsonObj=new JSONObject(jsonString);
                    if(jsonObj.getInt("state")==0){
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
            Toast.makeText(ClassifyActivity.this,aBoolean?"删除成功":"删除失败",Toast.LENGTH_LONG).show();
            if(aBoolean){
                mProgressDialog.show();
                new ClassifyAsyncTask().execute();
            }
        }
    }
}
