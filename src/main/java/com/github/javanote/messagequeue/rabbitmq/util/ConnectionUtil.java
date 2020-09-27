package com.github.javanote.messagequeue.rabbitmq.util;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeoutException;

/**
 * @author: starc
 * @date: 2019/12/30
 */
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