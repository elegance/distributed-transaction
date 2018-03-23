# 使用Event-Sourcing 与 MQ 实现分布式事务控制

#### How to run

1. 环境 mysql, rabbitmq, 这里使用的是docker（如果已经有了就可以跳过这一步了）

   ```bash
   # rabbitmq
   docker run -d -p 15672:15672 -p 5672:5672 -e RABBITMQ_DEFAULT_USER=admin -e RABBITMQ_DEFAULT_PASS=admin --name rabbitmq rabbitmq:3-management

   # mysql
   docker run --name mysql -e MYSQL_ROOT_PASSWORD=root -d mysql
   ```

2. 导入表结构[src/main/resources/ddl.sql](src/main/resources/ddl.sql) 到mysql

3. 修改spring boot 配置文件 `application.yml` 数据源，RabbitMQ 地址分别配置下

4. 启动

   * 启动 `FooApplication`
   * 设置启动参数`--server.port=8081` ，启动 `BarApplication`（因为这两个Application是在一个工程里面，默认都使用8080会有冲突）

#### 什么是Event-Sourcing?

它是一种基于“事件溯源”的解决方案，一般将它应用在领域对象模型中。一个对象从创建到销毁的整个生命周期中，会产生大量的事件(Event)，然而每种事件都有自己的事件类型(Event Type)。例如，“创建”是一种事件类型，而“在某时刻创建一个产品”则是一个事件。

一般，我们只将对象的最终状态记录到数据库中，而不会去记录每个对象的历史事件变更。**Event-Sourcing 要求我们记录对象状态的同时，还要讲记录对象所发生的一系列事件**. 也就是说数据库中包含了对象所属的模型表，还包含对象产生事件的事件表。当创建、修改、删除一条模型对象时，要将此对象插入模型表，还要记录一条对应的事件到事件表中，这个过程称为“事件记录”。

事件大体包括以下字段：

* ID: 表示EventID, 具备唯一性
* Event Type: 表示事件类型，例如 CREATE, UPDATE, DELETE
* Model Name: 表示模型名称，如Foo, Bar等
* Model ID： 表示对应模型对象ID，具备唯一性
* Create Time： 表示创建事件的具体事件，一般精确到毫秒

#### Event Sourcing 与 MQ 实现分布式事务

例如我们有： Foo Service 与 Bar Service 两个服务，他分别在不同的进程中。首先Foo Service 插入一条 Foo对象到 Foo Table 中，随后通过 MQ 的方式去调用 Bar Service，最后 Bar Service 也插入一条 Bar 对象到 Bar Table中，这是一个经典的分布式调用场景。

若Bar Service 在插入 Bar对象时出现了异常，首先需要回滚自己的事务，同时 Foo Service 也要回滚曾经所提交的事务，这两个事务时分布式的，还要确保原子性。

在解决问题前，我们先定义一下几个类：

* Event：用于封装事件相关字段
* EventType: 用于定义各种事件类型
* EventManager: 用于封装事件相关操作

**第一步：操作模型表与事件表，并将事件写入消息队列。**

在 Foo Service 中插入一条 Foo 对象到 Foo Table中，同时创建一个名为 “CREATE Foo” 的事件到 Event Table中，者两个 JDBC 的操作很容易确保在同一个事务中提交。此外在事务提交前，可将插入 Event Table 中的 Event 对象插入 Success Queue（成功队列）中。这个过程实际包括以下四个任务：

1. 将 Foo 对象插入模型表中
2. 创建一个 Event 对象
3. 将 Event 对象插入事件表中
4. 将 Event 对象写入成功队列中

**第二步：从消息队列中获取事件，并操作模型表，若有异常，则将源事件再次写入消息队列。**

对于 Success Queue而言，Bar Service 是一个消费者，它将从 Success Queue 中获取 Event 对象，并完成自己的业务逻辑，即插入一个 Bar 对象到 Bar Table 中。但如果，在完成业务操作的过程中，发生异常，此时将从 Success Queue 中接收到的 Event 对象写入 Failure Queue（失败队列）。这个过程实际包括以下四个任务：

1. 从成功队列中获取 Event 对象
2. 将 Bar 对象插入模型表中
3. 故意抛出异常
4. 将 Event 对象写入失败队列中

**第三步：从消息列表中获取事件，操作事件表与模型表（即 “事件溯源” 过程）**

此时，Foo Service 不再是消息生产者，而是消息消费者，它需要从 Failure Queue 中取回曾经发送到 Success Queue 的 Event 对象，并根据 Event ID 到 Event Table 中查询出 Foo ID,随后根据 Foo ID 到 Foo Table中删除曾经创建的Foo对象。这个过程实际包括以下三个任务：

1. 从失败队列中取得 Event 对象
2. 根据 Event ID 事件表中获取 Foo ID
3. 根据 Foo ID 从模型中删除对应的记录