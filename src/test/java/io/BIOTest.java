package io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class BIOTest {
    public static void main(String[] args) throws Exception {
        ServerSocket serverSocket = new ServerSocket(8888, 20);
        System.out.println("BIO服务器启动成功，监听8888端口。。。");
        while (true) {
            Socket client = serverSocket.accept();  // 阻塞
            System.out.println("有客户端接入：" + client.getPort());

            /*
                必须抛出线程处理具体业务，否则会阻塞其它连接
             */
            new Thread(() -> {
                try(InputStream in = client.getInputStream()) {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(in));) {
                        String line = null;
                        while ((line = reader.readLine()) != null) {
                            System.out.println(client.getPort() + ":" +line);
                        }
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } finally {
                    try {
                        client.close();
                    } catch (IOException e) {

                    }
                }

            }).start();

        }
    }
}
