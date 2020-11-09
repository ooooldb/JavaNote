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

# 工作队列
*（使用Java客户端）*

![](https://www.rabbitmq.com/img/tutorials/python-two.png)

在[第一个教程][3]中，我们编写了程序来通过已命名队列发送和接收消息。在这一节中，我们将创建一个工作队列，该队列将用于在多个“工作人员”之间分配耗时的任务。

工作队列（又名：任务队列）的主要思想是避免立即执行资源密集型任务（需要等待它直至完成）。相反我们安排任务在之后完成。我们将任务封装为消息并将其发送到队列。在后台运行的工作进程将弹出任务并最终执行作业。当您运行许多工作人员时，任务将在他们之间共享（RabbitMQ会轮询这些消费者）。

这个概念在Web应用程序中特别有用，因为在Web应用程序中，不可能在较短的HTTP请求窗口内处理复杂的任务。

## 准备
在本教程的上半部分，我们发送了一条包含“ Hello World！”的消息。现在，我们将发送代表复杂任务的字符串。我们没有使用一个现实的任务，例如要调整大小的图像或要渲染的pdf文件，所以我们假装自己很忙-使用Thread.sleep()函数来伪造它。我们将字符串中包含`.`的个数作为它的复杂度。每个点将占“工作”的一秒钟。例如，Hello ...描述的虚假任务 将花费三秒钟。

我们将略微修改上一个示例中的Send.java代码，以允许从命令行发送任意消息。该程序会将任务调度到我们的工作队列中，因此将其命名为 NewTask.java：
```java
String message = String.join(" ", argv);

channel.basicPublish("", "hello", null, message.getBytes());
System.out.println(" [x] Sent '" + message + "'");
```

我们旧的Recv.java程序还需要进行一些更改：它需要为消息正文中的每个点假定一秒钟的工作。它将处理传递的消息并执行任务，因此我们将其称为Worker.java：
```java
DeliverCallback deliverCallback = (consumerTag, delivery) -> {
  String message = new String(delivery.getBody(), "UTF-8");

  System.out.println(" [x] Received '" + message + "'");
  try {
    doWork(message);
  } finally {
    System.out.println(" [x] Done");
  }
};
boolean autoAck = true; // acknowledgment is covered below
channel.basicConsume(TASK_QUEUE_NAME, autoAck, deliverCallback, consumerTag -> { });
```

我们模拟执行时间的假任务：
```java
private static void doWork(String task) throws InterruptedException {
    for (char ch: task.toCharArray()) {
        if (ch == '.') Thread.sleep(1000);
    }
}
```

如教程1中那样编译它们（在工作目录中使用jar文件，环境变量CP）：
```
javac -cp $ CP NewTask.java Worker.java
```

## 轮训分发
使用任务队列的优点之一是能够轻松并行化工作。如果我们正在积压工作，我们可以增加更多的工人，这样就可以轻松扩展。

首先，让我们尝试同时运行两个辅助实例。他们都将从队列中获取消息，但是究竟如何呢？让我们来看看。

您需要打开三个控制台。两个将运行Worker程序。这些控制台将成为我们的两个使用者-C1和C2。

```shell
# shell 1
java -cp $CP Worker
# => [*] Waiting for messages. To exit press CTRL+C
```
```
# shell 2
java -cp $CP Worker
# => [*] Waiting for messages. To exit press CTRL+C
```
在第三步中，我们将发布新任务。启动消费者之后，您可以发布一些消息：
```
# shell 3
java -cp $CP NewTask First message.
# => [x] Sent 'First message.'
java -cp $CP NewTask Second message..
# => [x] Sent 'Second message..'
java -cp $CP NewTask Third message...
# => [x] Sent 'Third message...'
java -cp $CP NewTask Fourth message....
# => [x] Sent 'Fourth message....'
java -cp $CP NewTask Fifth message.....
# => [x] Sent 'Fifth message.....'
```
让我们看看交付给我们工人的东西：
```
java -cp $CP Worker
# => [*] Waiting for messages. To exit press CTRL+C
# => [x] Received 'First message.'
# => [x] Received 'Third message...'
# => [x] Received 'Fifth message.....'
```

```
java -cp $CP Worker
# => [*] Waiting for messages. To exit press CTRL+C
# => [x] Received 'Second message..'
# => [x] Received 'Fourth message....'
```

默认情况下，RabbitMQ将按顺序将每个消息发送给下一个使用者。平均而言，每个消费者都会收到相同数量的消息。这种分发消息的方式称为轮询。尝试一下三个或更多的工人一起工作。

## 消息确认
完成一项任务可能需要几秒钟。您可能想知道，如果其中一个使用者开始一项漫长的任务并仅部分完成而死掉，会发生什么情况。使用我们当前的代码，RabbitMQ一旦向消费者传递了一条消息，便立即将其标记为删除。在这种情况下，如果您杀死一个工人，我们将丢失正在处理的消息。我们还将丢失所有发送给该特定工作人员但尚未处理的消息。

但是我们不想丢失任何任务。如果一个工人死亡，我们希望将任务交付给另一个工人。

为了确保消息永不丢失，RabbitMQ支持`消息确认`。消费者发送回确认，以告知RabbitMQ已经接收，处理了特定消息，并且RabbitMQ可以自由删除它。

如果使用者死了（其通道已关闭，连接已关闭或TCP连接丢失）而没有发送确认，RabbitMQ将了解消息未完全处理，并将重新排队。如果同时有其他消费者在线，它将很快将其重新分发给另一个消费者。这样，您可以确保即使工人偶尔死亡也不会丢失任何消息。

没有任何消息超时；消费者死亡时，RabbitMQ将重新传递消息。即使处理消息需要非常非常长的时间也没关系。

默认情况下，手动消息确认处于打开状态。在前面的示例中，我们通过autoAck = true 标志显式关闭了它们。现在，是时候将该标志设置为false，并在工作完成后从工作人员发送适当的确认。
```java
channel.basicQos（1）; //一次仅接受一条未经确认的消息（参见下文）

DeliverCallback deliverCallback = (consumerTag, delivery) -> {
  String message = new String(delivery.getBody(), "UTF-8");

  System.out.println(" [x] Received '" + message + "'");
  try {
    doWork(message);
  } finally {
    System.out.println(" [x] Done");
    channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
  }
};
boolean autoAck = false;
channel.basicConsume(TASK_QUEUE_NAME, autoAck, deliverCallback, consumerTag -> { });
```

使用此代码，我们可以确保，即使您在处理消息时使用CTRL + C杀死Worker，也不会丢失任何信息。Worker死亡后不久，所有未确认的消息将重新发送。

确认的Ack必须在收到消息的同一通道上发送。尝试使用其他通道进行确认将导致通道级协议异常。请参阅有关确认的文档指南 以了解更多信息。

    被遗忘的确认
    
    错过basicAck是一个常见的错误。这是一个简单的错误，但是后果很严重。当您的客户端退出时，消息将被重新发送（看起来像是随机重新发送），但是RabbitMQ将消耗越来越多的内存，因为它将无法释放任何未确认的消息。

    为了调试这种错误，您可以使用rabbitmqctl 打印messages_unacknowledged字段：

    sudo rabbitmqctl list_queues名称messages_ready messages_unacknowledged
    
    在Windows上，删除sudo：

    rabbitmqctl.bat list_queues名称messages_ready messages_unacknowledged
    
## 消息持久化

我们已经学会了如何确保即使消费者死亡，任务也不会丢失。但是，如果RabbitMQ服务器停止，我们的任务仍然会丢失。

RabbitMQ退出或崩溃时，除非您告诉它，否则它将丢失队列和消息。确保消息不会丢失需要做两件事：我们需要将队列和消息都标记为持久。

首先，我们需要确保该队列将在RabbitMQ节点重启后继续存在。为此，我们需要将其声明为持久的：
```java
boolean durable = true;
channel.queueDeclare("hello", durable, false, false, null);
```

尽管此命令本身是正确的，但在我们当前的设置中将无法使用。那是因为我们已经定义了一个叫hello的队列 ，它并不持久。RabbitMQ不允许您使用不同的参数重新定义现有队列，并且将向尝试执行此操作的任何程序返回错误。但是有一个快速的解决方法-让我们声明一个名称不同的队列，例如task_queue：

```java
boolean durable = true;
channel.queueDeclare("task_queue", durable, false, false, null);
```

该队列定义上的更改需要同时应用于生产者代码和使用者代码。

在这一点上，我们确定即使RabbitMQ重新启动，`task_queue`队列也不会丢失。现在，我们需要将消息标记为持久性-通过将`MessageProperties`（实现`BasicProperties`）设置为值`PERSISTENT_TEXT_PLAIN`。
```java
import com.rabbitmq.client.MessageProperties;

channel.basicPublish("", "task_queue",
            MessageProperties.PERSISTENT_TEXT_PLAIN,
            message.getBytes());
```

    有关消息持久性的说明
    将消息标记为持久性并不能完全保证不会丢失消息。尽管它告诉RabbitMQ将消息保存到磁盘，但是RabbitMQ接受消息但尚未将其保存时，仍有很短的时间。而且，RabbitMQ不会对每条消息都执行fsync（2）－它可能只是保存到缓存中，而没有真正写入磁盘。持久性保证并不强，但是对于我们的简单任务队列而言，这已经绰绰有余了。如果需要更强有力的保证，则可以使用 发布者确认。

### 消息的公平分发
您可能已经注意到，调度仍然无法完全按照我们的要求进行。例如，在有两名工人的情况下，当所有奇数消息都很重（消费者执行操作消耗的资源轻重），偶数消息很轻时，一位工人将一直忙碌而另一位工人几乎不做任何工作。好吧，RabbitMQ对此一无所知，并且仍将平均分派消息。

发生这种情况是因为RabbitMQ在消息进入队列时才调度消息。它不会查看消费者的未确认消息数。它只是盲目地将每个第n条消息发送给第n个使用者。


为了解决这个问题，我们可以将basicQos方法与 prefetchCount = 1设置一起使用。这告诉RabbitMQ一次不要给工人一个以上的消息。换句话说，在处理并确认上一条消息之前，不要将新消息发送给工作人员。而是将其分派给不忙的下一个工作程序。
```java
int prefetchCount = 1 ;
channel.basicQos（prefetchCount）;
```

    关于队列大小的注意
    如果所有工作人员都在忙碌，则RabbitMQ的队列可以被填满。您将需要关注这一点，并增加更多的工作人员，或者使用一些其他策略。

全部放在一起
NewTask.java类的最终代码：
```java
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.MessageProperties;

public class NewTask {

  private static final String TASK_QUEUE_NAME = "task_queue";

  public static void main(String[] argv) throws Exception {
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost("localhost");
    try (Connection connection = factory.newConnection();
         Channel channel = connection.createChannel()) {
        channel.queueDeclare(TASK_QUEUE_NAME, true, false, false, null);

        String message = String.join(" ", argv);

        channel.basicPublish("", TASK_QUEUE_NAME,
                MessageProperties.PERSISTENT_TEXT_PLAIN,
                message.getBytes("UTF-8"));
        System.out.println(" [x] Sent '" + message + "'");
    }
  }

}
```
还有我们的`Worker.java`:
```java
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

public class Worker {

  private static final String TASK_QUEUE_NAME = "task_queue";

  public static void main(String[] argv) throws Exception {
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost("localhost");
    final Connection connection = factory.newConnection();
    final Channel channel = connection.createChannel();

    channel.queueDeclare(TASK_QUEUE_NAME, true, false, false, null);
    System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

    channel.basicQos(1);

    DeliverCallback deliverCallback = (consumerTag, delivery) -> {
        String message = new String(delivery.getBody(), "UTF-8");

        System.out.println(" [x] Received '" + message + "'");
        try {
            doWork(message);
        } finally {
            System.out.println(" [x] Done");
            channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
        }
    };
    channel.basicConsume(TASK_QUEUE_NAME, false, deliverCallback, consumerTag -> { });
  }

  private static void doWork(String task) {
    for (char ch : task.toCharArray()) {
        if (ch == '.') {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException _ignored) {
                Thread.currentThread().interrupt();
            }
        }
    }
  }
}
```

使用消息确认和`prefetchCount`可以设置一个工作队列。持久化选项即使重新启动RabbitMQ也能使任务继续存在。

有关Channel方法和MessageProperty的更多信息，您可以在线浏览[JavaDocs][4]。

现在，我们可以继续进行教程3，并学习如何向许多消费者传达相同的信息。

[1]: https://groups.google.com/forum/#!forum/rabbitmq-users        ""
[2]: https://rabbitmq-slack.herokuapp.com/  ""
[3]: https://www.rabbitmq.com/tutorials/tutorial-one-java.html  ""
[4]: https://rabbitmq.github.io/rabbitmq-java-client/api/current/  ""
