package com.taro.springcloud.controller;

import com.taro.springcloud.entities.CommonResult;
import com.taro.springcloud.entities.Payment;
import com.taro.springcloud.service.PaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ClassName PaymentController
 * Author taro
 * Date 2022/9/16 14:38
 * Version 1.0
 */

@RestController
@Slf4j
@RequestMapping("/payment")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @Value("${server.port}")
    private String serverPort;

    @Autowired
    private DiscoveryClient discoveryClient;

    @PostMapping("/create")
    public CommonResult create(@RequestBody Payment payment) {
        int result = paymentService.create(payment);
        log.info("插入结果：{}", result);
        if(result > 0) {
            return new CommonResult(200, "插入成功, serverPort: " + serverPort, result);
        }else {
            return new CommonResult(404, "插入失败");
        }
    }

    @GetMapping("/get/{id}")
    public CommonResult getPaymentById(@PathVariable("id") Long id) {
        Payment payment = paymentService.getPaymentById(id);
        log.info("{} id 的查询结果： {}", id, payment);
        if(payment != null) {
            return new CommonResult(200, "查询成功", payment);
        }else {
            return new CommonResult(404, "查询失败");
        }
    }

    @GetMapping("/discovery")
    public Object discovery(){
        List<String> services = discoveryClient.getServices();
        //获取服务信息
        for(String service : services) {
            log.info("service: {}", service);
        }
        //获取对应服务的实例信息
        List<ServiceInstance> instances = discoveryClient.getInstances("CLOUD-PAYMENT-SERVICE");
        for(ServiceInstance instance : instances) {
            log.info(instance.getInstanceId() + "\t" + instance.getHost() + "\t" + instance.getPort() + "\t" + instance.getUri());
        }
        return this.discoveryClient;
    }
}
