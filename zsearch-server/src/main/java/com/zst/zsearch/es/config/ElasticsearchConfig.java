package com.zst.zsearch.es.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.JsonpMapper;
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
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.util.List;

@Configuration
@EnableConfigurationProperties(EsProperties.class)
public class ElasticsearchConfig {

    @Bean(destroyMethod = "close")
    public RestClient restClient(EsProperties properties) {
        List<HttpHost> hosts = properties.getUris().stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .map(HttpHost::create)
                .toList();
        if (hosts.isEmpty()) {
            throw new IllegalStateException("zsearch.elasticsearch.uris 不能为空");
        }

        RestClientBuilder builder = RestClient.builder(hosts.toArray(HttpHost[]::new));
        builder.setRequestConfigCallback(requestConfig -> requestConfig
                .setConnectTimeout(properties.getConnectTimeoutMs())
                .setSocketTimeout(properties.getSocketTimeoutMs()));

        if (StringUtils.hasText(properties.getUsername())) {
            CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(
                    properties.getUsername(),
                    properties.getPassword() == null ? "" : properties.getPassword()
            ));
            builder.setHttpClientConfigCallback(httpClient ->
                    httpClient.setDefaultCredentialsProvider(credentialsProvider));
        }

        return builder.build();
    }

    @Bean
    public JsonpMapper jsonpMapper(ObjectMapper objectMapper) {
        // 复制一份：ES 的 _source 使用 snake_case，HTTP 接口仍保持 camelCase
        ObjectMapper esObjectMapper = objectMapper.copy();
        esObjectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        return new JacksonJsonpMapper(esObjectMapper);
    }

    @Bean(destroyMethod = "close")
    public ElasticsearchTransport elasticsearchTransport(RestClient restClient, JsonpMapper jsonpMapper) {
        return new RestClientTransport(restClient, jsonpMapper);
    }

    @Bean
    public ElasticsearchClient elasticsearchClient(ElasticsearchTransport transport) {
        return new ElasticsearchClient(transport);
    }
}
