package com.sazima.proxynt.common;

import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sazima.proxynt.constant.MessageTypeConstant;
import com.sazima.proxynt.entity.MessageEntity;
import com.sazima.proxynt.entity.PushConfigEntity;
import com.sazima.proxynt.entity.TcpOverWebsocketMessage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

public class SeriallizerUtils {
    private final Integer HEADER_LEN = 38;
    private final Integer UID_LEN = 4;
    public static String key;

    @RequiresApi(api = Build.VERSION_CODES.O)
    public MessageEntity loads(byte[] bytes) throws NoSuchAlgorithmException, IOException {
        bytes = EncryptUtils.decrypt(bytes);
        byte[] type = new byte[1];
        byte[] bodyLen = new byte[4];
        System.arraycopy(bytes, 0, type, 0, 1);
        String typeString = new String(type);
        System.arraycopy(bytes, 1, bodyLen, 0, 4);
        long bodyLenInt = byteArrayToLeUnsignInt(bodyLen);
        MessageEntity messageEntity = null;
        byte[] bodyBytes;

        switch (typeString) {
            case MessageTypeConstant.PUSH_CONFIG:
                bodyBytes = new byte[(int) bodyLenInt];
                System.arraycopy(bytes, HEADER_LEN, bodyBytes, 0, (int) bodyLenInt);
                Gson gson = new Gson();
                String str = new String(bodyBytes);
                messageEntity = new Gson().fromJson(str, new TypeToken<MessageEntity<PushConfigEntity>>() {
                }.getType());
                break;
            case MessageTypeConstant.WEBSOCKET_OVER_TCP:
            case MessageTypeConstant.REQUEST_TO_CONNECT:
                bodyBytes = new byte[(int) bodyLenInt];
                System.arraycopy(bytes, HEADER_LEN, bodyBytes, 0, (int) bodyLenInt);
                int start = 0;
                byte[] lenName = new byte[1];
                byte[] lenIpPort = new byte[1];
                byte[] lenBytes = new byte[4];
                System.arraycopy(bodyBytes, 0, lenName, 0, 1);
                System.arraycopy(bodyBytes, 1, lenIpPort, 0, 1);
                System.arraycopy(bodyBytes, 4, lenBytes, 0, 4);

                start += 8;
                int lenNameInt = Byte.toUnsignedInt(lenName[0]);
                int lenIpPortInt = Byte.toUnsignedInt(lenIpPort[0]);
                long lenBytesInt = TypeConversion.bytesToUInt32(lenBytes, 0);
                byte[] uid = new byte[UID_LEN];
                byte[] name = new byte[lenNameInt];
                byte[] ipPort = new byte[lenIpPortInt];
                byte[] socketData = new byte[(int) lenBytesInt];
                System.arraycopy(bodyBytes, start, uid, 0, UID_LEN);
                start += UID_LEN;
                System.arraycopy(bodyBytes, start, name, 0, lenNameInt);
                start += lenNameInt;
                System.arraycopy(bodyBytes, start, ipPort, 0, lenIpPortInt);
                start += lenIpPortInt;
                System.arraycopy(bodyBytes, start, socketData, 0, (int) lenBytesInt);
                start += lenBytesInt;
                TcpOverWebsocketMessage tcpOverWebsocketMessage = new TcpOverWebsocketMessage();
                tcpOverWebsocketMessage.setData(socketData);
                tcpOverWebsocketMessage.setIp_port(new String(ipPort));
                tcpOverWebsocketMessage.setName(new String(name));
                tcpOverWebsocketMessage.setUid(uid);
                MessageEntity<TcpOverWebsocketMessage> messageMessageEntity = new MessageEntity<>();
                messageMessageEntity.setType_(typeString);
                messageMessageEntity.setData(tcpOverWebsocketMessage);
                messageEntity = new MessageEntity<>();
                messageEntity.setType_(typeString);
                messageEntity.setData(tcpOverWebsocketMessage);
                break;

            case MessageTypeConstant.PING:
                bodyBytes = new byte[(int) bodyLenInt];
                System.arraycopy(bytes, HEADER_LEN, bodyBytes, 0, (int) bodyLenInt);
                messageEntity = new MessageEntity<>();
                messageEntity.setType_(typeString);
                messageEntity.setData(null);
                break;
        }

        return messageEntity;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public byte[] dumps(MessageEntity messageEntity) throws NoSuchAlgorithmException, IOException {
        String type_ = messageEntity.getType_();
        Log.i("dumps", messageEntity.toString());
        byte[] body = new byte[5];
        if (type_.equals(MessageTypeConstant.PUSH_CONFIG) || type_.equals(MessageTypeConstant.PING)) {
            Gson gson = new Gson();
            String str = gson.toJson(messageEntity);
            body = str.getBytes();
            Log.i("msg", str);
        } else if (type_.equals(MessageTypeConstant.WEBSOCKET_OVER_TCP) || type_.equals(MessageTypeConstant.REQUEST_TO_CONNECT)) {
            TcpOverWebsocketMessage data = (TcpOverWebsocketMessage) messageEntity.getData();
            ByteArrayOutputStream bodyStream = new ByteArrayOutputStream();
            byte[] uid = data.getUid();
            byte[] nameBytes = data.getName().getBytes(StandardCharsets.UTF_8);
            bodyStream.write(unsignCharToByteArray(nameBytes.length));
            bodyStream.write(unsignCharToByteArray(data.getIp_port().length()));
            bodyStream.write(new byte[2]);
            bodyStream.write(unsignIntToByteArray(data.getData().length));
            bodyStream.write(uid);
            bodyStream.write(nameBytes);
            bodyStream.write(data.getIp_port().getBytes(StandardCharsets.UTF_8));
            bodyStream.write(data.getData());
            body = bodyStream.toByteArray();
        }
        int bodyLen = body.length;
        byte[] nonce = new byte[5];
        new Random().nextBytes(nonce);
        byte[] timestamp = unsignIntToByteArray((int) new Date().getTime());
        byte[] empty = new byte[8];
        ByteArrayOutputStream returnMessage = new ByteArrayOutputStream();
        returnMessage.write(type_.getBytes());
        returnMessage.write(unsignIntToByteArray(bodyLen));
        returnMessage.write(nonce);
        returnMessage.write(timestamp);
//        byte[] hash = new byte[16];
        ByteArrayOutputStream needSashSteam = new ByteArrayOutputStream();
        needSashSteam.write(nonce);
        byte[] bodyPrefix = new byte[12];
        System.arraycopy(body, 0, bodyPrefix, 0, 12);
        needSashSteam.write(timestamp);
        needSashSteam.write(bodyPrefix);
        needSashSteam.write(key.getBytes());
        byte[] hash = getMd5(needSashSteam.toByteArray());
        returnMessage.write(hash);
        returnMessage.write(empty);
        returnMessage.write(body);

        Log.i("encrypt", "start encrypt");
        try {
//            byte[] bytes = new EncryptUtils().encrypt("hello world".getBytes());
            byte[] bytes = EncryptUtils.encrypt(returnMessage.toByteArray());
            System.out.printf(String.valueOf(bytes));
            Log.i("encrypt", "end encrypt");
            return bytes;
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }


    private int byteArrayToLeUnsignInt(byte[] b) {
        ByteBuffer bb = ByteBuffer.wrap(b);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        int int1 = bb.getInt();
        return int1;
    }

    private byte[] unsignIntToByteArray(int i) {
        final ByteBuffer bb = ByteBuffer.allocate(4);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        bb.putInt(i);
        return bb.array();
    }

    private byte[] unsignCharToByteArray(int i) {
        return new byte[]{(byte) i};
//        return
//        ByteBuffer bb = ByteBuffer.allocate(1);
//        bb.order(ByteOrder.LITTLE_ENDIAN);
//        bb.putInt(i);
//        return bb.array();
    }

    private int byteArrayToLeUnsignChar(byte[] b) {
        return (int) b[0];
    }

    private byte[] getMd5(byte[] bytesOfMessage) {
//        byte[] bytesOfMessage = yourString.getBytes();
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        return md.digest(bytesOfMessage);
    }

}
