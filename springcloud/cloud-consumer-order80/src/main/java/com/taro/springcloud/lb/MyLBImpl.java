package com.taro.springcloud.lb;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ClassName MyLBImpl
 * Author taro
 * Date 2022/9/20 11:13
 * Version 1.0
 */

@Component
public class MyLBImpl implements LoadBalancer{

    private AtomicInteger atomicInteger = new AtomicInteger(0);
    @Override
    public ServiceInstance instance(List<ServiceInstance> serviceInstances) {
        int index = getAndIncrement(serviceInstances.size());
        return serviceInstances.get(index);
    }

    //轮询获取下一个实例的索引
    public final int getAndIncrement(int mod){
        //自旋锁
        for(;;) {
            int current = this.atomicInteger.get();
            int next = current == Integer.MAX_VALUE ? 0 : (current + 1) % mod;
            //CAS
            if(atomicInteger.compareAndSet(current, next)) {
                return next;
            }
        }
    }
}
