package com.taro.springcloud;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * ClassName NacosPaymentMain9004
 * Author taro
 * Date 2022/10/7 9:56
 * Version 1.0
 */

@SpringBootApplication
@EnableDiscoveryClient
public class NacosPaymentMain9004 {

    public static void main(String[] args) {
        SpringApplication.run(NacosPaymentMain9004.class,args);
    }
}
