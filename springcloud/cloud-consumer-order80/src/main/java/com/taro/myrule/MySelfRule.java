package com.taro.myrule;

import com.netflix.loadbalancer.ILoadBalancer;
import com.netflix.loadbalancer.IRule;
import com.netflix.loadbalancer.RandomRule;
import com.netflix.loadbalancer.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ClassName MySelfRule
 * Author taro
 * Date 2022/9/20 10:33
 * Version 1.0
 */

@Configuration
public class MySelfRule {

    @Bean
    public IRule myRule(){
        //指定负载均衡算法为随即算法
        return new RandomRule();
    }
}
