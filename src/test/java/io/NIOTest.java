package io;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.Objects;

public class NIOTest {
    public static void main(String[] args) throws IOException, InterruptedException {


        // 服务端开启，监听端口号，接受客户端
        ServerSocketChannel server = ServerSocketChannel.open();
        server.bind(new InetSocketAddress(8888));
        server.configureBlocking(false);    // 非阻塞 non-blocking

        // 既然是非阻塞，就需要有一个列表保存接受到的所有socketchannel
        // 这样就有机会在一个线程或者几个线程中处理已经获取的连接
        LinkedList<SocketChannel> clients = new LinkedList<>();
        while (true) {
            Thread.sleep(1000);
            SocketChannel client = server.accept(); // 已经设置为非阻塞!
            // 内核调用，如果为null表示没有接收到连接
            if (Objects.isNull(client)) {
                System.out.println("空转无连接。。。");
            } else {
                // 获取了client，得设置这个client channel也为非阻塞
                client.configureBlocking(false);
                int port = client.socket().getPort();
                System.out.println("你好，客户端: " + port);
                clients.add(client);
            }

            ByteBuffer buffer = ByteBuffer.allocate(4096);    // 分配到堆内
//            ((DirectBuffer)buffer).cleaner().clean(); // 堆外内存没有gc，需要手动回收申请的区域

            // 注意这里，在程序中遍历已经获取的全量socket客户端以获取数据
            // 这里是性能损耗的重点，因为很多客户端必然没有数据，但都要一个个去内核中询问
            // 涉及到循环的用户态和内核态的切换
            for (SocketChannel c : clients) {
                int res = c.read(buffer);   // 不会阻塞
                if (res > 0) {
                    buffer.flip();
                    byte[] bytes = new byte[buffer.limit()];
                    buffer.get(bytes);

                    String msg = new String(bytes);
                    System.out.println("客户端" + c.socket().getPort() + " 说：" + msg);
                    buffer.clear(); // 这是清空缓存，不是回收这块区域
                }
            }


        }


    }
}
