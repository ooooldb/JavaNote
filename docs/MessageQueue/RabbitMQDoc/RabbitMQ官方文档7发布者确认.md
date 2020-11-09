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


# 发布者确认
发布者确认是一个用来实现可靠发布的RabbitMQ扩展。在通道上启用发布者确认后，`broker`将异步确认客户端发布的消息，这意味着它们已在服务器端处理。

**（使用Java客户端）**
## 总览
在本教程中，我们将使用发布者确认来确保发布的消息已安全到达`broker`。我们将介绍几种使用发布者确认的策略并解释其优缺点。

## 在channel上启用发布者确认
发布者确认是一个AMQP 0.9.1协议的RabbitMQ扩展，因此默认情况下未启用它们。发布者确认是使用`confirmSelect`方法在`Channel`级别启用的：
```java
Channel channel = connection.createChannel();
channel.confirmSelect();
```
必须在希望使用发布者确认的每个`Channel`上调用此方法。且应仅启用一次，而不是对每个发布的消息都启用。

## 策略#1：发布独立的消息
让我们从使用确认发布的最简单方法开始，即发布消息并同步等待其确认：
```java
while (thereAreMessagesToPublish()) {
    byte[] body = ...;
    BasicProperties properties = ...;
    channel.basicPublish(exchange, queue, properties, body);
    // 设置5秒的超时等待
    channel.waitForConfirmsOrDie(5_000);
}
```

在前面的示例中，我们像往常一样发布一条消息，并通过`Channel#waitForConfirmsOrDie(long)`方法对其进行等待确认。确认消息后，该方法立即返回。如果未在超时时间内确认该消息或该消息无应答信号（这意味着`broker`出于某种原因无法处理该消息），则该方法将抛出异常。异常的处理通常包括记录错误消息和（或）重新发送消息。

不同的客户端库有不同的方式来同步处理发布者的确认，因此请确保仔细阅读所使用客户端的文档。

此技术非常简单，但也有一个主要缺点：由于消息的确认会阻止所有后续消息的发布，因此它会`显著降低发布速度`。这种方法不能够应对每秒超过数百条发布消息的吞吐量。但是，对于某些应用程序来说这可能已经足够了。

## 发布者确认异步吗？
我们在一开始提到`broker`以异步方式确认发布的消息，但是在第一个示例中，代码同步等待直到消息被确认。客户端实际上异步接收确认，并相应地取消阻止对`waitForConfirmsOrDie`的调用 。可以将`waitForConfirmsOrDie`视为依赖于幕后异步通知的同步助手。

## 策略#2：批量发布消息
为了改进前面的示例——我们可以发布一批消息，并等待整个批次被确认。以下示例使用了100个消息的批次：
```java
int batchSize = 100;
int outstandingMessageCount = 0;
while (thereAreMessagesToPublish()) {
    byte[] body = ...;
    BasicProperties properties = ...;
    channel.basicPublish(exchange, queue, properties, body);
    outstandingMessageCount++;
    if (outstandingMessageCount == batchSize) {
        ch.waitForConfirmsOrDie(5_000);
        outstandingMessageCount = 0;
    }
}
if (outstandingMessageCount > 0) {
    ch.waitForConfirmsOrDie(5_000);
}
```

与等待确认单个消息相比，等待一批消息被确认可以极大地提高吞吐量（对于远程RabbitMQ节点，这最多可以达到20-30倍）。一个缺点是我们不知道发生故障时到底出了什么问题，因此我们可能必须将整批消息保存在内存中，以记录有意义的内容或重新发布消息。而且该解决方案仍然是同步的，因此它还是会阻止消息的发布。

### 策略#3：异步处理发布者确认
`broker`异步确认已发布的消息，只需在客户端上注册一个回调即可收到这些确认的通知：
```java
Channel channel = connection.createChannel();
channel.confirmSelect();
channel.addConfirmListener((sequenceNumber, multiple) -> {
    // code when message is confirmed
}, (sequenceNumber, multiple) -> {
    // code when message is nack-ed
});

```
有2个回调：一个用于确认的消息，另一个用于未确认的消息（`broker`认为可以丢失的消息）。每个回调都有2个参数：

- `sequence number`：一个标识已确认或未确认消息的数字。我们很快将看到如何将其与已发布的消息相关联。
- `multiple`：这是一个布尔值。如果为false，则仅确认/否定一条消息；如果为true，则将确认/否定序列号较低或相等的所有消息。

可以在发布之前使用`Channel#getNextPublishSeqNo()`获得序列号：
```java
int sequenceNumber = channel.getNextPublishSeqNo());
ch.basicPublish(exchange, queue, properties, body);
```
将消息与序列号关联的一种简单方法是使用映射。假设我们要发布的是字符串——因为它们很容易转换成要发布的字节数组。这是一个使用映射将发布序列号与消息的字符串主体相关联的代码示例：

```java
ConcurrentNavigableMap<Long, String> outstandingConfirms = new ConcurrentSkipListMap<>();
// ... code for confirm callbacks will come later
String body = "...";
outstandingConfirms.put(channel.getNextPublishSeqNo(), body);
channel.basicPublish(exchange, queue, properties, body.getBytes());
```

现在，发布代码使用映射跟踪出站消息。我们需要在确认到达时清理该映射，并做一些类似在消息被否定时记录warning的操作：
```java
ConcurrentNavigableMap<Long, String> outstandingConfirms = new ConcurrentSkipListMap<>();
ConfirmCallback cleanOutstandingConfirms = (sequenceNumber, multiple) -> {
    if (multiple) {
        ConcurrentNavigableMap<Long, String> confirmed = outstandingConfirms.headMap(
          sequenceNumber, true
        );
        confirmed.clear();
    } else {
        outstandingConfirms.remove(sequenceNumber);
    }
};

channel.addConfirmListener(cleanOutstandingConfirms, (sequenceNumber, multiple) -> {
    String body = outstandingConfirms.get(sequenceNumber);
    System.err.format(
      "Message with body %s has been nack-ed. Sequence number: %d, multiple: %b%n",
      body, sequenceNumber, multiple
    );
    cleanOutstandingConfirms.handle(sequenceNumber, multiple);
});
// ... publishing code
```

上一个示例包含一个回调，当确认到达时，该回调将清除映射。请注意，此回调处理单个确认+多个确认。此回调在确认到达时被调用（作为`Channel#addConfirmListener`的第一个参数 
）。否定邮件的回调将检索邮件正文并发出警告。然后，它重新使用前一个回调来清理未完成确认的映射（无论消息是已确认还是未确认，都必须删除它们在映射中的对应条目。）

## 如何跟踪未完成的确认？
我们的示例使用`ConcurrentNavigableMap`来跟踪未完成的确认。由于一系列原因此数据结构很方便。它允许轻松地将序列号与消息相关联（无论消息数据是什么），还可以根据给定的序列ID轻松清除Entry
（来处理多个确认/否定）。最后，它支持并发访问，因为在客户端库拥有的线程中调用了确认回调，该线程应与发布线程保持不同。

除了使用复杂的映射实现之外，还有其他跟踪未完成确认的方法，例如使用简单的并发哈希映射和变量来跟踪发布序列的下限，但是它们通常涉及更多且不属于教程。

综上所述，异步处理发布者确认通常需要执行以下步骤：

- 提供一种将发布序列号与消息相关联的方法。
- 在通道上注册一个确认侦听器，以便在发布者确认/否认到达后执行相应的操作（例如记录或重新发布被拒绝的消息）时收到通知。序列号与消息的关联机制在此步骤中可能还需要进行一些清洗。
- 在发布消息之前跟踪发布序列号。
## 重新发布已被否定的消息？
从相应的回调中重新发布一个已被否定的消息可能很诱人，但是应该避免这种情况，因为确认回调是在不应执行的通道的I/O线程中分派的。更好的解决方案是将消息放入由发布线程轮询的内存队列中。诸如`ConcurrentLinkedQueue`之类的类
将是在确认回调和发布线程之间传输消息的理想选择。

## 概要
在某些应用程序中，确保将发布的消息发送给代理可能是必不可少的。发布者确认是RabbitMQ
功能——可以帮助满足此要求。发布者确认本质上是异步的，但也可以同步处理它们。没有定式可以实现发布者确认，这通常归结为应用程序和整个系统的约束。典型的技术有：

- 单独发布消息，同步等待确认：简单，但吞吐量非常有限。
- 批量发布消息，同步等待批量确认：简单，合理的吞吐量，但很难推断出什么时候出了问题。
- 异步处理：最佳性能和资源使用，在发生错误的情况下可以很好地控制，但是可以正确实施。

## 放在一起
该PublisherConfirms.java类包含了我们所覆盖的技术代码。我们可以对其进行编译，按原样执行并查看它们各自的性能：
```
javac -cp $CP PublisherConfirms.java
java -cp $CP PublisherConfirms
```
输出将如下所示：
```
Published 50,000 messages individually in 5,549 ms
Published 50,000 messages in batch in 2,331 ms
Published 50,000 messages and handled confirms asynchronously in 4,054 ms
```
如果客户端和服务器位于同一台计算机上，则计算机上的输出应看起来相似。单独发布消息的效果不理想，但

发布者确认非常依赖于网络，因此我们最好尝试使用远程节点，因为客户端和服务器通常不在生产中的同一台计算机上，所以这是更现实的选择。`PublisherConfirms.java`可以轻松更改为使用非本地节点：
```java
static Connection createConnection() throws Exception {
    ConnectionFactory cf = new ConnectionFactory();
    cf.setHost("remote-host");
    cf.setUsername("remote-user");
    cf.setPassword("remote-password");
    return cf.newConnection();
}
```
重新编译该类，再次执行它，然后等待结果：
```
Published 50,000 messages individually in 231,541 ms
Published 50,000 messages in batch in 7,232 ms
Published 50,000 messages and handled confirms asynchronously in 6,332 ms
```
我们现在看到单独发布的效果非常差。但是，通过客户端和服务器之间的网络，批处理发布和异步处理现在的执行方式类似，对于发布者确认的异步处理来说，这是一个很小的优势。

请记住，批量发布很容易实现，但是在发布者否定确认的情况下，不容易知道哪些消息无法发送给代理。处理发布者确认异步涉及更多的实现，但是提供更好的粒度和更好地控制在处理发布的消息时执行的操作。

[1]: https://groups.google.com/forum/#!forum/rabbitmq-users        ""
[2]: https://rabbitmq-slack.herokuapp.com/  ""