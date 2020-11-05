# 前提条件
本教程假定RabbitMQ已在标准端口（5672）的localhost上安装并运行。

如果您使用其他主机，端口或证书，则连接设置需要进行调整。

# 在哪里获得帮助
如果您在阅读本教程时遇到困难，可以通过[邮件列表][1]或[RabbitMQ社区Slack][2]与我们联系。

# 远程过程调用（RPC）
（使用Java客户端）

在第二个教程中，我们学习了如何使用工作队列在多个工作人员之间分配耗时的任务。

但是，如果我们需要在远程计算机上运行函数并等待结果怎么办？好吧，那是一个不同的故事。这种模式通常称为“远程过程调用”或“RPC”。

在本教程中，我们将使用RabbitMQ构建一个RPC系统：一个客户端和一个可伸缩RPC服务器。由于我们没有值得分配的耗时任务，因此我们将创建一个虚拟RPC服务，该服务返回斐波那契数。

## 客户端接口
为了说明如何使用RPC服务，我们将创建一个简单的客户端类。它将暴露一个名为`call`的方法，该方法发送RPC请求并阻塞，直到收到结果为止：
```java
FibonacciRpcClient fibonacciRpc = new FibonacciRpcClient();
String result = fibonacciRpc.call("4");
System.out.println( "fib(4) is " + result);
```
## 有关RPC的说明
尽管RPC是计算中非常普遍的模式，但它经常受到批评。当程序员不知道函数调用是本地的还是缓慢的RPC时，就会出现问题。这样的混乱会导致系统无法预测，并给调试增加了不必要的复杂性。滥用RPC可能会导致无法维护的意面式代码，而不是简化软件。

牢记这些，并且考虑以下建议：

- 确保函数调用属于local or remote是明显的。
- 记录您的系统。明确组件之间的依赖关系。
- 处理错误案例。RPC服务器长时间关闭后，客户端应如何反应？

如有疑问，请避免使用RPC。如果可以的话，应该使用异步管道-代替类似RPC的阻塞，将结果异步推送到下一个计算阶段。

## 回调队列
通常，通过RabbitMQ进行RPC很容易。客户端发送请求消息，服务器发送响应消息。为了接收响应，我们需要发送带有请求的“回调”队列地址。我们可以使用默认队列（在Java客户端中是唯一的）。让我们尝试一下：
```java
callbackQueueName = channel.queueDeclare().getQueue();

BasicProperties props = new BasicProperties
                            .Builder()
                            .replyTo(callbackQueueName)
                            .build();

channel.basicPublish("", "rpc_queue", props, message.getBytes());

// ... then code to read a response message from the callback_queue ...
```
## 消息属性
AMQP 0-9-1协议预定义了消息附带的14个属性集。除以下内容外，大多数属性很少使用：

- `deliveryMode`：将消息标记为持久性（值为`2`）或瞬态（任何其他值）。您可能还记得第二个教程中的此属性。
- `contentType`：用于描述编码的mime类型。例如，对于常用的JSON编码，将此属性设置为`application/json`是一个好习惯。
- `replyTo`：通常用于命名回调队列。
- `correlationId`：用于将RPC响应与请求相关联。

我们需要这个新的导入：
```java
import com.rabbitmq.client.AMQP.BasicProperties;
```
## Correlation Id(关联ID)
在上面介绍的方法中，我们建议为每个RPC请求创建一个回调队列。这是相当低效的，但是幸运的是，有一种更好的方法-让我们为每个客户端创建一个回调队列。

这引起了一个新问题，在该队列中收到响应后，尚不清楚响应属于哪个请求。那就是使用`correlationId`属性的时候了 。我们将为每个请求设置唯一值。稍后，当我们在回调队列中收到消息时，我们将查看该属性，并基于此属性将响应与请求进行匹配。如果我们看到一个未知的`correlationId`值，我们可以安全地丢弃该消息-它不属于我们的请求。

您可能会问，为什么我们应该忽略回调队列中的未知消息，而不是因错误而失败？这是由于服务器端可能出现竞争状况。尽管可能性不大，但RPC服务器可能会在向我们发送计算结果之后，发送请求的确认消息之前死亡。如果发生这种情况，重新启动的RPC服务器将再次处理该请求。这就是为什么在客户端上我们必须妥善处理重复的响应，并且理想情况下RPC应该是幂等的。

## 概要

![](https://www.rabbitmq.com/img/tutorials/python-six.png)

我们的RPC将像这样工作：

- 对于RPC请求，客户端发送一条具有两个属性的消息：`replyTo`（设置为仅为该请求创建的匿名互斥队列）和`correlationId`（设置为每个请求的唯一值）。
- 该请求被发送到`rpc_queue`队列。
- RPC服务器正在等待该队列上的请求。出现请求时，它会完成工作并将带有结果的消息发送回客户端，通过使用`replyTo`字段中的队列。
- 客户端等待答复队列中的数据。出现消息时，它会检查`correlationId`属性。如果它与请求中的值匹配，则将响应返回给应用程序。
## 全部放在一起
斐波那契任务：
```java
private static int fib(int n) {
    if (n == 0) return 0;
    if (n == 1) return 1;
    return fib(n-1) + fib(n-2);
}
```
我们声明我们的斐波那契函数。它仅假定有效的正整数输入。（不要指望这种方法适用于大的数，它可能是最慢的递归实现）。

我们的RPC服务器中的代码可以在这里找到：[RPCServer.java](https://github.com/rabbitmq/rabbitmq-tutorials/blob/master/java/RPCServer.java)。

服务器代码非常简单：

- 像往常一样，我们首先建立连接、通道并声明队列。
- 我们可能要运行多个服务器进程。为了将负载平均分配到多个服务器，我们需要在`channel.basicQos`中设置`prefetchCount`。
- 我们使用`basicConsume`访问队列，在队列中我们以对象（`DeliverCallback`）的形式提供回调，该回调将完成工作并将响应发送回去。

可以在这里找到我们的RPC客户端的代码：[RPCClient.java](https://github.com/rabbitmq/rabbitmq-tutorials/blob/master/java/RPCClient.java)。

客户端代码稍微复杂一些：

- 我们建立一个连接和通道。
- 我们的`call`方法发出RPC请求。
- 在这里，我们首先生成一个唯一的`correlationId`并将其保存-我们的消费者回调将使用该值来匹配适当的响应。
- 然后，我们为reply创建一个专用的排他队列并订阅它。
- 接下来，我们发布具有两个属性的请求消息：`replyTo`和`correlationId`。
- 在这一点上，我们可以坐下来等到正确的响应到达。
- 由于我们的消费者交付处理是在单独的线程中进行的，因此在响应到达之前，我们将需要一些东西来挂起主线程。使用`BlockingQueue`是一种可行的解决方案。在这里，我们创建容量设置为1的`ArrayBlockingQueue`，因为我们只需要等待一个响应即可。
- 消费者的工作非常简单，对于每一个响应消息，它都会检查`correlationId`是否为我们要寻找的消息。如果是这样，它将响应放入`BlockingQueue`。
- 同时，主线程正在等待响应，以将其从`BlockingQueue`中获取。
- 最后，我们将响应返回给用户。

发出客户端请求：
```java
RPCClient fibonacciRpc = new RPCClient();

System.out.println(" [x] Requesting fib(30)");
String response = fibonacciRpc.call("30");
System.out.println(" [.] Got '" + response + "'");

fibonacciRpc.close();
```
现在是时候看看[RPCClient.java](https://github.com/rabbitmq/rabbitmq-tutorials/blob/master/java/RPCClient.java)和[RPCServer.java](https://github.com/rabbitmq/rabbitmq-tutorials/blob/master/java/RPCServer.java)的完整示例源代码（包括基本异常处理） 。

像往常一样编译和设置类路径（请参阅教程一）：
```
javac -cp $CP RPCClient.java RPCServer.java
```
我们的RPC服务现已准备就绪。我们可以启动服务器：
```
java -cp $CP RPCServer
# => [x] Awaiting RPC requests
```
要请求斐波那契编号，请运行客户端：
```
java -cp $CP RPCClient
# => [x] Requesting fib(30)
```
这里介绍的设计不是RPC服务的唯一可能的实现，但是它具有一些重要的优点：

- 如果RPC服务器太慢，则可以通过运行另一台RPC服务器来进行扩展。尝试在新控制台中运行第二个`RPCServer`。
- 在客户端，RPC只需要发送和接收一条消息。不需要诸如`queueDeclare`之类的同步调用。结果，RPC客户端只需要一个网络往返就可以处理单个RPC请求。

我们的代码仍然非常简单，并且不会尝试解决更复杂（但很重要）的问题，例如：

- 如果没有服务器在运行，客户端应如何反应？
- 客户端是否应该为RPC设置某种超时时间？
- 如果服务器发生故障并引发异常，是否应该将其转发给客户端？
- 在处理之前防止无效的传入消息（例如检查边界，类型）。


    如果要进行实验，可能会发现管理UI对于查看队列很有用。