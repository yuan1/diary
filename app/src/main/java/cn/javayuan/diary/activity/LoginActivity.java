package cn.javayuan.diary.activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.mzlion.easyokhttp.HttpClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import cn.javayuan.diary.R;
import cn.javayuan.diary.utils.AppUtil;
import cn.javayuan.diary.utils.NetWorkUtil;

/**
 * 登录窗口
 */
public class LoginActivity extends AppCompatActivity {

    //当前窗口类型 0->登录 1->注册
    private int USER_LOGIN_TYPE=0;

    private final String REG_URL=AppUtil.URL+"/Login/region";

    private final String LOGIN_URL=AppUtil.URL+"/Login/loginTest";
    //登录任务
    private UserLoginTask mAuthTask = null;

    // UI
    private AutoCompleteTextView mEmailView;
    private EditText mPasswordView;
    private View mProgressView;
    private View mLoginFormView;
    private TextView mTvLoginSub;
    private  Button mEmailSignInButton;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_login);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            finish();
            }
        });
        setTitle("日记登录");
        // Set up the login form.
        mEmailView = (AutoCompleteTextView) findViewById(R.id.email);
        mPasswordView = (EditText) findViewById(R.id.password);
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });
        mEmailSignInButton = (Button) findViewById(R.id.email_sign_in_button);
        mEmailSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });
        mTvLoginSub= (TextView) findViewById(R.id.toolbar_login_sub);
        mTvLoginSub.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(USER_LOGIN_TYPE==0){
                    setTitle("邮箱注册");
                    mEmailSignInButton.setText("注册");
                    mTvLoginSub.setText("已有账号");
                    USER_LOGIN_TYPE=1;
                }else {
                    setTitle("日记登录");
                    mEmailSignInButton.setText("登录");
                    mTvLoginSub.setText("注册");
                    USER_LOGIN_TYPE=0;
                }
            }
        });
        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);
    }

    @Override
    public void setTitle(CharSequence title) {
        TextView textView= (TextView) findViewById(R.id.toolbar_login_title);
        if(title!=null&&!title.equals("")){
            textView.setText(title);
        }
    }


    /**
     * 登录操作
     */
    private void attemptLogin() {
        if (mAuthTask != null) {
            return;
        }

        // Reset errors.
        mEmailView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        String email = mEmailView.getText().toString();
        String password = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(email)) {
            mEmailView.setError(getString(R.string.error_field_required));
            focusView = mEmailView;
            cancel = true;
        } else if (!isEmailValid(email)) {
            mEmailView.setError(getString(R.string.error_invalid_email));
            focusView = mEmailView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            if(NetWorkUtil.isNetworkConnected(this)){
                showProgress(true);
                mAuthTask = new UserLoginTask(email, password);
                mAuthTask.execute((Void) null);
            }else {
                Toast.makeText(this,"请检查您的网络连接",Toast.LENGTH_LONG).show();
            }

        }
    }

    private boolean isEmailValid(String email) {
        return email.contains("@");
    }

    private boolean isPasswordValid(String password) {
        return password.length() > 4;
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);
        mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            }
        });
        mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
        mProgressView.animate().setDuration(shortAnimTime).alpha(
                show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        });
    }


    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    private class UserLoginTask extends AsyncTask<Void, Void, Boolean> {

        private final String mEmail;
        private final String mPassword;

        private String token;
        UserLoginTask(String email, String password) {
            mEmail = email;
            mPassword = password;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            boolean val=false;
            try {
                String jsonString=HttpClient.post(USER_LOGIN_TYPE==0?LOGIN_URL:REG_URL)
                            .param("username",mEmail)
                            .param("password",mPassword)
                            .execute().asString();
                if(jsonString!=null){
                    JSONObject jsonObject=new JSONObject(jsonString);
                    if(jsonObject.getInt("state")==0){
                        jsonObject=jsonObject.getJSONObject("data");
                        token=jsonObject.getString("token");
                        val=true;

                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return val;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mAuthTask = null;
            showProgress(false);
            if (success) {
                SharedPreferences sp=getSharedPreferences("Diary",MODE_PRIVATE);
                SharedPreferences.Editor editor=sp.edit();
                editor.putString("token",token);
                editor.apply();
                editor.commit();
                Toast.makeText(LoginActivity.this,USER_LOGIN_TYPE==0?"登录成功":"注册成功！",Toast.LENGTH_LONG).show();
                finish();
            } else {
                if(USER_LOGIN_TYPE==0){
                    mPasswordView.setError(getString(R.string.error_incorrect_password));
                    mPasswordView.requestFocus();
                }else {
                    mEmailView.setError("用户名已存在");
                    mEmailView.requestFocus();
                }
            }
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            showProgress(false);
        }
    }
}

