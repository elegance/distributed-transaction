package org.orh.bar.service;

import org.orh.event.Event;
import org.orh.event.EventManager;
import org.orh.event.EventType;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class BarService {
    private JdbcTemplate jdbcTemplate;
    private EventManager eventManager;

    @Autowired
    public BarService(JdbcTemplate jdbcTemplate, EventManager eventManager) {
        this.jdbcTemplate = jdbcTemplate;
        this.eventManager = eventManager;
    }

    @Transactional
    @RabbitListener(queues = "foo-success-queue")
    public void handlerFooSuccess(Event event) {
        String barId = UUID.randomUUID().toString();
        try {
            // 将 Bar 对象插入模型表中
            jdbcTemplate.update("INSERT INTO bar(id, name) VALUES(?, ?)", barId, "bar");

            // 故意抛出异常
            throw new RuntimeException();
        } catch (Exception e) {
            // 将 Event 对象写入失败队列中
            eventManager.sendEventQueue("bar-failure-queue", event);

            // 让事务回滚
            // 如果不抛出 AmqpRejectAndDontRequeueException, Event 对象将重新进入 foo-success-queue 队列
            // 再次调用 handlerFooSuccess 方法，重复不断循环，知道达到 RabbitMQ 限制的 Requeue 次数。
            // 该消息进入 DLQ(Dead Letter Queue 死信队列)，其中的消息被丢弃，永不被消费。
            throw new AmqpRejectAndDontRequeueException(e);
        }
    }
}
