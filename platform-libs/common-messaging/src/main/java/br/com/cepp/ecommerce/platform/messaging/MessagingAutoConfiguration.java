package br.com.cepp.ecommerce.platform.messaging;

import br.com.cepp.ecommerce.platform.domain.event.Topics;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.util.backoff.ExponentialBackOff;
import tools.jackson.databind.json.JsonMapper;

@AutoConfiguration(afterName = {
        "org.springframework.boot.jdbc.autoconfigure.JdbcTemplateAutoConfiguration",
        "org.springframework.boot.kafka.autoconfigure.KafkaAutoConfiguration"})
@EnableScheduling
public class MessagingAutoConfiguration {

    @Bean
    @ConditionalOnBean(JdbcTemplate.class)
    @ConditionalOnMissingBean
    OutboxRecorder outboxRecorder(JdbcTemplate jdbc, JsonMapper json) {
        return new OutboxRecorder(jdbc, json);
    }

    @Bean
    @ConditionalOnBean(JdbcTemplate.class)
    @ConditionalOnMissingBean
    ProcessedMessages processedMessages(JdbcTemplate jdbc) {
        return new ProcessedMessages(jdbc);
    }

    @Bean
    @ConditionalOnBean({JdbcTemplate.class, KafkaTemplate.class})
    @ConditionalOnProperty(name = "messaging.outbox.relay.enabled", havingValue = "true", matchIfMissing = true)
    OutboxRelay outboxRelay(JdbcTemplate jdbc, KafkaTemplate<String, String> kafka,
                            @Value("${messaging.outbox.relay.batch-size:50}") int batchSize) {
        return new OutboxRelay(jdbc, kafka, batchSize);
    }

    /** Retries with exponential backoff, then sends the record to <topic>.dlt. */
    @Bean
    @ConditionalOnBean(KafkaTemplate.class)
    @ConditionalOnMissingBean
    DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, String> kafka) {
        var recoverer = new DeadLetterPublishingRecoverer(kafka,
                (record, ex) -> new TopicPartition(Topics.dlt(record.topic()), -1));
        var backOff = new ExponentialBackOff(1000L, 2.0);
        backOff.setMaxAttempts(5);
        return new DefaultErrorHandler(recoverer, backOff);
    }
}