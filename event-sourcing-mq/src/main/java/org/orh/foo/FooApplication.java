package org.orh.foo;

import org.orh.event.EventManager;
import org.orh.foo.service.FooService;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootApplication
public class FooApplication {

    @Bean
    public Queue successQueue() {
        return new Queue("foo-success-queue");
    }

    @Bean
    public Queue failureQueue() {
        return new Queue("bar-failure-queue");
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public EventManager eventManager(JdbcTemplate jdbcTemplate, RabbitTemplate rabbitTemplate) {
        return new EventManager(jdbcTemplate, rabbitTemplate);
    }

    public static void main(String[] args) {
        SpringApplication.run(FooApplication.class, args);
    }
}
