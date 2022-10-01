package com.taro.sprinfcloud.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Controller;

/**
 * ClassName ConsumerController
 * Author taro
 * Date 2022/9/30 17:25
 * Version 1.0
 */

@Controller
@EnableBinding(Sink.class)
public class ConsumerController {

    @Value("${server.port}")
    private String serverPort;

    @StreamListener(Sink.INPUT)
    public void input(Message<String> message){
        System.out.println("consumer 2 receive: " + message.getPayload() + "\t port: " + serverPort);
    }
}
