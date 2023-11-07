package dudu.ynet.io;

/**
 * 主线程
 * 不做业务逻辑
 */
public class MainThread {

    public static void main(String[] args) {
        // 创建出SelectorThread，可以是一个或多个
        // 只有一个线程负责accept连接，每个线程都会被分配cient,进行R/W，混杂模式
        SelectorThreadGroup stg = new SelectorThreadGroup(3);
//        SelectorThreadGroup stg = new SelectorThreadGroup(3);

        // 注册监听到某个selector上
        stg.bind(8888);

    }
}
