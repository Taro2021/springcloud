package com.taro.springcloud.controller;

import com.taro.springcloud.entities.CommonResult;
import com.taro.springcloud.entities.Payment;
import com.taro.springcloud.service.PaymentFeignService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ClassName OrderFeignController
 * Author taro
 * Date 2022/9/20 23:06
 * Version 1.0
 */

@RestController
@RequestMapping("/consumer/payment")
public class OrderFeignController {

    @Autowired
    private PaymentFeignService paymentFeignService;

    @GetMapping("/get/{id}")
    public CommonResult<Payment> getPaymentById(@PathVariable("id") Long id){
        return paymentFeignService.getPaymentById(id);
    }
}
