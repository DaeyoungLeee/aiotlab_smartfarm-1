package kr.co.aiotlab.www;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PersistableBundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import org.json.JSONException;
import org.json.JSONObject;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import kr.co.aiotlab.www.Main_UI.BottomFirstFragment;
import kr.co.aiotlab.www.Main_UI.BottomFourthFragment;
import kr.co.aiotlab.www.Main_UI.BottomSecondFragment;
import kr.co.aiotlab.www.Main_UI.BottomSettingActivity;
import kr.co.aiotlab.www.Main_UI.BottomThirdFragment;

import static android.widget.Toast.LENGTH_SHORT;
import static kr.co.aiotlab.www.Main_UI.BottomFirstFragment.getFire;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private long backBtnTime = 0;
    DrawerLayout mDrawerlayout;
    public static String temp, humi;
    public static int dust;
    public static String fire_state = "OFF";

    JSONObject jsonObject, jsonObject_fire = null;
    String jsonMQTT, jsonMQTT_fire;
    private static final String TAG = "MainActivity";
    /**
     * 인터넷 연결시킬 주소 설정
     */

    private ActionBarDrawerToggle mToggle;

    // navigation bottom 버튼 클릭 시 동작 버튼
    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {


        /** 바텀 네비게이션 아이템 클릭 시 이동경로 */
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.bottomBarItemOne:
                    showFragment(BottomFirstFragment.newInstance());
                    return true;
                case R.id.bottomBarItemSecond:
                    showFragment(BottomSecondFragment.newInstance());
                    return true;
                case R.id.bottomBarItemThird:
                    showFragment(BottomThirdFragment.newInstance());
                    return true;
                case R.id.bottomBarItemFourth:
                    showFragment(BottomFourthFragment.newInstance());
                    return true;
                case R.id.bottomBarItemFifth:
                    Intent intent_settings = new Intent(MainActivity.this, BottomSettingActivity.class);
                    startActivity(intent_settings);
                    return false;
                default:
                    return false;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // FCM 현재 토큰 검색
        FirebaseInstanceId.getInstance().getInstanceId()
                .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                    @Override
                    public void onComplete(@NonNull Task<InstanceIdResult> task) {
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "getInstanceId failed", task.getException());
                            return;
                        }
                        // Get new Instance ID token
                        String token = task.getResult().getToken();

                        // Log and toast
                        Log.d(TAG, token);
                        //Toast.makeText(MainActivity.this, token, Toast.LENGTH_SHORT).show();
                    }
                });


        //첫 화면
        if (savedInstanceState == null) {
            showFragment(BottomFirstFragment.newInstance());
        }
        // bottom 네이게이션 아이디 받아옴
        BottomNavigationView navigation = findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        // drawer 네비게이션 아이디
        // drawer
        mDrawerlayout = findViewById(R.id.drawer);
        mToggle = new ActionBarDrawerToggle(this, mDrawerlayout, R.string.open, R.string.close);
        mDrawerlayout.addDrawerListener(mToggle);
        mToggle.syncState();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        /////  드로어 창 아이템선택 세팅 (홈페이지, 로그아웃 등등)
        NavigationView nav_View = findViewById(R.id.nav_View);
        nav_View.setNavigationItemSelectedListener(this);

        /** MQTT init **/
        // mqtt 초기설정
        MqttSet ms_temp_humid_dust = new MqttSet(getApplicationContext(),
                "222.113.57.108:1883",
                "Sensor/Dust_DHT22",
                1);
        ms_temp_humid_dust.getDataFromServer();

        MqttSet ms_fire = new MqttSet(getApplicationContext(),
                "222.113.57.108:1883",
                "Sensor/Fire_Motion",
                1);
        ms_fire.getDataFromServer();

    }

    //getAutorun은 BottomSettingActivity의 스위치 상태를 보고 ON이면 동작, OFF면 일시중지하는 함수로, 생명주기에서 MainActivity로 돌아갈 때(onResume/ onRestart) 확인하는 함수.
    private void getAutorun() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        if (sharedPreferences.getBoolean("autoSwitch", false) == true) {
            //쓰레드 동작
            brightServiceStart();
            Log.d(TAG, "thread start");

        } else if (sharedPreferences.getBoolean("autoSwitch", false) == false) {
            //쓰레드 일시정지
            brightServiceStop();
            Log.d(TAG, "thread paused");
        }
    }

    private void getAutoSafetyMode() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        if (sharedPreferences.getBoolean("autoSafety", false) == true) {
            safetymodeServiceStart();
        } else {
            safetymodeServiceStop();
        }

    }

    private void getAutoPushMode() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        if (sharedPreferences.getBoolean("push_all", false) == true) {
            pushServiceStart();
        } else {
            pushServiceStop();
        }

    }

    /**
     * showFrag 메소드 : 각각의 프래그먼트를 보여줌
     */
    public void showFragment(Fragment f) {
        getSupportFragmentManager().beginTransaction().replace(R.id.frame_main, f).commit();
    }


    /**
     * Drawer 좌상단 버튼(드로어 열고 닫는 동작)이 클릭됐을 때
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // drawer를 좌상단 버튼으로 클릭해서 열고 닫게 하기 위한 코드
        if (mToggle.onOptionsItemSelected(item)) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            // 달력창으로 가기
            case R.id.calender:
                Intent intent_calendar = new Intent(MainActivity.this, CalendarActivity.class);
                startActivity(intent_calendar);
                break;
            //홈페이지 들어가기
            case R.id.homepage:
                Intent browserIntent = new Intent(Intent.ACTION_VIEW);
                browserIntent.setData(Uri.parse("http://www.aiotlab.co.kr"));
                startActivity(browserIntent);
                break;
            // 센서 수동 컨트롤
            case R.id.sensor_control:
                Intent intent = new Intent(MainActivity.this, SensorControlActivity.class);
                startActivity(intent);
                break;
            // ip 입력
            case R.id.cctv_ip:
                Intent ipsetting_intent = new Intent(MainActivity.this, IP_Setting_Activity.class);
                startActivity(ipsetting_intent);
                break;
            //로그아웃
            case R.id.logout:
                FirebaseAuth.getInstance().signOut();
                Toast.makeText(this, "로그아웃 되었습니다.", LENGTH_SHORT).show();
                finish();
                break;
        }
        return true;
    }



    // 불꽃감지 MQTT통신으로 받아오기
    @SuppressLint("HandlerLeak")
    final Handler fire_handler = new Handler() {
        public void handleMessage(Message msg) {
            jsonParse_fire();
        }
    };

    private void jsonParse_fire() {
        try {
            jsonObject_fire = new JSONObject(jsonMQTT_fire);
            String fire = jsonObject_fire.getString("fire");

            if (fire.equals("1")) {
                getFire.setText("안전");
                getFire.setTextColor(Color.parseColor("#28BBED"));
                if (fire_state.equals("ON")) {
                    new SocketProtocol(getApplicationContext()).execute("6");
                    fire_state = "OFF";
                }

            } else {
                getFire.setText("불꽃 감지");
                getFire.setTextColor(Color.RED);
                if (fire_state.equals("OFF")) {
                    new SocketProtocol(getApplicationContext()).execute("7");
                    fire_state = "ON";
                }
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    /**
     * 뒤로가기 버튼이 눌렸을 경우 동작
     */
    @Override
    public void onBackPressed() {
        if (mDrawerlayout.isDrawerOpen(GravityCompat.START)) {
            mDrawerlayout.closeDrawers();
        } else {
            long curTime = System.currentTimeMillis();
            long gapTime = curTime - backBtnTime;

            if (gapTime >= 0 && gapTime <= 2000) {
                super.onBackPressed();
            } else {
                backBtnTime = curTime;
                Toast.makeText(this, "한 번 더 누르면 종료됩니다~", Toast.LENGTH_SHORT).show();
            }
        }

    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);

    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }


    // 밝기에 따른 Foreground 서비스 실행
    public void brightServiceStart() {
        Intent serviceIntent = new Intent(this, AppService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getApplicationContext().startForegroundService(serviceIntent);
            Log.d(TAG, "밝기 오레오 포그라운드서비스");
        } else {
            startActivity(serviceIntent);
        }
    }

    // 밝기에 따른 Foreground 서비스 중지
    public void brightServiceStop() {
        Intent serviceIntent = new Intent(this, AppService.class);
        stopService(serviceIntent);
    }

    // 방범모드 Foreground 서비스 실행
    public void safetymodeServiceStart() {
        Intent serviceIntent = new Intent(this, AppService2.class);
        startService(serviceIntent);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getApplicationContext().startForegroundService(serviceIntent);
        } else {
            startActivity(serviceIntent);
        }
    }

    // 방범모드 Foreground 서비스 중지
    public void safetymodeServiceStop() {
        Intent serviceIntent = new Intent(this, AppService2.class);
        stopService(serviceIntent);
    }

    // 온도에 따른 Foreground 서비스 실행
    public void pushServiceStart() {
        Intent serviceIntent = new Intent(this, AppService_push.class);
        startService(serviceIntent);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.d(TAG, "온도 오레오 포그라운드서비스");
            getApplicationContext().startForegroundService(serviceIntent);
        } else {
            startActivity(serviceIntent);
        }
    }


    // 온도에 따른 Foreground 서비스 중지
    public void pushServiceStop() {
        Intent serviceIntent = new Intent(this, AppService_push.class);
        stopService(serviceIntent);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d("MainActivity", "onPause");
    }

    @Override
    protected void onStop() {
        super.onStop();
        // 스위치 상태를 기억해서 중복실행되지 않게하기위함
        Log.d("MainActivity", "onStop");


    }

    @Override
    protected void onRestart() {
        super.onRestart();
        getAutorun();
        getAutoSafetyMode();
        getAutoPushMode();
        Log.d("MainActivity", "onRestart");
    }

    @Override
    protected void onResume() {
        super.onResume();

        Log.d("MainActivity", "onResume");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // 액티비티 강제종료 혹은 종료 시
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        //그당시 자동 스위치 상태를 확인해서 저장해둔다.
        SharedPreferences ss = getSharedPreferences("SWITCHSTATE", MODE_PRIVATE);
        SharedPreferences.Editor past_switch = ss.edit();
        past_switch.putBoolean("PASTSWITCH", sharedPreferences.getBoolean("autoSwitch", false));
        past_switch.commit();

        Log.d(TAG, "onDestroy");
    }

}
