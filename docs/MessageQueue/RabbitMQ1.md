# RabbitMQ

## 安装

### Linux


#### Docker

1.获取镜像(Latest)
```
docker pull rabbitmq
docker pull rabbitmq:3-management
```
2.启动镜像
```
docker run -itd --rm --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq
```
3.修改密码

先进入容器

```
docker exec -it rabbitmq bash
```

查看当前用户列表

```
rabbitmqctl  list_users  
```

修改密码

```
rabbitmqctl  change_password  guest  '123456'
```


### 入门实例
1.依赖
```
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-amqp</artifactId>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
        <exclusions>
            <exclusion>
                <groupId>org.junit.vintage</groupId>
                <artifactId>junit-vintage-engine</artifactId>
            </exclusion>
        </exclusions>
    </dependency>
    <dependency>
        <groupId>org.springframework.amqp</groupId>
        <artifactId>spring-rabbit-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```
2.生产者
```
package com.ooooldb.demo.rabbitmq.main;

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
            connection = getConnection();
            channel = connection.createChannel();
            channel.queueDeclare(QUEUE, false, false, false, null);
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

    public static Connection getConnection() throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("101.37.76.240");
        factory.setPort(5672);
        factory.setUsername("guest");
        factory.setPassword("Qaz_mlp");
        return factory.newConnection();
    }
}
```
3.消费者
```
package com.ooooldb.demo.rabbitmq.main;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

public class ConsumerDemo {
    private static final String QUEUE = "helloworld";

    public static void main(String[] args) {
        Connection connection = null;
        Channel channel = null;
        try {
            // 1.创建连接和通道
            connection = ProducerDemo.getConnection();
            channel = connection.createChannel();

            // 2.为通道声明队列
            channel.queueDeclare(QUEUE, false, false, false, null);
            System.out.println(" **** keep alive ,waiting for messages, and then deal them");
            // 3.通过回调生成消费者
            Consumer consumer = new DefaultConsumer(channel) {
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope,
                                           com.rabbitmq.client.AMQP.BasicProperties properties, byte[] body) throws IOException {

                    //获取消息内容然后处理
                    String msg = new String(body, "UTF-8");
                    System.out.println("*********** HelloConsumer" + " get message :[" + msg + "]");
                }
            };

            //4.消费消息
            channel.basicConsume(QUEUE, true, consumer);

        } catch (IOException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
    }
}
```
