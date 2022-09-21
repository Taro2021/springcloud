package com.taro.springcloud.config;

import feign.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ClassName FeignConfig
 * Author taro
 * Date 2022/9/21 13:02
 * Version 1.0
 */

@Configuration
public class FeignConfig {

    @Bean
    Logger.Level loggerLevel(){
        return Logger.Level.FULL;
    }
}
