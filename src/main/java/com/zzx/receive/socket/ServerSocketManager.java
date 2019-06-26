package com.zzx.receive.socket;

import org.springframework.stereotype.Component;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.*;


@Component

public class ServerSocketManager { //ServerSocketManager.java开始

    private static ServerSocketManager manager;
    // 存储客户端ip-SocketChannel
    static Map<String, SocketChannel> map;
    // 连接监听
    private static SelectorLoop connectionBell;
    // 读取监听
    private static List<SelectorLoop> readBells;
    private ServerSocketChannel ssc;
    // 标识是否已经开启读取客户端数据的线程
    public boolean isReadBellRunning = false;
    // 客户端连接断开监听
    private List<OnClientListener> clientListeners;
    // 接收客户端信息监听
    private List<OnReceivedMessageListener> messageListeners;

    public void addOnClientListener(OnClientListener clientListener) {
        if (!this.clientListeners.contains(clientListener)) {
            this.clientListeners.add(clientListener);
        }
    }

    public void removeClientListener(OnClientListener clientListener) {
        this.clientListeners.remove(clientListener);
    }

    public void addOnReveicedMessageListener(
            OnReceivedMessageListener messageListener) {
        if (!this.messageListeners.contains(messageListener)) {
            this.messageListeners.add(messageListener);
        }
    }

    public void removeOnReveicedMessageListener(
            OnReceivedMessageListener messageListener) {
        this.messageListeners.remove(messageListener);
    }

    private ServerSocketManager() {
        map = new HashMap<String, SocketChannel>();
        clientListeners = new LinkedList<OnClientListener>();
        messageListeners = new LinkedList<ServerSocketManager.OnReceivedMessageListener>();
    }

    public synchronized static ServerSocketManager getInstance() {
        if (manager == null) {
            manager = new ServerSocketManager();
        }
        return manager;
    }

    public void startServer(String host, int port) throws IOException {
        System.out.println("-------ip-----" + host);
        readBells = new LinkedList<ServerSocketManager.SelectorLoop>();
        connectionBell = new SelectorLoop();
        // 开启一个server channel来监听
        ssc = ServerSocketChannel.open();
        // 开启非阻塞模式
        ssc.configureBlocking(false);
        ServerSocket socket = ssc.socket();
        socket.bind(new InetSocketAddress(host, port));
        // 给闹钟规定好要监听报告的事件,这个闹钟只监听新连接事件.
        ssc.register(connectionBell.getSelector(), SelectionKey.OP_ACCEPT);
        new Thread(connectionBell).start();
    }

    public class SelectorLoop implements Runnable {
        private Selector selector;
        private ByteBuffer temp = ByteBuffer.allocate(1024);
        private boolean stop;
        private boolean using;


        public SelectorLoop() throws IOException {
            this.selector = Selector.open();
        }

        public Selector getSelector() {
            return this.selector;
        }

        public void stop() throws IOException {
            this.stop = true;
            if (this.selector.isOpen()) {
                this.selector.close();
                this.selector = null;
            }
        }

        @Override
        public void run() {
            using = true;
            while (!stop) {
                System.out.println("-----------");
                try {
                    // 阻塞,只有当至少一个注册的事件发生的时候才会继续.
                    if (this.selector.select() > 0) {
                        Set<SelectionKey> selectKeys = this.selector
                                .selectedKeys();
                        Iterator<SelectionKey> it = selectKeys.iterator();
                        while (it.hasNext()) {
                            SelectionKey key = it.next();
                            it.remove();
                            // 处理事件. 可以用多线程来处理.
                            this.dispatch(key);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (Exception e) {

                }
            }
            stop = true;
        }

        public void dispatch(SelectionKey key) throws IOException, InterruptedException {
            System.out.println("-----dispatch----------");
            // 测试此键的通道是否已准备好接受新的套接字连接。
            if (key.isAcceptable()) {
                System.out.println("-----isAcceptable----------");
                // 这是一个connection accept事件, 并且这个事件是注册在serversocketchannel上的.
                // 返回创建此键的通道
                ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
                // 接受一个连接.
                SocketChannel sc = ssc.accept();
                // 对新的连接的channel注册read事件. 使用readBell闹钟.
                sc.configureBlocking(false);
                SelectorLoop readBell = new SelectorLoop();
                readBells.add(readBell);
                sc.register(readBell.getSelector(), SelectionKey.OP_READ);
                String host = ((InetSocketAddress) sc.getRemoteAddress())
                        .getHostString();

                int port = ((InetSocketAddress) sc.getRemoteAddress()).getPort();//部署在外网时，需要获取随机的端口
                host = host + ":" + port;
                // 存放连接的socket
                map.put(host, sc);
                if (!clientListeners.isEmpty()) {
                    // 触发连接监听
                    for (OnClientListener cl : clientListeners) {
                        cl.onConnected(host);
                    }
                }
                // 如果读取线程还没有启动,那就启动一个读取线程.
                // synchronized (ServerSocketManager.this) {
                // if (!ServerSocketManager.this.isReadBellRunning) {
                // ServerSocketManager.this.isReadBellRunning = true;
                new Thread(readBell).start();
                // }
                // }
            } else if (key.isReadable()) {
                // 这是一个read事件,并且这个事件是注册在socketchannel上的.
                SocketChannel sc = (SocketChannel) key.channel();
                String host = ((InetSocketAddress) sc.getRemoteAddress())
                        .getHostString();

                int port = ((InetSocketAddress) sc.getRemoteAddress()).getPort();//部署在外网时，需要获取随机的端口
                host = host + ":" + port;
                // 写数据到buffer
                int count = sc.read(temp);
                System.out.println("-------count-------" + count);
                if (count < 0) {
                    map.remove(host);
                    stop();
                    if (!clientListeners.isEmpty()) {
                        for (OnClientListener cl : clientListeners) {
                            cl.onDisconnected(host);
                        }
                    }
                    // 客户端已经断开连接.
                    key.cancel();
                    sc.close();
                    return;
                }
                // 切换buffer到读状态,内部指针归位.
                temp.flip();
                String msg = Charset.forName("UTF-8").decode(temp).toString();
                // 清空buffer
                temp.clear();
                if (msg != null && !"".equals(msg)
                        && !messageListeners.isEmpty()) {
                    for (OnReceivedMessageListener rml : messageListeners) {
                        rml.onReceivedMessage(host, msg);
                    }
                }
            }
        }
    }


    /**
     * 监听客户端连接和断开
     *
     * @author YJH
     */
    public interface OnClientListener {
        /**
         * 有客户端连接时调用
         *
         * @param host
         */
        void onConnected(String host);

        /**
         * 有客户端断开连接时调用
         *
         * @param host
         */
        void onDisconnected(String host);
    }

    /**
     * 监听客户端发送来的消息
     *
     * @author YJH
     */
    public interface OnReceivedMessageListener {
        /**
         * 接收到消息
         *
         * @param host 客户端ip
         * @param msg  客户端信息
         */
        void onReceivedMessage(String host, String msg);
    }


    /**
     * 发送信息
     *
     * @param signKey
     * @param msg
     * @throws IOException
     */

    public int sendMessage(String signKey, byte[] msg) throws IOException {
        //通过singKey获取host
        String host = ServerSocketService.mapSignal.get(signKey);
        System.out.println("host");
        System.out.println("host====" + host);
        SocketChannel sc = map.get(host);
        int retnum = 0;
        if (sc == null) {
            // System.out.println("--------发送失败,未找到对应的连接信息---------");
            return 0;
        }
        if (sc.isConnected()) {
            // echo back.
            sc.write(ByteBuffer.wrap(msg));
            retnum = 1;
        } else {
            System.out.println("---SocketChannel未开启--");
            retnum = -1;
        }
        return retnum;
    }

    /**
     * 发送信息
     *
     * @param host
     * @param msg
     * @param charset
     * @throws IOException
     */
    public int sendMessage(String host, String msg, String charset)
            throws IOException {
        return sendMessage(host, msg.getBytes(charset));
    }

    /**
     * 发送信息 charset default utf-8
     *
     * @param host
     * @param msg
     * @throws IOException
     */
    public int sendMessage(String host, String msg) throws IOException {
        return sendMessage(host, msg.getBytes("UTF-8"));
    }


}