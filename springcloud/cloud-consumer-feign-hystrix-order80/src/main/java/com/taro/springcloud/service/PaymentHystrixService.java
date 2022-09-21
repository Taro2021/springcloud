package com.taro.springcloud.service;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Service
@FeignClient("CLOUD-HYSTRIX-PAYMENT")
public interface PaymentHystrixService {

    @GetMapping("/payment/hystrix/ok/{id}")
    String getPayment_OK(@PathVariable("id") Long id);

    @GetMapping("/timeout/{id}")
    String getPaymentTimeout(@PathVariable("id") Long id);

}
