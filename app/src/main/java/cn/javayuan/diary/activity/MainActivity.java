package cn.javayuan.diary.activity;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.github.sundeepk.compactcalendarview.CompactCalendarView;
import com.mzlion.core.lang.StringUtils;
import com.mzlion.easyokhttp.HttpClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import cn.javayuan.diary.R;
import cn.javayuan.diary.adapter.DiaryListAdapter;
import cn.javayuan.diary.bean.DiaryBean;
import cn.javayuan.diary.bean.UserBean;
import cn.javayuan.diary.utils.AppUtil;
import cn.javayuan.diary.utils.NetWorkUtil;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {
    //加载弹窗
    private ProgressDialog mProgressDialog;
    //app bar
    private AppBarLayout mAppBarLayout;

    public static String[] mClassifyItems = {"默认"};

    public static int CLASSIFY_SEL =0;
    //设置日历控件格式
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yy/MM/dd EE", Locale.CHINESE);

    private CompactCalendarView mCompactCalendarView;

    private ListView mListView;

    private static final String LIST_DIARY_URL = AppUtil.URL+"/Daily/dailyList";
    private static final String DEL_DIARY_URL = AppUtil.URL+"/Daily/deleteDaily";
    private static final String TOKEN_LOGIN_URL = AppUtil.URL+"/Login/tokenLogin";
    private static final String USER_UPDATE_NAME_URL = AppUtil.URL+"/User/updateName";
    private static final String USER_UPDATE_DESC_URL = AppUtil.URL+"/User/updateDesc";
    private static final String CLASSIFY_LIST_URL = AppUtil.URL+"/Tag/tagList";
    private boolean isExpanded = false;

    private static final int    ADD    = 0;
    private static final int    EDIT    = 1;
    private static final int    LOGIN    = 2;
    private static final int IMAGES=3;
    private static final int CLASSIFY=4;
    private static final int SETTING=5;
    private TextView mTvNavHeadName,mTvNavHeadDesc,mTvMainCount;

    //登录成功1
    public static int LOGIN_STATE=0;
    private DiaryListAdapter diaryListAdapter;

    //空为当天
    private static String NOW_DATE_TIME="";

    public static int NOW_USER_ID=0;

    private static String COUNT_MAIN_STR;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_main);
        setSupportActionBar(toolbar);
        //添加按钮
        FloatingActionButton diary_add = (FloatingActionButton) findViewById(R.id.diary_add);
        diary_add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
               Intent intent=new Intent(MainActivity.this,AddDiaryActivity.class);
                startActivityForResult(intent, ADD);
            }
        });
        //设置一个progressdialog的加载弹窗
        mTvMainCount= (TextView) findViewById(R.id.main_count);
        mProgressDialog = ProgressDialog.show(this, null, "正在加载，请稍候...", true, false);
        mProgressDialog.show();
        //ListView 设置
        mListView = (ListView) findViewById(R.id.content_list);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                int diaryId=((DiaryBean)diaryListAdapter.getItem(position)).getId();
                Intent intent=new Intent(MainActivity.this,AddDiaryActivity.class);
                Bundle bundle=new Bundle();
                bundle.putInt("diaryId",diaryId);
                intent.putExtras(bundle);
                startActivityForResult(intent,EDIT);
            }
        });
        mListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                if(LOGIN_STATE==1){
                    final  int diaryId=((DiaryBean)diaryListAdapter.getItem(position)).getId();
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("提示")
                            .setMessage("确认删除？")
                            .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    mProgressDialog.show();
                                    new DeleteDiaryAsyncTask().execute(diaryId);
                                }
                            })
                            .setNegativeButton("取消", null)
                            .show();
                }
                return true;
            }
        });

        //侧滑栏
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        //获取nav head view
        View headView=navigationView.getHeaderView(0);
        mTvNavHeadName= (TextView) headView.findViewById(R.id.nav_header_name);
        mTvNavHeadName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(LOGIN_STATE==1){
                    final EditText mNavHeadNameText = new EditText(MainActivity.this);
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("修改昵称")
                            .setView(mNavHeadNameText,80,80,80,0)
                            .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    String inputName = mNavHeadNameText.getText().toString();
                                    if(!StringUtils.isEmpty(inputName)){
                                        new UpdateNameAsyncTask().execute(inputName);
                                    }
                                }
                            })
                            .setNegativeButton("取消", null)
                            .show();
                }
            }
        });
        mTvNavHeadDesc= (TextView) headView.findViewById(R.id.nav_header_desc);
        mTvNavHeadDesc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(LOGIN_STATE==1){
                    final EditText mNavHeadDescText = new EditText(MainActivity.this);
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("修改简介")
                            .setView(mNavHeadDescText,80,80,80,0)
                            .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    String inputDesc = mNavHeadDescText.getText().toString();
                                    if(!StringUtils.isEmpty(inputDesc)){
                                        new UpdateDescAsyncTask().execute(inputDesc);
                                    }
                                }
                            })
                            .setNegativeButton("取消", null)
                            .show();
                }
            }
        });
        //设置toolbar中的日历
        mAppBarLayout = (AppBarLayout) findViewById(R.id.app_bar_main);
        // 设置 CompactCalendarView
        mCompactCalendarView = (CompactCalendarView) findViewById(R.id.compactcalendar_view);
        final ImageView arrow = (ImageView) findViewById(R.id.date_picker_arrow);
        // 设置 English
        mCompactCalendarView.setLocale(TimeZone.getDefault(), Locale.ENGLISH);
        mCompactCalendarView.setShouldDrawDaysHeader(true);
        mCompactCalendarView.setListener(new CompactCalendarView.CompactCalendarViewListener() {
            @Override
            public void onDayClick(Date dateClicked) {
                NOW_DATE_TIME=dateFormat.format(dateClicked);
                setTitle(NOW_DATE_TIME);
                ViewCompat.animate(arrow).rotation(0).start();
                mAppBarLayout.setExpanded(false, true);
                isExpanded = false;
                if(LOGIN_STATE==1){
                    mProgressDialog.show();
                    new DiaryAsyncTask().execute();
                }

            }
            @Override
            public void onMonthScroll(Date firstDayOfNewMonth) {
                setTitle(dateFormat.format(firstDayOfNewMonth));
            }
        });
        //设置为当前日期
        setCurrentDate(new Date());
        RelativeLayout datePickerButton = (RelativeLayout) findViewById(R.id.date_picker_button);
        datePickerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isExpanded) {
                    ViewCompat.animate(arrow).rotation(0).start();
                    mAppBarLayout.setExpanded(false, true);
                    isExpanded = false;
                } else {
                    ViewCompat.animate(arrow).rotation(180).start();
                    mAppBarLayout.setExpanded(true, true);
                    isExpanded = true;
                }
            }
        });
        if(NetWorkUtil.isNetworkConnected(this)){
            new AutoLoginSyncTask().execute();
        }else {
            mProgressDialog.dismiss();
            Toast.makeText(this,"请检查您的网络连接",Toast.LENGTH_LONG).show();
        }
    }

    public void setCurrentDate(Date date) {
        setTitle(dateFormat.format(date));
        if (mCompactCalendarView != null) {
            mCompactCalendarView.setCurrentDate(date);
        }

    }

    @Override
    public void setTitle(CharSequence title) {
        TextView datePickerTextView = (TextView) findViewById(R.id.date_picker_text_view);

        if (datePickerTextView != null) {
            datePickerTextView.setText(title);
        }
    }


    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.main_toolbar_syn) {
            if(NetWorkUtil.isNetworkConnected(this)){
                mProgressDialog.show();
                new AutoLoginSyncTask().execute();
            }else {
                Toast.makeText(this,"请检查您的网络连接",Toast.LENGTH_LONG).show();
            }

        }else if(id==R.id.main_toolbar_classify){
            alertClassifyDialog();
        }else if(id==R.id.main_toolbar_about){
            alertAbout();
        }

        return super.onOptionsItemSelected(item);
    }
    /**
     * 分类提示框
     */
    private void alertClassifyDialog() {
        new AlertDialog.Builder(MainActivity.this).setTitle("选择分类").setSingleChoiceItems(
                MainActivity.mClassifyItems, CLASSIFY_SEL,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        CLASSIFY_SEL =which;
                        mProgressDialog.show();
                        dialog.dismiss();
                        new DiaryAsyncTask().execute();
                    }
                })
                .setNeutralButton("编辑分类", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                       startActivityForResult(new Intent(MainActivity.this,ClassifyActivity.class),CLASSIFY);
                    }
                })
                .setNegativeButton("取消", null).show();
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_index) {
        }else if(id==R.id.nav_gallery){
            startActivityForResult(new Intent(MainActivity.this,ImagesActivity.class),IMAGES);
        }else if(id==R.id.nav_about){
            alertAbout();
        }else if(id==R.id.nav_classify){
            startActivityForResult(new Intent(MainActivity.this,ClassifyActivity.class),CLASSIFY);
        }else if(id==R.id.nav_manager) {
           startActivityForResult(new Intent(MainActivity.this,SettingActivity.class),SETTING);
        }

        return true;
    }

    /**
     * 获取日记列表转换json数据
     * @return
     */
    private List<DiaryBean> getDiaryListJSONData() {
        List<DiaryBean> list = new ArrayList<>();
        try {
            String jsonString = HttpClient
                    .post(LIST_DIARY_URL)
                    .param("tag",mClassifyItems[CLASSIFY_SEL])
                    .param("date",NOW_DATE_TIME)
                    .execute()
                    .asString();
                if(!StringUtils.isEmpty(jsonString)){
                JSONObject jsonObject;
                jsonObject =new JSONObject(jsonString);
                if(jsonObject.getInt("state")==0){
                    JSONArray jsonArray=jsonObject.getJSONArray("data");
                    COUNT_MAIN_STR =jsonObject.getString("str");
                    for (int i=0;i<jsonArray.length();i++){
                        DiaryBean diaryBean =new DiaryBean();
                        jsonObject=jsonArray.getJSONObject(i);
                        diaryBean=AppUtil.convertJsonToDiaryBean(jsonObject);
                        list.add(diaryBean);
                    }
                }
            }
        } catch ( JSONException e) {
            e.printStackTrace();
        }
        return list;
    }
    /**
     * 实现异步自动登录加载
     */
    private class AutoLoginSyncTask extends AsyncTask<Void,Void,UserBean>{
        @Override
        protected UserBean doInBackground(Void... params) {
            SharedPreferences sp =getSharedPreferences("Diary", MODE_PRIVATE);
            String token=sp.getString("token", "limingyuan");
            if(!token.equals("limingyuan")){
                try {
                    String jsonString = HttpClient
                            .post(TOKEN_LOGIN_URL)
                            .param("token",token)
                            .execute()
                            .asString();
                    if(!StringUtils.isEmpty(jsonString)){
                        JSONObject jsonObject=new JSONObject(jsonString);
                        if(jsonObject.getInt("state")==0){
                            jsonObject=jsonObject.getJSONObject("data");
                            UserBean userBean=new UserBean();
                            userBean.setId(jsonObject.getInt("id"));
                            userBean.setName(jsonObject.getString("name"));
                            userBean.setPassword(jsonObject.getString("password"));
                            userBean.setDesc(jsonObject.getString("desc"));
                            userBean.setToken(jsonObject.getString("token"));
                            return userBean;
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(UserBean userBean) {
            super.onPostExecute(userBean);
            String name="未设置昵称";
            String desc="记录你的日子";
            if(userBean!=null){
                LOGIN_STATE=1;
                NOW_USER_ID=userBean.getId();
                if(userBean.getName()!=null&&!userBean.getName().equals("")){
                    name=userBean.getName();
                }
                if(userBean.getDesc()!=null&&!userBean.getDesc().equals("")){
                    desc=userBean.getDesc();
                }
                Toast.makeText(MainActivity.this,"自动登录成功！",Toast.LENGTH_LONG).show();
                //获取分类列表
                new ClassifyAsyncTask().execute();
                // 获取日记列表
                new DiaryAsyncTask().execute();
            }else {
                name="未登录";
                mProgressDialog.dismiss();
            }
            mTvNavHeadName.setText(name);
            mTvNavHeadDesc.setText(desc);
        }
    }
    /**
     * 实现网络异步加载
     */
   private class DiaryAsyncTask extends AsyncTask<Void,Void,List<DiaryBean>>{

        @Override
        protected List<DiaryBean> doInBackground(Void... params) {
            return getDiaryListJSONData();
        }

        @Override
        protected void onPostExecute(List<DiaryBean> diaryBeen) {
            super.onPostExecute(diaryBeen);
            //设置listView 的adapter
            diaryListAdapter=new DiaryListAdapter(MainActivity.this,diaryBeen);
            mListView.setAdapter(diaryListAdapter);
            mTvMainCount.setText("——"+ COUNT_MAIN_STR +"——");
            //隐藏加载框
            mProgressDialog.dismiss();
        }
    }
    private class UpdateNameAsyncTask extends AsyncTask<String,Void,Boolean>{
        private String name;

        @Override
        protected Boolean doInBackground(String... params) {
            name=params[0];
            String jsonString=HttpClient.post(USER_UPDATE_NAME_URL)
                    .param("name",name)
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
            if(aBoolean){
                mTvNavHeadName.setText(name);
                Toast.makeText(MainActivity.this,"修改成功！",Toast.LENGTH_LONG).show();
            }
        }
    }
    private class UpdateDescAsyncTask extends AsyncTask<String,Void,Boolean>{
        private String desc;
        @Override
        protected Boolean doInBackground(String... params) {
            desc=params[0];
            String jsonString=HttpClient.post(USER_UPDATE_DESC_URL)
                    .param("desc",desc)
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
            if(aBoolean){
                mTvNavHeadDesc.setText(desc);
                Toast.makeText(MainActivity.this,"修改成功！",Toast.LENGTH_LONG).show();
            }
        }
    }
    private class ClassifyAsyncTask extends AsyncTask<Void,Void,Void>{
        @Override
        protected Void doInBackground(Void... params) {
            String jsonString=HttpClient.post(CLASSIFY_LIST_URL)
                    .execute().asString();
            if(!StringUtils.isEmpty(jsonString)){
                try {
                    JSONObject jsonObj=new JSONObject(jsonString);
                    if(jsonObj.getInt("state")==0){
                        JSONArray jsonArray=jsonObj.getJSONArray("data");
                        mClassifyItems=new String[jsonArray.length()];
                        for (int i = 0; i <jsonArray.length() ; i++) {
                            jsonObj=jsonArray.getJSONObject(i);
                            mClassifyItems[i]=jsonObj.getString("tag");
                        }

                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }
    }
    
    private class DeleteDiaryAsyncTask extends AsyncTask<Integer,Void,Boolean>{

        @Override
        protected Boolean doInBackground(Integer... params) {
            String jsonString=HttpClient.post(DEL_DIARY_URL)
                    .param("id",String.valueOf(params[0]))
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
            Toast.makeText(MainActivity.this,aBoolean?"删除成功":"删除失败",Toast.LENGTH_LONG).show();
            mProgressDialog.dismiss();
            if(aBoolean){
                mProgressDialog.show();
                new DiaryAsyncTask().execute();
            }
        }
    }
    /**
     * 重写判断是否登录
     * @param intent
     */
    @Override
    public void startActivity(Intent intent) {
        if(LOGIN_STATE==0){
            intent=new Intent(MainActivity.this,LoginActivity.class);
        }
        super.startActivity(intent);
    }

    /**
     * 重写判断是否登录
     * @param intent
     * @param requestCode
     */
    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
        if(LOGIN_STATE==0){
            intent=new Intent(MainActivity.this,LoginActivity.class);
            requestCode=LOGIN;
        }
        super.startActivityForResult(intent, requestCode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case ADD:
                mProgressDialog.show();
                new DiaryAsyncTask().execute();
                break;
            case LOGIN:
                if(NetWorkUtil.isNetworkConnected(this)){
                    mProgressDialog.show();
                    new AutoLoginSyncTask().execute();
                }else {
                    Toast.makeText(this,"请检查您的网络连接",Toast.LENGTH_LONG).show();
                }
                break;
            case SETTING:
                if(NetWorkUtil.isNetworkConnected(this)){
                    mProgressDialog.show();
                    new AutoLoginSyncTask().execute();
                }else {
                    Toast.makeText(this,"请检查您的网络连接",Toast.LENGTH_LONG).show();
                }
                break;
            case EDIT:
                mProgressDialog.show();
                new DiaryAsyncTask().execute();
                break;
            case CLASSIFY:
                mProgressDialog.show();
                new ClassifyAsyncTask().execute();
                new DiaryAsyncTask().execute();
                break;
        }
    }

    private void alertAbout(){
        new AlertDialog.Builder(MainActivity.this)
                .setTitle("关于")
                .setMessage("课程设计作品")
                .setNegativeButton("确定", null)
                .show();
    }

}
