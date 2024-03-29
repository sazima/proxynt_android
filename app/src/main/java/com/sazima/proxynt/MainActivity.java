package com.sazima.proxynt;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.databinding.tool.util.StringUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.sazima.proxynt.common.DaemonService;
import com.sazima.proxynt.common.SeriallizerUtils;
import com.sazima.proxynt.common.TableEncrypt;
import com.sazima.proxynt.databinding.ActivityMainBinding;
import com.sazima.proxynt.entity.ClientConfigEntity;
import com.sazima.proxynt.service.JWebSocketClientService;

import java.net.URI;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class MainActivity extends AppCompatActivity {
    private final String TAG = "MainActivity";

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;
    //    JWebSocketClient client;
    private final String JSONKEY = "c_json";
    private final String DON_NOT_REQUES_TBATTERY = "donNotRequestBattery";
    private final String DON_NOT_REQUES_TBATTERY_VALUE = "DON_NOT_REQUES_TBATTERY_VALUE";
    private boolean isConnectButton = true;
    private JWebSocketClient client;
    private InnerServiceConnection serviceConnection = new InnerServiceConnection();
    private JWebSocketClientService.InnerIBinder binder;
    private JWebSocketClientService jWebSClientService;

    private Lock lock = new ReentrantLock();


    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MyHandler.mainActivity = this;
        try {
            requestBattery();
        } catch (Exception e) {
            e.printStackTrace();
        }

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        String oldJson = readString(JSONKEY);
        EditText textArea = (EditText) findViewById(R.id.jsonTextArea);
        textArea.setText(oldJson);
//        findViewById(R.id.action_star).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//            }
//        });

        findViewById(R.id.button_first).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                if (!lock.tryLock()) {
                    Log.i(TAG, "点击过快, 跳过");
                    return;
                }
                try {
                    Button mButton = (Button) findViewById(R.id.button_first);
                    mButton.setEnabled(false);
                    if (!isConnectButton) {
                        Message message = new Message();
                        message.what = MyHandler.ON_CLICK_DISCONNECT;
                        MyHandler.handler.sendMessage(message);
                        return;
                    }
                    EditText jsonTextArea = (EditText) findViewById(R.id.jsonTextArea);
                    String jsonString = jsonTextArea.getText().toString();
                    if (!StringUtils.isNotBlank(jsonString)) {
                        showDialog("请输入配置json", "错误");
                        mButton.setEnabled(true);
                        return;
                    }
                    ClientConfigEntity clientConfigEntity;
                    Gson gson = new Gson();
                    try {
                        clientConfigEntity = gson.fromJson(jsonString, ClientConfigEntity.class);
                    } catch (JsonSyntaxException e) {
                        e.printStackTrace();
                        showDialog("请检查json格式", "错误");
                        mButton.setEnabled(true);
                        return;
                    }
                    try {
                        writeString(JSONKEY, jsonString);
                        String url = "";
                        ClientConfigEntity.Server server = clientConfigEntity.getServer();
                        url += server.isHttps() ? "wss://" : "ws://";
                        url += server.getHost() + ":" + server.getPort() + server.getPath();
                        TableEncrypt.initTable(server.getPassword());
                        SeriallizerUtils.key = server.getPassword();//# todo?
                        URI uri = URI.create(url);
                        JWebSocketClientService.clientConfigEntity = clientConfigEntity;
                        JWebSocketClientService.uri = uri;
                        //启动service
                        startJWebSClientService();
                        //绑定服务
                        bindService();
                    } catch (Exception e) {
                        e.printStackTrace();
                        showDialog("请检查json配置" + e.getMessage(), "错误");
                        mButton.setEnabled(true);
                        return;
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    lock.unlock();
                }
                return;
            }

        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }


    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }

    @Override
    protected void onResume() {
        Log.i(TAG, "onResume");
        Message message0 = new Message();
        message0.what = MyHandler.HEATBEAT;
        MyHandler.handler.sendMessage(message0);
        super.onResume();
    }


    public void showDialog(String msg, String title) {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(title)//设置标题
                .setMessage(msg)//设置要显示的内容
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Toast.makeText(MainActivity.this, "点击了取消按钮", Toast.LENGTH_SHORT).show();
                        dialogInterface.dismiss();//销毁对话框
                    }
                })
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(MainActivity.this, "点击了确定的按钮", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();//销毁对话框
                    }
                }).create();//create（）方法创建对话框
        dialog.show();//显示对话框
    }

    /*
    websocket
     */


    public void writeString(String KEY, String content) {
        SharedPreferences settings = getApplicationContext().getSharedPreferences(KEY, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("homeScore", content);
        editor.apply();
    }

    public String readString(final String KEY) {
        SharedPreferences settings = getApplicationContext().getSharedPreferences(KEY, 0);
        String homeScore = settings.getString("homeScore", "");
        return homeScore;
    }

    public void setButtonToClose() {
        Button mButton = (Button) findViewById(R.id.button_first);
        isConnectButton = false;
        mButton.setText("断开");
    }

    public void setButtonToConnect() {
        Button mButton = (Button) findViewById(R.id.button_first);
        isConnectButton = true;
        mButton.setText("连接");
    }

    /**
     * 电池策略设置成无限制
     */
    public void requestBattery() {
        if (readString(DON_NOT_REQUES_TBATTERY).equals(DON_NOT_REQUES_TBATTERY_VALUE)) {
            return;
        }
        String msg = "将电池策略设置成无限制, 以便不被后台清理";
        Context context = getApplicationContext();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String packageName = context.getPackageName();
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                AlertDialog dialog = new AlertDialog.Builder(this)
                        .setMessage(msg)//设置要显示的内容
                        .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                Toast.makeText(MainActivity.this, "跳过", Toast.LENGTH_SHORT).show();
                                dialogInterface.dismiss();//销毁对话框
                            }
                        })
                        .setNeutralButton("不再提示", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // todo
                                Toast.makeText(MainActivity.this, "跳过", Toast.LENGTH_SHORT).show();
                                dialogInterface.dismiss();//销毁对话框
                                writeString(DON_NOT_REQUES_TBATTERY, DON_NOT_REQUES_TBATTERY_VALUE);
                            }
                        })
                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent intent = new Intent();
                                intent.setAction(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                                intent.setFlags(FLAG_ACTIVITY_NEW_TASK);
                                intent.setData(Uri.parse("package:" + packageName));
                                context.startActivity(intent);
                            }
                        }).create();//create（）方法创建对话框
                dialog.show();//显示对话框
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_star) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.github_url)));
            startActivity(intent);
        } else if (id == R.id.scan) {
            // 创建IntentIntegrator对象
            IntentIntegrator intentIntegrator = new IntentIntegrator(MainActivity.this);
            // 开始扫描
            intentIntegrator.initiateScan();

        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // 获取解析结果
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() == null) {
                Toast.makeText(this, "取消扫描", Toast.LENGTH_LONG).show();
            } else {
//                Toast.makeText(this, "扫描内容:" + result.getContents(), Toast.LENGTH_LONG).show();
                EditText textArea = (EditText) findViewById(R.id.jsonTextArea);
                textArea.setText(result.getContents());
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void startJWebSClientService() {
        Context mContext = getApplicationContext();
        Intent intent = new Intent(mContext, JWebSocketClientService.class);
        startService(intent);
    }

    private void bindService() {
        Intent bindIntent = new Intent(this, JWebSocketClientService.class);
        bindService(bindIntent, serviceConnection, BIND_AUTO_CREATE);
    }

    public void unbindService() {
        unbindService(serviceConnection);
    }


    private class InnerServiceConnection implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d("Service", "onServiceConnected()->当Activity和Service连接");
            binder = (JWebSocketClientService.InnerIBinder) service;
            jWebSClientService = binder.getService();
            client = jWebSClientService.client;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d("Service", "onServiceConnected()->当Activity和Service断开连接");
        }
    }


    /**
     * 点击事件
     *
     * @param ev
     * @return
     */
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            //返回具有焦点的当前视图
            View v = getCurrentFocus();
            if (isShouldHideInput(v, ev)) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
                }
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    private boolean isShouldHideInput(View v, MotionEvent ev) {
        if (v != null) {
            if (v instanceof EditText) {
                //命名一个元素为2个的整数数组
                int[] leftTop = {0, 0};
                //返回两个整数值,分别为X和Y,此X和Y表示此视图,在其屏幕中的坐标(以左上角为原点的坐标)
                v.getLocationInWindow(leftTop);
                int left = leftTop[0],
                        top = leftTop[1],
                        bottom = top + v.getHeight(),
                        right = left + v.getWidth();
                if (ev.getX() > left && ev.getX() < right && ev.getY() > top && ev.getY() < bottom) {
                    //如果点击的是输入框的区域,则返回false
                    return false;
                } else {
                    return true;
                }
            }
        }
        return false;
    }
}