package com.zzx.receive.tools;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Utils {

    public static String md5(String str){
        StringBuilder sb = new StringBuilder();
        try {
            //获取MD5加密器
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] bytes = str.getBytes();
            byte[] digest = md.digest(bytes);

            for (byte b : digest) {
                //把每个字节转换成16进制数
                int d = b & 0xff;//0x00 00 00 00 ff
                String hexString = Integer.toHexString(d);
                if(hexString.length() == 1){
                    hexString = "0" + hexString;
                }
                sb.append(hexString);
            }
        } catch (NoSuchAlgorithmException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return String.valueOf(sb);
    }
}
