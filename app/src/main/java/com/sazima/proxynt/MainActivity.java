package com.sazima.proxynt;

import android.content.DialogInterface;
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

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.sazima.proxynt.common.TableEncrypt;
import com.sazima.proxynt.databinding.ActivityMainBinding;
import com.sazima.proxynt.entity.ClientConfigEntity;

import java.net.MalformedURLException;
import java.net.URI;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;
    private boolean isStartConnect = false;
    JWebSocketClient client;
    private final String jsonkey = "c_json";
    private final Integer connectFailFlag = 1;

    private  Handler mHandler = new Handler(){
        @Override
        public void handleMessage(@NonNull Message msg) {
            Button mButton = (Button) findViewById(R.id.button_first);
            System.out.println("-----------" + msg );
            if (msg.what == connectFailFlag) {
                showDialog("连接失败", "错误");
                mButton.setText("connect");
                isStartConnect = false;
                mButton.setEnabled(true);
            }
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
                if (isStartConnect) { // 改成暂停
                    client.close();
                    mButton.setText("connect");
                    isStartConnect = false;
                    mButton.setEnabled(true);
                    return;
                }
                EditText jsonTextArea = (EditText) findViewById(R.id.jsonTextArea);
                String jsonString = jsonTextArea.getText().toString();
                if (!StringUtils.isNotBlank(jsonString)) {
                    showDialog("请输入配置json", "错误");
                    return;
                }
                ClientConfigEntity clientConfigEntity;
                Gson gson = new Gson();
                try {
                    clientConfigEntity = gson.fromJson(jsonString, ClientConfigEntity.class);
                } catch (JsonSyntaxException e) {
                    e.printStackTrace();
                    showDialog("请检查json格式", "错误");
                    return;
                }
                writeString(jsonkey, jsonString);
                isStartConnect = true;
                mButton.setText("disconnect");
                String url = "";
                ClientConfigEntity.Server server = clientConfigEntity.getServer();
                url += server.isHttps() ? "wss://" : "ws://";
                url += server.getHost() + ":" + server.getPort() + server.getPath();
                new TableEncrypt().initTable(server.getPassword());
                URI uri = URI.create(url);
                Thread1 t = new Thread1();
                t.uri = uri;
                new Thread(t).start();
                mButton.setEnabled(true);
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

//            return true;
//        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }

    private void showDialog(String msg, String title) {
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

        @RequiresApi(api = Build.VERSION_CODES.N)
        public synchronized void run() {
            connectOtherThread(uri);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void connectOtherThread(URI uri) {
        client = new JWebSocketClient(uri) {
        };
        try {
            client.connectBlocking();
        } catch (Exception e) {
            Message message = new Message();
            message.what = connectFailFlag;
            mHandler.sendMessage(message);
            isStartConnect = false;
            return;
        }
        if (!client.isOpen()) {
            isStartConnect = false;
            Message message = new Message();
            message.what = connectFailFlag;
            mHandler.sendMessage(message);
            return;
        }
        while (isStartConnect) {
            if (!client.isOpen()) {
                Log.i("reconnect", "reconnect");
                try {
//                            client.close();
                    client.reconnectBlocking();
                    if (!isStartConnect) {
                        client.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
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
}