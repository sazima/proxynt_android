package com.sazima.proxynt.common;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Set;


/**
 * tcp channel test
 * 
 * @author eric
 * @date Sep 2, 2012 9:17:40 PM
 */
public class TcpChannelTest {
    public String serverHost = "localhost";
    public int serverPort = 12345;

    private ServerSocketChannel server;
    private int clientSerial = 0;
    private int clientCount = 5;

    // test tcp non-blocking channel,
    public void testTcpNonBlockingChanne() throws IOException, InterruptedException {
        // start server
        startServerNonBlocking();

        Thread.sleep(500); // wait server to be ready, before start client,

        // start clients
        for (int i = 0; i < clientCount; i++) {
            startClientOnce();
        }

        // shutdown server,
        Thread.sleep(500); // wait client to be handled,
        shutdownServer();
    }

    // start non-blocking server,
    private void startServerNonBlocking() throws IOException {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    server = ServerSocketChannel.open();
                    server.socket().bind(new InetSocketAddress(serverHost, serverPort)); // bind,
                    server.configureBlocking(false); // non-blocking mode,

                    Selector selector = Selector.open();
                    server.register(selector, SelectionKey.OP_ACCEPT);

                    while (true) {
                        selector.select();
                        Set<SelectionKey> readyKeys = selector.selectedKeys();

                        // process each ready key...
                        Iterator<SelectionKey> iterator = readyKeys.iterator();
                        while (iterator.hasNext()) {
                            SelectionKey key = (SelectionKey) iterator.next();
                            iterator.remove();

                            if (key.isAcceptable()) {
                                SocketChannel client = server.accept();
                                System.out.printf("[%s]:\t%s\n", Thread.currentThread().getName(), "accept connection");
                                client.configureBlocking(false);

                                // prepare for read,
                                client.register(selector, SelectionKey.OP_READ);
                            } else if (key.isReadable()) {
                                // read
                                SocketChannel client = (SocketChannel) key.channel();
                                ByteBuffer inBuf = ByteBuffer.allocate(1024);
                                while (client.read(inBuf) > 0) {
                                    System.out.printf("[%s]:\t%s\n", Thread.currentThread().getName(), new String(inBuf.array(), StandardCharsets.UTF_8));
                                }

                                // prepare for write,
                                client.register(selector, SelectionKey.OP_WRITE);
                            } else if (key.isWritable()) {
                                SocketChannel client = (SocketChannel) key.channel();
                                String response = "hi - from non-blocking server";
                                byte[] bs = response.getBytes(StandardCharsets.UTF_8);
                                ByteBuffer buffer = ByteBuffer.wrap(bs);
                                client.write(buffer);

                                // switch to read, and disable write,
                                client.register(selector, SelectionKey.OP_READ);
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }, "t-server-threads").start();
    }

    // close server,
    private void shutdownServer() {
        try {
            if (server != null) {
                server.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * <p>
     * tcp client - via channel,
     * </p>
     * <p>
     * It send once request.
     * </p>
     * 
     * @throws IOException
     */
    private void startClientOnce() throws IOException {
        // start client in a new thread
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    SocketChannel client = SocketChannel.open(new InetSocketAddress(serverHost, serverPort));
                    // write
                    String request = "hello - from client [" + Thread.currentThread().getName() + "}";
                    byte[] bs = request.getBytes(StandardCharsets.UTF_8);
                    ByteBuffer buffer = ByteBuffer.wrap(bs);
                    while (buffer.hasRemaining()) {
                        client.write(buffer);
                    }

                    // read
                    ByteBuffer inBuf = ByteBuffer.allocate(1024);
                    while (client.read(inBuf) > 0) {
                        System.out.printf("[%s]:\t%s\n", Thread.currentThread().getName(), new String(inBuf.array(), StandardCharsets.UTF_8));
                    }

                    client.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }, "t-channelClient-" + clientSerial++).start();
    }
}