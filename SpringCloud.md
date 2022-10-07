# SpringCloud

[SpringCloud 中文文档](www.springcloud.cc)

# 微服务模块

1. 建立 module
2. 改写子模块的 pom 配置文件
3. 写 yml 配置信息
4. 编写主启动类
5. 编写业务类

## payment 模块的编写

### 1. 建表

```mysql
create table `payment` (
`id` bigint(20) not null auto_increment comment 'ID', 
`serial` varchar(200) default "",primary key(`id`)
)engine=InnoDB auto_increment=1 default charset=utf8;
```



### 2. 根据建立对应的实体类 entities

```java
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Payment implements Serializable {

    private Long id;
    private String serial;
    
}
```



### 3. dao

Dao 接口

```java
@Mapper
public interface PaymentDao {

    public int create(Payment payment);

    public Payment getPaymentById(@Param("id") Long id);
}
```

mybatis Mapper 映射 xml 文件

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.taro.springcloud.dao.PaymentDao">

    <!--public int create(Payment payment);-->
    <insert id="create" parameterType="Payment" useGeneratedKeys="true" keyProperty="id">
        insert into payment(serial) values(#{serial});
    </insert>


    <resultMap id="BaseResultMap" type="com.taro.springcloud.entities.Payment">
        <id column="id" property="id" jdbcType="BIGINT"/>
        <id column="serial" property="serial" jdbcType="VARCHAR"/>
    </resultMap>
    <!--public Payment getPaymentById(@Param("id") Long id);-->
    <select id="getPaymentById" parameterType="long" resultMap="BaseResultMap">
        select * from payment where id = #{id};
    </select>
</mapper>
```



### 4. service

Service 接口

```java
public interface PaymentService {
    public int create(Payment payment);

    public Payment getPaymentById(@Param("id") Long id);
}
```

Service 实现类

```java
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
```



### 5.  controller

tip : idea 自带的 Http Client 插件可以替代 postman 模拟请求

```java
@RestController
@Slf4j
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @PostMapping("/payment/create")
    public CommonResult create(Payment payment) {
        int result = paymentService.create(payment);
        log.info("插入结果：{}", result);
        if(result > 0) {
            return new CommonResult(200, "插入成功", result);
        }else {
            return new CommonResult(404, "插入失败");
        }
    }

    @GetMapping("/payment/get/{id}")
    public CommonResult getPaymentById(@PathVariable("id") Long id) {
        Payment payment = paymentService.getPaymentById(id);
        log.info("{} id 的查询结果： {}", id, payment);
        if(payment != null) {
            return new CommonResult(200, "查询成功", payment);
        }else {
            return new CommonResult(404, "查询失败");
        }
    }
}
```



##  热部署 DevTools

引入依赖

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-devtools</artifactId>
</dependency>
```

父工程中添加 maven 插件

```xml
<plugin>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-maven-plugin</artifactId>
    <configuration>
        <fork>true</fork>
        <addResources>true</addResources>
    </configuration>
</plugin>
```

在设置中配置选项

![image-20220916151204194](https://taro-note-pic.oss-cn-hangzhou.aliyuncs.com/image-20220916151204194.png)

ctrl + shift + alt + / 唤出注册表



## cosumer  模块

cosumer 通过 **RestTemplate** 向 payment 模块发送请求

### config

在 ioc 容器中注册  **RestTemplate**

```java
@Configuration
public class ApplicationContextConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
```

### controller

```java
@RestController
@Slf4j
@RequestMapping("/consumer/payment")
public class OrderController {

    public static final String PAYMENT_URL = "http://localhost:8001";
	//@Resource 默认 byName
    //@Autowired 默认 byType
    @Resource
    private RestTemplate restTemplate;

    @GetMapping("/create")
    public CommonResult<Payment> create(Payment payment) {
        return restTemplate.postForObject(PAYMENT_URL + "/payment/create",payment ,CommonResult.class );
    }

    @GetMapping("/get/{id}")
    public CommonResult<Payment> getPayment(@PathVariable("id") Long id){
        return restTemplate.getForObject(PAYMENT_URL + "/payment/get/" + id, CommonResult.class);
    }
}
```



## 公共部分的模块抽取

给模块在自己的 pom 配置文件中引入公共模块的依赖

```xml
<dependency>
    <groupId>com.taro</groupId>
    <artifactId>cloud-api-commons</artifactId>
    <version>${project.version}</version>
</dependency>
```





# Eureka 服务注册与发现

Eureka 采用了 CS 的设计架构

两个组件：服务注册，服务治理

![image-20220917102932288](https://taro-note-pic.oss-cn-hangzhou.aliyuncs.com/image-20220917102932288.png)



## 单机 Eureka

### 1. 建立 Euraka 服务模块

#### pom

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <parent>
        <artifactId>springcloud</artifactId>
        <groupId>com.taro</groupId>
        <version>1.0-SNAPSHOT</version>
    </parent>
    <modelVersion>4.0.0</modelVersion>

    <artifactId>eureka-server</artifactId>

    <dependencies>
        <!-- https://mvnrepository.com/artifact/org.springframework.cloud/spring-cloud-starter-eureka-server -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-eureka-server</artifactId>
        </dependency>

        <dependency>
            <groupId>com.taro</groupId>
            <artifactId>cloud-api-commons</artifactId>
            <version>${project.version}</version>
        </dependency>

        <!-- https://mvnrepository.com/artifact/org.springframework.boot/spring-boot-starter-web -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- https://mvnrepository.com/artifact/org.springframework.boot/spring-boot-starter-web  -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>

        <!-- https://mvnrepository.com/artifact/org.springframework.boot/spring-boot-devtools -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-devtools</artifactId>
            <scope>runtime</scope>
            <optional>true</optional>
        </dependency>

        <!-- https://mvnrepository.com/artifact/org.projectlombok/lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
        </dependency>

        <!-- https://mvnrepository.com/artifact/org.springframework.boot/spring-boot-starter-test -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>junit</groupId>
            <artifactId>junit</artifactId>
        </dependency>

    </dependencies>

</project>
```

#### application.yml

```yml
server:
  port: 7001

eureka:
  instance:
    hostname: localhost #eurake 服务端的实例名称
  client:
    register-with-eureka: false #false 表示不向注册中心注册自己
    fetch-registry: false # false 表示自己就是注册中心，只需要维护服务实例，而不需要去检索服务
    service-url:
      #设置与eureka server 交互的地址查询服务和注册服务都需要依赖这地址
      defaultZone: http://${eureka.instance.hostname}:${server.port}/eureka/
```

#### 主启动类

```java
@SpringBootApplication
@EnableEurekaServer //7001 端口的服务为 Eureka 的z'ju
public class EurekaMain7001 {
    public static void main(String[] args) {
        SpringApplication.run(EurekaMain7001.class, args);
    }
}
```



### 2. 微服务模块在 Eureka 服务端上注册

**需要注册的服务模块 pom 中添加 Eureka Client 的依赖**

```xml
<!-- 加入 Eureka client 的依赖-->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
```

**修改 yml 编写 eureka 的配置信息**

```yml
eureka:
  client:
    register-with-eureka: true #表示是否将自己注册进 EurekaServer 默认为 true
    fetch-registry: true #是否从 EurekaServer 抓取已有的注册信息，默认为 true。单点无所谓，在集群中必须设置为 true 才能配合 ribbon使用负载均衡
    service-url:
      defaultZone: http://localhost:7001/eureka
```

**在主启动类中添加注解**

`@EnableEurekaClient`



## Eureka 的集群部署

![image-20220917154642094](https://taro-note-pic.oss-cn-hangzhou.aliyuncs.com/image-20220917154642094.png)

集群内的 Eureka Server互相注册，互相守望



### 集群环境的构建  **Eureka Server** 服务注册管理集群

参考 cloud-eureka-server7001 建立 cloud-eureka-server7002 服务

yml

7001

```yml
server:
  port: 7001

eureka:
  instance:
    hostname: erueka7001.com #eurake 服务端的实例名称
  client:
    register-with-eureka: false #false 表示不向注册中心注册自己
    fetch-registry: false # false 表示自己就是注册中心，只需要维护服务实例，而不需要去检索服务
    service-url:
      #设置与eureka server 交互的地址查询服务和注册服务都需要依赖这地址
      defaultZone: http://erueka7002.com:7002/eureka/
```

7002

```yml
server:
  port: 7002

eureka:
  instance:
    hostname: erueka7002.com #eurake 服务端的实例名称
  client:
    register-with-eureka: false #false 表示不向注册中心注册自己
    fetch-registry: false # false 表示自己就是注册中心，只需要维护服务实例，而不需要去检索服务
    service-url:
      #设置与eureka server 交互的地址查询服务和注册服务都需要依赖这地址
      defaultZone: http://erueka7001.com:7001/eureka/
```



### 将微服务模块发布到集群上

以 80 端口的 consumer 服务为例

修改 yml

```yml
server:
  port: 80

spring:
  application:
    name: cloud-order-service

eureka:
  client:
    register-with-eureka: true
    fetch-registry: true
    service-url:
      defaultZone: http://eureka7001.com:7001/eureka,http://eureka7002.com:7002/eureka
```



### 微服务 provider 集群环境搭建

​	以为 8001 服务为例，复制一个服务 8002 

​	需要修改 8001/8002 的 Controller，同一服务集群中的不同的服务提供者对外暴露的服务名是同一个

​	cosumer 访问 provider 时需要不在根据 ip:port 访问集群中的 provider，而是根据集群的服务名去访问

```java
//80 端口的 consumer 为例
@RestController
@Slf4j
@RequestMapping("/consumer/payment")
public class OrderController {
	//集群中的微服务名
    public static final String PAYMENT_URL = "http://CLOUD-PAYMENT-SERVICE";

    @Resource
    private RestTemplate restTemplate;

    @GetMapping("/create")
    public CommonResult<Payment> create(Payment payment) {
        return restTemplate.postForObject(PAYMENT_URL + "/payment/create",payment ,CommonResult.class );
    }

    @GetMapping("/get/{id}")
    public CommonResult<Payment> getPayment(@PathVariable("id") Long id){
        return restTemplate.getForObject(PAYMENT_URL + "/payment/get/" + id, CommonResult.class);
    }
}
```

​	在 consumer 的配置类中开启 RestTemplate 的负载均衡功能，这样才能确定使用微服务集群中的哪一个 provider 来提供服务

```java
@Configuration
public class ApplicationContextConfig {

    @Bean
    @LoadBalanced //开启 RestTemplate 的负载均衡功能
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
```

为集群中的每个 provider 实例配置唯一的名称

```yml
eureka:
  client:
    register-with-eureka: true
    fetch-registry: true
    service-url:
      defaultZone: http://eureka7001.com:7001/eureka,http://eureka7002.com:7002/eureka
  instance:
    instance-id: payment8002 #配置实例名称
    prefer-ip-address: true  #访问信息显示 ip 地址
```

### DiscoveryClient 获取信息

主启动类上加上 `@EnableDiscoveryClient` 注解

```java
@SpringBootApplication
@EnableEurekaClient
@EnableDiscoveryClient
public class PaymentMain8001 {

    public static void main(String[] args) {
        SpringApplication.run(PaymentMain8001.class, args);
    }

}
```

controller 中注入 DiscoveryClient 

```java
@RestController
@Slf4j
@RequestMapping("/payment")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @Value("${server.port}")
    private String serverPort;

    @Autowired
    private DiscoveryClient discoveryClient;
	
    //...

    @GetMapping("/discovery")
    public Object discovery(){
        List<String> services = discoveryClient.getServices();
        //获取服务信息
        for(String service : services) {
            log.info("service: {}", service);
        }
        //获取对应服务的实例信息
        List<ServiceInstance> instances = discoveryClient.getInstances("CLOUD-PAYMENT-SERVICE");
        for(ServiceInstance instance : instances) {
            log.info(instance.getInstanceId() + "\t" + instance.getHost() + "\t" + instance.getPort() + "\t" + instance.getUri());
        }
        return this.discoveryClient;
    }
}
```



## Eureka 的自我保护

​	在 自我保护模式中， Eureka Server 会保护服务注册表中的信息，而不会再注销任何服务实例。自我保护模式是一种应对网络异常的安全保护措施，其架构哲学就是宁可保留所有的微服务也不盲目的注销任何微服务。使用自我保护模式，使得 Eureka 集群更加的健壮稳定。

### Eureka 服务端设置

​	`eureka.server.enable-self-preservation: true `自我保护模式默认是开启的

​	`eureka.server.eviction-interval-timer-in-ms: 2000` 设置心跳时间为 2000 ms

### Eureka 客户端设置

​	`eureka.instance.lease-renewal-interval-in-seconds: 1` 设置实例发送心跳的时间间隔 1s

​	`eureka.instance.lease-expiration-duration-in-seconds: 2` 设置该实例在 Eureka 服务端收到最后一次心跳后的等待时间上限，超时剔除服务， 2s



# Zookeeper  服务注册

[zookeeper-3.4.9下载](https://archive.apache.org/dist/zookeeper/zookeeper-3.4.9/)

关闭服务器的防火墙 `systemctl top firewalld`



## 建立微服务模块

1. 新建模块

2. 在 pom 文件中引入 zookeeper 依赖

   ```xml
   <dependency>
       <groupId>org.springframework.cloud</groupId>
       <artifactId>spring-cloud-starter-zookeeper-discovery</artifactId>
   </dependency>
   ```

3. yml 中配置 zookeeper 注册服务的地址

   ```yml
   server:
     port: 8004
   
   spring:
     application:
       name: cloud-provider-payment
     cloud:
       zookeeper:
         connect-string: 192.168.78.94:2181
   ```

4. 主启动类

   ```java
   @SpringBootApplication
   @EnableDiscoveryClient
   public class PaymentMain8004 {
   
       public static void main(String[] args) {
           SpringApplication.run(PaymentMain8004.class, args);
       }
   }
   ```

5. controller

   ```java
   @RestController
   @Slf4j
   public class PaymentController {
   
       @Value("${server.port}")
       private String serverPort;
   
       @RequestMapping("/payment/zk")
       public String paymentzk(){
           return "springcloud with zookeeper: " + serverPort + "\t" + UUID.randomUUID();
       }
   }
   ```

   

## 测试 

1. 启动服务器上的 zookeeper 注册微服务，执行在 zookeeper 文件夹下的 zkServer.sh 文件夹下的 start shell 脚本

   `/usr/local/zookeeper-3.4.9/bin/zkServer.sh start`

2. `/usr/local/zookeeper-3.4.9/bin/zkCli.sh` 去连接 zookeeper 服务端命令行查看 server（tip: 若client 不能正常启动，查看配置文件名是否正确）

   ​	此时仅有一个 根服务 ![image-20220918170751278](https://taro-note-pic.oss-cn-hangzhou.aliyuncs.com/image-20220918170751278.png)

微服务的 zookpeer 依赖版本需要与 zookeper 注册微服务的版本一致。

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-zookeeper-discovery</artifactId>
    <!--排除zk3.5.3-->
    <exclusions>
        <exclusion>
            <groupId>org.apache.zookeeper</groupId>
            <artifactId>zookeeper</artifactId>
        </exclusion>
    </exclusions>
</dependency>
<!--添加zk 3.4,9版本-->
<!-- https://mvnrepository.com/artifact/org.apache.zookeeper/zookeeper -->
<dependency>
    <groupId>org.apache.zookeeper</groupId>
    <artifactId>zookeeper</artifactId>
    <version>3.4.9</version>
</dependency>
```

​	微服务在 zookeeper 注册后:<img src="https://taro-note-pic.oss-cn-hangzhou.aliyuncs.com/image-20220918170955228.png" alt="image-20220918170955228"  />

==zookeeper 的节点是临时性的==：单某个微服务退出时，zookeeper并不会继续保留该微服务的节点信息。



# Consul 服务注册

[consul 官方文档](https://www.consul.io/docs/intro) go 语言开发

[Downloads | Consul by HashiCorp](https://www.consul.io/downloads)

windows 开发者模式启动 `.\consul agent -dev`

启动后可以在 http://localhost:8500 访问 consul 首页



1. 新建模块

2.  pom

   ```xml
   <?xml version="1.0" encoding="UTF-8"?>
   <project xmlns="http://maven.apache.org/POM/4.0.0"
            xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
            xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
       <parent>
           <artifactId>springcloud</artifactId>
           <groupId>com.taro</groupId>
           <version>1.0-SNAPSHOT</version>
       </parent>
       <modelVersion>4.0.0</modelVersion>
   
       <artifactId>cloud-providerconsul-payment8006</artifactId>
   
       <dependencies>
           <!-- https://mvnrepository.com/artifact/org.springframework.cloud/spring-cloud-starter-consul-discovery -->
           <dependency>
               <groupId>org.springframework.cloud</groupId>
               <artifactId>spring-cloud-starter-consul-discovery</artifactId>
           </dependency>
   
           <dependency>
               <groupId>com.taro</groupId>
               <artifactId>cloud-api-commons</artifactId>
               <version>1.0-SNAPSHOT</version>
           </dependency>
   
   
           <!-- https://mvnrepository.com/artifact/org.springframework.boot/spring-boot-starter-web -->
           <dependency>
               <groupId>org.springframework.boot</groupId>
               <artifactId>spring-boot-starter-web</artifactId>
           </dependency>
   
           <!-- https://mvnrepository.com/artifact/org.springframework.boot/spring-boot-starter-web -->
           <dependency>
               <groupId>org.springframework.boot</groupId>
               <artifactId>spring-boot-starter-actuator</artifactId>
           </dependency>
   
           <!-- https://mvnrepository.com/artifact/org.springframework.boot/spring-boot-devtools -->
           <dependency>
               <groupId>org.springframework.boot</groupId>
               <artifactId>spring-boot-devtools</artifactId>
               <scope>runtime</scope>
               <optional>true</optional>
           </dependency>
   
           <!-- https://mvnrepository.com/artifact/org.projectlombok/lombok -->
           <dependency>
               <groupId>org.projectlombok</groupId>
               <artifactId>lombok</artifactId>
               <optional>true</optional>
           </dependency>
   
           <!-- https://mvnrepository.com/artifact/org.springframework.boot/spring-boot-starter-test -->
           <dependency>
               <groupId>org.springframework.boot</groupId>
               <artifactId>spring-boot-starter-test</artifactId>
               <scope>test</scope>
           </dependency>
   
   
   
       </dependencies>
   
   </project>
   ```

   

# Ribbon 服务调用

​	Ribbon + RestTemplete

​	Ribbon 组件 eureka 组件自带，不需要自己额外引入

​	Ribbon 负载均衡的核心组件，IRule 通过它来选择使用哪种负载均衡算法，默认轮询算法

​	自带的算法有：

![image-20220920102622466](https://taro-note-pic.oss-cn-hangzhou.aliyuncs.com/image-20220920102622466.png)

## 自定义Ribbon负载均衡算法

​	Ribbon 的负载均衡是基于客户端的，所以我们对 Ribbon 负载均衡算法的自定义配置不能放在 `@ComponentScan` 所扫描的包及其子包下，不然自定义配置会被所有的模块的共享。`@SpringBootApplication` 是包含 `@ComponentScan`  的复合注解，所以需要在它的上级目录，新建一个包。

![image-20220920103401644](https://taro-note-pic.oss-cn-hangzhou.aliyuncs.com/image-20220920103401644.png)

**MySelfRule**

```java
@Configuration
public class MySelfRule {

    @Bean
    public IRule myRule(){
        //指定负载均衡算法为随即算法
        return new RandomRule();
    }
}
```

**在主启动类上添加 `@RibbonClient` 注解**

```java
@SpringBootApplication(exclude= {DataSourceAutoConfiguration.class, DruidDataSourceAutoConfigure.class})
@EnableEurekaClient
//指明哪个微服务使用自定义的负载均衡算法
@RibbonClient(name = "CLOUD-PAYMENT-SERVICE", configuration = MySelfRule.class)
public class OrderMain80 {
    public static void main(String[] args) {
        SpringApplication.run(OrderMain80.class, args);
    }
}
```



## 手写轮询算法

核心 ： CAS + 自旋锁

8001/8002 微服务的修改

```java
@GetMapping("/lb")
public String getServerPort(){
    return  serverPort;
}
```

80 微服务

接口

```java
public interface LoadBalancer {
    ServiceInstance instance(List<ServiceInstance> serviceInstances);
}
```

实现类

```java
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
```

调用自己的轮询算法

```java
@Autowired
private DiscoveryClient discoveryClient;

@Autowired
private LoadBalancer myLB;

@GetMapping("/lb")
public String getPaymentLB(){
    List<ServiceInstance> instances = discoveryClient.getInstances("CLOUD-PAYMENT-SERVICE");
    if(instances == null || instances.size() == 0) {
        return null;
    }
    URI uri = myLB.instance(instances).getUri();
    return restTemplate.getForObject(uri + "/payment/lb", String.class);
}
```



# OpenFeign 服务调用

![image-20220920215320063](https://taro-note-pic.oss-cn-hangzhou.aliyuncs.com/image-20220920215320063.png)

OpenFeign 也自带了 Ribbon 负载均衡组件，同样是客户端组件

替代了 Ribbon + RestTempelete

微服务调用接口+@FeignClient

```java
@SpringBootApplication
@EnableFeignClients
public class OrderFeignMain80 {
    public static void main(String[] args) {
        SpringApplication.run(OrderFeignMain80.class, args);
    }
}
```

客户端 service

```java
@Service
@FeignClient("CLOUD-PAYMENT-SERVICE")
public interface PaymentFeignService {

    @GetMapping("/payment/get/{id}")
    public CommonResult<Payment> getPaymentById(@PathVariable("id") Long id);
}
```

Controller

```java
@RestController
@RequestMapping("/consumer/payment")
public class OrderFeignController {

    @Autowired
    private PaymentFeignService paymentFeignService;

    @GetMapping("/get/{id}")
    public CommonResult<Payment> getPaymentById(@PathVariable("id") Long id){
        return paymentFeignService.getPaymentById(id);
    }
}
```

​	OpenFeign 客户端默认只等待一秒，服务端超过一秒未响应就返回报错。所以需要开启超时控制。

​	yml 中配置开启超时控制

```yml
ribbon:
  ReadTimeout:  5000
  ConnectTimeout: 5000
```

## 日志增强

![image-20220921130132712](https://taro-note-pic.oss-cn-hangzhou.aliyuncs.com/image-20220921130132712.png)

在配置类中向 ioc 容器注入日志级别的对象

```java
@Configuration
public class FeignConfig {

    @Bean
    Logger.Level loggerLevel(){
        return Logger.Level.FULL;
    }
}
```

yml 配置文件中开启日志增强

```yml
logging:
  level:
    com.taro.springcloud.service.PaymentFeignService: debug
```



# Hystrix 服务降级

豪猪

![image-20220921131419786](https://taro-note-pic.oss-cn-hangzhou.aliyuncs.com/image-20220921131419786.png)

https://github.com/Netflix/Hystrix/wiki/How-To-Use

三个主要概念：

- 服务降级 fallaback 服务器忙，请稍候再试，不让客户端等待并立刻返回一个友好提示，fallback
- 服务熔断 break 类比保险丝达到最大服务访问后，直接拒绝访问，拉闸限电，然后调用服务降级的方法并返回友好提示
- 服务限流 flowllimit 秒杀高并发等操作，严禁一窝蜂的过来拥挤，大家排队，一秒钟N个，有序进行



编写超时服务，进行线程压力测试

service

```java
@Service
public class PaymentService {

    //正常服务
    public String getPayment_OK(Long id) {
        return  "线程id：" + Thread.currentThread().getName() + ",  getPayment_OK 订单 id ：" + id.toString();
    }

    //超时服务
    public String getPayment_TimeOut(Long id) {
        try {
            TimeUnit.SECONDS.sleep(3);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return  "线程id：" + Thread.currentThread().getName() + ",  getPayment_OK 订单 id ：" + id.toString();
    }
}
```

controller

```java
@RestController
@Slf4j
@RequestMapping("/payment/hystrix")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @Value("${server.port")
    private String serverPort;

    @GetMapping("/ok/{id}")
    public String getPayment_OK(@PathVariable("id") Long id) {
        String ret = paymentService.getPayment_OK(id);
        log.info("msg: {}", ret);
        return ret;
    }

    @GetMapping("/timeout/{id}")
    public String getTimeout(@PathVariable("id") Long id) {
        String ret = paymentService.getPayment_TimeOut(id);
        log.info("msg: {}", ret);
        return ret;
    }
}
```

Jmeter 并发测试 20000个请求去请求超时服务，此时正常服务的处理速度也被拖慢



## 服务降级

`@HystrixConmmand`

### Provider 的降级

​	服务的 provider 设置自身的调用的时间峰值，在峰值内可以正常运行，超过峰值需要有兜底的方法处理，作为服务降级的 fallback

```java
//超时服务，为超时服务提供降级方法
@HystrixCommand(fallbackMethod = "getPayment_TimeOutHandler", commandProperties = {
            @HystrixProperty(name = "execution.isolation.thread.timeoutInMilliseconds",value = "3000")
    })
public String getPayment_TimeOut(Long id) {
    try {
        TimeUnit.SECONDS.sleep(5);
    } catch (InterruptedException e) {
        e.printStackTrace();
    }
    return  "线程id：" + Thread.currentThread().getName() + ",  getPayment_OK 订单 id ：" + id.toString();
}

public String getPayment_TimeOutHandler(Long id) {
    return  "线程id：" + Thread.currentThread().getName() + ",  getPayment_TimeOutHandler 订单 id ：" + id.toString();
}
```

主启动类上激活 `@EnableCircuitBreaker` //断路器



### Consumer 的降级

yml 配置

```yml
feign:
  hystrix:
    enabled: true
```

！！！! 启用一个组件的功能不要忘了给主启动类加上 EnableXXX  注解

```java
@SpringBootApplication
@EnableFeignClients
@EnableHystrix
public class OrderFeignHystrixMain80 {

    public static void main(String[] args) {
        SpringApplication.run(OrderFeignHystrixMain80.class, args);
    }
}
```

controller

```java
@GetMapping("/timeout/{id}")
@HystrixCommand(fallbackMethod = "getPaymentTimeoutHandler", commandProperties = {
    @HystrixProperty(name = "execution.isolation.thread.timeoutInMilliseconds",value = "3000")
})
public String getPaymentTimeout(@PathVariable("id") Long id) {
    return paymentHystrixService.getPaymentTimeout(id);
}

public String getPaymentTimeoutHandler(@PathVariable("id") Long id){
    return "支付服务繁忙";
}
```



### 全局服务降级

```java
@RestController
@RequestMapping("/consumer/payment")
@Slf4j
@DefaultProperties(defaultFallback = "payment_Global_FallBack")
public class PaymentHystrixController {

    @Autowired
    private PaymentHystrixService paymentHystrixService;

    @GetMapping("/timeout/{id}")
    @HystrixCommand
    public String getPayment_Timeout(@PathVariable("id") Long id) {
        String ret = paymentHystrixService.getPayment_TimeOut(id);
        log.info("msg:{}", ret);
        return ret;
    }

    public String payment_Global_FallBack(){
        return "global_fallback";
    }
}
```

服务降级与业务代码解耦

service

```java
@Service
@FeignClient(value = "CLOUD-HYSTRIX-PAYMENT", fallback = PaymentFallback.class) //配置我们自己编写的 fallback 类
public interface PaymentHystrixService {

    @GetMapping("/payment/hystrix/ok/{id}")
    String getPayment_OK(@PathVariable("id") Long id);

    @GetMapping("/payment/hystrix/timeout/{id}")
    String getPayment_TimeOut(@PathVariable("id") Long id);

}
```

FallbackService

```java
@Component
public class PaymentFallback implements PaymentHystrixService{
    @Override
    public String getPayment_OK(Long id) {
        return "PaymentFallback: getPayment_OK";
    }

    @Override
    public String getPayment_TimeOut(Long id) {
        return "PaymentFallback: getPayment_TimeOut";
    }
}
```



## 服务熔断

![image-20220921222722440](https://taro-note-pic.oss-cn-hangzhou.aliyuncs.com/image-20220921222722440.png)

![image-20220921222825155](https://taro-note-pic.oss-cn-hangzhou.aliyuncs.com/image-20220921222825155.png)

service

```java
//服务熔断
@HystrixCommand(fallbackMethod = "paymentCircuitBreaker_fallback",commandProperties = {
    @HystrixProperty(name = "circuitBreaker.enabled",value = "true"),  //是否开启断路器
    @HystrixProperty(name = "circuitBreaker.requestVolumeThreshold",value = "10"),   //请求次数
    @HystrixProperty(name = "circuitBreaker.sleepWindowInMilliseconds",value = "10000"),  //时间范围
    @HystrixProperty(name = "circuitBreaker.errorThresholdPercentage",value = "60"), //失败率达到多少后跳闸
})
public String paymentCircuitBreaker(@PathVariable("id") Integer id){
    if (id < 0){
        throw new RuntimeException("*****id 不能负数");
    }
    String serialNumber = IdUtil.simpleUUID();

    return Thread.currentThread().getName()+"\t"+"调用成功,流水号："+serialNumber;
}
public String paymentCircuitBreaker_fallback(@PathVariable("id") Integer id){
    return "id 不能负数，请稍候再试,(┬＿┬)/~~     id: " +id;
}
```

controller

```java
//服务熔断
@GetMapping("/circuit/{id}")
public String paymentCircuitBreaker(@PathVariable("id") Integer id){
    String result = paymentService.paymentCircuitBreaker(id);
    log.info("result: {}", result);
    return result;
}
```

![image-20220921230228797](https://taro-note-pic.oss-cn-hangzhou.aliyuncs.com/image-20220921230228797.png)

1. 请求达到熔断阈值 
2. 错误率达到熔断阈值 
3. 熔断机制打开 
4. 当熔断机制打开的时候拒绝一切请求
5. 一段时间后，熔断机制会进入半开状态，会放行单个请求，若这个请求失败，会回到熔断机制的打开状态，成功，则关闭熔断

### 服务熔断的所有配置

![image-20220921232644210](https://taro-note-pic.oss-cn-hangzhou.aliyuncs.com/image-20220921232644210.png)



## hystrix 的图形化微服务监控界面

需要自己编写一个模块来提供 hystrix 的图形化界面服务

pom 添加

```xml
<!--新增hystrix dashboard-->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-hystrix-dashboard</artifactId>
</dependency>
```

主启动类添加激活监控功能的注解

```java
@SpringBootApplication
@EnableHystrixDashboard //开启 hystrix 的图形化监控界面
public class HystrixDashBoardMain9001 {

    public static void main(String[] args) {
        SpringApplication.run(HystrixDashBoardMain9001.class, args);
    }
     
    /**
     * 此配置是为了服务监控而配置，与服务容错本身无关，是 SpringCloud 升级后的坑
     * ServletRegistrationBean 因为 SpringBoot 的默认路径不是 "/hystrix.stream"，
     * 只要在自己的项目配置下面的servlet就可以
     */
    @Bean
    public ServletRegistrationBean getServlet(){
        HystrixMetricsStreamServlet streamServlet = new HystrixMetricsStreamServlet();
        ServletRegistrationBean registrationBean = new ServletRegistrationBean(streamServlet);
        registrationBean.setLoadOnStartup(1);
        registrationBean.addUrlMappings("/hystrix.stream");
        registrationBean.setName("HystrixMetricsStreamServlet");
        return registrationBean;
    }

}
```

http://localhost:9001/hystrix 访问监控页面



# Gateway 服务网关

https://cloud.spring.io/spring-cloud-static/spring-cloud-gateway/2.2.1.RELEASE/reference/html/

三个主要部分：

- Route 路由，路由是构建网关的基本模块，它由ID，目标URI，一系列的断言和过滤器组成，如果断言为true则匹配该路由
- Predicate 断言，参考的是java8的 java.util.function.Predicate 开发人员可以匹配HTTP请求中的所有内容（例如请求头或请求参数），如果请求与断言相匹配则进行路由
- Filter 过滤，指的是Spring框架中GatewayFilter的实例，使用过滤器，可以在请求被路由前或者之后对请求进行修改。

核心逻辑：==逻辑转发 + 执行过滤器链==



## Gateway 网关配置

方式一：在 yml 中配置

```yml
server:
  port: 9527

spring:
  application:
    name: cloud-gateway
  cloud:
    gateway:
      routes:
        - id: payment_routh #路由的ID，没有固定规则但要求唯一，建议配合服务名
          uri: http://localhost:8001   #匹配后提供服务的路由地址
          predicates:
            - Path=/payment/get/**   #断言,路径相匹配的进行路由

        - id: payment_routh2
          uri: http://localhost:8001
          predicates:
            - Path=/payment/lb/**   #断言,路径相匹配的进行路由

eureka:
  instance:
    hostname: cloud-gateway-service
  client:
    service-url:
      register-with-eureka: true
      fetch-registry: true
      defaultZone: http://eureka7001.com:7001/eureka
```

9527 端口就配置为 8001 端口的两个访问路径的网关



方式二：在 IOC 中注入网关类

```java
@Configuration
public class MyGateWayConfig {

    @Bean
    public RouteLocator consumerRouteLocator(RouteLocatorBuilder routeLocatorBuilder) {
        RouteLocatorBuilder.Builder routes = routeLocatorBuilder.routes();
		
        routes.route("path_route_taro", r -> r.path("/guonei").uri("http://news.baidu.com/guonei")).build();

        return routes.build();
    }
}
```



## 动态路由配置

网关 yml 配置

```yml
server:
  port: 9527
spring:
  application:
    name: cloud-gateway
  cloud:
    gateway:
      discovery:
        locator:
          enabled: true  #开启从注册中心动态创建路由的功能，利用微服务名进行路由
      routes:
        - id: payment_routh #路由的ID，没有固定规则但要求唯一，建议配合服务名
          #uri: http://localhost:8001 匹配后提供服务的路由地址
          uri: lb://CLOUD-PAYMENT-SERVICE
          predicates:
            - Path=/payment/get/**   #断言,路径相匹配的进行路由

        - id: payment_routh2
          #uri: http://localhost:8001 匹配后提供服务的路由地址
          uri: lb://CLOUD-PAYMENT-SERVICE
          predicates:
            - Path=/payment/lb/**   #断言,路径相匹配的进行路由


eureka:
  instance:
    hostname: cloud-gateway-service
  client:
    service-url:
      defaultZone: http://eureka7001.com:7001/eureka
    register-with-eureka: true
    fetch-registry: true
```



## 常用的 Predicate

```yml
server:
  port: 9527
spring:
  application:
    name: cloud-gateway
  cloud:
    gateway:
      discovery:
        locator:
          enabled: true  #开启从注册中心动态创建路由的功能，利用微服务名进行路由
      routes:
        - id: payment_routh #路由的ID，没有固定规则但要求唯一，建议配合服务名
          #uri: http://localhost:8001   #匹配后提供服务的路由地址
          uri: lb://cloud-payment-service
          predicates:
            - Path=/payment/get/**   #断言,路径相匹配的进行路由
 
        - id: payment_routh2
          #uri: http://localhost:8001   #匹配后提供服务的路由地址
          uri: lb://cloud-payment-service
          predicates:
            - Path=/payment/lb/**   #断言,路径相匹配的进行路由
            #- After=2020-03-08T10:59:34.102+08:00[Asia/Shanghai]
            #- Cookie=username,zhangshuai #并且Cookie是username=zhangshuai才能访问
            #- Header=X-Request-Id, \d+ #请求头中要有X-Request-Id属性并且值为整数的正则表达式
            #- Host=**.atguigu.com
            #- Method=GET
            #- Query=username, \d+ #要有参数名称并且是正整数才能路由
 
 
eureka:
  instance:
    hostname: cloud-gateway-service
  client:
    service-url:
      register-with-eureka: true
      fetch-registry: true
      defaultZone: http://eureka7001.com:7001/eureka
 

```



## 自定义的 Filter

```java
@Component
@Slf4j
public class MyLogGateWayFilter implements GlobalFilter, Ordered {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        log.info("come int MyLogGateWayFilter： {}", new Date());
        String uname = exchange.getRequest().getQueryParams().getFirst("uname");
        if(uname == null) {
            log.info("用户名为 null");
            exchange.getResponse().setStatusCode(HttpStatus.NOT_ACCEPTABLE);
            return exchange.getResponse().setComplete();
        }
        //放行后的过滤器链传递
        return chain.filter(exchange);
    }

    /**
     * 获取自定义过滤器的优先级
     * @return
     */
    @Override
    public int getOrder() {
        return 0;
    }
}
```





# SpringCloud Config

![image-20220925211703920](https://taro-note-pic.oss-cn-hangzhou.aliyuncs.com/image-20220925211703920.png)

[SpringCloud_Config 官方文档](https://cloud.spring.io/spring-cloud-static/spring-cloud-config/2.2.1.RELEASE/reference/html/)

## Server

pom 添加依赖

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-config-server</artifactId>
</dependency>
```

yml 配置

```yml
server:
  port: 3344
spring:
  application:
    name: cloud-config-center
  cloud:
    config:
      server:
        git:
          uri: https://github.com/Taro2021/springcloud-config.git
          search-paths:
            - springcloud-config
        default-label: main #默认分支

eureka:
  client:
    service-url:
      defaultZone:  http://localhost:7001/eureka
```

主启动类

```java
//port 3344
@SpringBootApplication
@EnableConfigServer
@EnableEurekaClient
public class ConfigCenterMain3344 {

    public static void main(String[] args) {
        SpringApplication.run(ConfigCenterMain3344.class, args);
    }
}
```

![image-20220925212116336](https://taro-note-pic.oss-cn-hangzhou.aliyuncs.com/image-20220925212116336.png)

**配置读取规则**

```
/{label}/{application}-{profile}.yml
/{application}-{profile}.yml
/{application}-{profile}[/{label}]
```



## client

pom

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-config</artifactId>
</dependency>
```

yml

**需要配置 bootstrap.yml 替代 application.yml**

![image-20220925214000463](https://taro-note-pic.oss-cn-hangzhou.aliyuncs.com/image-20220925214000463.png)

```yml
server:
  port: 3355

spring:
  application:
    name: config-client
  cloud:
    config:
      label: main  #分支
      name: config #配置文件名
      profile: dev #读取后缀名 main 分支上的 config-dev.yml
      uri: http://localhost:3344 #配置
eureka:
  client:
    service-url:
      defaultZone: http://eureka7001.com:7001/eureka
```



controller

```java
//port 3355
@RestController
public class ConfigClientController {

    @Value("${config.info}")
    private String configInfo;

    @GetMapping("/configInfo")
    public String getConfigInfo() {
        return configInfo;
    }
}
```

​	当前客户端，github 上的配置文件修改，配置中心 server 3344 会立刻响应修改，而客户端 3355 并不会，需要重启客户端才会获取修改的配置信息。

​	因为我们编写的客户端的 controller 中，第一次启动了之后通过Value注入到私有变量中了,然后3355访问的接口的返回值是刚才的私有变量,这时 github 上面发生改变,但是3355中的那个变量的值已经被注入过了, 并不会再次注入而是直接返回已经注入的对象。



## 客户端的动态刷新

（手动挡）

修改客户端

pom

```xml
<!-- 引入监控模块（一般已经与 web 模块一同引入）-->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

修改 yml 暴露监控端口

```yml
# 暴露端点
management:
  endpoints:
    web:
      exposure:
        include: "*"
```

controller 添加刷新注解

```java
@RestController
@RefreshScope //刷新注解
public class ConfigClientController {
    
    @Value("${config.info}")
    private String configInfo;

    @GetMapping("/configInfo")
    public String getConfigInfo() {
        return configInfo;
    }
}
```

​	!!!还需要向客户端发送一个刷新的 **post 请求 `http://localhost:3355/actuator/refresh`** 去向监控模块发送请求通知刷新。

​	多个客户端只能逐个手动通知刷新。

# SpringCloud Bus

![image-20220927193530518](https://taro-note-pic.oss-cn-hangzhou.aliyuncs.com/image-20220927193530518.png)

![image-20220927193812226](https://taro-note-pic.oss-cn-hangzhou.aliyuncs.com/image-20220927193812226.png)

![image-20220930161145729](https://taro-note-pic.oss-cn-hangzhou.aliyuncs.com/image-20220930161145729.png)

## RabbitMQ  配置

1. 下载 erlang http://erlang.org/download/otp_win64_21.3.exe

### docker 部署 

```shell
 docker search rabbitmq:management #查看镜像版本
 docker pull rabbitmq:management #拉取镜像
 docker run -d -p 15672:15672  -p  5672:5672  -e RABBITMQ_DEFAULT_USER=admin -e RABBITMQ_DEFAULT_PASS=admin --name rabbitmq1 --hostname=rabbitmqhostone  rabbitmq:management #启动容器 设置端口映射，设置用户名，密码
```

图形化界面访问地址 ip:15672



## SpringCloud bus 动态刷新全局广播

消息总线通知的两种设计思想：

1. 利用消息总线触发一个客户端/bus/refresh,而刷新所有客户端的配置

   ![image-20220930154137712](https://taro-note-pic.oss-cn-hangzhou.aliyuncs.com/image-20220930154137712.png)

2. 利用消息总线触发一个服务端ConfigServer的/bus/refresh端点,而刷新所有客户端的配置（更加推荐）

   ![image-20220930154214450](https://taro-note-pic.oss-cn-hangzhou.aliyuncs.com/image-20220930154214450.png)



**第二种架构更加的合理：**

- 传染方式的架构打破了微服务的职责单一性，因为微服务本身是业务模块，它本不应该承担配置刷新职责
- 传染方式破坏了微服务各节点的对等性
- 传染方式有一定的局限性。例如，微服务在迁移时，它的网络地址常常会发生变化，此时如果想要做到自动刷新，那就会增加更多的修改



### 配置中心服务端添加消息总线的支持

pom

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-bus-amqp</artifactId>
</dependency>
```

yml

```yml
server:
  port: 3344
spring:
  application:
    name: cloud-config-center
  cloud:
    config:
      server:
        git:
          uri: https://github.com/Taro2021/springcloud-config.git
          search-paths:
            - springcloud-config
        default-label: main
  #rabbitMQ 相关配置
  rabbitmq:
    host: 121.199.78.94
    port: 5672
    username: admin
    password: admin

management:
  endpoints: #暴露 bus 刷新配置的端点
    web:
      exposure:
        include: 'bus-refresh'

eureka:
  client:
    service-url:
      defaultZone:  http://localhost:7001/eureka
```



### 客户端添加总线支持

pom

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-bus-amqp</artifactId>
</dependency>
```

yml 添加 rabbitMQ 配置信息

```yml
  rabbitmq:
    host: 121.199.78.94
    port: 5672
    username: admin
    password: admin
```



### 向配置中心服务端发出总线刷新请求

```
POST http://localhost:3344/actuator/bus-refresh
```

一处刷新处处生效



## SpringCloud bus 动态刷新定点通知

​	指定具体某一个实例生效而不是全部

​	公式：`http://配置中心ip:配置中心的端口号/actuator/bus-refresh/{destination}`





# SpringCloud Stream

​	消息驱动：屏蔽底层消息中间件的差异，降低切换版本，统一消息的编程模型

![image-20220930172828662](https://taro-note-pic.oss-cn-hangzhou.aliyuncs.com/image-20220930172828662.png)

![image-20220930163551497](https://taro-note-pic.oss-cn-hangzhou.aliyuncs.com/image-20220930163551497.png)



## 消息提供者

pom 添加消息驱动关于 rabbitMQ 的依赖，kafk 同理

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-stream-rabbit</artifactId>
</dependency>
```

yml

```yml
server:
  port: 8801

spring:
  application:
    name: cloud-stream-provider
  cloud:
    stream:
      binders: #配置要绑定的 rabbitmq 的服务信息
        defaultRabbit: #表示定义的名称，用于 binding 整合
          type: rabbit #消息队列组件类型
          environment: #设置 ribbitmq 的相关环境配置
            spring:
              rabbitmq:
                host: 121.199.78.94
                port: 5672
                username: admin
                password: admin
      bindings: #服务的整合处理
        output: # 这个名字是一个通道的名称
          destination: studyExchange #表示使用的 Exchange 名称定义
          content-type: application/json #设置消息类型
          binder: {defaultRabbit} #设置要绑定的消息符文u的具体设置

eureka:
  client: # 客户端进行Eureka注册的配置
    service-url:
      defaultZone: http://localhost:7001/eureka
  instance:
    lease-renewal-interval-in-seconds: 2 # 设置心跳的时间间隔（默认是30秒）
    lease-expiration-duration-in-seconds: 5 # 如果现在超过了5秒的间隔（默认是90秒）
    instance-id: send-8801.com  # 在信息列表时显示主机名称
    prefer-ip-address: true     # 访问的路径变为IP地址
```

service

```java
//生产者的 service 不再是去调用 dao 而是去调用消息中间件去推送消息
@EnableBinding(Source.class) //定义消息的推送管道
public class IMessageProviderImpl implements IMessageProvider {

    @Resource
    private MessageChannel output; //消息发送管道

    @Override
    public String send() {
        String serial = UUID.randomUUID().toString();
        output.send(MessageBuilder.withPayload(serial).build());
        return null;
    }
}
```



## 消费者

consumer 与 provider 的配置唯一区别是通道类型

```yml
server:
  port: 8802

spring:
  application:
    name: cloud-stream-consumer
  cloud:
    stream:
      binders: 
        defaultRabbit: 
          type: rabbit
          environment:
            spring:
              rabbitmq:
                host: 121.199.78.94
                port: 5672
                username: admin
                password: admin
      bindings:
        input: # 这个名字是一个通道的名称
          destination: studyExchange
          content-type: application/json 
          binder: {defaultRabbit} 

eureka:
  client: 
    service-url:
      defaultZone: http://localhost:7001/eureka
  instance:
    lease-renewal-interval-in-seconds: 2 
    lease-expiration-duration-in-seconds: 5 
    instance-id: send-8801.com  
    prefer-ip-address: true    
```

controller

```java
@Controller
@EnableBinding(Sink.class)
public class ConsumerController {

    @Value("${server.port}")
    private String serverPort;
	
    //消费者从管道接收消息
    @StreamListener(Sink.INPUT)
    public void input(Message<String> message){
        System.out.println("consumer 1 receive: " + message.getPayload() + "\t port: " + serverPort);
    }
}
```



## 分组消费与持久化

==配置group属性==

再建立一个同样的消费者模块。启动后遇到两个问题：

			1. **存在重复消费问题**
			1. **消息持久化问题**

微服务应用放置于同一个 group 中，就能够保证消息只会被其中一个应用消费一次。**不同的组是可以全面消费的**，**同一个组内会发生竞争关系**，只有其中一个可以消费。



修改两个消费者微服务模块的分组，分在同一组中则每一次只能有一个服务消费消息

```yml
      bindings:
        input:
          destination: studyExchange
          content-type: application/json
          binder: {defaultRabbit}
          group: taro1 #自定义分组名
```

![image-20220930175945934](https://taro-note-pic.oss-cn-hangzhou.aliyuncs.com/image-20220930175945934.png)



**配置了 group 属性后：**该组的消息就自动持久化。这种持久化是队列持久化，如果指定分组那么创建的队列就是持久化队列，再次上线连接上队列一样可以获取到队列当中的消息。如果不指定分组就会随机分组，随机分组创建的队列是非持久化的，消费者宕机后队列就销毁。



# SpringCloud Sleuth

分布式请求链路追踪

https://github.com/spring-cloud/spring-cloud-sleuth

Spring Cloud Sleuth提供了一套完整的服务跟踪的解决方案，在分布式系统中提供追踪解决方案并且兼容支持了zipkin。



## zipkin 链路追踪

SpringCloud从F版起已不需要自己构建 Zipkin server了，只需要调用jar包即可

https://dl.bintray.com/openzipkin/maven/io/zipkin/java/zipkin-server/

启动 zipkin `java -jar zipkin-server-2.14.1-exec.jar`

http://localhost:9411/zipkin/

![image-20221001094913192](https://taro-note-pic.oss-cn-hangzhou.aliyuncs.com/image-20221001094913192.png)



## 被监控微服务

pom

```xml
<!--包含了sleuth+zipkin-->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-zipkin</artifactId>
</dependency>
```

yml 添加配置

```yml
spring:
  zipkin:
    base-url: http://localhost:9411
  sleuth:
    sampler:
      probability: 1 #采样率介于 0 到 1 之间，1 表示全部采集
```



provider controller

```java
@GetMapping("/zipkin")
public String paymentZipkin(){
    return "I'm payementZipkin server fall back!";
}
```

consumer controller

```java
@GetMapping("/zipkin")
public String consumerZipkin(){
    return restTemplate.getForObject("http://localhost:8001" + "/payment/zipkin/", String.class);
}
```

![image-20221001101428610](https://taro-note-pic.oss-cn-hangzhou.aliyuncs.com/image-20221001101428610.png)

---





# SpringCloud Alibaba

## 主要功能

* **服务限流降级**：默认支持 WebServlet、WebFlux、OpenFeign、RestTemplate、Spring Cloud Gateway、Zuul、Dubbo 和 RocketMQ 限流降级功能的接入，可以在运行时通过控制台实时修改限流降级规则，还支持查看限流降级 Metrics 监控。
* **服务注册与发现**：适配 Spring Cloud 服务注册与发现标准，默认集成了 Ribbon 的支持。
* **分布式配置管理**：支持分布式系统中的外部化配置，配置更改时自动刷新。
* **消息驱动能力**：基于 Spring Cloud Stream 为微服务应用构建消息驱动能力。
* **分布式事务**：使用 @GlobalTransactional 注解， 高效并且对业务零侵入地解决分布式事务问题。
* **阿里云对象存储**：阿里云提供的海量、安全、低成本、高可靠的云存储服务。支持在任何应用、任何时间、任何地点存储和访问任意类型的数据。
* **分布式任务调度**：提供秒级、精准、高可靠、高可用的定时（基于 Cron 表达式）任务调度服务。同时提供分布式的任务执行模型，如网格任务。网格任务支持海量子任务均匀分配到所有 Worker（schedulerx-client）上执行。
* **阿里云短信服务**：覆盖全球的短信服务，友好、高效、智能的互联化通讯能力，帮助企业迅速搭建客户触达通道。


更多功能请参考 [Roadmap](https://github.com/alibaba/spring-cloud-alibaba/blob/master/Roadmap-zh.md)。

https://github.com/alibaba/spring-cloud-alibaba/blob/master/README-zh.md

https://spring.io/projects/spring-cloud-alibaba#overview



父 pom dependencyManagement 依赖管理添加 

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-alibaba-dependencies</artifactId>
            <version>2.2.9.RELEASE</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

# Nacos 

服务注册中心 + 配置中心

[nacos](https://nacos.io/zh-cn/)

docker 配置 nacos-server 

- 镜像： `docker pull nacos/nacos-server`
- 容器启动：`docker run -d --name nacos -p 8848:8848 -e PREFER_HOST_MODE=hostname -e MODE=standalone nacos/nacos-server`
- nacos所有元数据都会保存在容器内部。倘若容器迁移则nacos元数据则不复存在，所以通常我们通常会将nacos元数据保存在mysql中。这里仅学习 nacos 没有进行修改。



## 基于 Nacos 的微服务注册

pom 添加依赖

```xml
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
</dependency>
```

yml

```yml
server:
  port: 9001

spring:
  application:
    name: nacos-payment-provider
  cloud:
    nacos:
      discovery:
        server-addr: 121.199.78.94:8848 #配置Nacos地址

management:
  endpoints:
    web:
      exposure:
        include: '*'
```

主启动类

```java
@SpringBootApplication
@EnableDiscoveryClient
public class Payment9001 {
    public static void main(String[] args) {
        SpringApplication.run(Payment9001.class, args);
    }
}
```

​	nacos 集成了ribbon 自带负载均衡组件 `@LoadBalanced`，记得要在 IOC 容器中注册 RestTemplate 的 bean。

​	在 Spring Boot 1.3版本中，会默认提供一个[RestTemplate](https://so.csdn.net/so/search?q=RestTemplate&spm=1001.2101.3001.7020)的实例Bean，而在 Spring Boot 1.4以及以后的版本中，这个默认的bean不再提供了，我们需要在Application启动时，手动创建一个RestTemplate的配置。





## Nacos支持AP和CP模式的切换

![image-20221001153748597](https://taro-note-pic.oss-cn-hangzhou.aliyuncs.com/image-20221001153748597.png)



## Nacos 作为配置中心

### client

引入依赖

```xml
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-config</artifactId>
</dependency>
<!-- bootstrap.yml不生效问题。2020版本之后需要配置bootstrap.yml生效，添加一下依赖即可 -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-bootstrap</artifactId>
</dependency>
```

需要配置两个 yml 配置文件

bootstrap.yml

```yml
server:
  port: 3377

spring:
  application:
    name: nacos-config-client
  cloud:
    nacos:
      discovery:
        server-addr: 121.199.78.94:8848 #服务注册中心地址
      config:
        server-addr: 121.199.78.94:8848 #配置中心地址
        file-extension: yaml #指定配置文件格式
```

application.yml

```yml
spring:
  profiles:
    active: dev
```

主启动类

```java
@SpringBootApplication
@EnableDiscoveryClient
public class NacosConfigClient3377 {
    public static void main(String[] args) {
        SpringApplication.run(NacosConfigClient3377.class, args);
    }
}
```

controller

```java
@RestController
@RefreshScope //配置实时更新
public class NacosConfigClientController {

    @Value("${config.info}")
    private String configInfo;

    @GetMapping("/config/info")
    public String getConfigInfo(){
        return configInfo;
    }
}
```



### Nacos-server

在 Nacos Spring Cloud 中，`dataId` 的完整格式如下：==!!!神坑注意格式一点不能错==

```
${spring.application.name}-${spring.profiles.active}.${file-extension}
nacos-config-client-dev.yaml
```

![image-20221001160748542](https://taro-note-pic.oss-cn-hangzhou.aliyuncs.com/image-20221001160748542.png)

![image-20221001172242555](https://taro-note-pic.oss-cn-hangzhou.aliyuncs.com/image-20221001172242555.png)

在 nacos 配置中心修改配置文件，client 即可动态刷新配置信息



## Nacos 分类配置

![image-20221003155306306](https://taro-note-pic.oss-cn-hangzhou.aliyuncs.com/image-20221003155306306.png)

![image-20221003155408914](https://taro-note-pic.oss-cn-hangzhou.aliyuncs.com/image-20221003155408914.png)



### DataID方案

指定spring.profile.active和配置文件的DataID来使不同环境下读取不同的配置

![image-20221003160028402](https://taro-note-pic.oss-cn-hangzhou.aliyuncs.com/image-20221003160028402.png)

![image-20221003160107600](https://taro-note-pic.oss-cn-hangzhou.aliyuncs.com/image-20221003160107600.png)

配置是什么就加载什么



### Group方案

![image-20221003160354047](https://taro-note-pic.oss-cn-hangzhou.aliyuncs.com/image-20221003160354047.png)

修改 bootstrap 在config下增加一条group的配置即可。可配置为DEV_GROUP或TEST_GROUP

```yml
server:
  port: 3377

spring:
  application:
    name: nacos-config-client
  cloud:
    nacos:
      discovery:
        server-addr: 121.199.78.11:8848 
      config:
        server-addr: 121.199.78.11:8848 
        file-extension: yaml 
        group: DEV_GROUP #配置组信息
```



### Namespace方案

![image-20221003161017110](https://taro-note-pic.oss-cn-hangzhou.aliyuncs.com/image-20221003161017110.png)

![image-20221003161445598](https://taro-note-pic.oss-cn-hangzhou.aliyuncs.com/image-20221003161445598.png)

修改 bootstrap 配置文件，添加命名空间

```yml
server:
  port: 3377

spring:
  application:
    name: nacos-config-client
  cloud:
    nacos:
      discovery:
        server-addr: 121.199.78.11:8848
      config:
        server-addr: 121.199.78.11:8848 
        file-extension: yaml 
        group: DEV_GROUP 
        namespace: be16709e-08b4-43d5-8555-fb64628ed302 #配置命名空间
```



## Nacos 集群和持久化配置

### docker nacos 持久化

mysql version ：8.0.28 MySQL Community Server

docker nacos version：2.1.1

拉取 nacos 镜像，创建容器，将容器中的配置文件挂载到宿主机上

```shell
docker pull nacos/nacos-server
docker run -d -p 8848:8848 -e MODE=standalone --restart always --name nacos nacos/nacos-server
docker cp container_id:/home/nacos ~/mydata/nacos
```

```shell
#减少启动内存分配，配置文件挂载
docker run -d -p 8848:8848 -e JVM_XMS=128m -e JVM_XMX=128m -e JVM_XMN=64m -e MODE=standalone -v ~/mydata/nacos/:/home/nacos/ --restart always --name nacos nacos/nacos-server

-e JVM_XMN=16m -e JVM_MS=8m -e JVM_MMS=8m
```

修改 nacos 的 conf/application.propertities 将 nacos 默认使用嵌入的 derby 数据库改为宿主机的 mysql 数据库

```properties
spring.datasource.platform=mysql

db.num=1
#连接8.0以上的 mysql 数据源需要加上时区
db.url.0=jdbc:mysql://172.30.167.211:3306/nacos_config?characterEncoding=utf8&connectTimeout=1000&socketTimeout=3000&autoReconnect=true&useSSL=false&serverTimezone=UTC
db.user=root
db.password=123456
```

执行 nacos 所需数据库的建立脚本，需要对应 nacos 版本的 sql 脚本

```sql
/*
 * Copyright 1999-2018 Alibaba Group Holding Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/******************************************/
/*   数据库全名 = nacos_config   */
/*   表名称 = config_info   */
/******************************************/
CREATE TABLE `config_info` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'id',
  `data_id` varchar(255) NOT NULL COMMENT 'data_id',
  `group_id` varchar(255) DEFAULT NULL,
  `content` longtext NOT NULL COMMENT 'content',
  `md5` varchar(32) DEFAULT NULL COMMENT 'md5',
  `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  `src_user` text COMMENT 'source user',
  `src_ip` varchar(50) DEFAULT NULL COMMENT 'source ip',
  `app_name` varchar(128) DEFAULT NULL,
  `tenant_id` varchar(128) DEFAULT '' COMMENT '租户字段',
  `c_desc` varchar(256) DEFAULT NULL,
  `c_use` varchar(64) DEFAULT NULL,
  `effect` varchar(64) DEFAULT NULL,
  `type` varchar(64) DEFAULT NULL,
  `c_schema` text,
  `encrypted_data_key` text NOT NULL COMMENT '秘钥',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_configinfo_datagrouptenant` (`data_id`,`group_id`,`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='config_info';

/******************************************/
/*   数据库全名 = nacos_config   */
/*   表名称 = config_info_aggr   */
/******************************************/
CREATE TABLE `config_info_aggr` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'id',
  `data_id` varchar(255) NOT NULL COMMENT 'data_id',
  `group_id` varchar(255) NOT NULL COMMENT 'group_id',
  `datum_id` varchar(255) NOT NULL COMMENT 'datum_id',
  `content` longtext NOT NULL COMMENT '内容',
  `gmt_modified` datetime NOT NULL COMMENT '修改时间',
  `app_name` varchar(128) DEFAULT NULL,
  `tenant_id` varchar(128) DEFAULT '' COMMENT '租户字段',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_configinfoaggr_datagrouptenantdatum` (`data_id`,`group_id`,`tenant_id`,`datum_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='增加租户字段';


/******************************************/
/*   数据库全名 = nacos_config   */
/*   表名称 = config_info_beta   */
/******************************************/
CREATE TABLE `config_info_beta` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'id',
  `data_id` varchar(255) NOT NULL COMMENT 'data_id',
  `group_id` varchar(128) NOT NULL COMMENT 'group_id',
  `app_name` varchar(128) DEFAULT NULL COMMENT 'app_name',
  `content` longtext NOT NULL COMMENT 'content',
  `beta_ips` varchar(1024) DEFAULT NULL COMMENT 'betaIps',
  `md5` varchar(32) DEFAULT NULL COMMENT 'md5',
  `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  `src_user` text COMMENT 'source user',
  `src_ip` varchar(50) DEFAULT NULL COMMENT 'source ip',
  `tenant_id` varchar(128) DEFAULT '' COMMENT '租户字段',
  `encrypted_data_key` text NOT NULL COMMENT '秘钥',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_configinfobeta_datagrouptenant` (`data_id`,`group_id`,`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='config_info_beta';

/******************************************/
/*   数据库全名 = nacos_config   */
/*   表名称 = config_info_tag   */
/******************************************/
CREATE TABLE `config_info_tag` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'id',
  `data_id` varchar(255) NOT NULL COMMENT 'data_id',
  `group_id` varchar(128) NOT NULL COMMENT 'group_id',
  `tenant_id` varchar(128) DEFAULT '' COMMENT 'tenant_id',
  `tag_id` varchar(128) NOT NULL COMMENT 'tag_id',
  `app_name` varchar(128) DEFAULT NULL COMMENT 'app_name',
  `content` longtext NOT NULL COMMENT 'content',
  `md5` varchar(32) DEFAULT NULL COMMENT 'md5',
  `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  `src_user` text COMMENT 'source user',
  `src_ip` varchar(50) DEFAULT NULL COMMENT 'source ip',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_configinfotag_datagrouptenanttag` (`data_id`,`group_id`,`tenant_id`,`tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='config_info_tag';

/******************************************/
/*   数据库全名 = nacos_config   */
/*   表名称 = config_tags_relation   */
/******************************************/
CREATE TABLE `config_tags_relation` (
  `id` bigint(20) NOT NULL COMMENT 'id',
  `tag_name` varchar(128) NOT NULL COMMENT 'tag_name',
  `tag_type` varchar(64) DEFAULT NULL COMMENT 'tag_type',
  `data_id` varchar(255) NOT NULL COMMENT 'data_id',
  `group_id` varchar(128) NOT NULL COMMENT 'group_id',
  `tenant_id` varchar(128) DEFAULT '' COMMENT 'tenant_id',
  `nid` bigint(20) NOT NULL AUTO_INCREMENT,
  PRIMARY KEY (`nid`),
  UNIQUE KEY `uk_configtagrelation_configidtag` (`id`,`tag_name`,`tag_type`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='config_tag_relation';

/******************************************/
/*   数据库全名 = nacos_config   */
/*   表名称 = group_capacity   */
/******************************************/
CREATE TABLE `group_capacity` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `group_id` varchar(128) NOT NULL DEFAULT '' COMMENT 'Group ID，空字符表示整个集群',
  `quota` int(10) unsigned NOT NULL DEFAULT '0' COMMENT '配额，0表示使用默认值',
  `usage` int(10) unsigned NOT NULL DEFAULT '0' COMMENT '使用量',
  `max_size` int(10) unsigned NOT NULL DEFAULT '0' COMMENT '单个配置大小上限，单位为字节，0表示使用默认值',
  `max_aggr_count` int(10) unsigned NOT NULL DEFAULT '0' COMMENT '聚合子配置最大个数，，0表示使用默认值',
  `max_aggr_size` int(10) unsigned NOT NULL DEFAULT '0' COMMENT '单个聚合数据的子配置大小上限，单位为字节，0表示使用默认值',
  `max_history_count` int(10) unsigned NOT NULL DEFAULT '0' COMMENT '最大变更历史数量',
  `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_group_id` (`group_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='集群、各Group容量信息表';

/******************************************/
/*   数据库全名 = nacos_config   */
/*   表名称 = his_config_info   */
/******************************************/
CREATE TABLE `his_config_info` (
  `id` bigint(64) unsigned NOT NULL,
  `nid` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `data_id` varchar(255) NOT NULL,
  `group_id` varchar(128) NOT NULL,
  `app_name` varchar(128) DEFAULT NULL COMMENT 'app_name',
  `content` longtext NOT NULL,
  `md5` varchar(32) DEFAULT NULL,
  `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `src_user` text,
  `src_ip` varchar(50) DEFAULT NULL,
  `op_type` char(10) DEFAULT NULL,
  `tenant_id` varchar(128) DEFAULT '' COMMENT '租户字段',
  `encrypted_data_key` text NOT NULL COMMENT '秘钥',
  PRIMARY KEY (`nid`),
  KEY `idx_gmt_create` (`gmt_create`),
  KEY `idx_gmt_modified` (`gmt_modified`),
  KEY `idx_did` (`data_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='多租户改造';


/******************************************/
/*   数据库全名 = nacos_config   */
/*   表名称 = tenant_capacity   */
/******************************************/
CREATE TABLE `tenant_capacity` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` varchar(128) NOT NULL DEFAULT '' COMMENT 'Tenant ID',
  `quota` int(10) unsigned NOT NULL DEFAULT '0' COMMENT '配额，0表示使用默认值',
  `usage` int(10) unsigned NOT NULL DEFAULT '0' COMMENT '使用量',
  `max_size` int(10) unsigned NOT NULL DEFAULT '0' COMMENT '单个配置大小上限，单位为字节，0表示使用默认值',
  `max_aggr_count` int(10) unsigned NOT NULL DEFAULT '0' COMMENT '聚合子配置最大个数',
  `max_aggr_size` int(10) unsigned NOT NULL DEFAULT '0' COMMENT '单个聚合数据的子配置大小上限，单位为字节，0表示使用默认值',
  `max_history_count` int(10) unsigned NOT NULL DEFAULT '0' COMMENT '最大变更历史数量',
  `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='租户容量信息表';


CREATE TABLE `tenant_info` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'id',
  `kp` varchar(128) NOT NULL COMMENT 'kp',
  `tenant_id` varchar(128) default '' COMMENT 'tenant_id',
  `tenant_name` varchar(128) default '' COMMENT 'tenant_name',
  `tenant_desc` varchar(256) DEFAULT NULL COMMENT 'tenant_desc',
  `create_source` varchar(32) DEFAULT NULL COMMENT 'create_source',
  `gmt_create` bigint(20) NOT NULL COMMENT '创建时间',
  `gmt_modified` bigint(20) NOT NULL COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_info_kptenantid` (`kp`,`tenant_id`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='tenant_info';

CREATE TABLE `users` (
	`username` varchar(50) NOT NULL PRIMARY KEY,
	`password` varchar(500) NOT NULL,
	`enabled` boolean NOT NULL
);

CREATE TABLE `roles` (
	`username` varchar(50) NOT NULL,
	`role` varchar(50) NOT NULL,
	UNIQUE INDEX `idx_user_role` (`username` ASC, `role` ASC) USING BTREE
);

CREATE TABLE `permissions` (
    `role` varchar(50) NOT NULL,
    `resource` varchar(255) NOT NULL,
    `action` varchar(8) NOT NULL,
    UNIQUE INDEX `uk_role_permission` (`role`,`resource`,`action`) USING BTREE
);

INSERT INTO users (username, password, enabled) VALUES ('nacos', '$2a$10$EuWPZHzz32dJN7jexM34MOeYirDdFAZm2kuWj7VEOJhhZkDrxfvUu', TRUE);

INSERT INTO roles (username, role) VALUES ('nacos', 'ROLE_ADMIN');
```



nginx 网关 + nocas 集群部署暂时先跳过







# Sentinel

流量控制，熔断降级

https://github.com/alibaba/Sentinel/releases

![image-20221006105450155](https://taro-note-pic.oss-cn-hangzhou.aliyuncs.com/image-20221006105450155.png)

`java -jar sentinel-dashboard-1.7.0.jar ` 默认使用 8080 端口 http://localhost:8080

默认用户民密码：sentinel

docker :

```shell
docker pull bladex/sentinel-dashboard
docker run -d -p 8858:8858 --name sentinel image_id
```



## 测试

### 启动 nacos

### 新建模块

#### pom

```xml
<!-- 添加依赖 -->
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
</dependency>

<dependency>
    <groupId>com.alibaba.csp</groupId>
    <artifactId>sentinel-datasource-nacos</artifactId>
</dependency>

<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-sentinel</artifactId>
</dependency>
```

#### yml

```yml
server:
  port: 8401

spring:
  application:
    name: cloudalibaba-sentinel-service
  cloud:
    nacos:
      discovery:
        server-addr: 121.199.78.11:8848
    sentinel:
      transport:
        dashboard: localhost:8080 #sentinel监控与微服务在同一个内网中
        port: 8719 #sentinel 后台和我们微服务通信的端口

management:
  endpoints:
    web:
      exposure:
        include: '*'
```

#### 主启动类

```java
@SpringBootApplication
@EnableDiscoveryClient
public class SentinelService8401 {
    public static void main(String[] args) {
        SpringApplication.run(SentinelService8401.class,args);
    }
}
```

#### controller

```java
@RestController
public class FlowLimitController {

    @GetMapping("/test1")
    public String test1(){
        return "test1";
    }

    @GetMapping("test2")
    public String test2(){
        return "test2";
    }
}
```



## @SentinelResource

![](https://taro-note-pic.oss-cn-hangzhou.aliyuncs.com/image-20221006163425237.png)



### 按资源名限流

1. 在方法上配置 @SentinelResource 注解 value 值为资源名，blockHandler 为阻塞降级的备用方法，只关 senrinel 配置的规则下的阻塞，不管运行异常，方法需要有形参 阻塞异常

2. fallback 来处理运行时异常的方法降级

   ```java
   @GetMapping("/test3")
       @SentinelResource(value = "test3", blockHandler = "dealTest3", fallback = "handlerFallback")
      public String test3(){
           log.info("测试降级规则，异常比例"  );
           int err = 1 / 0;
           return "test3";
       }
       //阻塞控制方法
       public String dealTest3(BlockException exception){
           return "test3 blockHandler method";
       }
   	//服务降级 fallback
   	public String handlerFallback(Throwable e){
           return "test3 fallback method";
       }
   ```

3. 在各类规则中的资源栏中填写 @SentinelResource 注解 value 值资源名，这样才能调用自定义的方法

   ![image-20221006165632490](https://taro-note-pic.oss-cn-hangzhou.aliyuncs.com/image-20221006165632490.png)

   ![image-20221006165645070](https://taro-note-pic.oss-cn-hangzhou.aliyuncs.com/image-20221006165645070.png)



### 按 URL 限流

普通配置，走默认限流备用方法，不会使用自定义方法



### 解耦

全局的阻塞控制类

```java
public class CustomerBlockHandler {

    public static CommonResult handlerException1(BlockException exception) {
        return new CommonResult(444, "global block exception handler1");
    }

    public static CommonResult handlerException2(BlockException exception) {
        return new CommonResult(444, "global block exception handler2");
    }
}
```

controller

```java
    @GetMapping("/limit/customerBlockHandler")
    @SentinelResource(value = "customerBlockHandler", 
            blockHandlerClass = CustomerBlockHandler.class, 
            blockHandler = "handlerException1")
    public CommonResult customerBlockHandler(){
        return new CommonResult(200, "success", new Payment(2022L, "serial106"));
    }
```





## 流控规则

### 流控模式

![image-20221006141326054](https://taro-note-pic.oss-cn-hangzhou.aliyuncs.com/image-20221006141326054.png)

或者

![image-20221006141504869](https://taro-note-pic.oss-cn-hangzhou.aliyuncs.com/image-20221006141504869.png)

![image-20221006141813675](https://taro-note-pic.oss-cn-hangzhou.aliyuncs.com/image-20221006141813675.png)

超过设置每秒请求阈值，被 sentinel 阻塞

![image-20221006142109550](https://taro-note-pic.oss-cn-hangzhou.aliyuncs.com/image-20221006142109550.png)

[logs (sentinelguard.io)](https://sentinelguard.io/zh-cn/docs/logs.html)

- 直接：只与自己相关
- 关联：A 关联 B，B 不行了，A 也停止
- 链路：多个请求调用了同一个微服务，入口资源阻塞，全部阻塞





### 流控效果

#### 快速失败：

​	到达阈值，该秒拒绝所有访问请求

#### 关联模式 + 冷启动

![image-20221006152213769](https://taro-note-pic.oss-cn-hangzhou.aliyuncs.com/image-20221006152213769.png)

默认认的 coldFactor = 3 即在设置的预热时间 5 秒内阈值为 threshold / 3 ，经过预热时长逐渐升至设定的 QPS 阈值

![image-20221006153118994](https://taro-note-pic.oss-cn-hangzhou.aliyuncs.com/image-20221006153118994.png)

![image-20221006153129313](https://taro-note-pic.oss-cn-hangzhou.aliyuncs.com/image-20221006153129313.png)

#### 排队等待

匀速排队，阈值必须设置为QPS

![image-20221006153504252](https://taro-note-pic.oss-cn-hangzhou.aliyuncs.com/image-20221006153504252.png)

---



## 降级规则

![image-20221006155237880](https://taro-note-pic.oss-cn-hangzhou.aliyuncs.com/image-20221006155237880.png)

- RT

  ![image-20221006155313870](https://taro-note-pic.oss-cn-hangzhou.aliyuncs.com/image-20221006155313870.png)

- 异常比例

  ![image-20221006155347872](https://taro-note-pic.oss-cn-hangzhou.aliyuncs.com/image-20221006155347872.png)
  
- 异常数
  
  ![image-20221006155434142](https://taro-note-pic.oss-cn-hangzhou.aliyuncs.com/image-20221006155434142.png)
  
  
  
  
  

## ==热点规则==

https://github.com/alibaba/Sentinel/wiki/热点参数限流

配置方法

```java
    @GetMapping("/testHotKey")
    @SentinelResource(value = "testHotKey", blockHandler = "dealTestHotKey")
    public String testHotKey(@RequestParam(value = "p1", required = false) String p1,
                             @RequestParam(value = "p2", required = false) String p2){
        return "test hot key";
    }

    public String dealTestHotKey(String p1, String p2, BlockException exception){
        return "fallback method";
    }
```



### 普通配置

配置热点规则

![image-20221006171756704](https://taro-note-pic.oss-cn-hangzhou.aliyuncs.com/image-20221006171756704.png)

error：http://localhost:8401/testHotKey?p1=1

error：http://localhost:8401/testHotKey?p1=1&p2=1

right：http://localhost:8401/testHotKey?p2=1



### 参数例外项

例如：我们期望p1参数当它是某个特殊值时，它的限流值和平时不一样，配置当p1的值等于5时，它的阈值可以达到200

![image-20221006172213236](https://taro-note-pic.oss-cn-hangzhou.aliyuncs.com/image-20221006172213236.png)

right：http://localhost:8401/testHotKey?p1=5



## 系统规则

![image-20221006173707729](https://taro-note-pic.oss-cn-hangzhou.aliyuncs.com/image-20221006173707729.png)



## 服务熔断

sentinel整合ribbon+openFeign+fallback

和之前一样，是本地的负载均衡

---



### Ribbon 

**consumer**

在配置类中注册 RestTemplate 加上负载均衡注解

```java
@Configuration
public class ApplicationContextConfig {

    @Bean
    @LoadBalanced
    public RestTemplate getRestTemplate(){
        return new RestTemplate();
    }
}
```

controller

```java
@RestController
@RequestMapping("/consumer")
@Slf4j
public class CircleBreakerController {

    public static final String SERVICE_URL = "http://nacos-payment-provider";

    @Resource
    private RestTemplate restTemplate;

    @RequestMapping("/consumer/fallback/{id}")
    //@SentinelResource(value = "fallback") //没有配置
    //@SentinelResource(value = "fallback",fallback = "handlerFallback") //fallback只负责业务异常
    //@SentinelResource(value = "fallback",blockHandler = "blockHandler") //blockHandler只负责sentinel控制台配置违规
    //exceptionsToIgnore 属性忽略某个异常
    @SentinelResource(value = "fallback",fallback = "handlerFallback",blockHandler = "blockHandler",
            exceptionsToIgnore = {IllegalArgumentException.class})
    public CommonResult<Payment> fallback(@PathVariable Long id) {
        CommonResult<Payment> result = restTemplate.getForObject(SERVICE_URL + "/payment/"+id, CommonResult.class,id);

        if (id == 4) {
            throw new IllegalArgumentException ("IllegalArgumentException,非法参数异常....");
        }else if (result.getData() == null) {
            throw new NullPointerException ("NullPointerException,该ID没有对应记录,空指针异常");
        }

        return result;
    }

    //fallback
    public CommonResult handlerFallback(@PathVariable Long id,Throwable e) {
        Payment payment = new Payment(id,"null");
        return new CommonResult<>(444,"兜底异常handlerFallback,exception内容  "+e.getMessage(),payment);
    }

    //blockHandler
    public CommonResult blockHandler(@PathVariable Long id, BlockException blockException) {
        Payment payment = new Payment(id,"null");
        return new CommonResult<>(445,"blockHandler-sentinel限流,无此流水: blockException  "+blockException.getMessage(),payment);
    }

}
```





### OpenFeign

pom

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-openfeign</artifactId>
</dependency>
```

yml 添加激活sentinel 对 feign 的支持配置信息

```yml
feign:
  sentinel:
    enabled: true
```

主启动类添加 @EnableFeignClients

service 添加 FeignClient 注解

```java
@FeignClient(value = "nacos-payment-provider", fallback = PaymentFallbackService.class)
public interface PaymentService {
    //openFeign 来完成 RestTempalte 的转发请求
    @GetMapping("/payment/{id}")
    public CommonResult<Payment> paymentQuery(@PathVariable("id") Long id);
}
```

实现类，编写降级方法

```java
@Service
public class PaymentFallbackService implements PaymentService {
    @Override
    public CommonResult<Payment> paymentQuery(Long id) {
        return new CommonResult<>(44444,"PaymentFallbackService",new Payment(id,"errorSerial"));
    }
}
```

controller

```java
	@Resource
    private PaymentService paymentService;

    @GetMapping("/payment/{id}")
	//openFeign 来处理服务降级，同时配置 sentinel 的阻塞方法
    @SentinelResource(value = "blockHandler", blockHandler = "blockHandler")
    public CommonResult<Payment> paymentOpenFeign(@PathVariable("id") Long id) {
        return paymentService.paymentQuery(id);
    }

	 //blockHandler
    public CommonResult blockHandler(@PathVariable  Long id, BlockException blockException) {
        Payment payment = new Payment(id,"null");
        return new CommonResult<>(445,"blockHandler-sentinel限流,无此流水: blockException  "+blockException.getMessage(),payment);
    }
```

---



## 规则持久化

将限流配置规则持久化进Nacos保存，只要刷新8401某个rest地址，sentinel控制台的流控规则就能看到，只要Nacos里面的配置不删除，针对8401上Sentinel上的流控规则持续有效

在需要持久化规则的微服务中添加 pom 依赖

```xml
<dependency>
    <groupId>com.alibaba.csp</groupId>
    <artifactId>sentinel-datasource-nacos</artifactId>
</dependency>
```

yml

```yml
server:
  port: 8401

spring:
  application:
    name: cloudalibaba-sentinel-service
  cloud:
    nacos:
      discovery:
        server-addr: 121.199.78.94:8848
    sentinel:
      transport:
        dashboard: localhost:8080
        port: 8719
      datasource: #配置sentinel 规则持久化的数据源
        ds1:
          nacos:
            server-addr:  121.199.78.94:8848
            dataId: cloudalibaba-sentinel-service
            data-type: json
            rule-type: flow

management:
  endpoints:
    web:
      exposure:
        include: '*'
  endpoint:
    sentinel:
      enabled: true
```

在 nacos 中添加配置

![image-20221007152349243](https://taro-note-pic.oss-cn-hangzhou.aliyuncs.com/image-20221007152349243.png)

```json
[
    {
         "resource": "/retaLimit/byUrl",
         "limitApp": "default",
         "grade":   1,
         "count":   1,
         "strategy": 0,
         "controlBehavior": 0,
         "clusterMode": false    
    }
]
```

![image-20221007152421987](https://taro-note-pic.oss-cn-hangzhou.aliyuncs.com/image-20221007152421987.png)

---



# Seata

分布式事务处理

http://seata.io/zh-cn/































































