package com.taro.springcloud.service.impl;

import com.taro.springcloud.dao.PaymentDao;
import com.taro.springcloud.entities.Payment;
import com.taro.springcloud.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * ClassName PaymentServiceImpl
 * Author taro
 * Date 2022/9/16 14:32
 * Version 1.0
 */

@Service
public class PaymentServiceImpl implements PaymentService {

    //@Resource 是 Java 提供的注解，同样可以注入对象
    @Autowired // Autowired spring 提供的注解
    private PaymentDao paymentDao;

    @Override
    public int create(Payment payment) {
        return paymentDao.create(payment);
    }

    @Override
    public Payment getPaymentById(Long id) {
        return paymentDao.getPaymentById(id);
    }
}
