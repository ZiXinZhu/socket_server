package com.zzx.receive.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.zzx.receive.bo.SocketBo;
import com.zzx.receive.socket.ServerSocketManager;
import com.zzx.receive.socket.ServerSocketService;
import com.zzx.receive.tools.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.io.IOException;


/**
 * https://www.cnblogs.com/Sailsail/p/6612932.html
 */
@RestController
@RequestMapping("/socket")
public class SocketController {

    @Autowired
    ServerSocketManager serverSocketManager;

    /**
     * 获取app返回的支付链接
     *
     * @param orderNo
     * @return
     */
    @GetMapping("/receive")
    public String getUrl(String orderNo) {
        String url = ServerSocketService.mapPay.get(orderNo);
        if (url == null) {
            for (int i = 0; i < 3; i++) {
                try {
                    if (url != null) {
                        break;
                    }
                    Thread.sleep(500);
                    url = ServerSocketService.mapPay.get(orderNo);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        ServerSocketService.mapPay.remove(orderNo);
        return url;
    }


    /**
     * 请求app
     *
     * @param signKey
     * @param data
     * @return
     * @throws IOException
     */
    @GetMapping("/send")
    public int sends(String signKey, SocketBo data) throws IOException {
        String signbefor = "money=" + data.getMoney() + "&orderNo=" + data.getOrderNo() + "&type=" + data.getType() + "&key=888888";
        String sign = Utils.md5(signbefor).toUpperCase();
        JSONObject object = new JSONObject();
        object.put("money", data.getMoney());
        object.put("orderNo", data.getOrderNo());
        object.put("type", data.getType());
        object.put("sign", sign);
        String after = JSON.toJSONString(object);
        return serverSocketManager.sendMessage(signKey, after.getBytes());
    }
}
