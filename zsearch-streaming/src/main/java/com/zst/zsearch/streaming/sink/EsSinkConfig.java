package com.zst.zsearch.streaming.sink;

import com.zst.zsearch.streaming.dlq.DlqConfig;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

@Getter
@Setter
public class EsSinkConfig implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String uris;
    private String username;
    private String password;
    private String index;

    private int batchSize = 500;
    private long flushIntervalMs = 1000;
    private int maxBufferBytes = 5 * 1024 * 1024;
    private int maxRetries = 5;
    private long backoffBaseMs = 200;

    private String dlqBootstrapServers;
    private String dlqTopic;

    public DlqConfig toDlqConfig() {
        DlqConfig config = new DlqConfig();
        config.setBootstrapServers(dlqBootstrapServers);
        config.setTopic(dlqTopic);
        return config;
    }
}
