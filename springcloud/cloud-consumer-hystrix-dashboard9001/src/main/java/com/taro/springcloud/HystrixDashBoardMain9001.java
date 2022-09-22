package com.taro.springcloud;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.hystrix.dashboard.EnableHystrixDashboard;

/**
 * ClassName HystrixDashBoardMain9001
 * Author taro
 * Date 2022/9/22 13:45
 * Version 1.0
 */

@SpringBootApplication
@EnableHystrixDashboard
public class HystrixDashBoardMain9001 {

    public static void main(String[] args) {
        SpringApplication.run(HystrixDashBoardMain9001.class, args);
    }
}
