package com.library.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.util.StringUtils;

@Configuration
@EnableElasticsearchRepositories(basePackages = "com.library.repository")
@ConditionalOnProperty(
    prefix = "library.search.elasticsearch",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = false
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
        var builder = ClientConfiguration.builder()
            .connectedTo(elasticsearchUris.replace("http://", ""))
            .withConnectTimeout(3000)
            .withSocketTimeout(5000);

        if (StringUtils.hasText(username)) {
            return builder.withBasicAuth(username, password == null ? "" : password).build();
        }

        return builder.build();
    }
}
