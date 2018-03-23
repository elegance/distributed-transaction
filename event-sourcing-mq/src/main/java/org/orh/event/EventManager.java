package org.orh.event;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

public class EventManager {
    /**
     * jdbcTemplate 用于向 Event Table 插入数据
     */
    private final JdbcTemplate jdbcTemplate;

    /**
     * rabbitTemplate 用于向 RabbitMQ 发送消息
     */
    private final RabbitTemplate rabbitTemplate;

    public EventManager(JdbcTemplate jdbcTemplate, RabbitTemplate rabbitTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.rabbitTemplate = rabbitTemplate;
    }

    public void insertEvent(Event event) {
        jdbcTemplate.update("INSERT INTO event (id, event_type, model_name, model_id, created_time)" +
                        "values(?, ?, ?, ?, ?)",
                event.getId(), event.getEventType().toString(), event.getModelName(), event.getModelId(), event.getCreatedTime());
    }

    public String queryModelId(String eventId) {
        return jdbcTemplate.queryForObject(
                "SELECT model_id FROM event where id = ?",
                String.class, eventId);
    }

    public void sendEventQueue(String queueName, Event event) {
        rabbitTemplate.convertAndSend(queueName, event);
    }
}
