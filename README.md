                                                socket通信数据文档



|  服务器： |    数据类型   |    含义 |
|----------|--------------|--------|
|orderNo   |    String    |   订单号|
|money     |    String    |   金额  |
|type      |    String    |   类型  |
|sign      |    String    |   加签  |
|----------|--------------|--------|
|客服端：   |    数据类型   |    含义  |
|code      |    String    |  1（心跳）|
|signKey   |    String    |  唯一标识 |
|----------|--------------|--------|
|客服端：   |    数据类型   |    含义   |
|code      |    String    |  2（返回）|
|signKey   |    String    |  唯一标识 |
|orderNo   |    String    |  订单号   |
|url       |    String    |  支付链接 |

实例：
orderNo=123456789&type=alipay&money=23&key="888888"

{"orderNo":"987654321","money":"1","type":"alipay","sign":""}

code=""&orderNo=""&signKey=""&url=""
