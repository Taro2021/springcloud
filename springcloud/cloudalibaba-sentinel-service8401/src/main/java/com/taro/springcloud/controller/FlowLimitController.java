package com.taro.springcloud.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.taro.springcloud.entities.CommonResult;
import com.taro.springcloud.entities.Payment;
import com.taro.springcloud.myhandler.CustomerBlockHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * ClassName FlowLimitController
 * Author taro
 * Date 2022/10/6 11:23
 * Version 1.0
 */

@RestController
@Slf4j
public class FlowLimitController {

    @GetMapping("/test1")
    public String test1(){
        return "test1";
    }

    @GetMapping("test2")
    public String test2(){
        return "test2";
    }

    @GetMapping("/test3")
    @SentinelResource(value = "test3", blockHandler = "dealTest3")
   public String test3(){
        log.info("测试降级规则，异常比例"  );
        int err = 1 / 0;
        return "test3";
    }

    @GetMapping("/testHotKey")
    @SentinelResource(value = "testHotKey", blockHandler = "dealTestHotKey")
    public String testHotKey(@RequestParam(value = "p1", required = false) String p1,
                             @RequestParam(value = "p2", required = false) String p2){
        return "test hot key";
    }


    public String dealTestHotKey(String p1, String p2, BlockException exception){
        return "test hot key fallback method";
    }

    public String dealTest3(BlockException exception){
        return "test3 fallback method";
    }

    @GetMapping("/limit/customerBlockHandler")
    @SentinelResource(value = "customerBlockHandler",
            blockHandlerClass = CustomerBlockHandler.class,
            blockHandler = "handlerException2")
    public CommonResult customerBlockHandler(){
        return new CommonResult(200, "success", new Payment(2022L, "serial106"));
    }
}
