package com.sazima.proxynt.common;

import android.util.Log;

import com.sazima.proxynt.cilent.TcpForwardClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class SelectPool {
    private Selector selector;
    private TcpForwardClient tcpForwardClient;

    public void run() throws IOException {
        Log.i("select", "start select");
        while (selector.isOpen()) {
            try {
                selector.select(1);
                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> iter = selectedKeys.iterator();
                while (iter.hasNext()) {
                    SelectionKey key = iter.next();
                    int interestOps = key.interestOps();
                    if (!key.isReadable()) {
                        continue;
                    }
                    SocketChannel client = (SocketChannel) key.channel();
                    tcpForwardClient.processMessage(client);
//                    client.read(buffer);
//                    buffer.flip();
//                    client.write(buffer);
//                    buffer.clear();
                    iter.remove();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    public void open() throws IOException {
        if (selector == null) {
            selector = Selector.open();
        }
    }
    public void stop() throws IOException {
        if (selector != null) {
            selector.close();
        }
    }

    public boolean register(SocketChannel socketChannel) {
        try {
            socketChannel.register(selector, SelectionKey.OP_READ);
            return true;
        } catch (ClosedChannelException e) {
            e.printStackTrace();
            return false;
        }
//        catch (Exception e) {
//            e.printStackTrace();
//            return false;
//        }
    }


    public TcpForwardClient getTcpForwardClient() {
        return tcpForwardClient;
    }

    public void setTcpForwardClient(TcpForwardClient tcpForwardClient) {
        this.tcpForwardClient = tcpForwardClient;
    }
}
