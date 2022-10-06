package com.taro.springcloud;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * ClassName SentinelService8401
 * Author taro
 * Date 2022/10/6 11:22
 * Version 1.0
 */

@SpringBootApplication
@EnableDiscoveryClient
public class SentinelService8401 {
    public static void main(String[] args) {
        SpringApplication.run(SentinelService8401.class,args);
    }
}
