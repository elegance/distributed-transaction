package org.orh.foo.service;

import org.orh.event.Event;
import org.orh.event.EventManager;
import org.orh.event.EventType;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class FooService {

    private JdbcTemplate jdbcTemplate;
    private EventManager eventManager;

    @Autowired
    public FooService(JdbcTemplate jdbcTemplate, EventManager eventManager) {
        this.jdbcTemplate = jdbcTemplate;
        this.eventManager = eventManager;
    }

    @Transactional
    public void insertFoo(String name) {
        String fooId = UUID.randomUUID().toString();
        try {
            // 将 Foo 对象插入模型表中
            jdbcTemplate.update("INSERT INTO foo(id, name) VALUES(?, ?)", fooId, name);
        } finally {
            // 创建 一个Event对象
            Event event = new Event(EventType.CREATE, "Foo", fooId);

            // 插入 Event 事件表
            eventManager.insertEvent(event);

            // event 写入成功队列
            eventManager.sendEventQueue("foo-success-queue", event);
        }
    }

    @Transactional
    @RabbitListener(queues = "bar-failure-queue")
    public void handleBarFailure(Event event) {
        // 根据 Event ID 从事件表中 获取 Foo ID
        String fooId = eventManager.queryModelId(event.getId());

        // 根据 Foo ID 从模型表中删除对应记录
        jdbcTemplate.update("DELETE FROM foo where id = ?", fooId);
    }
}
