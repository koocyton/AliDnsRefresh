# AliDnsRefresh

自动更新在阿里云上的 dns 记录，相当于自己的花生壳

##### how run 
``` html
java -jar dnsReferesh-1.0.jar you_dir/dnsProperty.properties
```

##### dnsProperty.properties  Example
``` html
oldIp=127.0.0.1
ipQueryServer=http://www.taobao.com/help/getip.php
regionId=cn-hangzhou
accessKeyId=you aliyun accessKey
accessKeySecret=you aliyun accessKeySecret
refreshDomain=wxlrs.gauss,
rootDomain=doopp.com
```
