package dudu.ynet.io;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 用于管理组内的SelectorThread
 */
public class SelectorThreadGroup {

    SelectorThread[] sts;
    ServerSocketChannel server;
    private final AtomicInteger xid = new AtomicInteger(0);


    /**
     * @param num 线程数
     */
    public SelectorThreadGroup(int num) {
        sts = new SelectorThread[num];
        for (int i = 0; i < sts.length; i++) {
            sts[i] = new SelectorThread(this);
            new Thread(sts[i]).start();
        }
    }

    public void bind(int port) {
        try {
            server = ServerSocketChannel.open();
            server.configureBlocking(false);
            server.bind(new InetSocketAddress(port));

            // 把server注册到某一个selector(轮询)
            nextSelector(server);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * 轮询
     */
    private SelectorThread next() {
        int index = xid.incrementAndGet() % sts.length;
        return sts[index];
    }

    /**
     * c有可能是server，有可能是client
     */
    void nextSelector(Channel c) {
        SelectorThread st = next();

//        // 放入队列
        st.channelQueue.add(c);
//        // 再wakeup，让对应的线程自己去完成注册
        st.selector.wakeup();

        /*  这种写法不好，多线程下会有些无法预料的问题
        try {
            if (c instanceof ServerSocketChannel) {
                ServerSocketChannel s = (ServerSocketChannel) c;
//                st.selector.wakeup();
                s.register(st.selector, SelectionKey.OP_ACCEPT);
                st.selector.wakeup();
                System.out.println("server...");

            } else if (c instanceof SocketChannel) {
                SocketChannel client = (SocketChannel) c;
//                st.selector.wakeup();
                ByteBuffer buffer = ByteBuffer.allocate(4096);
                client.register(st.selector, SelectionKey.OP_READ, buffer);
                st.selector.wakeup();
                System.out.println("client....");
            }
        } catch (ClosedChannelException e) {
            throw new RuntimeException(e);
        }
*/
    }

}
