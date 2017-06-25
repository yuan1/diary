package cn.javayuan.diary.activity;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.mzlion.core.lang.StringUtils;
import com.mzlion.easyokhttp.HttpClient;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.javayuan.diary.R;
import cn.javayuan.diary.utils.AppUtil;

public class SettingActivity extends AppCompatActivity {

    private final String UPDATE_PASSWORD_URL= AppUtil.URL+"/User/updatePassword";
    private ListView mSettingList;
    private List<Map<String, Object>> settingList =new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_setting);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        mSettingList= (ListView) findViewById(R.id.list_setting);
        initList();
        String[] strings = {"content"};//Map的key集合数组
        int[] ids = {R.id.setting_content};//对应布局文件的id
        SimpleAdapter simpleAdapter = new SimpleAdapter(SettingActivity.this,
                settingList, R.layout.list_setting, strings, ids);
        mSettingList.setAdapter(simpleAdapter);//绑定适配器
        mSettingList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(settingList.get(position).get("content").equals("修改密码")){
                    final EditText mInputNameText = new EditText(SettingActivity.this);
                    new AlertDialog.Builder(SettingActivity.this)
                            .setTitle("请输入新密码")
                            .setView(mInputNameText,80,80,80,0)
                            .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    String inputName = mInputNameText.getText().toString();
                                    new UpdatePasswordAsyncTask().execute(inputName);
                                }
                            })
                            .setNegativeButton("取消", null)
                            .show();
                }else if(settingList.get(position).get("content").equals("注销登录")){
                    Toast.makeText(SettingActivity.this,"注销成功！",Toast.LENGTH_LONG).show();
                    setLogout();
                }
            }
        });
    }

    private void initList(){
        HashMap<String ,Object> map1=new HashMap<>();
        map1.put("content","修改密码");
        HashMap<String ,Object> map2=new HashMap<>();
        map2.put("content","注销登录");
        settingList.add(map1);
        settingList.add(map2);
    }

    private class UpdatePasswordAsyncTask extends AsyncTask<String,Void,Boolean>{

        @Override
        protected Boolean doInBackground(String... params) {
            String jsonString=HttpClient.post(UPDATE_PASSWORD_URL)
                    .param("password",params[0])
                    .execute().asString();
            if(!StringUtils.isEmpty(jsonString)){
                try {
                    JSONObject jsonObject=new JSONObject(jsonString);
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
            if(aBoolean){
                setLogout();
            }
            Toast.makeText(SettingActivity.this,aBoolean?"修改成功，请重新登录":"修改失败",Toast.LENGTH_LONG).show();
        }
    }

    private void setLogout(){
        SharedPreferences sp=getSharedPreferences("Diary",MODE_PRIVATE);
        SharedPreferences.Editor editor=sp.edit();
        editor.putString("token","limingyuan");
        editor.apply();
        editor.commit();
        MainActivity.LOGIN_STATE=0;
        finish();
    }

}
