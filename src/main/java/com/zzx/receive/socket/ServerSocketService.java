package com.zzx.receive.socket;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.zzx.receive.tools.Utils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service    //ServerSocketService.java开始
public class ServerSocketService {

    private ServerSocketManager manager;
    public static Map<String, String> mapSignal = new HashMap<>();
    public static Map<String, String> mapPay = new HashMap<>();

    private ServerSocketManager.OnClientListener clientListener = new ServerSocketManager.OnClientListener() {
        @Override
        public void onDisconnected(String host) {
            System.out.println("断开:" + host);
            //断开连接时要做的操作写这里
        }

        @Override
        public void onConnected(String host) {
            System.out.println("接入:" + host);
            //连接上时要做的操作写这里
        }
    };


    private ServerSocketManager.OnReceivedMessageListener messageListener = new ServerSocketManager.OnReceivedMessageListener() {

        @Override   //host是接入的ip，msg是接收到的信息
        public void onReceivedMessage(String host, String msg) {
            System.out.println("----msg--------" + msg + "host是：" + host);
            //TODO 待处理
            JSONObject object = JSON.parseObject(msg);
            switch (String.valueOf(object.get("code"))) {
                case "1":
                    String signKey = String.valueOf(object.get("signKey"));
                    mapSignal.remove(signKey);
                    mapSignal.put(signKey, host);
                    System.out.println(signKey);
                    break;
                case "2":
                    String newsign = "code=" + object.get("code") + "&orderNo=" + object.get("orderNo") + "&signKey=" + object.get("signKey") + "&url=" + object.get("url") + "&key=888888";
                    newsign = Utils.md5(newsign).toUpperCase();
                    if (newsign.equals(object.get("sign"))) {
                        mapPay.put(String.valueOf(object.get("orderNo")), String.valueOf(object.get("url")));
                    } else {
                        System.out.println("验签失败！");
                    }
                    break;
                default:
                    System.out.println();
            }


        }
    };

    {
        try {
            manager = ServerSocketManager.getInstance();
            manager.addOnClientListener(clientListener);
            manager.addOnReveicedMessageListener(messageListener);
            //TODO 服务器ip
            String ip = "192.168.0.107";
            if (ip != null && !"".equals(ip)) {
                manager.startServer(ip, 9090);
                System.out.println("-------ServerSocketManager------");
            } else {
                System.out.println("-------读取ip配置失败------");
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }


}
