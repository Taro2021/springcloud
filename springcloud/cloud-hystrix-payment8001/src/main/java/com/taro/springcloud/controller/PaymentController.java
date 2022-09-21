package com.taro.springcloud.controller;

import com.taro.springcloud.service.PaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ClassName PaymentController
 * Author taro
 * Date 2022/9/21 13:42
 * Version 1.0
 */

@RestController
@Slf4j
@RequestMapping("/payment")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @Value("${server.port")
    private String serverPort;

    @GetMapping("/hystrix/ok/{id}")
    public String getPayment_OK(@PathVariable("id") Long id) {
        String ret = paymentService.getPayment_OK(id);
        log.info("msg: {}", ret);
        return ret;
    }


    @GetMapping("/hystrix/timeout/{id}")
    public String getPaymentTimeout(@PathVariable("id") Long id){
        String ret = paymentService.getPayment_TimeOut(id);
        log.info("msg: {}", ret);
        return ret;
    }

    //===服务熔断
    @GetMapping("/circuit/{id}")
    public String paymentCircuitBreaker(@PathVariable("id") Integer id){
        String result = paymentService.paymentCircuitBreaker(id);
        log.info("result: {}", result);
        return result;
    }
}
