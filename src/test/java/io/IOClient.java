package io;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;

public class IOClient {
    public static void main(String[] args) {
        LinkedList<SocketChannel> clients = new LinkedList<>();
        InetSocketAddress serverAddr = new InetSocketAddress("127.0.0.1", 8888);

        for (int i = 10000; i < 15000; i++) {
            try {
                SocketChannel client1 = SocketChannel.open();

                SocketChannel client2 = SocketChannel.open();

                client1.bind(new InetSocketAddress("127.0.0.1", i));
                //  192.168.150.1：10000   192.168.150.11：9090
                boolean connect = client1.connect(serverAddr);
                ByteBuffer allocate = ByteBuffer.wrap("你好，服务器\n".getBytes("UTF-8"));
                client1.write(allocate);
                clients.add(client1);
            } catch (IOException e) {
                e.printStackTrace();
            }


        }
        System.out.println("clients "+ clients.size());

        try {
            System.in.read();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
