package com.taro.springcloud;

import com.sun.javaws.Main;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

import javax.swing.text.html.HTMLDocument;

/**
 * ClassName PaymentHystrixMain8001
 * Author taro
 * Date 2022/9/21 13:34
 * Version 1.0
 */

@SpringBootApplication
@EnableEurekaClient
@EnableCircuitBreaker
public class PaymentHystrixMain8001 {
    public static void main(String[] args) {
        SpringApplication.run(PaymentHystrixMain8001.class, args);
    }
}

