package com.zst.zsearch.common.model;

import lombok.Getter;
import lombok.Setter;

/**
 * Kafka 消息信封。传输层元数据放在外层，原始业务字段放在 article 中。
 */
@Getter
@Setter
public class IngestEnvelope {

    private Integer schemaVersion;
    private Integer idVersion;
    private String articleId;
    private Long rawVersion;
    private String crawledAt;
    private RawArticle article;
}
