# 前提条件
本教程假定RabbitMQ已在标准端口（5672）的localhost上安装并运行。

如果您使用其他主机，端口或证书，则连接设置需要进行调整。

# 在哪里获得帮助
如果您在阅读本教程时遇到困难，可以通过[邮件列表][1]或[RabbitMQ社区Slack][2]与我们联系。

# 发布/订阅
（使用Java客户端）

在上一个教程中，我们创建了一个工作队列。工作队列背后的假设是，每个任务都恰好交付给一个工人。在这一部分中，我们将做一些完全不同的事情-我们将消息传达给多个消费者。这种模式称为“发布/订阅”。

为了说明这种模式，我们将构建一个简单的日志记录系统。它包含两个程序-第一个程序将发出日志消息，第二个程序将接收并打印它们。

在我们的日志系统中，接收程序的每个运行副本都将获取消息。这样，我们将能够运行一个接收程序并将日志定向到磁盘。同时我们将能够运行其他接收器并在屏幕上查看日志。

本质上，已发布的日志消息将被广播到所有接收者。

## 交换器
在本教程的前面部分中，我们向队列发送消息和从队列接收消息。现在是时候介绍Rabbit中完整的消息传递模型了。

让我们快速回顾一下先前教程中介绍的内容：

- 生产者是发送消息的应用程序。
- 队列是用于存储消息的缓冲区。
- 消费者是接收消息的应用程序。

RabbitMQ消息传递模型中的核心思想是生产者从不将任何消息直接发送到队列。实际上，生产者甚至根本不知道消息会被传递到哪个队列。

其实生产者只能将消息发送到交换机。交换是一件非常简单的事情。一方面，它接收来自生产者的消息，另一方面，将它们推入队列。交换必须确切知道如何处理收到的消息。是将其添加到特定队列？还是将其添加到多个队列中？还是应该丢弃它。交换规则由交换类型定义 。


有几种交换类型可用：`direct`，`topic`，`headers` 和`fanout`。我们将集中讨论最后一个——fanout。下面我们创建一个fanout类型的交换器，并将其称为`logs`：

channel.exchangeDeclare("logs", "fanout");

fanout交换器非常简单。正如您可能从名称中猜测的那样，它只是将接收到的所有消息广播到它知道的所有队列中。而这正是我们的日志记录器所需要的。

#### 交换器列表
    
要列出服务器上的交换器，您可以运行`rabbitmqctl`：
```
sudo rabbitmqctl list_exchanges
```

在此列表中，将有一些`amq.*`的交换器和默认的（未命名的）交换器。这些是默认创建的，但是目前您不太可能需要使用它们。
    
    
#### 默认交换器

在本教程的前面部分中，我们对交换器一无所知，但仍然能够将消息发送到队列。这是可能的，因为我们使用的是默认交换器，我们通过空字符串（""）进行标识。

回想一下我们之前如何发布消息：

```java
channel.basicPublish("", "hello", null, message.getBytes());
```

第一个参数是交换器的名称。空字符串表示默认或无名称的交换器：消息将以routingKey指定的名称路由到队列（如果存在）。


现在，我们可以发布我们命名的交换器作为替代：
```java
channel.basicPublish( "logs", "", null, message.getBytes());
```

临时队列

您可能还记得，我们使用的是具有特定名称的队列（还记得`hello`和`task_queue`吗？）。能够命名队列对我们来说至关重要-我们需要将工人指向同一队列。当您想在生产者和消费者之间共享队列时，给队列起的名字就很重要。

但这不是我们的记录器的情况。我们希望听到所有日志消息，而不仅仅是它们的一部分。我们也只对当前正在发送的消息感兴趣，而对旧消息不感兴趣。为了解决这个问题，我们需要两件事。

首先，无论何时连接到Rabbit，我们都需要一个全新的空队列。为此，我们可以创建一个具有随机名称的队列，或者更好的——直接让服务器为我们选择一个随机队列名称。

其次，一旦我们断开了使用者的连接，队列将被自动删除。

在Java客户端中，当我们不向`queueDeclare()`提供任何参数时，我们将使用随机生成的名称创建一个非持久的，排他的，自动删除的队列：
```java
String queueName = channel.queueDeclare().getQueue();
```

您可以在[队列指南](https://www.rabbitmq.com/queues.html)中了解有关`exclusive`标志和其他队列属性。

此时，`queueName`包含一个随机队列名称。例如，它可能看起来像`amq.gen-JzTY20BRgKO-HjmUJj0wLg`。

#### 绑定（Bindings）

![](https://www.rabbitmq.com/img/tutorials/bindings.png)

我们已经创建了一个`fanout`交换器和队列。现在我们需要告诉交换器将消息发送到我们的队列。建立交换器和队列之间的关系称为绑定。
```java
channel.queueBind(queueName, "logs", "");
```
从现在开始，`logs`交换器将把消息添加到我们的队列中。

##### 查看绑定列表
您可以列出已使用的绑定
```
rabbitmqctl list_bindings
```

#### 放在一起
```java
public class EmitLog {

  private static final String EXCHANGE_NAME = "logs";

  public static void main(String[] argv) throws Exception {
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost("localhost");
    try (Connection connection = factory.newConnection();
         Channel channel = connection.createChannel()) {
        channel.exchangeDeclare(EXCHANGE_NAME, "fanout");

        String message = argv.length < 1 ? "info: Hello World!" :
                            String.join(" ", argv);

        channel.basicPublish(EXCHANGE_NAME, "", null, message.getBytes("UTF-8"));
        System.out.println(" [x] Sent '" + message + "'");
    }
  }
}
```
如您所见，在建立连接后，我们声明了交换器。由于禁止发布到不存在的交换器，因此此步骤是必需的。

如果没有队列绑定到交换机，则消息将丢失，但这对我们来说是可以接受的。如果没有消费者在收听，我们可以安全地丢弃该消息。

`ReceiveLogs.java`的代码：
```java
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

public class ReceiveLogs {
  private static final String EXCHANGE_NAME = "logs";

  public static void main(String[] argv) throws Exception {
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost("localhost");
    Connection connection = factory.newConnection();
    Channel channel = connection.createChannel();

    channel.exchangeDeclare(EXCHANGE_NAME, "fanout");
    String queueName = channel.queueDeclare().getQueue();
    channel.queueBind(queueName, EXCHANGE_NAME, "");

    System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

    DeliverCallback deliverCallback = (consumerTag, delivery) -> {
        String message = new String(delivery.getBody(), "UTF-8");
        System.out.println(" [x] Received '" + message + "'");
    };
    channel.basicConsume(queueName, true, deliverCallback, consumerTag -> { });
  }
}
```

像以前一样编译即可。
```
javac -cp $CP EmitLog.java ReceiveLogs.java
```
如果要将日志保存到文件，只需打开控制台并键入：
```
java -cp $ CP ReceiveLogs> logs_from_rabbit.log
```
如果希望在屏幕上查看日志，请生成一个新的终端并运行：
```
java -cp $ CP ReceiveLogs
```
当然，要发出日志，请输入：
```
java -cp $ CP EmitLog
```
使用`rabbitmqctl list_bindings`，您可以验证代码是否按照我们所想的创建了`bingdings`和`queue`。 运行两个ReceiveLogs.java程序后，您应该会看到类似以下内容的内容：
```
sudo rabbitmqctl list_bindings
# => Listing bindings ...
# => logs    exchange        amq.gen-JzTY20BRgKO-HjmUJj0wLg  queue           []
# => logs    exchange        amq.gen-vso0PVvyiRIL2WoV3i48Yg  queue           []
# => ...done.
```
结果的解释很简单：交换器日志中的数据进入到了服务器分配名称的两个队列。这正是我们想要的。

要了解如何侦听消息的子集，让我们继续进行教程4