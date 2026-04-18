package com.library.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

@Configuration
@EnableElasticsearchRepositories(basePackages = "com.library.repository")
@ConditionalOnProperty(
    prefix = "library.search.elasticsearch",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class ElasticsearchConfig extends ElasticsearchConfiguration {

    @Value("${spring.elasticsearch.uris}")
    private String elasticsearchUris;

    @Value("${spring.elasticsearch.username}")
    private String username;

    @Value("${spring.elasticsearch.password}")
    private String password;

    @Override
    public ClientConfiguration clientConfiguration() {
        return ClientConfiguration.builder()
            .connectedTo(elasticsearchUris.replace("http://", ""))
            .withBasicAuth(username, password)
            .withConnectTimeout(3000)
            .withSocketTimeout(5000)
            .build();
    }
}
