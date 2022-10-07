package com.taro.springcloud.service.impl;

import com.taro.springcloud.entities.CommonResult;
import com.taro.springcloud.entities.Payment;
import com.taro.springcloud.service.PaymentService;
import org.springframework.stereotype.Service;

/**
 * ClassName PaymentFallbackService
 * Author taro
 * Date 2022/10/7 10:54
 * Version 1.0
 */
@Service
public class PaymentFallbackService implements PaymentService {
    @Override
    public CommonResult<Payment> paymentQuery(Long id) {
        return new CommonResult<>(44444,"PaymentFallbackService",new Payment(id,"errorSerial"));
    }
}
