package com.zst.zsearch.streaming.dlq;

import com.alibaba.fastjson2.JSONObject;
import com.zst.zsearch.streaming.model.IngestRecord;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * DLQ 生产器：同步发送，失败只记日志不阻塞主链路。
 * 消息体包含原始 payload 与来源定位，headers 便于按错误类型筛选。
 */
public class DlqProducer implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(DlqProducer.class);
    private static final int MAX_ERROR_MESSAGE_LENGTH = 500;

    private final DlqConfig config;
    private final KafkaProducer<String, String> producer;

    public DlqProducer(DlqConfig config) {
        if (config == null || config.getTopic() == null || config.getTopic().isBlank()) {
            throw new IllegalArgumentException("DLQ topic 不能为空");
        }
        this.config = config;

        Properties properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, config.getBootstrapServers());
        properties.put(ProducerConfig.ACKS_CONFIG, "all");
        properties.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true");
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 10_000);
        properties.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 15_000);
        this.producer = new KafkaProducer<>(properties);
    }

    public void send(IngestRecord record, String errorType, String errorMessage) {
        String type = errorType == null ? "PIPELINE_ERROR" : errorType;
        String message = truncate(errorMessage);

        ProducerRecord<String, String> producerRecord =
                new ProducerRecord<>(config.getTopic(), record.getArticleId(), payload(record, type, message));
        producerRecord.headers().add("error_type", type.getBytes(StandardCharsets.UTF_8));
        producerRecord.headers().add("error_msg", message.getBytes(StandardCharsets.UTF_8));
        producerRecord.headers().add("src_topic", nullSafe(record.getTopic()).getBytes(StandardCharsets.UTF_8));
        producerRecord.headers().add("src_partition", String.valueOf(record.getPartition()).getBytes(StandardCharsets.UTF_8));
        producerRecord.headers().add("src_offset", String.valueOf(record.getOffset()).getBytes(StandardCharsets.UTF_8));
        producerRecord.headers().add("failed_at",
                OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME).getBytes(StandardCharsets.UTF_8));

        try {
            producer.send(producerRecord).get(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("DLQ 发送被中断, topic={}, articleId={}", config.getTopic(), record.getArticleId());
        } catch (Exception e) {
            log.error("DLQ 发送失败, topic={}, articleId={}, type={}, message={}",
                    config.getTopic(), record.getArticleId(), type, e.getMessage());
        }
    }

    public static String payload(IngestRecord record, String errorType, String errorMessage) {
        JSONObject root = new JSONObject();
        root.put("errorType", errorType);
        root.put("errorMessage", truncate(errorMessage));
        root.put("failedAt", OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        if (record.getArticleId() != null) {
            root.put("articleId", record.getArticleId());
        }

        JSONObject source = new JSONObject();
        source.put("topic", record.getTopic());
        source.put("partition", record.getPartition());
        source.put("offset", record.getOffset());
        source.put("kafkaTimestamp", record.getKafkaTimestamp());
        root.put("source", source);
        root.put("payload", record.getRawPayload());
        return root.toJSONString();
    }

    private static String truncate(String message) {
        if (message == null) {
            return "";
        }
        return message.length() <= MAX_ERROR_MESSAGE_LENGTH
                ? message
                : message.substring(0, MAX_ERROR_MESSAGE_LENGTH);
    }

    private static String nullSafe(String value) {
        return value == null ? "" : value;
    }

    @Override
    public void close() {
        producer.close();
    }
}
