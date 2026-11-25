package com.zst.zsearch.es.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "zsearch.elasticsearch")
public class EsProperties {

    private List<String> uris = List.of("http://192.168.88.101:9200");
    private String username;
    private String password;
    private String indexName = "zsearch_article";
    private int connectTimeoutMs = 3000;
    private int socketTimeoutMs = 30000;
}
