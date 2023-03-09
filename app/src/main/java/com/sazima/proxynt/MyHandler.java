package com.sazima.proxynt;

import android.os.Handler;
import android.os.Message;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.material.snackbar.Snackbar;

import java.util.concurrent.TimeUnit;

public class MyHandler {
    public static MainActivity mainActivity;
    public static  JWebSocketClient jWebSocketClient;

    public final static int ON_CLOSE_WEBSOCKET = 1;
    public final static int ON_WEBSOCKET_OPEN = 2;
    public final static int ON_WEBSOCKETCONNECT_ERROR = 3;
    public final static int ON_WEBSOCKETCONNECT_SUCCESS = 4;
    public final static int ON_CLICK_DISCONNECT = 5;
    public final static int SEND_MESSAGE = 6;

    /*
    如果是true的话, 断开就需要重连
     */
    private static boolean websocketRunning = false;


    public static final Handler handler = new Handler() {
        @Override
        public void handleMessage(@NonNull Message msg) {
            Button mButton = (Button) mainActivity.findViewById(R.id.button_first);
            switch (msg.what) {
                case ON_WEBSOCKETCONNECT_ERROR:
                    String content = (String) msg.obj;
                    mainActivity.showDialog(content, "连接服务器错误");
//                    Toast.makeText(mainActivity, "点击了确定的按钮", Toast.LENGTH_SHORT).show();
                    mButton.setEnabled(true);
                    mainActivity.unbindService();
                    break;
                case ON_WEBSOCKETCONNECT_SUCCESS:
                    String content1 = (String) msg.obj;
                    Toast.makeText(mainActivity, content1, Toast.LENGTH_SHORT).show();
                    // 变成 disconnect
//                    mButton.setText("断开");
                    mainActivity.setButtonToClose();
                    websocketRunning = true;
                    mButton.setEnabled(true);
                    break;
                case ON_CLOSE_WEBSOCKET:
                    if (websocketRunning) {
                        String content2 = (String) msg.obj;
                        Toast.makeText(mainActivity, content2, Toast.LENGTH_SHORT).show();
                        jWebSocketClient.reconnect();
                    }
                    break;
                case ON_CLICK_DISCONNECT:
                    Toast.makeText(mainActivity, "close", Toast.LENGTH_SHORT).show();
                    websocketRunning = false;
                    try {
                        jWebSocketClient.closeBlocking();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    mainActivity.setButtonToConnect();
                    mainActivity.unbindService();
                    mButton.setEnabled(true);
                    break;
                case ON_WEBSOCKET_OPEN:
                    String content2 = (String) msg.obj;
                    Toast.makeText(mainActivity, content2, Toast.LENGTH_SHORT).show();
                    break;
                case SEND_MESSAGE:
                    String content3 = (String) msg.obj;
                    Toast.makeText(mainActivity, content3, Toast.LENGTH_SHORT).show();
                    break;
            }

        }
    };

}
