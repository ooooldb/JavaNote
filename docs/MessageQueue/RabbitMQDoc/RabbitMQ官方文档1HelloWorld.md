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

# 介绍
RabbitMQ是一个消息代理服务器：它接收并转发消息。

可以将其想象成邮局：将要投递的邮件放在邮箱中时，你可以确定邮差小哥最终会将邮件交付给收件人。

以此类推，RabbitMQ是邮箱+邮局+邮递员。

RabbitMQ与邮局之间的主要区别在于：它不处理纸张，而是接收，存储和转发二进制数据。

RabbitMQ以及消息传递一般会使用一些术语：

- 生产仅意味着发送。发送消息的程序是生产者，表示形式如下：

![](https://www.rabbitmq.com/img/tutorials/producer.png)

队列是RabbitMQ内部邮箱的名称。尽管消息流经RabbitMQ和您的应用程序，但它们只能存储在队列中。一个队列仅由主机的内存和磁盘限制约束，它本质上是一个大的消息缓冲器。许多生产者可以将消息发送到一个队列，许多消费者可以尝试从一个队列接收数据。表示形式如下：

![](https://www.rabbitmq.com/img/tutorials/queue.png)

消费与接收具有相似的含义。消费者是一个通常在等待接收消息的程序：

![](https://www.rabbitmq.com/img/tutorials/consumer.png)

请注意，生产者，消费者和消息代理服务器不必位于同一主机上。实际上，在大多数应用程序中它们不是。一个应用程序既可以是生产者，也可以是消费者。

## Hello World

**（使用Java客户端）**
在本教程的这一部分中，我们将用Java编写两个程序。一个发送单个消息的生产者和一个接收消息并打印出来的消费者。我们将介绍Java API中的一些细节，仅着眼于此非常简单的事情。这是消息传递的“Hello World”。

在下图中，“P”是我们的生产者，“ C”是我们的消费者。中间的框是一个队列-一个RabbitMQ代使用者保留的消息缓冲区。

![](https://www.rabbitmq.com/img/tutorials/python-one.png)

    Java客户端库
    
    RabbitMQ使用多种协议。本教程使用AMQP 0-9-1，这是一种开放的通用消息传递协议。RabbitMQ有许多不同语言的客户端。我们将使用RabbitMQ提供的Java客户端。
    下载客户端库 及其依赖项（SLF4J API和 SLF4J Simple）。将这些文件和教程Java文件一起复制到您的工作目录中。
    请注意，对于教程而言，SLF4J Simple足够了，但是您应该在生产中使用成熟的日志记录库，例如Logback。
    （RabbitMQ Java客户端也位于中央Maven存储库中，带有groupId com.rabbitmq和artifactId amqp-client。）

现在我们有了Java客户端及其依赖项，我们可以编写一些代码。

发送消息

![](https://www.rabbitmq.com/img/tutorials/sending.png)

我们称其为消息发布者（发送者）Send和我们的消息消费者（接收者）Recv。发布者将连接到RabbitMQ，发送一条消息，然后退出。

在 Send.java中，我们需要导入一些类：

```java
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;
```

编写类并命名队列：
```java
public class Send {
  private final static String QUEUE_NAME = "hello";
  public static void main(String[] argv) throws Exception {
      //...
  }
}    
```

然后我们可以创建一个到服务器的连接：
```java
ConnectionFactory factory = new ConnectionFactory();
factory.setHost("localhost");
try (Connection connection = factory.newConnection();
     Channel channel = connection.createChannel()) {

}
```
该连接抽象了套接字连接，并为我们处理协议版本协商和认证等。在这里，我们连接到本地计算机上的RabbitMQ节点-因此是 本地主机。如果我们要连接到另一台计算机上的节点，则只需在此处指定其主机名或IP地址即可。

接下来，我们创建一个通道，该通道是完成工作的大多数API所在的位置。注意，我们可以使用try-with-resources语句，因为Connection和Channel都实现java.io.Closeable。这样，我们无需在代码中显式关闭它们。

要发送，我们必须声明要发送到的队列。然后我们可以在try-with-resources语句中将消息发布到队列中：
```java
channel.queueDeclare(QUEUE_NAME, false, false, false, null);
String message = "Hello World!";
channel.basicPublish("", QUEUE_NAME, null, message.getBytes());
System.out.println(" [x] Sent '" + message + "'");
```
声明队列是幂等的-仅当队列不存在时才创建。消息内容是一个字节数组，因此您可以在那里编码任何内容。

[这是整个Send.java类][3]。

    发送不起作用！
    如果这是您第一次使用RabbitMQ，但没有看到“已发送”消息，那么您可能会不知所措，想知道可能是什么问题。代理可能是在没有足够的可用磁盘空间的情况下启动的（默认情况下，它至少需要200 MB的可用空间），因此拒绝接受消息。检查代理日志文件以确认并减少限制（如有必要）。该配置文件文档会告诉你如何设置disk_free_limit。  
##### 接收
这就是我们的发布者。我们的消费者监听来自RabbitMQ的消息，因此与发布单个消息的发布者不同，我们将使消费者保持运行状态以监听消息并打印出来。

![](https://www.rabbitmq.com/img/tutorials/receiving.png)

该代码（在Recv.java中）具有与Send几乎相同的导入：
```java
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
```

我们将使用额外的DeliverCallback接口来缓冲服务器推送给我们的消息。

设置上与发布者相同：我们打开一个连接和一个通道，并声明要消费的队列。请注意，这与发布者发布到RabbitMQ的队列匹配。
```java
public class Recv {

  private final static String QUEUE_NAME = "hello";

  public static void main(String[] argv) throws Exception {
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost("localhost");
    Connection connection = factory.newConnection();
    Channel channel = connection.createChannel();

    channel.queueDeclare(QUEUE_NAME, false, false, false, null);
    System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

  }
}
```

请注意，我们也在这里声明队列。因为我们可能在发布者之前启动消费者，所以我们想确保在尝试使用队列中的消息之前该队列存在。

为什么不使用try-with-resource语句自动关闭通道和连接？这样我们就可以简单地使程序继续运行，关闭所有内容并退出！这将很尴尬，因为我们希望在消费者异步侦听消息到达时，该过程保持有效。

我们将告诉服务器将队列中的消息传递给我们。由于它将异步地向我们发送消息，因此我们以对象的形式提供了一个回调，该回调将缓冲消息，直到我们准备使用它们为止。这就是DeliverCallback子类所做的。

```java
DeliverCallback deliverCallback = (consumerTag, delivery) -> {
    String message = new String(delivery.getBody(), "UTF-8");
    System.out.println(" [x] Received '" + message + "'");
};
channel.basicConsume(QUEUE_NAME, true, deliverCallback, consumerTag -> { });
```

[这是整个Recv.java类][5]。

## 全部放在一起
您可以仅使用classpath上的RabbitMQ Java客户端来编译这两个文件：
```
javac -cp amqp-client-5.7.1.jar Send.java Recv.java
```

要运行它们，您需要把rabbitmq-client.jar及其依赖放在classpath。接着在命令行中，运行使用者（接收方）：
```
java -cp .:amqp-client-5.7.1.jar:slf4j-api-1.7.26.jar:slf4j-simple-1.7.26.jar Recv
```
然后，运行发布者（发送者）：
```
java -cp .:amqp-client-5.7.1.jar:slf4j-api-1.7.26.jar:slf4j-simple-1.7.26.jar Send
```
在Windows上，使用分号代替冒号来分隔类路径中的项目。

消费者将通过RabbitMQ打印从发布者那里得到的消息。消费者将持续运行，等待消息（使用Ctrl-C停止它），因此请尝试从另一个终端运行发布者。

    队列列表
    您可能希望查看RabbitMQ的队列以及队列中有多少条消息。您可以使用rabbitmqctl工具进行操作：
    sudo rabbitmqctl list_queues
    
    在Windows上，省略sudo：
    
    rabbitmqctl.bat list_queues
是时候进入第二部分并建立一个简单的工作队列了。

    提示
    要保存类型，可以为类路径设置环境变量，例如
    
    export CP=.:amqp-client-5.7.1.jar:slf4j-api-1.7.26.jar:slf4j-simple-1.7.26.jar
    java -cp $CP Send
    
    或在Windows上：
    
    set CP=.;amqp-client-5.7.1.jar;slf4j-api-1.7.26.jar;slf4j-simple-1.7.26.jar
    java -cp %CP% Send

生产非适用性免责声明
请记住，本教程和其他教程都是教程。它们一次展示一种新概念，并且可能有意过分简化了某些事情，而忽略了其他事情。例如，为简洁起见，很大程度上省略了诸如连接管理，错误处理，连接恢复，并发和度量收集之类的主题。此类简化的代码不应视为已准备就绪。

在使用您的应用程序之前，请先阅读其余文档。我们特别推荐以下指南：[发布者确认和消费者确认][6]， [生产确认清单][7]和[监控][8]。

原文链接: [RabbitMQ_HelloWorld_Java][3]

[1]: https://groups.google.com/forum/#!forum/rabbitmq-users
[2]: https://rabbitmq-slack.herokuapp.com/
[3]: https://www.rabbitmq.com/tutorials/tutorial-one-java.html
[4]: https://github.com/rabbitmq/rabbitmq-tutorials/blob/master/java/Send.java
[5]: https://github.com/rabbitmq/rabbitmq-tutorials/blob/master/java/Recv.java
[6]: https://www.rabbitmq.com/confirms.html
[7]: https://www.rabbitmq.com/production-checklist.html
[8]: https://www.rabbitmq.com/monitoring.html