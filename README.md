# ActiveMQ-practice  
  
## ActiveMQ 原理源码分析  
  
- [什么是消息中间件](#什么是消息中间件)  
- [消息中间件能做什么](#消息中间件能做什么)  
- [ActiveMQ简介](#activemq简介)  
- [ActiveMQ安装](#activemq安装) 
- [从JMS规范来了解ActiveMQ](#从jms规范来了解activemq)  
  - [JMS定义](#jms定义)  
  - [什么是MOM](#什么是mom)  
  - [MOM的特点](#mom的特点)  
  - [JMS规范](#jms规范)  
  - [JMS的体系结构](#jms的体系结构)  
  - [细化JMS的基本功能](#细化jms的基本功能) 
  - [JMS消息的可靠性机制](#jms消息的可靠性机制)
- [消息的持久化存储](#消息的持久化存储)
  - [持久化消息和非持久化消息的发送策略](#持久化消息和非持久化消息的发送策略)  
  - [消息同步发送和异步发送](#消息同步发送和异步发送)
- [消息的发送原理分析](#消息的发送原理分析)  
  - [消息发送的流程图](#消息发送的流程图)  
  - [ProducerWindowSize的含义](#producerwindowsize的含义)  
- [消息发送的源码分析](#消息发送的源码分析) 
- [持久化消息和非持久化消息的存储原理](#持久化消息和非持久化消息的存储原理)
  - [消息的持久化策略分析](#消息的持久化策略分析) 
- [消费端消费消息源码分析](#消费端消费消息源码分析)
  - [消息消费流程图](#消息消费流程图)
  - [消费端的PrefetchSize](#消费端的prefetchsize)
- [ActiveMQ静态网络配置配置说明](#activemq静态网络配置配置说明)
  
### 什么是消息中间件  
消息中间件是值利用高效可靠的消息传递机制进行平台无关的数据交流，并基于数据通信来进行分布式系统的集成。通过提供消息传递和消息排队模型，可以在分布式架构下扩展进程之间的通信。
### 消息中间件能做什么  
消息中间件主要解决的就是分布式系统之间消息传递的问题，它能够屏蔽各种平台以及协议之间的特性，实现应用程序之间的协同。  
举个非常简单的例子，就拿一个电商平台的注册功能来简单分析下，用户注册这一个服务，不单单只是 insert 一条数据到数据库里面就完事了，还需要发送激活邮件、发送新人红包或者积分、发送短信等一系列操作。假如说这里面的每一个操作，都需要消耗 1s， 那么整个注册过程就需要耗时 4s 才能响应给用户。  
![](https://github.com/YufeizhangRay/image/blob/master/ActiveMQ/%E4%BC%A0%E7%BB%9F%E6%A8%A1%E5%9E%8B.jpeg)  
  
但是我们从注册这个服务可以看到，每一个子操作都是相对独立的，同时，基于领域划分以后，发送激活邮件、发送短信、赠送积分及红包都属于不同的子域。所以我们可以对这些子操作进行来实现异步化执行，类似于多线程并行处理的概念。 
如何实现异步化呢?用多线程能实现吗?多线程当然可以实现，只是消息的持久化、消息的重发这些条件，多线程并不能满足。所以需要借助一些开源中间件来解决。  
分布式消息队列就是一个非常好的解决办法，引入分布式消 息队列以后，架构图就变成这样了(下图是异步消息队列的场景)。  
![](https://github.com/YufeizhangRay/image/blob/master/ActiveMQ/MQ%E6%A8%A1%E5%9E%8B.jpeg)  
  
通过引入分布式队列，就能够大大提升程序的处理效率，并且还解决了各个模块之间的耦合问题。  
  
我们再来展开一种场景，通过分布式消息队列来实现流量整形。比如在电商平台的秒杀场景下，流量会非常大，通过消息队列的方式可以很好的缓解高流量的问题。  
![](https://github.com/YufeizhangRay/image/blob/master/ActiveMQ/%E7%A7%92%E6%9D%80.jpeg)
用户提交过来的请求，先写入到消息队列。消息队列是有长度的，如果消息队列长度超过指定长度，直接抛弃。
秒杀的具体核心处理业务，接收消息队列中消息进行处理，这里的消息处理能力取决于消费端本身的吞吐量。
当然，消息中间件还有更多应用场景，比如在弱一致性事务模型中，可以采用分布式消息队列的实现最大能力通知方式来实现数据的最终一致性等等。

### ActiveMQ简介
ActiveMQ 是完全基于 JMS 规范实现的一个消息中间件产品。是 Apache 开源基金会研发的消息中间件。  
ActiveMQ 主要应用在分布式系统架构中，帮助构建高可用、高性能、可伸缩的企业级面向消息服务的系统。  
  
### ActiveMQ安装
>1.登录到 http://activemq.apache.org/activemq-5150- release.html，找到 ActiveMQ 的下载地址  
>2.直接 copy 到服务器上通过 tar -zxvf apache- activeMQ.tar.gz  
>3. 启动运行  
>>a)普通启动:到 bin 目录下， sh activemq start  
>>b)启动并指定日志文件 sh activemq start > /tmp/activemqlog  
>4.检查是否已启动  
ActiveMQ 默认采用 61616 端口提供 JMS 服务，使用 8161 端口提供管理控制台服务，执行以下命令可以检查是否成功启动 ActiveMQ 服务  
netstat -an|grep 61616  
>5.通过 http://192.168.138.188:8161 访问 activeMQ 管理页面，默认帐号密码 admin/admin  
>6.关闭 ActiveMQ; sh activemq stop  
  
### 从JMS规范来了解ActiveMQ  
  
#### JMS定义  
Java 消息服务(Java Message Service)是 Java 平台中关于面向消息中间件的 API，用于在两个应用程序之间，或者分布式系统中发送消息，进行异步通信。
JMS 是一个与具体平台无关的 API，绝大多数 MOM (Message Oriented Middleware)(面向消息中间件)提供商都对 JMS 提供了支持，ActiveMQ 就是其中一个实现。  
#### 什么是MOM  
MOM 是面向消息的中间件，使用消息传送提供者来协调消息传送操作。MOM 需要提供 API 和管理工具。客户端使用 API 调用，把消息发送到由提供者管理的目的地。在发送消息之后，客户端会继续执行其他工作，并且在接收方收到这个消息确认之前，提供者一直保留该消息。  
![](https://github.com/YufeizhangRay/image/blob/master/ActiveMQ/JMS%E8%A7%84%E8%8C%83.jpeg)  
  
#### MOM的特点  
>1.消息异步接收，发送者不需要等待消息接受者响应  
>2.消息可靠接收，确保消息在中间件可靠保存。只有接收方收到后才删除消息。  
  
#### JMS规范  
我们已经知道了 JMS 规范的目的是为了使得 Java 应用程序能够访问现有 MOM (消息中间件)系统，形成一套统一的标准规范，解决不同消息中间件之间的协作问题。在创建 JMS 规范时，设计者希望能够结合现有的消息传送的精髓，比如说  
>1.不同的消息传送模式或域，例如点对点消息传送和发布 /订阅消息传送  
>2.提供接收同步和异步消息的工具  
>3.对可靠消息传送的支持  
>4.常见消息格式，例如流、文本和字节  
   
#### JMS的体系结构  
![](https://github.com/YufeizhangRay/image/blob/master/ActiveMQ/JMS%E7%BB%93%E6%9E%84.jpeg)  
  
#### 细化JMS的基本功能  
消息传递域  
JMS 规范中定义了两种消息传递域:点对点(point-to- point)消息传递域 和发布/订阅 消息传递域 (publish/subscribe)  
简单理解就是:有点类似于我们通过 qq 聊天的时候，在群里面发消息和给其中一个同学私聊消息。在群里发消息，所有群成员都能收到消息。私聊消息只能被私聊的学员能收到消息。  
点对点消息传递域
>1.每个消息只能有一个消费者  
>2.消息的生产者和消费者之间没有时间上的相关性。无论消费者在生产者发送消息的时候是否处于运行状态，都可以提取消息  
  
![](https://github.com/YufeizhangRay/image/blob/master/ActiveMQ/P2P.jpeg)  
  
发布订阅消息传递域  
>1.每个消息可以有多个消费者  
>2.生产者和消费者之间有时间上的相关性。订阅一个主题的消费者只能消费自它订阅之后发布的消息。JMS 规范允许客户创建持久订阅，这在一定程度上降低了时间上的相关性要求。持久订阅允许消费者消费它在未处于激活状态时发送的消息。  
  
![](https://github.com/YufeizhangRay/image/blob/master/ActiveMQ/%E8%AE%A2%E9%98%85.jpeg)  
  
消息结构组成  
JMS 消息由及部分组成:消息头、属性、消息体  
  
消息头  
消息头(Header) - 消息头包含消息的识别信息和路由信息，消息头包含一些标准的属性如:
>1.JMSDestination 消息发送的目的地，queue 或者 topic   
>2.JMSDeliveryMode 传送模式。持久模式和非持久模式   
>3.JMSPriority 消息优先级(优先级分为10个级别，从0(最低)到9(最高). 如果不设定优先级，默认级别是4。需要注意的是，JMS provider 并不一定保证按照优先级的顺序提交消息)  
>4.JMSMessageID 唯一识别每个消息的标识  
  
属性  
按类型可以分为应用设置的属性，标准属性和消息中间件定义的属性  
应用程序设置和添加的属性，比如  
```
Message.setStringProperty(“key”,”value”); 
```
通过下面的代码可以获得自定义属性的，在接收端的代码中编写在发送端，定义消息属性
```
message.setStringProperty("zyf","HelloWorld"); 
```
在接收端接收数据
```
Enumeration enumeration=message.getPropertyNames();
 while(enumeration.hasMoreElements()){
 String name=enumeration.nextElement().toString();
   System.out.println("name:"+name+":"+message.getStringProperty(name));
 System.out.println();
 }
```
  
消息体
就是我们需要传递的消息内容，JMS API 定义了 5 种消息体格式，可以使用不同形式发送接收数据，并可以兼容现有的消息格式，其中包括  
>TextMessage:java.lang.String 对象，如 xml 文件内容  
>MapMessage:名/值对的集合，名是 String 对象，值类型可以是 Java 任何基本类型  
>BytesMessage:字节流  
>StreamMessage:Java 中的输入输出流  
>ObjectMessage:Java 中的可序列化对象  
>Message:没有消息体，只有消息头和属性  
  
绝大部分的时候，我们只需要基于消息体进行构造。

持久订阅  
持久订阅的概念，也很容易理解，比如还是以 QQ 为例，我们把 QQ 退出了，但是下次登录的时候，仍然能收到离线的消息。持久订阅就是这样一个道理，持久订阅有两个特点:  
>1.持久订阅者和非持久订阅者针对的 Domain 是 Pub/Sub，而不是 P2P  
>2.当 Broker 发送消息给订阅者时，如果订阅者处于未激活状态状态:持久订阅者可以收到消息，而非持久订阅者则收不到消息。  
  
当然这种方式也有一定的影响:
当持久订阅者处于未激活状态时，Broker 需要为持久订阅者保存消息;
如果持久订阅者订阅的消息太多则会溢出。

```
 connection=connectionFactory.createConnection();
 connection.setClientID("zyf-001");
 connection.start();
 Session session=connection.createSession(Boolean.TRUE,Session.AUTO_ACKNOWLEDGE);
 Topic destination=session.createTopic("myTopic");
 MessageConsumer consumer=session.createDurableSubscriber(destination,"zyf-001");
 TextMessage message=(TextMessage)consumer.receive();
 System.out.println(message.getText());
```        

持久订阅时，客户端向JMS服务器注册一个自己身份的ID，当这个客户端处于离线时，JMS Provider 会为这个 ID 保存所有发送到主题的消息，当客户再次连接到 JMS Provider 时，会根据自己的 ID 得到所有当自己处于离线时发送到主题的消息。  
这个身份 ID，在代码中的体现就是 connection 的 ClientID，这个其实很好理解，你要想收到朋友发送的 qq 消息，前提就是你得先注册个 QQ 号，而且还要有台能上网的设备，电脑或手机。设备就相当于是 clientId 是唯一的;qq 号相当于是订阅者的名称，在同一台设备上，不能用同一个 qq 号挂 2 个客户端。连接的 clientId 必须是唯一的，订阅者的名称在同一个连接内必须唯一。这样才能唯一的确定连接和订阅者。  
   
#### JMS消息的可靠性机制
理论上来说，我们需要保证消息中间件上的消息，只有被消费者确认过以后才会被签收，相当于我们寄一个快递出去，收件人没有收到快递，就认为这个包裹还是属于待签
收状态，这样才能保证包裹能够安全达到收件人手里。消息中间件也是一样。   
消息的消费通常包含 3 个阶段:  
>1.客户接收消息  
>2.客户处理消息  
>3.消息被确认  
  
首先，来简单了解 JMS 的事务性会话和非事务性会话的概念。  
JMS Session 接口提供了 commit 和 rollback 方法。事务提交意味着生产的所有消息被发送，消费的所有消息被确认; 事务回滚意味着生产的所有消息被销毁，消费的所有消息被恢复并重新提交，除非它们已经过期。事务性的会话总是牵涉到事务处理中，commit 或 rollback 方法一旦被调用，一个事务就结束了，而另一个事务被开始。关闭事务性会话将回滚其中的事务。  
  
事务型会话  
在事务状态下进行发送操作，消息并未真正投递到中间件， 而只有进行 session.commit 操作之后，消息才会发送到中间件，再转发到适当的消费者进行处理。如果是调用 rollback 操作，则表明当前事务期间内所发送的消息都取消掉。通过在创建 session 的时候使用 true or false 来决定 当前的会话是事务性还是非事务性。
```
connection.createSession(Boolean.TRUE,Session.AUTO_ ACKNOWLEDGE);
```
在事务性会话中，消息的确认是自动进行，也就是通过 session.commit()以后，消息会自动确认。  
`必须保证发送端和接收端都是事务性会话。`  
  
在非事务型会话中  
消息何时被确认取决于创建会话时的应答模式 `(acknowledgement mode)`。  
有三个可选项  
  
Session.AUTO_ACKNOWLEDGE  
当客户成功的从 receive 方法返回的时候，或者从 MessageListenner.onMessage 方法成功返回的时候，会话自动确认客户收到消息。  
  
Session.CLIENT_ACKNOWLEDGE  
客户通过调用消息的 acknowledge 方法确认消息。
在这种模式中，确认是在会话层上进行，确认一个被消费的消息将自动确认所有已被会话消费的消息。列如，如果一个消息消费者消费了 10 个消息，然后确认了第 5 个消息，那么 0~5 的消息都会被确认。  
  
Session.DUPS_ACKNOWLEDGE  
消息延迟确认。指定消息提供者在消息接收者没有确认发送时重新发送消息，这种模式不在乎接受者收到重复的消息。  
  
### 消息的持久化存储  
  
消息的持久化存储也是保证可靠性最重要的机制之一，也就是消息发送到 Broker 上以后，如果 broker 出现故障宕机了，那么存储在 broker 上的消息不应该丢失。可以通过下面的代码来设置消息发送端的持久化和非持久化特性
```
MessageProducer producer=session.createProducer(destination);
producer.setDeliveryMode(DeliveryMode.PERSISTENT);
```
对于非持久的消息，JMS provider 不会将它存到文件/数据库等稳定的存储介质中。也就是说非持久消息驻留在内存中，如果 JMS provider 宕机，那么内存中的非持久消息会丢失。  
对于持久消息，消息提供者会使用存储-转发机制，先将消息存储到稳定介质中，等消息发送成功后再删除。如果 JMS provider 挂掉了，那么这些未送达的消息不会丢失;JMS provider 恢复正常后，会重新读取这些消息，并传送给对应的消费者。  

#### 持久化消息和非持久化消息的发送策略  
  
#### 消息同步发送和异步发送  
  
ActiveMQ支持同步、异步两种发送模式将消息发送到broker上。  
同步发送过程中，发送者发送一条消息会阻塞直到broker反馈一个确认消息，表示消息已经被broker处理。这个机制提供了消息的安全性保障，但是由于是阻塞的操作，会影响到客户端消息发送的性能。  
异步发送的过程中，发送者不需要等待broker提供反馈，所以性能相对较高。但是可能会出现消息丢失的情况。所以使用异步发送的前提是在某些情况下允许出现数据丢失的情况。  
`默认情况下，非持久化消息是异步发送的，持久化消息并且是在非事务模式下是同步发送的。但是在开启事务的情况下，消息都是异步发送。`  
由于异步发送的效率会比同步发送性能更高。所以在发送持久化消息的时候，尽量去开启事务会话。  
三种开启异步发送的方法  
```
1.ConnectionFactory connectionFactory=new ActiveMQConnectionFactory("tcp://192.168.138.188:61616?jms.useAsyncSend=true");
2.((ActiveMQConnectionFactory) connectionFactory).setUseAsyncSend(true);
3.((ActiveMQConnection)connection).setUseAsyncSend(true);
```
### 消息的发送原理分析  
   
#### 消息发送的流程图  
![](https://github.com/YufeizhangRay/image/blob/master/ActiveMQ/%E6%B6%88%E6%81%AF%E5%8F%91%E9%80%81%E6%B5%81%E7%A8%8B.jpeg)  
  
#### ProducerWindowSize的含义 
producer每发送一个消息，统计一下发送的字节数，当字节数达到ProducerWindowSize值时，需要等待broker的确认，才能继续发送。  
主要用来约束在异步发送时producer端允许积压的(尚未ACK)的消息的大小，且只对异步发送有意义。每次发送消息之后，都将会导致memoryUsage大小增加(+message.size)，当broker返回producerAck时，memoryUsage尺寸减少(producerAck.size，此size表示先前发送消息的大小)。  
可以通过如下2种方式设置:  
>1.在brokerUrl中设置: "tcp://localhost:61616?jms.producerWindowSize=1048576",这种设置将会对所有的producer生效。  
>2.在destinationUri中设置: "test-queue?producer.windowSize=1048576",此参数只会对使用此Destination实例的producer失效，将会覆盖brokerUrl中的producerWindowSize值。  
  
注意:此值越大，意味着消耗Client端的内存就越大。  
  
### 消息发送的源码分析 

以producer.send为入口  
```
public void send(Destination destination, Message message, int deliveryMode, int priority, long
timeToLive, AsyncCallback onComplete) throws JMSException {
checkClosed(); //检查session的状态，如果session关闭则抛异常 
if (destination == null) {
        if (info.getDestination() == null) {
            throw new UnsupportedOperationException("A destination must be specified.");
}
        throw new InvalidDestinationException("Don't understand null destinations");
    }
    ActiveMQDestination dest;
if (destination.equals(info.getDestination())) {//检查destination的类型，如果符合要求，就转变为 ActiveMQDestination
        dest = (ActiveMQDestination)destination;
    } else if (info.getDestination() == null) {
        dest = ActiveMQDestination.transform(destination);
    } else {
        throw new UnsupportedOperationException("This producer can only send messages to: " +
this.info.getDestination().getPhysicalName());
    }
    if (dest == null) {
        throw new JMSException("No destination specified");
    }
    if (transformer != null) {
        Message transformedMessage = transformer.producerTransform(session, this, message);
        if (transformedMessage != null) {
            message = transformedMessage;
        }
}
if (producerWindow != null) {//如果发送窗口大小不为空，则判断发送窗口的大小决定是否阻塞
        try {
            producerWindow.waitForSpace();
        } catch (InterruptedException e) {
            throw new JMSException("Send aborted due to thread interrupt.");
} }
//发送消息到broker的topic
this.session.send(this, dest, message, deliveryMode, priority, timeToLive, producerWindow,
sendTimeout, onComplete);
    stats.onMessage();
}
```
ActiveMQSession的send方法  
```
 protected void send(ActiveMQMessageProducer producer, ActiveMQDestination destination, Message
message, int deliveryMode, int priority, long timeToLive,MemoryUsage producerWindow, int sendTimeout, 
AsyncCallback onComplete) throws JMSException {
    checkClosed();
    if (destination.isTemporary() && connection.isDeleted(destination)) {
  
   throw new InvalidDestinationException("Cannot publish to a deleted Destination: " + destination);
}
synchronized (sendMutex) { //互斥锁，如果一个session的多个producer发送消息到这里，会保证消息发送的有序性
// tell the Broker we are about to start a new transaction doStartTransaction();//告诉broker开始一个新事务，只有事务型会话中才会开启
TransactionId txid = transactionContext.getTransactionId();//从事务上下文中获取事务id 
long sequenceNumber = producer.getMessageSequence();
//Set the "JMS" header fields on the original message, see 1.1 spec section 3.4.11 message.setJMSDeliveryMode(deliveryMode); //在JMS协议头中设置是否持久化标识
long expiration = 0L;//计算消息过期时间
if (!producer.getDisableMessageTimestamp()) {
            long timeStamp = System.currentTimeMillis();
            message.setJMSTimestamp(timeStamp);
            if (timeToLive > 0) {
                expiration = timeToLive + timeStamp;
             }
} message.setJMSExpiration(expiration);//设置消息过期时间 
message.setJMSPriority(priority);//设置消息的优先级 
message.setJMSRedelivered(false);//设置消息为非重发
// transform to our own message format here //将不通的消息格式统一转化为ActiveMQMessage
        ActiveMQMessage msg = ActiveMQMessageTransformation.transformMessage(message,connection);
        msg.setDestination(destination);//设置目的地 //生成并设置消息id
        msg.setMessageId(new MessageId(producer.getProducerInfo().getProducerId(),sequenceNumber));
// Set the message id.
if (msg != message) {//如果消息是经过转化的，则更新原来的消息id和目的地
            message.setJMSMessageID(msg.getMessageId().toString());
            // Make sure the JMS destination is set on the foreign messages too.
            message.setJMSDestination(destination);
}
        //clear the brokerPath in case we are re-sending this message
        msg.setBrokerPath(null);
        msg.setTransactionId(txid);
        if (connection.isCopyMessageOnSend()) {
            msg = (ActiveMQMessage)msg.copy();
        }
msg.setConnection(connection); 
msg.onSend();//把消息属性和消息体都设置为只读，防止被修改 
msg.setProducerId(msg.getMessageId().getProducerId()); 
if (LOG.isTraceEnabled()) {
            LOG.trace(getSessionId() + " sending message: " + msg);
        }
         //如果onComplete没有设置，且发送超时时间小于0，且消息不需要反馈，且连接器不是同步发送模式，且消息非持久化或者连接器是异步发送模式
//或者存在事务id的情况下，走异步发送，否则走同步发送
        if (onComplete==null && sendTimeout <= 0 && !msg.isResponseRequired() &&
!connection.isAlwaysSyncSend() && (!msg.isPersistent() || connection.isUseAsyncSend() || txid !=
null)) {
            this.connection.asyncSendPacket(msg);
            if (producerWindow != null) {
                // Since we defer lots of the marshaling till we hit the
                // wire, this might not
                // provide and accurate size. We may change over to doing
                // more aggressive marshaling,
                // to get more accurate sizes.. this is more important once
                // users start using producer window
                // flow control.
                  int size = msg.getSize(); //异步发送的情况下，需要设置producerWindow的大小 
                  producerWindow.increaseUsage(size);
             }
        } else {
        if (sendTimeout > 0 && onComplete==null) { 
            this.connection.syncSendPacket(msg,sendTimeout); //带超时时间的同步发送
        }else {
            this.connection.syncSendPacket(msg, onComplete); //带回调的同步发送
        }
}
} 
}
```
ActiveMQConnection. doAsyncSendPacket
```
  private void doAsyncSendPacket(Command command) throws JMSException {
    try {
        this.transport.oneway(command);
    } catch (IOException e) {
        throw JMSExceptionSupport.create(e);
    }
}
    
```
这个地方问题来了，this.transport是什么东西?在哪里实例化的?按照以前看源码的惯例来看，它肯定不是一个单纯的对象，一定是在创建连接的过程中初始化的。所以我们定位到代码 transport的实例化过程  
从`connection=connectionFactory.createConnection();`  
这行代码作为入口，一直跟踪到 `ActiveMQConnectionFactory. createActiveMQConnection`这个方法中。代码如下  
```
   protected ActiveMQConnection createActiveMQConnection(String userName, String password) throws JMSException {
    if (brokerURL == null) {
        throw new ConfigurationException("brokerURL not set.");
    }
    ActiveMQConnection connection = null;
    try {
        Transport transport = createTransport();
        connection = createActiveMQConnection(transport, factoryStats);
        connection.setUserName(userName);
        connection.setPassword(password); //省略后面的代码
}
```
createTransport  
调用`ActiveMQConnectionFactory.createTransport`方法，去创建一个transport对象。
>1.构建一个URI  
>2.根据URL去创建一个连接TransportFactory.connect  
  
默认使用的是tcp的协议  
```
  protected Transport createTransport() throws JMSException {
    try {
        URI connectBrokerUL = brokerURL;
        String scheme = brokerURL.getScheme();
        if (scheme == null) {
            throw new IOException("Transport not scheme specified: [" + brokerURL + "]");
        }
        if (scheme.equals("auto")) {
            connectBrokerUL = new URI(brokerURL.toString().replace("auto", "tcp"));
        } else if (scheme.equals("auto+ssl")) {
            connectBrokerUL = new URI(brokerURL.toString().replace("auto+ssl", "ssl"));
        } else if (scheme.equals("auto+nio")) {
            connectBrokerUL = new URI(brokerURL.toString().replace("auto+nio", "nio"));
        } else if (scheme.equals("auto+nio+ssl")) {
            connectBrokerUL = new URI(brokerURL.toString().replace("auto+nio+ssl", "nio+ssl"));
}
        return TransportFactory.connect(connectBrokerUL);
    } catch (Exception e) {
        throw JMSExceptionSupport.create("Could not create Transport. Reason: " + e, e);
    }
}
```
TransportFactory.findTransportFactory  
>1.从TRANSPORT_FACTORYS这个Map集合中，根据scheme去获得一个TransportFactory指定的实例对象  
>2.如果Map集合中不存在，则通过TRANSPORT_FACTORY_FINDER去找一个并且构建实例  
  
这个地方又有点类似于SPI的思想,他会从META- INF/services/org/apache/activemq/transport/ 这个路径下，根据URI组装的scheme去找到匹配的class对象并且实例化，所以根据tcp为key去对应的路径下可以找到TcpTransportFactory  
```
  public static TransportFactory findTransportFactory(URI location) throws IOException {
    String scheme = location.getScheme();
    if (scheme == null) {
        throw new IOException("Transport not scheme specified: [" + location + "]");
    }
    TransportFactory tf = TRANSPORT_FACTORYS.get(scheme);
    if (tf == null) {
        // Try to load if from a META-INF property.
        try {
            tf = (TransportFactory)TRANSPORT_FACTORY_FINDER.newInstance(scheme);
            TRANSPORT_FACTORYS.put(scheme, tf);
        } catch (Throwable e) {
            throw IOExceptionSupport.create("Transport scheme NOT recognized: [" + scheme + "]",
e);
}
}
return tf; 
}
```
调用`TransportFactory.doConnect`去构建一个连接  
```
 public Transport doConnect(URI location) throws Exception {
    try {
        Map<String, String> options = new HashMap<String, String>(URISupport.parseParameters(location));
        if( !options.containsKey("wireFormat.host") ) {
            options.put("wireFormat.host", location.getHost());
         }
        WireFormat wf = createWireFormat(options);
        Transport transport = createTransport(location, wf); //创建一个Transport，创建一个socket连接 -> 终于找到真相了
        Transport rc = configure(transport, wf, options);//配置configure，这个里面是对Transport做链路包装
    }
    //remove auto
    IntrospectionSupport.extractProperties(options, "auto.");
    if (!options.isEmpty()) {
    throw new IllegalArgumentException("Invalid connect parameters: " + options);
              return rc;
    } catch (URISyntaxException e) {
        throw IOExceptionSupport.create(e);
    }
}
```
configure  
```
public Transport configure(Transport transport, WireFormat wf, Map options) throws Exception { 
//组装一个复合的transport，这里会包装两层，一个是IactivityMonitor.另一个是WireFormatNegotiator 
transport = compositeConfigure(transport, wf, options);
transport = new MutexTransport(transport); //再做一层包装,MutexTransport 
transport = new ResponseCorrelator(transport); //包装ResponseCorrelator
    return transport;
}
```
到目前为止，这个transport实际上就是一个调用链了，他的链结构为   
```
ResponseCorrelator(MutexTransport(WireFormatNegotiator(IactivityMonitor(TcpTransport())) 
```
>ResponseCorrelator 用于实现异步请求。  
>MutexTransport 实现写锁，表示同一时间只允许发送一个请求  
>WireFormatNegotiator 实现了客户端连接broker的时候先发送数据解析相关的协议信息，比如解析版本号，是否使用缓存等  
>InactivityMonitor 用于实现连接成功成功后的心跳检查机制，客户端每10s发送一次心跳信息。服务端每30s读取 一次心跳信息。  
  
同步发送和异步发送的区别  
```
  public Object request(Object command, int timeout) throws IOException { 
  FutureResponse response = asyncRequest(command, null);
return response.getResult(timeout); // 从future方法阻塞等待返回
}
```  
在ResponseCorrelator的request方法中，需要通过response.getResult去获得broker的反馈，否则会阻塞。   
  
### 持久化消息和非持久化消息的存储原理   
正常情况下，非持久化消息是存储在内存中的，持久化消息是存储在文件中的。能够存储的最大消息数据在 ${ActiveMQ_HOME}/conf/activemq.xml文件中的systemUsage节点。  
SystemUsage配置设置了一些系统内存和硬盘容量。  
```
  <systemUsage>
       <systemUsage>
           <memoryUsage> //该子标记设置整个ActiveMQ节点的“可用内存限制”。这个值不能超过ActiveMQ本身设置的最大内存大小。其中的 percentOfJvmHeap属性表示百分比。占用70%的堆内存
               <memoryUsage percentOfJvmHeap="70" />
           </memoryUsage>
           <storeUsage> //该标记设置整个ActiveMQ节点，用于存储“持久化消息”的“可用磁盘空间”。该子标记的limit属性必须要进行设置
               <storeUsage limit="100 gb"/>
           </storeUsage>
           <tempUsage> //一旦ActiveMQ服务节点存储的消息达到了memoryUsage的限制，非持久化消息就会被转储到 temp store区域，虽然我们说过非持久化消息不进行持久化存储，但是ActiveMQ为了防止“数据洪峰”出现时非持久化消息大量堆积致使内存耗尽的情况出现，还是会将非持久化消息写入到磁盘的临时区域——temp store。这个子标记就是为了设置这个temp store区域的“可用磁盘空间限制”
               <tempUsage limit="50 gb"/>
           </tempUsage>
       </systemUsage>
</systemUsage>
```
从上面的配置我们需要get到一个结论，当非持久化消息堆积到一定程度的时候，也就是内存超过指定的设置阈值时，ActiveMQ会将内存中的非持久化消息写入到临时文件，以便腾出内存。但是它和持久化消息的区别是，重启之后，持久化消息会从文件中恢复，非持久化的临时文件会直接删除。  
  
#### 消息的持久化策略分析  
消息持久性对于可靠消息传递来说是一种比较好的方法，即使发送者和接受者不是同时在线或者消息中心在发送者发送消息后宕机了，在消息中心重启后仍然可以将消息发送出去。消息持久性的原理很简单，就是在发送消息出去后，消息中心首先将消息存储在本地文件、内存或者远程数据库，然后把消息发送给接受者，发送成功后再把消息从存储中删除，失败则继续尝试。接下来我们来了解一下消息在broker上的持久化存储实现方式。  

持久化存储支持类型  
ActiveMQ支持多种不同的持久化方式，主要有以下几种，无论使用哪种持久化方式，消息的存储逻辑都是一致的。
>1.KahaDB存储(默认存储方式)  
>2.JDBC存储  
>3.Memory存储  
>4.LevelDB存储  
>5.JDBC With ActiveMQ Journal KahaDB存储  
  
KahaDB是目前默认的存储方式,可用于任何场景，提高了性能和恢复能力。消息存储使用一个事务日志和一个索引文件来存储它所有的地址。
KahaDB是一个专门针对消息持久化的解决方案，它对典型的消息使用模式进行了优化。在Kaha中，数据被追加到 data logs中。当不再需要log文件中的数据的时候，log文件会被丢弃。  
  
KahaDB的配置方式  
```
 <persistenceAdapter>
          <kahaDB directory="${activemq.data}/kahadb"/>
 </persistenceAdapter>
```
KahaDB的存储原理  
在data/kahadb这个目录下，会生成四个文件  
1.db.data 它是消息的索引文件，本质上是B-Tree(B树)，使用B-Tree作为索引指向db-*.log里面存储的消息  
2.db.redo 用来进行消息恢复  
3.db-*.log 存储消息内容。新的数据以APPEND的方式追加到日志文件末尾。属于顺序写入，因此消息存储是比较快的。默认是32M，达到阈值会自动递增。  
4.lock文件锁，表示当前获得kahadb读写权限的broker   
  
JDBC存储  
使用JDBC持久化方式，数据库会创建3个表:activemq_msgs，activemq_acks和activemq_lock。 
>ACTIVEMQ_MSGS 消息表，queue和topic都存在这个表中  
>ACTIVEMQ_ACKS 存储持久订阅的信息和最后一个持久订阅接收的消息ID  
>ACTIVEMQ_LOCKS 锁表，用来确保某一时刻，只能有一个ActiveMQ broker实例来访问数据库  
  
JDBC存储实践  
```
   <persistenceAdapter>
    <jdbcPersistenceAdapter dataSource="# MySQL-DS " createTablesOnStartup="true" />
   </persistenceAdapter>
```
 dataSource指定持久化数据库的bean，createTablesOnStartup是否在启动的时候创建数据表，默认值是true，这样每次启动都会去创建数据表了，一般是第一次启动的时候设置为true，之后改成false。  
  
Mysql持久化Bean配置  
```
  <bean id="Mysql-DS" class="org.apache.commons.dbcp.BasicDataSource" destroy-method="close">
      <property name="driverClassName" value="com.mysql.jdbc.Driver"/>
      <property name="url" value="jdbc:mysql://192.168.11.156:3306/activemq?relaxAutoCommit=true"/>
      <property name="username" value="root"/>
      <property name="password" value="root"/>
</bean>
```
添加Jar包依赖  
![](https://github.com/YufeizhangRay/image/blob/master/ActiveMQ/JAR.jpeg)  
  
LevelDB存储  
LevelDB持久化性能高于KahaDB，虽然目前默认的持久化方式仍然是KahaDB。
```
 <persistenceAdapter>
      <levelDBdirectory="activemq-data"/>
</persistenceAdapter>
```
Memory 消息存储  
基于内存的消息存储，内存消息存储主要是存储所有的持久化的消息在内存中。persistent=”false”，表示不设置持久化存储，直接存储到内存中。
```
<beans>
<broker brokerName="test-broker" persistent="false" xmlns="http://activemq.apache.org/schema/core">
<transportConnectors>
<transportConnector uri="tcp://localhost:61635"/>
</transportConnectors> 
</broker>
</beans>
```
JDBC Message store with ActiveMQ Journal  
这种方式克服了JDBC Store的不足，JDBC每次消息过来，都需要去写库和读库。   
ActiveMQ Journal，使用高速缓存写入技术，大大提高了性能。  
当消费者的消费速度能够及时跟上生产者消息的生产速度时，journal文件能够大大减少需要写入到DB中的消息。举个例子，生产者生产了1000条消息，这1000条消息会保存到journal文件，如果消费者的消费速度很快的情况 下，在journal文件还没有同步到DB之前，消费者已经消费了90%的以上的消息，那么这个时候只需要同步剩余的10%的消息到DB。  
如果消费者的消费速度很慢，这个时候journal文件可以使消息以批量方式写到DB。 
将原来的标签注释掉，添加如下标签  
```
<persistenceFactory>
       <journalPersistenceAdapterFactory dataSource="#Mysql-DS" dataDirectory="activemq-data"/>
</persistenceFactory>
```
在服务端循环发送消息。可以看到数据是延迟同步到数据库的。  
  
### 消费端消费消息的原理  
  
有两种方法可以接收消息，一种是使用同步阻塞的MessageConsumer#receive方法。另一种是使用消息监听器MessageListener。  
这里需要注意的是，在同一个session下，这两者不能同时工作，也就是说不能针对不同消息采用不同的接收方式。否则会抛出异常。  
至于为什么这么做，最大的原因还是在事务性会话中，两种消费模式的事务不好管控。 

### 消费端消费消息源码分析  
  
ActiveMQMessageConsumer.receive  
消费端同步接收消息的源码入口  
```
  public Message receive() throws JMSException {
    checkClosed();
    checkMessageListener(); //检查receive和MessageListener是否同时配置在当前的会话中 
    sendPullCommand(0); //如果PrefetchSizeSize为0并且unconsumerMessage为空，则发起pull命令 
    MessageDispatch md = dequeue(-1); //从unconsumerMessage出队列获取消息
    if (md == null) {
        return null;
    }
    beforeMessageIsConsumed(md); 
    afterMessageIsConsumed(md, false); //发送ack给到broker 
    return createActiveMQMessage(md);//获取消息并返回
}
```
sendPullCommand  
发送pull命令从broker上获取消息，前提是prefetchSize=0并且unconsumedMessages为空。  
unconsumedMessage表示未消费的消息，这里面预读取的消息大小为prefetchSize的值。
```
  protected void sendPullCommand(long timeout) throws JMSException {
    clearDeliveredList();
    if (info.getCurrentPrefetchSize() == 0 && unconsumedMessages.isEmpty()) {
       MessagePull messagePull = new MessagePull(); 
       messagePull.configure(info);
       messagePull.setTimeout(timeout); 
       session.asyncSendPacket(messagePull); //向服务端异步发送messagePull指令
     } 
 }
```
clearDeliveredList  
在上面的sendPullCommand方法中，会先调用clearDeliveredList方法，主要用来清理已经分发的消息链表 deliveredMessages。  
  
deliveredMessages，存储分发给消费者但还为应答的消息链表  
>如果session是事务的，则会遍历deliveredMessage中的消息放入到previouslyDeliveredMessage中来做重发  
>如果session是非事务的，根据ACK的模式来选择不同的应答操作
```
 private void clearDeliveredList() {
    if (clearDeliveredList) {
        synchronized (deliveredMessages) {
            if (clearDeliveredList) {
                if (!deliveredMessages.isEmpty()) {
                    if (session.isTransacted()) {
                    if (previouslyDeliveredMessages == null) {
                            previouslyDeliveredMessages = 
                        new PreviouslyDeliveredMap<MessageId,Boolean(session.getTransactionContext().getTransactionId());
                        }
                        for (MessageDispatch delivered : deliveredMessages) {
                          previouslyDeliveredMessages.put(delivered.getMessage().getMessageId(), false);
                        }
                        LOG.debug("{} tracking existing transacted {} delivered list ({}) ontransport interrupt",
                        getConsumerId(), previouslyDeliveredMessages.transactionId,deliveredMessages.size());
                    } else {
                        if (session.isClientAcknowledge()) {
        LOG.debug("{} rolling back delivered list ({}) on transport interrupt", getConsumerId(),deliveredMessages.size());
                            // allow redelivery
                             if (!this.info.isBrowser()) {
                                 for (MessageDispatch md: deliveredMessages) {
                                     this.session.connection.rollbackDuplicate(this,md.getMessage());
                                 }
                             } 
                      }
        LOG.debug("{} clearing delivered list ({}) on transport interrupt",getConsumerId(), deliveredMessages.size());
                        deliveredMessages.clear();
                        pendingAck = null;
                    }
                }
                clearDeliveredList = false;
            }
        } 
     }
}
```
dequeue  
从unconsumedMessage中取出一个消息，在创建一个消费者时，就会未这个消费者创建一个未消费的消息通道，这个通道分为两种，一种是简单优先级队列分发通道SimplePriorityMessageDispatchChannel；另一种是先进先出的分发通道FifoMessageDispatchChannel。  
至于为什么要存在这样一个消息分发通道，大家可以想象一下，如果消费者每次去消费完一个消息以后再去 broker 拿一个消息，效率是比较低的。所以通过这样的设计可以允许session能够一次性将多条消息分发给一个消费者。 默认情况下对于queue来说，prefetchSize的值是1000。  
  
beforeMessageIsConsumed  
这里面主要是做消息消费之前的一些准备工作，如果ACK类型不是DUPS_OK_ACKNOWLEDGE或者队列模式(简单来说就是除了Topic和DupAck这两种情况)，所有的消息先放到deliveredMessages链表的开头。并且如果当前是事务类型的会话，则判断transactedIndividualAck，如果为true，表示单条消息直接返回ack。
否则，调用ackLater，批量应答, client端在消费消息后暂且不发送ACK，而是把它缓存下来(pendingACK)，等到这些消息的条数达到一定阈值时，只需要通过一个ACK指令把它们全部确认。这比对每条消息都逐个确认，在性能上要提高很多。
```
  private void beforeMessageIsConsumed(MessageDispatch md) throws JMSException {
    md.setDeliverySequenceId(session.getNextDeliveryId());
    lastDeliveredSequenceId = md.getMessage().getMessageId().getBrokerSequenceId();
    if (!isAutoAcknowledgeBatch()) {
        synchronized(deliveredMessages) {
            deliveredMessages.addFirst(md);
        }
        if (session.getTransacted()) {
            if (transactedIndividualAck) {
                immediateIndividualTransactedAck(md);
            } else {
                ackLater(md, MessageAck.DELIVERED_ACK_TYPE);
            } 
         }
      } 
}
```
afterMessageIsConsumed  
这个方法的主要作用是执行应答操作，这里面做以下几个操作  
>如果消息过期，则返回消息过期的ack  
>如果是事务类型的会话，则不做任何处理  
>如果是AUTOACK或者(DUPS_OK_ACK且是队列)，并且是优化ack操作，则走批量确认ack   
>如果是DUPS_OK_ACK，则走ackLater逻辑  
>如果是CLIENT_ACK，则执行ackLater  
```
 private void afterMessageIsConsumed(MessageDispatch md, boolean messageExpired) throws JMSException {
    if (unconsumedMessages.isClosed()) {
        return;
    }
    if (messageExpired) {
        acknowledge(md, MessageAck.EXPIRED_ACK_TYPE);
        stats.getExpiredMessageCount().increment();
    } else {
    stats.onMessage();
     if (session.getTransacted()) {
            // Do nothing.
        } else if (isAutoAcknowledgeEach()) {
            if (deliveryingAcknowledgements.compareAndSet(false, true)) {
                synchronized (deliveredMessages) {
                    if (!deliveredMessages.isEmpty()) {
                        if (optimizeAcknowledge) {
                            ackCounter++;
                            // AMQ-3956 evaluate both expired and normal msgs as
                            // otherwise consumer may get stalled
                            if (ackCounter + deliveredCounter >= (info.getPrefetchSize() * .65)
                    || (optimizeAcknowledgeTimeOut > 0 && System.currentTimeMillis() >= (optimizeAckTimestamp +
                     optimizeAcknowledgeTimeOut))) {
                                MessageAck ack = makeAckForAllDeliveredMessages(MessageAck.STANDARD_ACK_TYPE);
                                if (ack != null) {
                                    deliveredMessages.clear();
                                    ackCounter = 0;
                                    session.sendAck(ack);
                                    optimizeAckTimestamp = System.currentTimeMillis();
                                }
                                // AMQ-3956 - as further optimization send
                                // ack for expired msgs when there are any.
                                // This resets the deliveredCounter to 0 so that
                                // we won't sent standard acks with every msg just
                                // because the deliveredCounter just below
                                // 0.5 * prefetch as used in ackLater()
                                if (pendingAck != null && deliveredCounter > 0) {
                                    session.sendAck(pendingAck);
                                    pendingAck = null;
                                    deliveredCounter = 0;
                                } 
                            }
                        } else {
                            MessageAck ack = makeAckForAllDeliveredMessages(MessageAck.STANDARD_ACK_TYPE);
                            if (ack!=null) {
                                deliveredMessages.clear();
                                session.sendAck(ack);
                            }
                        } 
                    }
                }
                deliveryingAcknowledgements.set(false);
            }
        } else if (isAutoAcknowledgeBatch()) {
            ackLater(md, MessageAck.STANDARD_ACK_TYPE);
        } else if (session.isClientAcknowledge()||session.isIndividualAcknowledge()) {
            boolean messageUnackedByConsumer = false;
            synchronized (deliveredMessages) {
                messageUnackedByConsumer = deliveredMessages.contains(md);
            }
 
             if (messageUnackedByConsumer) {
                ackLater(md, MessageAck.DELIVERED_ACK_TYPE);
             } 
        } else {
            throw new IllegalStateException("Invalid session state.");
      } 
   }
}
```
### 消息消费流程图  
![](https://github.com/YufeizhangRay/image/blob/master/ActiveMQ/%E6%B6%88%E6%81%AF%E6%B6%88%E8%B4%B9%E6%B5%81%E7%A8%8B.jpeg)  
  
unconsumedMessages 数据的获取过程  
来看看 ActiveMQConnectionFactory.createConnection 里面做了什么事情。
>1.动态创建一个传输协议  
>2.创建一个连接  
>3.通过transport.start()  
```
protected ActiveMQConnection createActiveMQConnection(StringuserName, String password) throws JMSException { 
      if (brokerURL == null) {
         throw new ConfigurationException("brokerURL not set."); 
      }
      ActiveMQConnection connection = null;
      try {
           Transport transport = createTransport(); 
           connection = createActiveMQConnection(transport,factoryStats); 
           connection.setUserName(userName); 
           connection.setPassword(password); 
           configureConnection(connection); 
           transport.start();
           if (clientID != null) {
              connection.setDefaultClientID(clientID); 
           }
       return connection;
 }
 ```                                         
transport.start()  
我们前面在分析消息发送的时候，已经知道 transport 是一个链式的调用，是一个多层包装的对象。  
```
ResponseCorrelator(MutexTransport(WireFormatNegotiator(InactivityMon itor(TcpTransport())))
```
最终调用 TcpTransport.start()方法，然而这个类中并没有 start，而是在父类 ServiceSupport.start()中。  
```
public void start() throws Exception {
if (started.compareAndSet(false, true)) {
       boolean success = false;
       stopped.set(false);
       try {
           preStart();
           doStart();
           success = true;
      } finally {
           started.set(success);
}
for(ServiceListener l:this.serviceListeners) {
           l.started(this);
      }
    }
}
```
通信层都是独立来实现及解耦的, ActiveMQ 也是一样，提供了 Transport 接口和 TransportSupport 类。这个接口的主要作用是为了让客户端有消息被异步发送、同步发送和被消费的能力。接下来沿着 doStart()往下看，又调用 TcpTransport.doStart() ，接着通过 super.doStart()，调用 TransportThreadSupport.doStart()创建了一个线程，传入的是 this，调用子类的 run 方法，也就是 TcpTransport.run()。  
  
TcpTransport.run  
run 方法主要是从 socket 中读取数据包，只要 TcpTransport 没有停止，它就会不断去调用 doRun。 
```
   public void run() {
      LOG.trace("TCP consumer thread for " + this + " starting");
      this.runnerThread=Thread.currentThread();
      try {
        while (!isStopped()) {
          doRun();
        }
      } catch (IOException e) {
        stoppedLatch.get().countDown();
        onException(e);
      } catch (Throwable e){
        stoppedLatch.get().countDown();
        IOException ioe = new IOException("Unexpected error occurred: "+ e);
        ioe.initCause(e);
        onException(ioe);
      }finally {
      stoppedLatch.get().countDown();
      }
   }
```
TcpTransport.doRun
doRun 中，通过 readCommand 去读取数据  
```
  protected void doRun() throws IOException {
      try {
        Object command = readCommand();
        doConsume(command);
      } catch (SocketTimeoutException e) {
      } catch (InterruptedIOException e) {
      }
  }
```
TcpTransport.readCommand  
这里面，通过 wireFormat 对数据进行格式化，可以认为这是一个反序列化过程。wireFormat 默认实现是 OpenWireFormat，activeMQ 自定义的跨语言的 wire 协议
```
protected Object readCommand() throws IOException {
      return wireFormat.unmarshal(dataIn);
}
```
分析到这，差不多明白了传输层的主要工作是获得数据并且把数据转换为对象，再把对象对象传给 ActiveMQConnection  

TransportSupport.doConsume  
TransportSupport 类中最重要的方法是 doConsume，它的作用就是用来“消费消息”
```
    public void doConsume(Object command) {
      if (command != null) {
        if (transportListener != null) {
          transportListener.onCommand(command);
        } else {
           LOG.error("No transportListener available to processinbound command: " + command);
        }
      }
    }
```
TransportSupport 类中唯一的成员变量是 TransportListener transportListener，这也意味着一个 Transport 支持类绑定一个传送监听器类，传送监听器接口 TransportListener 最重要的方法就是 void onCommand(Object command)，它用来处理命令，这个 transportListener 是在哪里赋值的呢?再回到 ActiveMQConnection 的构造方法中。
传递了 ActiveMQConnection 自己本身，(ActiveMQConnection 是 TransportListener 接口的实现类之一) 于是，消息就这样从传送层到达了我们的连接层上。
```
protected ActiveMQConnection(final Transport transport, IdGenerator clientIdGenerator, 
      IdGenerator connectionIdGenerator, JMSStatsImpl factoryStats) throws Exception {
      this.transport = transport; 绑定传输对象
      this.clientIdGenerator = clientIdGenerator;
      this.factoryStats = factoryStats;
      // Configure a single threaded executor who's core thread can timeout if idle
      executor = new ThreadPoolExecutor(1, 1, 5, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), 
      new ThreadFactory() {
           @Override
           public Thread newThread(Runnable r) {
                 Thread thread = new Thread(r, "ActiveMQ Connection Executor: " + transport);
                 thread.setDaemon(true);
             return thread;
           }
      });
      // asyncConnectionThread.allowCoreThreadTimeOut(true);
      String uniqueId = connectionIdGenerator.generateId();
      this.info = new ConnectionInfo(new ConnectionId(uniqueId));
      this.info.setManageable(true);
      this.info.setFaultTolerant(transport.isFaultTolerant());
      this.connectionSessionId = new SessionId(info.getConnectionId(), -1);
      this.transport.setTransportListener(this); //当 transport 绑定为自 己
      this.stats = new JMSConnectionStatsImpl(sessions, this instanceof XAConnection);
      this.factoryStats.addConnection(this);
      this.timeCreated = System.currentTimeMillis();
      this.connectionAudit.setCheckForDuplicates(transport.isFaultTolerant());
}
```
从构造函数可以看出，创建 ActiveMQConnection 对象时，除了和 Transport 相互绑定，还对线程池执行器 executor 进行了初始化。下面我们看看该类的核心方法。  
  
onCommand  
这里面会针对不同的消息做分发，比如传入的command是 MessageDispatch，那么这个 command 的 visit 方法就会调用 processMessageDispatch 方法。
```
 public void onCommand(final Object o) {
      final Command command = (Command)o;
      if (!closed.get() && command != null) {
        try {
          command.visit(new CommandVisitorAdapter() {
            @Override
            public Response processMessageDispatch(MessageDispatchmd) throws Exception {
              // 等待 Transport 中断处理完成
              waitForTransportInterruptionProcessingToComplete(); // 这里通过消费者 ID 来获取消费者对象
            //(ActiveMQMessageConsumer 实现了 ActiveMQDispatcher 接口)，所以 MessageDispatch 包含了消息应该被分配到那个消费者的映射信息
            //在创建 MessageConsumer 的时候，调用 ActiveMQMessageConsumer 的第 282 行，调用 ActiveMQSession 的 1798 行将当前的消费者绑定到 dispatchers 中，所以这里拿到的是 ActiveMQSession
              ActiveMQDispatcher dispatcher = dispatchers.get(md.getConsumerId());
              if (dispatcher != null) {
                 Message msg = md.getMessage();
                 if (msg != null) {
                     msg = msg.copy();
                     msg.setReadOnlyBody(true); 
                     msg.setReadOnlyProperties(true); 
                     msg.setRedeliveryCounter(md.getRedeliveryCounter());
                     msg.setConnection(ActiveMQConnection.this)
                     msg.setMemoryUsage(null);
                     md.setMessage(msg);
                 }
                 dispatcher.dispatch(md); // 调用会话 ActiveMQSession 自己的 dispatch 方法来处理这条消息
                 } else {
                     LOG.debug("{} no dispatcher for {} in {}", this, md, dispatchers);
                 }
                 return null;
               }
               //如果传入的是 ProducerAck，则调用的是下面这个方法，这里我 们仅仅关注 MessageDispatch 就行了
               @Override
               public Response processProducerAck(ProducerAck pa) throws Exception {
                  if (pa != null && pa.getProducerId() != null) {
                      ActiveMQMessageProducer producer = producers.get(pa.getProducerId());
                      if (producer != null) {
                          producer.onProducerAck(pa);
                     }
                  }
            return null;
}
//...后续的方法就不分析了
```
在现在这个场景中，我们只关注 processMessageDispatch 方法，在这个方法中，只是简单的去调用 ActiveMQSession 的 dispatch 方法来处理消息。  
tips: command.visit, 这里使用了适配器模式，如果 command 是一个 MessageDispatch，那么它就会调用 processMessageDispatch 方法，其他方法他不会关心，代码如下:  
MessageDispatch.visit  
```
   @Override
   public Response visit(CommandVisitor visitor) throws Exception {
      return visitor.processMessageDispatch(this);
   }
```
ActiveMQSession.dispatch(md)  
executor 这个对象其实是一个成员对象 ActiveMQSessionExecutor，专门负责来处理消息分发。  
```
  @Override
  public void dispatch(MessageDispatch messageDispatch) {
      try {
           executor.execute(messageDispatch);
      } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          connection.onClientInternalException(e);
      }
  }
```
ActiveMQSessionExecutor.execute  
这个方法的核心功能就是处理消息的分发。  
```
void execute(MessageDispatch message) throws InterruptedException {
     if (!startedOrWarnedThatNotStarted) {
         ActiveMQConnection connection = session.connection;
         long aboutUnstartedConnectionTimeout = connection.getWarnAboutUnstartedConnectionTimeout();
         if (connection.isStarted() || aboutUnstartedConnectionTimeout < 0L) {
               startedOrWarnedThatNotStarted = true; 
         } else {
               long elapsedTime = System.currentTimeMillis() - connection.getTimeCreated();
               // lets only warn when a significant amount of time has passed
               // just in case its normal operation
               if (elapsedTime > aboutUnstartedConnectionTimeout) {
               LOG.warn("Received a message on a connection which is not yet started. 
               Have you forgotten to call Connection.start()? Connection: " + connection
               + " Received: " + message); startedOrWarnedThatNotStarted = true;
               }
         }
     }
     //如果会话不是异步分发并且没有使用 sessionpool 分发，则调用 dispatch 发送消 息(这里不展开)
      if (!session.isSessionAsyncDispatch() && !dispatchedBySessionPool) {
               dispatch(message);
      } else {
      //将消息直接放到队列里 messageQueue.enqueue(message); wakeup();
      }
} 
```
默认是采用异步消息分发。所以，直接调用 messageQueue.enqueue，把消息放到队列中，并且调用 wakeup 方法。
   
异步分发的流程  
```
public void wakeup() {
   if (!dispatchedBySessionPool) { //进一步验证
      if (session.isSessionAsyncDispatch()) { //判断 session 是否为异步分发
          try {
             TaskRunner taskRunner = this.taskRunner;
                 if (taskRunner == null) {
                    synchronized (this) { 
                       if (this.taskRunner == null) {
                          if (!isRunning()) {
                              // stop has been called
                             return; 
                          }
             //通过 TaskRunnerFactory 创建了一个任务运行类 taskRunner，这里把自己作为一 个 task 传入到 createTaskRunner 中，说明当前
             //的类一定是实现了 Task 接口的. 简单来说，就是通过线程池去执行一个任务，完成异步调度。
                            this.taskRunner = session.connection.getSessionTaskRunner().createTaskRunner(this,
                                 "ActiveMQ Session: " + session.getSessionId());
                       }
                       taskRunner = this.taskRunner;
                    }
                 }
              taskRunner.wakeup();
          } catch (InterruptedException e) { Thread.currentThread().interrupt();
       }
    } else {
       while (iterate()) { //同步分发 }
    } 
   }
}
```
所以，对于异步分发的方式，会调用 ActiveMQSessionExecutor 中的 iterate 方法，我们来看看这个方法的代码  
  
iterate  
这个方法里面做两个事  
>1.把消费者监听的所有消息转存到待消费队列中  
>2.如果messageQueue还存在遗留消息，同样把消息分发出去  
```
public boolean iterate() {
      // Deliver any messages queued on the consumer to their listeners.
    for (ActiveMQMessageConsumer consumer : this.session.consumers) {
       if (consumer.iterate()) {
         return true;
       }
    }
      // No messages left queued on the listeners.. so now dispatch messages
      // queued on the session
    MessageDispatch message = messageQueue.dequeueNoWait();
    if (message == null) {
      return false;
    } else {
      dispatch(message);
      return !messageQueue.isEmpty();
    }
}
```
ActiveMQMessageConsumer.iterate  
```
public boolean iterate() {
   MessageListener listener = this.messageListener.get();
   if (listener != null) {
      MessageDispatch md = unconsumedMessages.dequeueNoWait();
      if (md != null) {
         dispatch(md);
         return true;
      }
   }
   return false;
}
  ``` 
同步分发的流程  
同步分发的流程，直接调用 ActiveMQSessionExcutor 中的 dispatch 方法，代码如下  
```
void dispatch(MessageDispatch message) {
 // TODO - we should use a Map for this indexed by consumerId
   for (ActiveMQMessageConsumer consumer : this.session.consumers) {
      ConsumerId consumerId = message.getConsumerId();
      if (consumerId.equals(consumer.getConsumerId())) {
         consumer.dispatch(message);
         break;
      }
    }
 }
 ```
ActiveMQMessageConsumer.dispatch  
调用 ActiveMQMessageConsumer.dispatch 方法，把消息转存到 unconsumedMessages 消息队列中。  
```
public void dispatch(MessageDispatch md) {
   MessageListener listener = this.messageListener.get();
   try {
      clearMessagesInProgress();
      clearDeliveredList();
       synchronized (unconsumedMessages.getMutex()) {
         if (!unconsumedMessages.isClosed()) { 
              if (this.info.isBrowser() || !session.connection.isDuplicate(this, md.getMessage())) { 
                  if (listener != null && unconsumedMessages.isRunning()) {
                      if (redeliveryExceeded(md)) {
                          posionAck(md, "listener dispatch[" + md.getRedeliveryCounter() + "] to " + getConsumerId() + 
                           " exceeds redelivery policy limit:" + redeliveryPolicy); 
                            return;
                      }
                      ActiveMQMessage message = createActiveMQMessage(md);
                      beforeMessageIsConsumed(md);
                      try {
                          boolean expired = isConsumerExpiryCheckEnabled() && message.isExpired();
                          if (!expired) {
                              listener.onMessage(message);
                          }
                          afterMessageIsConsumed(md, expired);
                      } catch (RuntimeException e) {
            LOG.error("{} Exception while processing message: {}", getConsumerId(), md.getMessage().getMessageId(), e);
                     md.setRollbackCause(e);
                     if (isAutoAcknowledgeBatch() || isAutoAcknowledgeEach() || session.isIndividualAcknowledge()) {
                         // schedual redelivery and possible dlq processing the next message.
                     rollback();
                     } else {
                      // Transacted or Client ack: Deliver
                      afterMessageIsConsumed(md, false);
                     }
                  }
              } else {
                  delivered
                  md.getMessage());
              }
              if (!unconsumedMessages.isRunning()) {
              // delayed redelivery, ensure it can be re
                 session.connection.rollbackDuplicate(this,
                 if (md.getMessage() == null) {
                    // End of browse or pull request timeout. unconsumedMessages.enqueue(md);
                 } else {
                        if (!consumeExpiredMessage(md)) { 
                             unconsumedMessages.enqueue(md); 
                             if (availableListener != null) {
                                      availableListener.onMessageAvailable(this);
                              }
                        } else {
                              beforeMessageIsConsumed(md); 
                              afterMessageIsConsumed(md, true);
                        // Pull consumer needs to check if pull
                        // a new pull command if not.
                        if (info.getCurrentPrefetchSize() == 0)
                            unconsumedMessages.enqueue(null);
                        }
                 }
               }
            }
         } else {
             // deal with duplicate delivery
            ConsumerId consumerWithPendingTransaction;
              if (redeliveryExpectedInCurrentTransaction(md,true)) {
                   LOG.debug("{} tracking transacted redelivery {}", getConsumerId(), md.getMessage());
                    if (transactedIndividualAck) { 
                         immediateIndividualTransactedAck(md);
                     } else {
                          session.sendAck(new MessageAck(md,MessageAck.DELIVERED_ACK_TYPE, 1));
                     }
               } else if ((consumerWithPendingTransaction = redeliveryPendingInCompetingTransaction(md)) != null) {
         LOG.warn("{} delivering duplicate {}, pending transaction completion on {} will rollback", getConsumerId(),
md.getMessage(), consumerWithPendingTransaction); 
               session.getConnection().rollbackDuplicate(this, md.getMessage());
                      dispatch(md);
                 } else {
                LOG.warn("{} suppressing duplicate delivery on connection, poison acking: {}", getConsumerId(), md);
                posionAck(md, "Suppressing duplicate delivery on connection, consumer " + getConsumerId());
                }
          }
       }
    }
    if (++dispatchedCount % 1000 == 0) {
      dispatchedCount = 0;
      Thread.yield();
    }
   } catch (Exception e) {
      session.connection.onClientInternalException(e);
   }
}
 ```
到这里为止，消息如何接受以及他的处理方式的流程，我们已经搞清楚了。  

#### 消费端的PrefetchSize  
  
原理剖析  
activemq 的 consumer 端也有窗口机制，通过 prefetchSize 就可以设置窗口大小。不同的类型的队列，prefetchSize 的默认值也是不一样的  
>持久化队列和非持久化队列的默认值为 1000   
>持久化topic默认值为100  
  
消费端会根据 prefetchSize 的大小批量获取数据，比如默认值是 1000，那么消费端会预先加载 1000 条数据到本地的内存中。  
  
prefetchSize 的设置方法  
在 createQueue 中添加 consumer.prefetchSize，就可以看到效果  
```
Destination destination=session.createQueue("myQueue?consumer.prefetchSize=10");
```
既然有批量加载，那么一定有批量确认，这样才算是彻底的优化。  
  
optimizeAcknowledge  
ActiveMQ 提供了 optimizeAcknowledge 来优化确认，它表示是否开启“优化 ACK”，只有在为 true 的情况下，prefetchSize 以及 optimizeAcknowledgeTimeout 参数才会有意义。  
优化确认一方面可以减轻 client 负担(不需要频繁的确认消息)、减少通信开销，另一方面由于延迟了确认(默认 ack 了 0.65*prefetchSize 个消息才确认)，broker 再次发送消息时又可以批量发送。
如果只是开启了 prefetchSize，每条消息都去确认的话，broker 在收到确认后也只是发送一条消息，并不是批量发布，当然也可以通过设置 DUPS_OK_ACK 来手动延迟确认，我们需要在 brokerUrl 指定 optimizeACK 选项。
  ```
ConnectionFactory connectionFactory= new ActiveMQConnectionFactory ("tcp://192.168.138.188:61616?jms.optimizeAcknowledge=true&jms.optimiz eAcknowledgeTimeOut=10000");
 ```
注意，如果optimizeAcknowledge为true，那么prefetchSize必须大于0. 当 prefetchSize=0 的时候，表示 consumer 通过 PULL 方式从 broker 获取消息。  
   
到目前为止，我们知道了 optimizeAcknowledge 和 prefetchSize 的作用，两者协同工作，通过批量获取消息、并延迟批量确认，来达到一个高效的消息消
费模型。它比仅减少了客户端在获取消息时的阻塞次数，还能减少每次获取消息时的网络通信开销。  
需要注意的是，如果消费端的消费速度比较高，通过这两者组合是能大大提升 consumer 的性能。如果 consumer 的消费性能本身就比较慢，设置比较大的 prefetchSize 反而不能有效的达到提升消费性能的目的。因为过大的prefetchSize 不利于 consumer 端消息的负载均衡。通常情况下，我们都会部署多个 consumer 节点来提升消费端的消费性能。  
这个优化方案还会存在另外一个潜在风险，当消息被消费之后还没有来得及确认时，client 端发生故障，那么这些消息就有可能会被重新发送给其他consumer，那么这种风险就需要 client 端能够容忍“重复”消息。  
  
消息的确认过程  
  
ACK_MODE  
通过前面的源码分析，基本上已经知道了消息的消费过程，以及消息的批量获取和批量确认，那么接下来再了解下消息的确认过程。
从第一节课的学习过程中，我们知道，消息确认有四种 ACK_MODE，分别是 
>AUTO_ACKNOWLEDGE = 1 自动确认  
>CLIENT_ACKNOWLEDGE = 2 客户端手动确认  
>DUPS_OK_ACKNOWLEDGE = 3 自动批量确认  
>SESSION_TRANSACTED = 0 事务提交并确认  
  
虽然 Client 端指定了 ACK 模式,但是在 Client 与 broker 在交换 ACK 指令的时 候，还需要告知 ACK_TYPE。ACK_TYPE 表示此确认指令的类型，不同的
ACK_TYPE 将传递着消息的状态，broker 可以根据不同的 ACK_TYPE 对消息进行不同的操作。  
  
ACK_TYPE  
>DELIVERED_ACK_TYPE = 0 消息"已接收"，但尚未处理结束。   
>POSION_ACK_TYPE = 1 消息"错误",通常表示"抛弃"此消息，比如消息重发多次后，都无法正确处理时，消息将会被删除或者 DLQ(死信队列)。  
>STANDARD_ACK_TYPE = 2 "标准"类型,通常表示为消息"处理成功"，broker 端可以删除消息了。  
>REDELIVERED_ACK_TYPE = 3 消息需"重发"，比如 consumer 处理消息时抛出了异常，broker 稍后会重新发送此消息。  
>INDIVIDUAL_ACK_TYPE = 4 表示只确认"单条消息",无论在任何 ACK_MODE 下。  
>UNMATCHED_ACK_TYPE = 5 在 Topic 中，如果一条消息在转发给“订阅者”时，发现此消息不符合 Selector 过滤条件，那么此消息将 不会转发给订阅者，消息将会被存储引擎删除(相当于在 Broker 上确认了消息)。  
  
Client 端在不同的 ACK 模式时,将意味着在不同的时机发送 ACK 指令,每个 ACK Command 中会包含 ACK_TYPE,那么 broker 端就可以根据 ACK_TYPE 来决定
此消息的后续操作。  
  
消息的重发机制原理  
  
消息重发的情况  
在正常情况下，有几中情况会导致消息重新发送  
>1.在事务性会话中，没有调用session.commit确认消息或者调用session.rollback 方法回滚消息。  
>2.在非事务性会话中，ACK模式为CLIENT_ACKNOWLEDGE的情况下，没有调用 acknowledge 或者调用了 recover 方法;  
  
一个消息被 redelivedred 超过默认的最大重发次数(默认 6 次)时，消费端会给 broker 发送一个”poison ack”，表示这个消息有毒，告诉 broker 不要再发了。这个时候 broker 会把这个消息放到 DLQ(死信队列)。  
  
死信队列  
ActiveMQ 中默认的死信队列是 ActiveMQ.DLQ，如果没有特别的配置，有毒的消息都会被发送到这个队列。默认情况下，如果持久消息过期以后，也会被送到 DLQ 中。  
  
死信队列配置策略  
缺省所有队列的死信消息都被发送到同一个缺省死信队列，不便于管理，可以通过 individualDeadLetterStrategy 或 sharedDeadLetterStrategy 策略来进 行修改。  
```
<destinationPolicy>
  <policyMap>
    <policyEntries>
      <policyEntry topic=">" >
        <pendingMessageLimitStrategy>
          <constantPendingMessageLimitStrategy limit="1000"/>
        </pendingMessageLimitStrategy>
      </policyEntry>
      // “>”表示对所有队列生效，如果需要设置指定队列，则直接写队列名称 
      <policyEntry queue=">">
        <deadLetterStrategy>
          //queuePrefix:设置死信队列前缀
          //useQueueForQueueMessage 设置队列保存到死信。
          <individualDeadLetterStrategy queuePrefix="DLQ."useQueueForQueueMessages="true"/>
        </deadLetterStrategy>
      </policyEntry>
    </policyEntries>
  </policyMap>
</destinationPolicy>
```
自动丢弃过期消息
```
<deadLetterStrategy>
      <sharedDeadLetterStrategy processExpired="false" />
</deadLetterStrategy>
```
死信队列的再次消费  
当定位到消息不能消费的原因后，就可以在解决掉这个问题之后，再次消费死信队列中的消息，因为死信队列仍然是一个队列。  
  
### ActiveMQ静态网络配置配置说明  
  
修改 activeMQ 服务器的 activeMQ.xml，增加如下配置
```
<networkConnectors>
      <networkConnector uri="static://(tcp://192.168.188.138:61616,tcp://192.168.188.139:61616)"/>
</networkConnectors>
```
两个 Brokers 通过一个 static 的协议来进行网络连接。一个 Consumer 连接到 BrokerB 的一个地址上，当 Producer 在 BrokerA 上以相同的地址发送消息 是，此时消息会被转移到 BrokerB 上，也就是说 BrokerA 会转发消息到 BrokerB 上  
  
消息回流  
replayWhenNoConsumers 属性可以用来解决当 broker1 上有需要转发的消息但是没有消费者时，把消息回流到它原始的 broker。同时把 enableAudit 设置为 false，为了防止消息回流后被当作重复消息而不被分发。  
通过如下配置，在 activeMQ.xml 中。 分别在两台服务器都配置。即可完成消 息回流处理
```
<policyEntry queue=">" enableAudit="false">
    <networkBridgeFilterFactory>
      <conditionalNetworkBridgeFilterFactory replayWhenNoConsumers="true"/>
    </networkBridgeFilterFactory>
</policyEntry>
```
动态网络连接  
ActiveMQ 使用 Multicast 协议将一个 Service 和其他的 Broker 的 Service 连 接起来。Multicast 能够自动的发现其他 broker，从而替代了使用 static 功能 列表 brokers。用 multicast 协议可以在网络中频繁
multicast://ipadaddress:port?transportOptions  
