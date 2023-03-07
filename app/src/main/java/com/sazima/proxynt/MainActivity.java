package com.sazima.proxynt;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.databinding.tool.util.StringUtils;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.android.tools.r8.code.AputBoolean;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.sazima.proxynt.common.SeriallizerUtils;
import com.sazima.proxynt.common.TableEncrypt;
import com.sazima.proxynt.common.YourService;
import com.sazima.proxynt.databinding.ActivityMainBinding;
import com.sazima.proxynt.entity.ClientConfigEntity;

import java.net.MalformedURLException;
import java.net.URI;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;
    JWebSocketClient client;
    private final String jsonkey = "c_json";
    private boolean isConnectButton = true;



    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startService(new Intent(this, YourService.class));

        MyHandler.mainActivity = this;

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        String oldJson = readString(jsonkey);
        EditText textArea = (EditText) findViewById(R.id.jsonTextArea);
        textArea.setText(oldJson);

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
                writeString(jsonkey, jsonString);
//                mButton.setText("disconnect");
                String url = "";
                ClientConfigEntity.Server server = clientConfigEntity.getServer();
                url += server.isHttps() ? "wss://" : "ws://";
                url += server.getHost() + ":" + server.getPort() + server.getPath();
                new TableEncrypt().initTable(server.getPassword());
                SeriallizerUtils.key = server.getPassword();//# todo?
                URI uri = URI.create(url);
                Thread1 t = new Thread1();
                t.uri = uri;
                t.clientConfigEntity = clientConfigEntity;
                new Thread(t).start();
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
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        return super.onOptionsItemSelected(item);
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
    class Thread1 implements Runnable {
        public URI uri;
        public ClientConfigEntity clientConfigEntity;

        @RequiresApi(api = Build.VERSION_CODES.N)
        public synchronized void run() {
            client = new JWebSocketClient(uri, clientConfigEntity);
            MyHandler.jWebSocketClient = client;
            try {
                client.connectBlocking();
            } catch (Exception e) {
                Message message = new Message();
                message.what = MyHandler.ON_WEBSOCKETCONNECT_ERROR;
                message.obj = e.getMessage();
                MyHandler.handler.sendMessage(message);
                return;
            }
            if (!client.isOpen()) {
                Message message = new Message();
                message.what = MyHandler.ON_WEBSOCKETCONNECT_ERROR;
                message.obj = "client is not open";
                MyHandler.handler.sendMessage(message);
                return;
            } else {
                Message message = new Message();
                message.what = MyHandler.ON_WEBSOCKETCONNECT_SUCCESS;
                message.obj = "连接成功";
                MyHandler.handler.sendMessage(message);
            }
//            connectOtherThread(uri, clientConfigEntity);
        }
    }


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
}