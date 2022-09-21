package com.taro.springcloud.controller;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixProperty;
import com.taro.springcloud.service.PaymentHystrixService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ClassName PaymentHystrixController
 * Author taro
 * Date 2022/9/21 14:28
 * Version 1.0
 */

@RestController
@RequestMapping("/consumer/payment")
@Slf4j
public class PaymentHystrixController {

    @Autowired
    private PaymentHystrixService paymentHystrixService;

    @GetMapping("/ok/{id}")
    public String getPaymentOk(@PathVariable("id") Long id) {
        return paymentHystrixService.getPayment_OK(id);
    }

    @GetMapping("/timeout/{id}")
    @HystrixCommand(fallbackMethod = "getPayment_TimeoutHandler", commandProperties = {
            @HystrixProperty(name = "execution.isolation.thread.timeoutInMilliseconds",value = "3000")
    })
    public String getPayment_Timeout(@PathVariable("id") Long id) {
        return paymentHystrixService.getPayment_Timeout(id);
    }

    public String getPayment_TimeoutHandler(@PathVariable("id") Long id){
        return "支付服务繁忙";
    }
}
