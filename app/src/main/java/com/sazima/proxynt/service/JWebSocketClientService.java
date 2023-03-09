package com.sazima.proxynt.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.sazima.proxynt.JWebSocketClient;
import com.sazima.proxynt.MyHandler;
import com.sazima.proxynt.entity.ClientConfigEntity;

import java.net.URI;

public class JWebSocketClientService extends Service {
    private final String TAG = "JWebSocketClientService";
    public static ClientConfigEntity clientConfigEntity ;
    public static URI uri;
    private InnerIBinder mBinder = new InnerIBinder();
    public  JWebSocketClient client;


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "on bind");
        return mBinder;
    }

    public class InnerIBinder extends Binder {
        public JWebSocketClientService getService() {
            return JWebSocketClientService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //初始化websocket
        initSocketClient();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
//        closeConnect(); // todo close
        super.onDestroy();
    }


    private void initSocketClient() {
        Thread1 t = new Thread1();
        t.uri = uri;
        t.clientConfigEntity = clientConfigEntity;
        new Thread(t).start();
    }

    class Thread1 implements Runnable {
        public URI uri;
        public ClientConfigEntity clientConfigEntity;

        @RequiresApi(api = Build.VERSION_CODES.N)
        public synchronized void run() {
//            JWebSocketClient client;
            try {
                client = new JWebSocketClient(uri, clientConfigEntity);
                MyHandler.jWebSocketClient = client;
            } catch ( Exception e) {
                e.printStackTrace();
                return;
            }
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
//    private void closeConnect() {
//        try {
//            if (null != client) {
//                client.close();
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        } finally {
//            client = null;
//        }
//    }
}