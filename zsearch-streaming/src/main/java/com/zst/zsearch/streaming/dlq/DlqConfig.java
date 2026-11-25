package com.zst.zsearch.streaming.dlq;

import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

@Getter
@Setter
public class DlqConfig implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String bootstrapServers;
    private String topic;
}
