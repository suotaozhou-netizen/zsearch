package com.zst.zsearch.streaming.sink;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * 与 server 保持一致：ES 使用 SNAKE_CASE；显式驼峰字段由 @JsonProperty 覆盖。
 */
public final class EsClientFactory {

    private EsClientFactory() {
    }

    public static EsClientHolder create(EsSinkConfig config) {
        List<HttpHost> hosts = Arrays.stream(config.getUris().split(","))
                .map(String::trim)
                .filter(uri -> !uri.isEmpty())
                .map(HttpHost::create)
                .toList();
        if (hosts.isEmpty()) {
            throw new IllegalArgumentException("es.uris 不能为空");
        }

        RestClientBuilder builder = RestClient.builder(hosts.toArray(HttpHost[]::new));
        if (config.getUsername() != null && !config.getUsername().isBlank()) {
            CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(
                    config.getUsername(),
                    config.getPassword() == null ? "" : config.getPassword()));
            builder.setHttpClientConfigCallback(httpClient ->
                    httpClient.setDefaultCredentialsProvider(credentialsProvider));
        }

        RestClient restClient = builder.build();
        ObjectMapper objectMapper = new ObjectMapper()
                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        ElasticsearchTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper(objectMapper));
        return new EsClientHolder(new ElasticsearchClient(transport), restClient);
    }

    public static final class EsClientHolder implements AutoCloseable {

        private final ElasticsearchClient client;
        private final RestClient restClient;

        private EsClientHolder(ElasticsearchClient client, RestClient restClient) {
            this.client = client;
            this.restClient = restClient;
        }

        public ElasticsearchClient client() {
            return client;
        }

        @Override
        public void close() {
            try {
                restClient.close();
            } catch (IOException | ElasticsearchException e) {
                throw new IllegalStateException("关闭 Elasticsearch 客户端失败", e);
            }
        }
    }
}
