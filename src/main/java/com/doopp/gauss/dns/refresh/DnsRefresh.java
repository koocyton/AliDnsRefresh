package com.doopp.gauss.dns.refresh;

import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.alidns.model.v20150109.*;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.profile.DefaultProfile;
import com.aliyuncs.profile.IClientProfile;
import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DnsRefresh implements Runnable {

    private String propertyFile = "";

    DnsRefresh(String propertyFile) {
        this.propertyFile = propertyFile;
    }

    @Override
    public void run() {

        System.out.print(" >>> " + propertyFile + "\n");
        Properties properties = this.getProperties();
        if (properties == null) {
            return;
        }

        // 要动态解析的子域名列表
        String[] domainArray = properties.getProperty("refreshDomain").split(",");
        // 旧的 IP
        String oldIp = properties.getProperty("oldIp");
        // IP 查询服务器
        String requestUrl = properties.getProperty("ipQueryServer");
        // 一级域名
        String rootDomain = properties.getProperty("rootDomain");
        // aliyun 配置
        String regionId = properties.getProperty("regionId");
        String accessKeyId = properties.getProperty("accessKeyId");
        String accessKeySecret = properties.getProperty("accessKeySecret");
        IClientProfile profile = DefaultProfile.getProfile(regionId, accessKeyId, accessKeySecret);

        // 若报Can not find endpoint to access异常，请添加以下此行代码
        // DefaultProfile.addEndpoint("cn-hangzhou", "cn-hangzhou", "Alidns", "alidns.aliyuncs.com");
        IAcsClient client = new DefaultAcsClient(profile);

        // recordId 对应域名的列表
        Map<String, String> recordRRList = new HashMap<>();

        // 获取子域名
        DescribeDomainRecordsRequest request = new DescribeDomainRecordsRequest();
        request.setDomainName(rootDomain);
        try {
            DescribeDomainRecordsResponse response = client.getAcsResponse(request);
            List<DescribeDomainRecordsResponse.Record> recordList = response.getDomainRecords();
            for (DescribeDomainRecordsResponse.Record record : recordList) {
                if (Arrays.asList(domainArray).contains(record.getRR())) {
                    // 取得要解析的 record id
                    recordRRList.put(record.getRecordId(), record.getRR());
                }
            }
        } catch (ClientException e) {
            System.out.print(" >>> Error : "+ e.getMessage() + "\n");
        }

        // 新的 IP 先和旧 IP 一样
        String newIp = oldIp;

        // 初始化更新域名解析的类
        UpdateDomainRecordRequest updateRequest = new UpdateDomainRecordRequest();
        updateRequest.setType("A");

        // 每一段时间循环一次
        while (true) {
            String ipHtml = this.httpGet(requestUrl);
            if (ipHtml != null) {
                Pattern p = Pattern.compile("(\\d+\\.\\d+\\.\\d+\\.\\d+)");
                Matcher m = p.matcher(ipHtml);
                if (m.find()) {
                    newIp = m.group(1);
                }
                // 如果 IP 发生了变化
                if (newIp != null && !newIp.equals(oldIp)) {
                    updateRequest.setValue(newIp);
                    // 将每个要解析的域名都处理一次
                    for (String recordId : recordRRList.keySet()) {
                        updateRequest.setRR(recordRRList.get(recordId));
                        updateRequest.setRecordId(recordId);
                        try {
                            UpdateDomainRecordResponse updateResponse = client.getAcsResponse(updateRequest);
                            System.out.println("UpdateDomainRecordResponse : " + updateResponse + "\n");
                        } catch (ClientException e) {
                            System.out.println("client.getAcsResponse Error : " + e.getMessage() + "\n");
                        }
                    }
                    oldIp = newIp;
                }

                synchronized (this) {
                    try {
                        this.wait(10 * 1000);
                    } catch (Exception e) {
                        System.out.println("System wait error : " + e.getMessage() + "\n");
                    }
                }
            }
        }
    }

    private String httpGet(String requestUrl) {
        AsyncHttpClient client = new AsyncHttpClient();
        Future<String> f = client.prepareGet(requestUrl).execute(new AsyncCompletionHandler<String>() {
            @Override
            public String onCompleted(Response response) throws Exception {
                return response.getResponseBody();
            }
        });
        try {
            return f.get();
        } catch (Exception e) {
            System.out.println("can not http request " + requestUrl + "\n");
        }
        return null;
    }


    private Properties getProperties() {
        try {
            InputStream is = new FileInputStream(propertyFile);
            Properties pros = new Properties();
            pros.load(is);
            return pros;
        } catch (IOException e) {
            System.out.println(propertyFile + " is can not read \n");
        }
        return null;
    }
}
