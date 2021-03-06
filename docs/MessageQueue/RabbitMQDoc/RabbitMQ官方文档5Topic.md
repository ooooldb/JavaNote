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

# Topics
（使用Java客户端）

在上一个教程中，我们改进了日志系统。替换了仅能进行全体广播的`fanout`交换器，我们使用`direct`交换机，并可以有选择地接收日志。

尽管使用`direct`交换器对我们的系统进行了改进，但它仍然存在局限性——它无法基于多个条件进行路由。

在我们的日志记录系统中，我们可能不仅要根据日志等级订阅日志，还要根据日志是谁发出的来判断。这个概念来自`syslog`unix tool，该工具根据`severity`（info/warn/crit...）和`facility`（auth / cron / kern ...）路由日志。

这将给我们带来很大的灵活性-我们可能只想听听来自'cron'的严重错误，也可以听听'kern'的所有日志。

为了在日志系统中实现这一点，我们需要学习更复杂的`Topic`交换器。

## Topic交换器
发送到`Topic`交换器的消息不能具有任意的`routing_key`——它必须是单词列表，以点分隔。这些词可以是任何东西，但通常它们指定与消息相关的某些功能。一些有效的路由关键示例：“ stock.usd.nyse ”，“ nyse.vmw ”，“ quick.orange.rabbit ”。路由关键字中可以包含任意多个单词，最多255个字节。

绑定密钥也必须采用相同的形式。主题交换背后的逻辑 类似于直接交换-用特定路由键发送的消息将传递到所有用匹配绑定键绑定的队列。但是，绑定键有两个重要的特殊情况：

- `*`（星号）可以代替一个单词。
- `#`（哈希）可以替代零个或多个单词。

最简单的解释方法就是举个例子说明：

![](https://www.rabbitmq.com/img/tutorials/python-five.png)

在此示例中，我们将发送的消息全部都是描述动物的。将使用包含三个词（两个点）的路由键发送消息。路由键中的第一个单词将描述速度，第二个是颜色，第三个是物种：“<speed>.<color>.<species>”。

我们创建了三个绑定：Q1与绑定键“\*.orange.\*”绑定，Q2与“\*.\*.rabbit”、“lazy.#”绑定。

这些绑定可以总结为：

- Q1对所有橙色动物都感兴趣。
- Q2想听有关兔子的一切，以及有关懒惰动物的一切。

路由键设置为“ quick.orange.rabbit ”的消息将传递到两个队列。消息“ lazy.orange.elephant ”也将发送给他们两个。另一方面，“quick.orange.fox ”只会进入第一个队列，而“ lazy.brown.fox ”只会进入第二个队列。“ lazy.pink.rabbit ”只会被传递到Q2一次，即使有两个绑定键匹配。“ quick.brown.fox ”与任何绑定都不匹配，因此将被丢弃。

如果我们违反合同并发送一个或四个单词的消息，例如“orange”或“ quick.orange.male.rabbit ”，会发生什么？好吧，这些消息将不匹配任何绑定键，并且将会被丢失。

另一方面，“ lazy.orange.male.rabbit ”即使有四个单词，也将匹配最后一个绑定，并将其传送到第二个队列。

## Topic 交换器

Topic 交换器功能强大，而且可以模拟其他交换器的行为。

当队列用“#”（哈希）绑定键绑定时，它将接收所有消息，而与路由键无关，就像在`fanout`交换器中一样。

当在绑定中不使用特殊字符“*”（星号）和“#”（哈希）时，行为就像`direct`交换器。

## 全部放在一起

我们将在日志记录系统中使用`topic`交换器。我们将从一个可行的假设开始，即日志的路由键将包含两个词：“<facility>.<severity>”。

该代码与上一教程中的代码几乎相同 。
```java
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public class EmitLogTopic {

  private static final String EXCHANGE_NAME = "topic_logs";

  public static void main(String[] argv) throws Exception {
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost("localhost");
    try (Connection connection = factory.newConnection();
         Channel channel = connection.createChannel()) {

        channel.exchangeDeclare(EXCHANGE_NAME, "topic");

        String routingKey = getRouting(argv);
        String message = getMessage(argv);

        channel.basicPublish(EXCHANGE_NAME, routingKey, null, message.getBytes("UTF-8"));
        System.out.println(" [x] Sent '" + routingKey + "':'" + message + "'");
    }
  }
  //..
}
```
ReceiveLogsTopic.java的代码：
```java
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

public class ReceiveLogsTopic {

  private static final String EXCHANGE_NAME = "topic_logs";

  public static void main(String[] argv) throws Exception {
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost("localhost");
    Connection connection = factory.newConnection();
    Channel channel = connection.createChannel();

    channel.exchangeDeclare(EXCHANGE_NAME, "topic");
    String queueName = channel.queueDeclare().getQueue();

    if (argv.length < 1) {
        System.err.println("Usage: ReceiveLogsTopic [binding_key]...");
        System.exit(1);
    }

    for (String bindingKey : argv) {
        channel.queueBind(queueName, EXCHANGE_NAME, bindingKey);
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
编译并运行示例，包括如教程1中所述的类路径-在Windows上，请使用%CP%。

编译：
```
javac -cp $CP ReceiveLogsTopic.java EmitLogTopic.java
```
接收所有日志：
```
java -cp $CP ReceiveLogsTopic "#"
```
接收“kern”的所有日志：
```
java -cp $CP ReceiveLogsTopic "kern.*"
```
接收“critical”的所有日志：
```
java -cp $CP ReceiveLogsTopic "*.critical"
```
您可以创建多个绑定：
```
java -cp $CP ReceiveLogsTopic "kern.*" "*.critical"
```
发出带有路由键“ kern.critical ”类型的日志：
```
java -cp $CP EmitLogTopic "kern.critical" "A critical kernel error"
```
请注意，该代码对路由键或绑定键没有任何假设，您可能需要使用两个以上的路由键参数。

接下来，在教程6中找出如何作为远程过程调用来回程消息

[1]: https://groups.google.com/forum/#!forum/rabbitmq-users        ""
[2]: https://rabbitmq-slack.herokuapp.com/  ""