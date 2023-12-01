package dudu.ynet.io;

/**
 * 主线程
 * 不做业务逻辑
 */
public class MainThread {

    public static void main(String[] args) {
        // 创建出SelectorThread，可以是一个或多个
        // 只有一个线程负责accept连接，每个线程都会被分配cient,进行R/W，混杂模式
        SelectorThreadGroup boss = new SelectorThreadGroup(2);
        SelectorThreadGroup worker = new SelectorThreadGroup(3);

        // boss持有worker
        boss.setWorker(worker);
        // 注册监听到某个selector上
        boss.bind(8888);
        boss.bind(9999);
        boss.bind(7777);

    }
}
