package com.sazima.proxynt.cilent;

import android.util.Log;

import com.sazima.proxynt.JWebSocketClient;
import com.sazima.proxynt.common.SelectPool;
import com.sazima.proxynt.common.SeriallizerUtils;
import com.sazima.proxynt.constant.MessageTypeConstant;
import com.sazima.proxynt.entity.MessageEntity;
import com.sazima.proxynt.entity.TcpOverWebsocketMessage;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

public class TcpForwardClient {
    private HashMap<ByteBuffer, SocketChannel> uidToSocket = new HashMap<>();
    private HashMap<SocketChannel, ByteBuffer> socketToUid = new HashMap<>();
    private HashMap<ByteBuffer, String> uidToName = new HashMap<>();
    private boolean isRunning = true;
    private JWebSocketClient webSocketClient;
    private SelectPool selectPool;
    private boolean isOpen = false;

    public boolean start() throws IOException {
        if (isOpen) {
            Log.i("tcp forward client", "already open");
            return true;
        }
        if (selectPool == null) {
            selectPool = new SelectPool();
            this.selectPool.setTcpForwardClient(this);
        }
        selectPool.open();
        class Thread1 implements Runnable {
            public synchronized void run() {
                try {
                    selectPool.run();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        Thread1 t = new Thread1();
        new Thread(t).start();
        isOpen = true;
        return true;
    }

    public void processMessage(SocketChannel socketChannel) {
        try {
            ByteBuffer buffer = ByteBuffer.allocate(10240);
            int read = socketChannel.read(buffer);
            Log.i("tcp forarwd", "size : " + read);
            byte[] allArrays = buffer.array();
            byte[] realArrays = new byte[0];
            if(read == -1) {
            } else {
                realArrays = new byte[read];
                System.arraycopy(allArrays, 0, realArrays, 0, read);
            }
            TcpOverWebsocketMessage tcpOverWebsocketMessage = new TcpOverWebsocketMessage();
            ByteBuffer uidBuffer = socketToUid.get(socketChannel);
            String name = uidToName.get(uidBuffer);

            tcpOverWebsocketMessage.setUid(uidBuffer.array());
            tcpOverWebsocketMessage.setName(name);
            tcpOverWebsocketMessage.setIp_port("");
            tcpOverWebsocketMessage.setData(realArrays);
            MessageEntity<TcpOverWebsocketMessage> messageMessageEntity = new MessageEntity<>();
            messageMessageEntity.setData(tcpOverWebsocketMessage);
            messageMessageEntity.setType_(MessageTypeConstant.WEBSOCKET_OVER_TCP);
            byte[] sendBytes = new SeriallizerUtils().dumps(messageMessageEntity);
            webSocketClient.send(sendBytes);
            Log.i("tcp forarwd", "send to websocket, len: " + sendBytes.length);
        } catch (Exception e) {
            e.printStackTrace();
        }
//        buffer.flip();

    }

    public void createSocket(String name, byte[] uid, String ipPort) {
        ByteBuffer uidBuffer = ByteBuffer.wrap(uid);
        if (uidToSocket.containsKey(uidBuffer)) {

            return;
        }
        String[] split = ipPort.split(":");
        String ip = split[0];
        int port = Integer.valueOf(split[1]);
        try {
            SocketChannel socketChannel = SocketChannel.open(new InetSocketAddress(ip, port));
            uidToSocket.put(uidBuffer, socketChannel);
            socketToUid.put(socketChannel, uidBuffer);
            uidToName.put(uidBuffer, name);
            socketChannel.configureBlocking(false);
            selectPool.register(socketChannel);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean sendByUid(byte[] uid, ByteBuffer msg) throws IOException {
        ByteBuffer uidBuffer = ByteBuffer.wrap(uid);
        SocketChannel socketChannel = uidToSocket.get(uidBuffer);
        if (socketChannel == null) {
            Log.e("tcpforward", "socketChannel is null");
        }
        if (msg.array().length == 0) {
            socketChannel.close();
            return true;
        }
        if (null == msg) {
            Log.e("tcpforward", "msg is null");
            return false;
        }
        try {
            socketChannel.write(msg);
            Log.i("tcpforwrad", "send to success, len: " + msg.array().length);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } catch (NullPointerException e) {

            e.printStackTrace();
            return false;
        }
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void setRunning(boolean running) {
        isRunning = running;
    }

    public JWebSocketClient getWebSocketClient() {
        return webSocketClient;
    }

    public void setWebSocketClient(JWebSocketClient webSocketClient) {
        this.webSocketClient = webSocketClient;
    }
}
