package com.doopp.gauss.dns.refresh;

public class KTApplication {

    public static void main(String[] args) {
        String properties = (args[0]==null) ? "/tmp/dnsProperty.properties" : args[0];
        DnsRefresh dnsRefresh = new DnsRefresh(properties);
        dnsRefresh.start();
    }
}
