# 系列所有

1. [Hello World](https://github.com/ooooldb/JavaNote1/blob/master/docs/MessageQueue/RabbitMQDoc/RabbitMQ官方文档1HelloWorld.md)
2. [工作队列](https://github.com/ooooldb/JavaNote1/blob/master/docs/MessageQueue/RabbitMQDoc/RabbitMQ官方文档2工作队列.md)
3. [发布订阅](https://github.com/ooooldb/JavaNote1/blob/master/docs/MessageQueue/RabbitMQDoc/RabbitMQ官方文档3发布订阅.md)
4. [路由](https://github.com/ooooldb/JavaNote1/blob/master/docs/MessageQueue/RabbitMQDoc/RabbitMQ官方文档4路由.md)
5. [Topic](https://github.com/ooooldb/JavaNote1/blob/master/docs/MessageQueue/RabbitMQDoc/RabbitMQ官方文档5Topic.md)
6. [RPC](https://github.com/ooooldb/JavaNote1/blob/master/docs/MessageQueue/RabbitMQDoc/RabbitMQ官方文档6RPC.md)
7. [发布者确认](https://github.com/ooooldb/JavaNote1/blob/master/docs/MessageQueue/RabbitMQDoc/RabbitMQ官方文档7发布者确认.md)

# 前提条件
本教程假定RabbitMQ已在标准端口（5672）的localhost上安装并运行。

如果您使用其他主机，端口或证书，则连接设置需要进行调整。

# 在哪里获得帮助
如果您在阅读本教程时遇到困难，可以通过[邮件列表][1]或[RabbitMQ社区Slack][2]与我们联系。

# 路由
（使用Java客户端）
在上一教程中，我们构建了一个简单的日志记录系统。我们能够向许多接收者广播日志消息。

在本教程中，我们将向其中添加功能-可以仅订阅消息的子集。例如，我们将只能将严重错误消息定向到日志文件（以节省磁盘空间），同时仍然能够在控制台上打印所有日志消息。

## Bindings(绑定)
在前面的示例中，我们已经在创建Bindings。您可能会想起以下代码：
```java
channel.queueBind(queueName, EXCHANGE_NAME, "");
```
绑定是交换器和队列之间的关系。可以简单地理解为：队列对来自此交换器的消息感兴趣。

绑定可以采用额外的`routingKey`参数。为了避免与`basic_publish`参数混淆，我们将其称为 `bingding key`。这是我们可以创建带有键绑定的方法：
```java
channel.queueBind（queueName，EXCHANGE_NAME，“ black”）;
```
绑定键的含义取决于交换器类型。我们之前使用的`fanout`类型会忽略绑定键的值。

## Direct 交换器
上一教程中的日志系统将所有消息广播给所有消费者。我们想要扩展它以允许根据日志的警告等级过滤消息。例如，我们可能希望将日志消息写入磁盘的程序仅接收`error`级别的日志，而不会在`warning`或`info`级别日志消息上浪费磁盘空间。

我们使用的是`fanout`类型的交换器，它并没有给我们带来太大的灵活性-它只能进行无意识的广播。

我们将使用`direct`类型的交换器。`direct`交换器背后的路由算法很简单-消息进入其`binding key`与消息的`routing key`完全匹配的队列 。

为了说明这一点，请看以下设置：

![](https://www.rabbitmq.com/img/tutorials/direct-exchange.png)

在此设置中，我们可以看到绑定了两个队列的`direct`交换器X。第一个队列由绑定键`orange`绑定，第二个队列有两个绑定，一个绑定键为`black`，另一个绑定为`green`。

在这样的设置中，路由键为`orange`的消息将被路由到队列Q1。路由键为`black`或`green`的消息被路由到Q2。所有其他消息将被丢弃。

多重绑定

![](https://www.rabbitmq.com/img/tutorials/direct-exchange-multiple.png)

用相同的绑定键绑定多个队列是完全合法的。在我们的示例中，我们可以使用绑定键black在X和Q1之间添加绑定。在这种情况下，直接交换的行为将类似于`fanout`，并将消息广播到所有匹配的队列。带有黑色路由键的消息将同时传递给 Q1和Q2。

## 发送日志
我们将在日志系统中使用此模型。我们将发送消息到`direct`交换器，而不是`fanout`。我们将使用日志严重性作为路由键。这样，接收程序将能够选择它想要接收的严重性。让我们首先关注发送日志。

与往常一样，我们需要首先创建一个交换：
```java
channel.exchangeDeclare(EXCHANGE_NAME, "direct");
```
我们已经准备好发送一条消息：
```java
channel.basicPublish(EXCHANGE_NAME, severity, null, message.getBytes());
```
为简化起见，我们将假定“严重性”可以是“info”，“warning”，“error”之一。

订阅

接收消息与上一教程一样，但有一个例外-我们将为感兴趣的每种严重性创建一个新的绑定。

```java
String queueName = channel.queueDeclare().getQueue();

for(String severity : argv){
  channel.queueBind(queueName, EXCHANGE_NAME, severity);
}
```

## 全部放在一起
![](https://www.rabbitmq.com/img/tutorials/python-four.png)
`EmitLogDirect.java`类的代码：
```java
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public class EmitLogDirect {

  private static final String EXCHANGE_NAME = "direct_logs";

  public static void main(String[] argv) throws Exception {
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost("localhost");
    try (Connection connection = factory.newConnection();
         Channel channel = connection.createChannel()) {
        channel.exchangeDeclare(EXCHANGE_NAME, "direct");

        String severity = getSeverity(argv);
        String message = getMessage(argv);

        channel.basicPublish(EXCHANGE_NAME, severity, null, message.getBytes("UTF-8"));
        System.out.println(" [x] Sent '" + severity + "':'" + message + "'");
    }
  }
  //..
}
```
`ReceiveLogsDirect.java`的代码：
```java
import com.rabbitmq.client.*;

public class ReceiveLogsDirect {

  private static final String EXCHANGE_NAME = "direct_logs";

  public static void main(String[] argv) throws Exception {
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost("localhost");
    Connection connection = factory.newConnection();
    Channel channel = connection.createChannel();

    channel.exchangeDeclare(EXCHANGE_NAME, "direct");
    String queueName = channel.queueDeclare().getQueue();

    if (argv.length < 1) {
        System.err.println("Usage: ReceiveLogsDirect [info] [warning] [error]");
        System.exit(1);
    }

    for (String severity : argv) {
        channel.queueBind(queueName, EXCHANGE_NAME, severity);
    }
    System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

    DeliverCallback deliverCallback = (consumerTag, delivery) -> {
        String message = new String(delivery.getBody(), "UTF-8");
        System.out.println(" [x] Received '" +
            delivery.getEnvelope().getRoutingKey() + "':'" + message + "'");
    };
    channel.basicConsume(queueName, true, deliverCallback, consumerTag -> { });
  }
}
```
照常编译（有关编译和类路径建议，请参见教程一）。为了方便起见，我们在运行示例时将环境变量$CP（在Windows中为％CP％）用于类路径。
```
javac -cp $CP ReceiveLogsDirect.java EmitLogDirect.java
```
如果您只想将“警告”和“错误”（而不是“信息”）日志消息保存到文件中，只需打开控制台并键入：
```
java -cp $CP ReceiveLogsDirect warning error > logs_from_rabbit.log
```
如果您想在屏幕上查看所有日志消息，请打开一个新终端并执行以下操作：
```
java -cp $CP ReceiveLogsDirect info warning error
# => [*] Waiting for logs. To exit press CTRL+C
```
例如，要发出错误日志消息，只需键入：
```
java -cp $CP EmitLogDirect error "Run. Run. Or it will explode."
# => [x] Sent 'error':'Run. Run. Or it will explode.'
```
继续学习第5篇教程，以了解如何根据模式侦听消息。

[1]: https://groups.google.com/forum/#!forum/rabbitmq-users        ""
[2]: https://rabbitmq-slack.herokuapp.com/  ""