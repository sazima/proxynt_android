package com.sazima.proxynt;

import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.sazima.proxynt.cilent.TcpForwardClient;
import com.sazima.proxynt.common.SeriallizerUtils;
import com.sazima.proxynt.constant.MessageTypeConstant;
import com.sazima.proxynt.entity.ClientConfigEntity;
import com.sazima.proxynt.entity.MessageEntity;
import com.sazima.proxynt.entity.PushConfigEntity;
import com.sazima.proxynt.entity.TcpOverWebsocketMessage;

import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ServerHandshake;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

public class JWebSocketClient extends WebSocketClient {
    TcpForwardClient tcpForwardClient;
    public ClientConfigEntity configEntity;

    public JWebSocketClient(URI serverUri, ClientConfigEntity clientConfigEntity) {
        super(serverUri, new Draft_6455());
        this.configEntity = clientConfigEntity;
    }

    @Override
    public void send(byte[] data) {
        super.send(data);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onOpen(ServerHandshake handshakedata) {
        try {
            tcpForwardClient = new TcpForwardClient();
            tcpForwardClient.setWebSocketClient(this);
            Log.i("openopen", "open--------------------");
            System.out.println("open open");
            tcpForwardClient.start();
            PushConfigEntity pushConfigEntity = new PushConfigEntity();
            pushConfigEntity.setConfig_list(new ArrayList<>());
            pushConfigEntity.setClient_name(configEntity.getClient_name());
//            pushConfigEntity.setVersion("");
            pushConfigEntity.setKey(configEntity.getServer().getPassword());
            MessageEntity<PushConfigEntity> objectMessageEntity = new MessageEntity<>();
            objectMessageEntity.setData(pushConfigEntity);
            objectMessageEntity.setType_(MessageTypeConstant.PUSH_CONFIG);
            byte[] dumps = new SeriallizerUtils().dumps(objectMessageEntity);
            send(dumps);
        } catch ( Exception e) {
            e.printStackTrace();
            Log.e("JWebSocketClient", "onOpen()");
        }
    }

    @Override
    public void onMessage(String message) {
    }

//    @Override
    @RequiresApi(api = Build.VERSION_CODES.O)
    public void onMessage(ByteBuffer buff){
        byte[] array = buff.array();
        try {
            MessageEntity messageEntity = new SeriallizerUtils().loads(array);
            Log.i("buffer-------------", messageEntity.toString());
            switch (messageEntity.getType_()){
                case MessageTypeConstant.PING:
                    send(array);
                    break;
                case MessageTypeConstant.PUSH_CONFIG:
                    break;
                case MessageTypeConstant.REQUEST_TO_CONNECT:
                    TcpOverWebsocketMessage data = (TcpOverWebsocketMessage) messageEntity.getData();
                    if (null == data) {
                        Log.e("e", "data is null");
                    }
                    tcpForwardClient.createSocket(data.getName(), data.getUid(), data.getIp_port());
                    break;
                case MessageTypeConstant.WEBSOCKET_OVER_TCP:
                    TcpOverWebsocketMessage data1 = (TcpOverWebsocketMessage) messageEntity.getData();

                    if (null == data1) {
                        Log.e("e", "data1 is null");
                    }
                    if (data1.getData() == null) {
                        Log.e("e", "data1 is null");
                    }
                    tcpForwardClient.sendByUid( data1.getUid(), ByteBuffer.wrap(data1.getData()));
                    break;
            }

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onClose(int code, String reason, boolean remote) {
        Log.e("JWebSocketClient", "onClose()");
    }

    @Override
    public void onError(Exception ex) {
        try {

            Log.e("error", ex.getMessage());
        } catch ( Exception e) {
            e.printStackTrace();
        }
    }



}