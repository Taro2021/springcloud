package com.taro.springcloud.service;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixProperty;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * ClassName PaymentService
 * Author taro
 * Date 2022/9/21 13:37
 * Version 1.0
 */

@Service
public class PaymentService {

    //正常服务
    public String getPayment_OK(Long id) {
        return  "线程id：" + Thread.currentThread().getName() + ",  getPayment_OK 订单 id ：" + id.toString();
    }

    //超时服务
    @HystrixCommand(fallbackMethod = "getPayment_TimeOutHandler", commandProperties = {
            @HystrixProperty(name = "execution.isolation.thread.timeoutInMilliseconds",value = "3000")
    })
    public String getPayment_TimeOut(Long id) {
        try {
            TimeUnit.SECONDS.sleep(5);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return  "线程id：" + Thread.currentThread().getName() + ",  getPayment_OK 订单 id ：" + id.toString();
    }

    public String getPayment_TimeOutHandler(Long id) {
        return  "线程id：" + Thread.currentThread().getName() + ",  getPayment_TimeOutHandler 订单 id ：" + id.toString();
    }
}
