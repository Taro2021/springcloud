package com.taro.springcloud.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

/**
 * ClassName OrderConsulController
 * Author taro
 * Date 2022/9/18 20:23
 * Version 1.0
 */

@RestController
@RequestMapping("/consumer/payment")
@Slf4j
public class OrderConsulController {

    public static final String INVOKE_URL = "http://consul-provider-payment";

    @Autowired
    private RestTemplate restTemplate;

    @GetMapping("/consul")
    public String paymentInfo(){
        String ret = restTemplate.getForObject(INVOKE_URL + "/payment/consul", String.class);
        return ret;
    }
}
