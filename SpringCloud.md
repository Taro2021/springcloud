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

主启动类上激活 `@EnableCircuitBreaker`



### Consumer 的降级

yml 配置

```yml
feign:
  hystrix:
    enabled: true
```

！！！! 启用一个组件的功能不要忘了给主启动类加上 EnableXXX z 注解

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



## RabbitMQ  配置

1. 下载 erlang http://erlang.org/download/otp_win64_21.3.exe
2. 





































































