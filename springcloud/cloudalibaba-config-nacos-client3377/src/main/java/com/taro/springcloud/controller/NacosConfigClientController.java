package com.taro.springcloud.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ClassName NacosConfigClientCONTROLLER
 * Author taro
 * Date 2022/10/1 15:51
 * Version 1.0
 */

@RestController
@RefreshScope
public class NacosConfigClientController {

    @Value("${config.info}")
    public String configInfo;

    @GetMapping("/config/info")
    public String getConfigInfo(){
        return configInfo;
    }
}
