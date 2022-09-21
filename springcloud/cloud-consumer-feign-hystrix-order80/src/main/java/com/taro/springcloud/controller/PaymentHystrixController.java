package com.taro.springcloud.controller;

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
    public String getPaymentTimeout(@PathVariable("id") Long id) {
        return paymentHystrixService.getPaymentTimeout(id);
    }
}
