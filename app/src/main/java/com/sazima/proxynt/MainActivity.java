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
                    Log.i(TAG, "????????????, ??????");
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
                        showDialog("???????????????json", "??????");
                        mButton.setEnabled(true);
                        return;
                    }
                    ClientConfigEntity clientConfigEntity;
                    Gson gson = new Gson();
                    try {
                        clientConfigEntity = gson.fromJson(jsonString, ClientConfigEntity.class);
                    } catch (JsonSyntaxException e) {
                        e.printStackTrace();
                        showDialog("?????????json??????", "??????");
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
                        //??????service
                        startJWebSClientService();
                        //????????????
                        bindService();
                    } catch (Exception e) {
                        e.printStackTrace();
                        showDialog("?????????json??????" + e.getMessage(), "??????");
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
                .setTitle(title)//????????????
                .setMessage(msg)//????????????????????????
                .setNegativeButton("??????", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Toast.makeText(MainActivity.this, "?????????????????????", Toast.LENGTH_SHORT).show();
                        dialogInterface.dismiss();//???????????????
                    }
                })
                .setPositiveButton("??????", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(MainActivity.this, "????????????????????????", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();//???????????????
                    }
                }).create();//create???????????????????????????
        dialog.show();//???????????????
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
        mButton.setText("??????");
    }

    public void setButtonToConnect() {
        Button mButton = (Button) findViewById(R.id.button_first);
        isConnectButton = true;
        mButton.setText("??????");
    }

    /**
     * ??????????????????????????????
     */
    public void requestBattery() {
        if (readString(DON_NOT_REQUES_TBATTERY).equals(DON_NOT_REQUES_TBATTERY_VALUE)) {
            return;
        }
        String msg = "?????????????????????????????????, ????????????????????????";
        Context context = getApplicationContext();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String packageName = context.getPackageName();
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                AlertDialog dialog = new AlertDialog.Builder(this)
                        .setMessage(msg)//????????????????????????
                        .setNegativeButton("??????", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                Toast.makeText(MainActivity.this, "??????", Toast.LENGTH_SHORT).show();
                                dialogInterface.dismiss();//???????????????
                            }
                        })
                        .setNeutralButton("????????????", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // todo
                                Toast.makeText(MainActivity.this, "??????", Toast.LENGTH_SHORT).show();
                                dialogInterface.dismiss();//???????????????
                                writeString(DON_NOT_REQUES_TBATTERY, DON_NOT_REQUES_TBATTERY_VALUE);
                            }
                        })
                        .setPositiveButton("??????", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent intent = new Intent();
                                intent.setAction(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                                intent.setFlags(FLAG_ACTIVITY_NEW_TASK);
                                intent.setData(Uri.parse("package:" + packageName));
                                context.startActivity(intent);
                            }
                        }).create();//create???????????????????????????
                dialog.show();//???????????????
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
            // ??????IntentIntegrator??????
            IntentIntegrator intentIntegrator = new IntentIntegrator(MainActivity.this);
            // ????????????
            intentIntegrator.initiateScan();

        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // ??????????????????
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() == null) {
                Toast.makeText(this, "????????????", Toast.LENGTH_LONG).show();
            } else {
//                Toast.makeText(this, "????????????:" + result.getContents(), Toast.LENGTH_LONG).show();
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
            Log.d("Service", "onServiceConnected()->???Activity???Service??????");
            binder = (JWebSocketClientService.InnerIBinder) service;
            jWebSClientService = binder.getService();
            client = jWebSClientService.client;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d("Service", "onServiceConnected()->???Activity???Service????????????");
        }
    }


    /**
     * ????????????
     *
     * @param ev
     * @return
     */
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            //?????????????????????????????????
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
                //?????????????????????2??????????????????
                int[] leftTop = {0, 0};
                //?????????????????????,?????????X???Y,???X???Y???????????????,????????????????????????(??????????????????????????????)
                v.getLocationInWindow(leftTop);
                int left = leftTop[0],
                        top = leftTop[1],
                        bottom = top + v.getHeight(),
                        right = left + v.getWidth();
                if (ev.getX() > left && ev.getX() < right && ev.getY() > top && ev.getY() < bottom) {
                    //????????????????????????????????????,?????????false
                    return false;
                } else {
                    return true;
                }
            }
        }
        return false;
    }
}