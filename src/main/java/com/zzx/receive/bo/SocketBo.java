package com.zzx.receive.bo;

import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Data
@ToString
public class SocketBo {
    private String orderNo;
    private String money;
    private String type;
    private String sign;
}
