package com.taro.springcloud;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

/**
 * ClassName com.taro.springcloud.PayMentMain8001
 * Author taro
 * Date 2022/9/16 13:53
 * Version 1.0
 */

@SpringBootApplication
@EnableEurekaClient
public class PaymentMain8002 {

    public static void main(String[] args) {
        SpringApplication.run(PaymentMain8002.class, args);
    }

}
