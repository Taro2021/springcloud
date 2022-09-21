package com.taro.springcloud.service;

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
    public String getPayment_TimeOut(Long id) {
        try {
            TimeUnit.SECONDS.sleep(3);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return  "线程id：" + Thread.currentThread().getName() + ",  getPayment_OK 订单 id ：" + id.toString();
    }
}
