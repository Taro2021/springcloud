package com.taro.springcloud.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ClassName CommonResult
 * Author taro
 * Date 2022/9/16 14:10
 * Version 1.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommonResult <T>{

    //200 code 表示操作成功
    private Integer code;

    private String message;

    private T data;

    public CommonResult(Integer code, String message) {
        this(code, message, null);
    }
}
