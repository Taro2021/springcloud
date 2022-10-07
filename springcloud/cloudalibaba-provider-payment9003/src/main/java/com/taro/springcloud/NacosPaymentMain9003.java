package com.taro.springcloud;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * ClassName NacosPaymentMain9003
 * Author taro
 * Date 2022/10/7 9:46
 * Version 1.0
 */

@SpringBootApplication
@EnableDiscoveryClient
public class NacosPaymentMain9003 {
    public static void main(String[] args) {
        SpringApplication.run(NacosPaymentMain9003.class,args);
    }
}
