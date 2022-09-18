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

2. pom

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

3. yml

   ```yml
   server:
     port: 8006
   spring:
     application:
       name: consul-provider-payment
   ##consul 注册中心
     cloud:
       consul:
         host: localhost
         port: 8500
         discovery:
           service-name: ${spring.application.name}
   ```

4. 主启动类

   ```java
   @SpringBootApplication
   @EnableDiscoveryClient
   public class PaymentMain8006 {
   
       public static void main(String[] args) {
           SpringApplication.run(PaymentMain8006.class, args);
       }
   }
   ```

5. controller

   ```java
   @RestController
   @Slf4j
   @RequestMapping("/payment")
   public class PaymentController {
   
       @Value("${server.port}")
       private String serverPort;
   
       @RequestMapping("/consul")
       public String paymentConsul(){
           return "springcloud with consul: " + serverPort + "\t" + UUID.randomUUID();
       }
   }
   ```



# CAP理论

C:Consistency(强一致性)

A:Availability(可用性)

P:Partition tolerance(分区容错)

CAP理论关注粒度是数据，而不是整体系统设计的策略

![image-20220918204441691](https://taro-note-pic.oss-cn-hangzhou.aliyuncs.com/image-20220918204441691.png)

![image-20220918205023798](https://taro-note-pic.oss-cn-hangzhou.aliyuncs.com/image-20220918205023798.png)



# Ribbon 负载均衡服务调用

​	是一套**客户端 负载均衡的工具**，ribbon本地负载均衡，在调用微服务接口时候，会在注册中心上获取注册信息服务列表之后缓存到 JVM 本地，从而在本地实现 RPC 远程服务调用技术。

​	Ribbon : 负载均衡 + RestTemplete 调用

​	Eureka 的客户端组件已经引入了 Ribbon 组件

p37























































