package com.taro.springcloud.myhandler;

import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.taro.springcloud.entities.CommonResult;

/**
 * ClassName CustomerBlockHandler
 * Author taro
 * Date 2022/10/6 17:54
 * Version 1.0
 */

public class CustomerBlockHandler {

    public static CommonResult handlerException1(BlockException exception) {
        return new CommonResult(444, "global block exception handler1");
    }

    public static CommonResult handlerException2(BlockException exception) {
        return new CommonResult(444, "global block exception handler2");
    }
}
