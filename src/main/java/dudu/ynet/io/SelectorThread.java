package dudu.ynet.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 多路复用器线程
 * 一个线程对应一个Selector
 * 多线程下，客户端的fd会被分配到不同的selector上
 * 不同的selector不会有交互
 * 相当于netty中的NioEventLoop
 */
public class SelectorThread implements Runnable {

    Selector selector;
    //
    LinkedBlockingQueue<Channel> channelQueue = new LinkedBlockingQueue<>();

    SelectorThreadGroup stg;

    public SelectorThread(SelectorThreadGroup stg) {
        this.stg = stg;
        try {
            selector = Selector.open();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void run() {
        // 类似nettyEventLoop，一直转圈
        while (true) {
            try {
//                System.out.println(Thread.currentThread().getName() + "   : before select(), " + selector.keys().size());
                // 1阻塞，问内核多路复用中是否有东西
                // 如果其它线程调用了此selector的wakeup，就不会阻塞了
                int num = selector.select();
//                System.out.println(Thread.currentThread().getName() + "   : after select()," + selector.keys().size());
                // 2处理selectKeys
                if (num > 0) {
                    // 拿到事件
                    Set<SelectionKey> keys = selector.selectedKeys();
                    Iterator<SelectionKey> iter = keys.iterator();
                    while (iter.hasNext()) {    // 线性处理
                        SelectionKey key = iter.next();
                        iter.remove();
                        if (key.isAcceptable()) {
                            // 接收客户端，多线程下，新的客户端要注册在哪一个selector上?
                            acceptHandler(key);

                        } else if (key.isReadable()) {
                            readHandler(key);
                        } else if (key.isWritable()) {

                        }


                    }

                }
                // 3处理注册
                if (!channelQueue.isEmpty()) {
                    Channel channel = channelQueue.poll();
                    if (channel instanceof ServerSocketChannel) {
                        ServerSocketChannel server = (ServerSocketChannel) channel;
                        server.register(selector, SelectionKey.OP_ACCEPT);
                    } else if (channel instanceof SocketChannel) {
                        SocketChannel client = (SocketChannel) channel;
                        ByteBuffer buffer = ByteBuffer.allocate(4096);
                        client.register(selector, SelectionKey.OP_READ, buffer);
                    }
                }


            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }


    private void acceptHandler(SelectionKey key) {
        System.out.println(Thread.currentThread().getName()  + " acceptHandler....");
        ServerSocketChannel server = (ServerSocketChannel) key.channel();
        try {
            SocketChannel client = server.accept();
            client.configureBlocking(false);
            // 需要选择一个selector，注册上，最简单的算法就是轮询
            stg.nextSelector(client);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    private void readHandler(SelectionKey key) {
        System.out.println(Thread.currentThread().getName() + " readHandler....");
        ByteBuffer buffer = (ByteBuffer) key.attachment();
        SocketChannel client = (SocketChannel) key.channel();
        buffer.clear();
        // 读取数据
        while (true) {
            try {
                int readNum = client.read(buffer);
                if (readNum > 0) {
                    // 写出
                    buffer.flip();  // 翻转buffer
                    while (buffer.hasRemaining()) {
                        client.write(buffer);
                    }
                    buffer.clear();
                } else if (readNum == 0) {
                    break;
                } else if (readNum < 0) {   // 客户端断开了
                    System.out.println(client.getRemoteAddress() + " 断开了");
                    key.cancel();
                    break;
                }


            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
