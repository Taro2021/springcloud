package com.taro.springcloud.service;

import org.springframework.stereotype.Component;

/**
 * ClassName PaymentHystrixServiceImpl
 * Author taro
 * Date 2022/9/21 22:14
 * Version 1.0
 */

@Component
public class PaymentFallback implements PaymentHystrixService{
    @Override
    public String getPayment_OK(Long id) {
        return "PaymentFallback: getPayment_OK";
    }

    @Override
    public String getPayment_TimeOut(Long id) {
        return "PaymentFallback: getPayment_TimeOut";
    }
}
