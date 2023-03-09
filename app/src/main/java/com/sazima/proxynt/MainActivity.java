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
import com.sazima.proxynt.common.DaemonService;
import com.sazima.proxynt.common.SeriallizerUtils;
import com.sazima.proxynt.common.TableEncrypt;
import com.sazima.proxynt.databinding.ActivityMainBinding;
import com.sazima.proxynt.entity.ClientConfigEntity;
import com.sazima.proxynt.service.JWebSocketClientService;

import java.net.URI;

public class MainActivity extends AppCompatActivity {

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
                Button mButton = (Button) findViewById(R.id.button_first);
                mButton.setEnabled(false);
                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                if (!isConnectButton) {
                    Message message = new Message();
                    message.what = MyHandler.ON_CLICK_DISCONNECT;
                    MyHandler.handler.sendMessage(message);
                    unbindService();
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
    public void requestBattery(){
        if (readString(DON_NOT_REQUES_TBATTERY).equals(DON_NOT_REQUES_TBATTERY_VALUE) ){
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
        }
        return super.onOptionsItemSelected(item);
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

    private void unbindService(){
        unbindService(serviceConnection);
    }


    private class InnerServiceConnection implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d("Service","onServiceConnected()->当Activity和Service连接");
//            LocalBinder binder = (LocalBinder) service;
//            service = (IBinder) binder.getService();
            binder = (JWebSocketClientService.InnerIBinder) service;
//            binder = (JWebSocketClientService.InnerIBinder) service;
            jWebSClientService = binder.getService();
            client = jWebSClientService.client;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d("Service","onServiceConnected()->当Activity和Service断开连接");
        }
    }
}