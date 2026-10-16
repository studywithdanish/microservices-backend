package com.danish.blog.notification.config;

import com.danish.blog.notification.observability.NotificationMetrics;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic postPublishedTopic(@Value("${app.kafka.topics.post-published}") String topic) {
        return TopicBuilder.name(topic).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic postPublishedDeadLetterTopic(
            @Value("${app.kafka.topics.post-published-dlt}") String topic
    ) {
        return TopicBuilder.name(topic).partitions(3).replicas(1).build();
    }

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(
            KafkaTemplate<String, String> kafkaTemplate,
            @Value("${app.kafka.topics.post-published-dlt}") String deadLetterTopic,
            NotificationMetrics metrics
    ) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, exception) -> new TopicPartition(deadLetterTopic, record.partition())
        );
        DefaultErrorHandler errorHandler = new DefaultErrorHandler((record, exception) -> {
            metrics.recordDeadLetter();
            recoverer.accept(record, exception);
        }, new FixedBackOff(1000L, 2L));
        errorHandler.setRetryListeners((record, exception, deliveryAttempt) -> metrics.recordRetry());
        errorHandler.addNotRetryableExceptions(IllegalArgumentException.class);
        return errorHandler;
    }
}
