package com.doopp.gauss.dns.refresh;

import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.alidns.model.v20150109.UpdateDomainRecordRequest;
import com.aliyuncs.alidns.model.v20150109.UpdateDomainRecordResponse;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.profile.DefaultProfile;
import com.aliyuncs.profile.IClientProfile;
import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.Future;

public class DnsRefresh implements Runnable {

    private String propertyFile = "";

    DnsRefresh(String propertyFile)
    {
        this.propertyFile = propertyFile;
    }

    @Override
    public void run() {

        System.out.print(" >>> " + propertyFile);
        Properties properties = this.getProperties();
        if (properties==null) {
            return;
        }

        String oldIp = properties.getProperty("oldIp");
        String requestUrl = properties.getProperty("ipQueryServer");

        String recordId = properties.getProperty("recordId");
        String regionId = properties.getProperty("regionId");
        String accessKeyId = properties.getProperty("accessKeyId");
        String accessKeySecret = properties.getProperty("accessKeySecret");
        IClientProfile profile = DefaultProfile.getProfile(regionId, accessKeyId, accessKeySecret);

        // 若报Can not find endpoint to access异常，请添加以下此行代码
        // DefaultProfile.addEndpoint("cn-hangzhou", "cn-hangzhou", "Alidns", "alidns.aliyuncs.com");
        IAcsClient client = new DefaultAcsClient(profile);

        while (true) {
            String newIp = this.httpGet(requestUrl);
            System.out.print(" >>> new ip : " + newIp + "\n");

            if (newIp!=null && !newIp.equals(oldIp)) {

                UpdateDomainRecordRequest updateRequest = new UpdateDomainRecordRequest();
                updateRequest.setRecordId(recordId);
                updateRequest.setRR("home");
                updateRequest.setType("A");
                updateRequest.setValue(newIp);

                try {
                    UpdateDomainRecordResponse updateResponse = client.getAcsResponse(updateRequest);
                    System.out.println("UpdateDomainRecordResponse : " + updateResponse);
                }
                catch (ClientException e) {
                    System.out.println("client.getAcsResponse Error : " + e.getMessage() + "\n");
                }
                oldIp = newIp;
            }

            synchronized (this) {
                try {
                    this.wait(10 * 1000);
                }
                catch (Exception e) {
                    System.out.println("System wait error : " + e.getMessage() + "\n");
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
        }
        catch(Exception e) {
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
        }
        catch (IOException e) {
            System.out.println( propertyFile + " is can not read \n");
        }
        return null;
    }
}
