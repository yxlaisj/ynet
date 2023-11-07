package io;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;

public class SingleThreadSelectorTest {
    private ServerSocketChannel server;
    private Selector selector;
    int port = 8888;

    public void initServer() {
        try {
            server = ServerSocketChannel.open();
            server.configureBlocking(false);
            server.bind(new InetSocketAddress(port));

            // 多路复用器
            // linux下，优先用epoll， 也可以是select poll kqueue等
            // epoll -> epoll_create
            selector = Selector.open();
            // 如果在selector,poll下， jvm里维护一个数组， fd放进去
            // epoll --> epoll_ctl(efd, ADD, serverFD)
            // java这么做的目的是统一用一套selector表示各种多路复用器!
            server.register(selector, SelectionKey.OP_ACCEPT);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public void start() {
        initServer();
        System.out.println("Server started!");
        try {
            while (true) {
                Set<SelectionKey> keys = selector.keys();
                System.out.println(keys.size() + " size");

                //1,调用多路复用器(select,poll  or  epoll  (epoll_wait))
                /*
                select()是啥意思：
                1，select，poll  其实  内核的select（fd4）  poll(fd4)
                2，epoll：  其实 内核的 epoll_wait()，不用传文件描述符
                *, 参数可以带时间：没有时间，0  ：  阻塞，有时间设置一个超时
                selector.wakeup()  结果返回0

                懒加载：
                其实再触碰到selector.select()调用的时候触发了epoll_ctl的调用

                 */
                while (selector.select() > 0) {
                    System.out.println("获取有状态的fd..");
                    Set<SelectionKey> selectionKeys = selector.selectedKeys();
                    Iterator<SelectionKey> iterator = selectionKeys.iterator();
                    // 遍历有状态的fd
                    while (iterator.hasNext()) {
                        SelectionKey nextKey = iterator.next();
                        iterator.remove();  // 不移除会重复处理
                        // 有连接进来
                        if (nextKey.isAcceptable()) {
                            // 重点：接收一个新的连接
                            //accept接受连接且返回新连接的FD
                            //那新的FD怎么办？
                            //select，poll，因为他们内核没有空间，那么在jvm中保存和前边的fd4那个listen的一起
                            //epoll： 通过epoll_ctl把新的客户端fd注册到内核空间的红黑树
                            acceptHandler(nextKey);
                        } else if (nextKey.isReadable()) {
                            readHandler(nextKey);  //连read 还有 write都处理了
                            //在当前线程，这个方法可能会阻塞  ，如果阻塞了十年，其他的IO早就没电了。。。
                            //所以提出了 IO THREADS
                            //redis  是不是用了epoll，redis是不是有个io threads的概念 ，redis是不是单线程的
                            //tomcat 8,9  异步的处理方式  IO  和   处理上  解耦
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public void acceptHandler(SelectionKey key) {
        try {
            ServerSocketChannel channel = (ServerSocketChannel) key.channel();
            SocketChannel client = channel.accept();    // 不阻塞
            client.configureBlocking(false);

            ByteBuffer buffer = ByteBuffer.allocate(8192);
            /*
            这里和server注册的时候一样，只不过客户端专注于read事件
            select，poll：jvm里开辟一个数组 fd7 放进去
            epoll：  epoll_ctl(fd3,ADD,fd7,EPOLLIN
             */
            client.register(selector, SelectionKey.OP_READ, buffer);
            System.out.println("-------------------------------------------");
            System.out.println("新客户端：" + client.getRemoteAddress());
            System.out.println("-------------------------------------------");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void readHandler(SelectionKey key) {
        SocketChannel client = (SocketChannel) key.channel();
        ByteBuffer buffer = (ByteBuffer) key.attachment();
        buffer.clear();
        int read = 0;
        try {
            while (true) {
                read = client.read(buffer);
                if (read > 0) {
                    buffer.flip();
                    while (buffer.hasRemaining()) {
                        // 写回给客户端
                        client.write(buffer);
                    }
                    buffer.clear();
                } else if (read == 0) {
                    break;
                } else {
                    // 客户端关闭连接了
                    System.out.println("客户端关闭了");
                    client.close();
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args) {
        SingleThreadSelectorTest threadSelectorTest = new SingleThreadSelectorTest();
        threadSelectorTest.start();
    }
}
