# 消息队列

消息（Message）是指在应用间传送的数据。

消息队列（Message Queue）是指利用高效可靠的消息传递机制进行与平台无关的数据交流。

开发中消息队列通常有如下应用场景：

1、任务异步处理

将不需要同步处理且耗时长的操作由消息队列通知消息接受方进行异步处理。提高了应用程序的响应时间。

2、应用程序解耦合

MQ相当于一个中介，生产方通过MQ与消费方交互，它将应用程序进行解耦合

市面上的消息队列

ActiveMQ，RabbitMQ，Kafka，RocketMQ

## RabbitMQ

用Erlang开发，基于AMQP（Advanced Message Queue Protocol高级消息队列协议）实现的消息队列。

### JMS

Java消息服务

### Erlang的安装

RabbitMQ使用需要Erlang，到官网http://www.erlang.org/上下载安装。然后配置Erlang的环境变量

下载RabbitMQ，http://www.rabbitmq.com/

然后打开开始菜单RabbitMQ Command Prompt (sbin dir)，输入

```powershell
rabbitmq-plugins enable rabbitmq_management
```

(无法识别就到安装目录，我的是D:\software\新建文件夹\rabbitmq_server-3.7.15\sbin下打开Power Shell，运行上面的命令）

浏览器登录http://localhost:15672，默认账号密码都是guest

### 连接RabbitMQ

```java
public class ConnectionUtil {

    public static Connection getConnection() throws Exception {
        //1.使用给定参数形式连接RabbitMQ
        //定义连接工厂
        ConnectionFactory factory = new ConnectionFactory();
        //设置服务地址
        factory.setHost("localhost");
        //端口
        factory.setPort(5672);
        //设置账号信息，用户名、密码、vhost
        factory.setVirtualHost("/");
        factory.setUsername("guest");
        factory.setPassword("guest");
        // 通过工程获取连接
        return factory.newConnection();
    }

    public static Connection getConnectionByUrl() throws NoSuchAlgorithmException, KeyManagementException, URISyntaxException, IOException, TimeoutException {
        //2.使用URI形式连接RabbitMQ
        ConnectionFactory factory = new ConnectionFactory();
        factory.setUri("amqp://guest:guest@localhost:5672/testhost");
        return factory.newConnection();
    }
}
```

### 相关概念

RabbitMQ整体上是一个生产者消费者模型，主要负责接收、存储和转发消息。可以把消息传递的过程想象成:当你将一个包裹送到邮局，邮局会暂存并最终将邮件通过邮递员送到收件人的手上，RabbitMQ就好比由邮局、邮箱和邮递员组成的一个系统。从计算机术语层面来说，RabbitMQ 模型更像是一种交换机模型 。

- 生产者和消费者

Producer：生产者，投递消息的一方。
生产者创建消息，发布到RabbitMQ中。消息一般可以包含2个部分：消息体和标签（Label）。消息体也可以称之为payLoad。在实际应用中，消息体一般是一个带有业务逻辑结构的数据，比如一个JSON字符串。消息的标签用来表述这条消息，比如一个交换器的名称和一个路由键。RabbitMQ会根据标签把消息发送给感兴趣的消费者。

Consumer：消费者，接受消息的一方。
消费者连接到RabbitMQ服务器，并订阅到队列上。当消费者消费一条消息时，只是消费消息的消息体。在消息路由的过程中，标签会丢弃。

Broker：消息中间件的服务节点。
一个RabbitMQ Broker可以简单地看作一个RabbitMQ服务节点。大多数情况下也可以将一个RabbitMQ Broker看作一台RabbitMQ服务器。

- 队列

Queue：队列，是RabbitMQ的内部对象，用于存储消息。

RabbitMQ中消息都只能存储在队列中，消费者从队列中获取消息并消费。

多个消费者可以订阅同一个队列，这是队列中的消息会被平均分摊（即轮询）给多个消费者进行处理，而不是每个消费者都收到所有的消息并处理。

- 交换器、路由键、绑定

Exchange：交换器。生产者投递消息的真实情况是，生产者将消息发送到Exchange，由交换器将消息路由到一个或多个队列中。如果路由不到，或许会返回给生产者，或许直接丢弃。
#### ExchangeType
- fanout：他会把所有发送到该交换器的消息路由到所有与该交换器绑定的队列中
- direct：会路由到那些BindingKey和RoutingKey完全匹配的队列中。
- topic：也是将消息路由到BindingKey和RoutingKey相匹配的队列中，可以使用*、#模糊匹配。
- headers：headers类型的交换器不依赖于路由键的匹配规则来路由消息，而是根据发送的消息内容中
          的 headers 属性进行匹配。在绑定队列和交换器时制定一组键值对 ， 当发送消息到交换器时，
          RabbitMQ 会获取到该消息的 headers (也是一个键值对的形式) ，对比其中的键值对是否完全
          匹配队列和交换器绑定时指定的键值对，如果完全匹配则消息会路由到该队列，否则不会路由
          到该队列 。
#### RoutingKey
路由键，用来指定这个消息的路由规则，Routing需要与交换器类型和绑定键（BindingKey）联合使用。

### 入门案例

#### 依赖

```
<dependency>
    <groupId>com.rabbitmq</groupId>
    <artifactId>amqp-client</artifactId>
    <version>5.7.2</version>
</dependency>
<!-- https://mvnrepository.com/artifact/org.springframework.amqp/spring-amqp -->
<dependency>
    <groupId>org.springframework.amqp</groupId>
    <artifactId>spring-amqp</artifactId>
    <version>2.1.7.RELEASE</version>
</dependency>
```

#### 生产者

```java
package com.xia.producer;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * @author: starc
 * @date: 2019/7/3
 */
public class ProducerDemo {
    private static final String QUEUE = "helloworld";

    public static void main(String[] args) throws IOException, TimeoutException {
        Connection connection = null;
        Channel channel = null;
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost("127.0.0.1");
            factory.setPort(5672);
            factory.setUsername("guest");
            factory.setPassword("guest");
            factory.setVirtualHost("/");
            connection = factory.newConnection();
            channel = connection.createChannel();
            channel.queueDeclare(QUEUE, true, false, false, null);
            String message = "我真的好烦啊" + System.currentTimeMillis();
            channel.basicPublish("", QUEUE, null, message.getBytes());
            System.out.println("send message is:'" + message + "'");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (channel != null) {
                channel.close();
            }
            if (connection != null) {
                connection.close();
            }
        }
    }
}
```

#### 消费者

```java
public class ConsumerDemo {
    private static final String QUEUE = "helloworld";

    public static void main(String[] args) throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("127.0.0.1");
        factory.setPort(5672);
        factory.setUsername("guest");
        factory.setPassword("guest");
        factory.setVirtualHost("/");
        Connection connection = null;
        Channel channel = null;
        connection = factory.newConnection();
        channel = connection.createChannel();
        channel.queueDeclare(QUEUE, true, false, false, null);
        DefaultConsumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                String exchange = envelope.getExchange();
                String routingKey = envelope.getRoutingKey();
                long deliveryTag = envelope.getDeliveryTag();
                String msg = new String(body, StandardCharsets.UTF_8);
                System.out.println("receive message.." + msg);
            }
        };
        channel.basicConsume(QUEUE, true, consumer);
    }
}
```

