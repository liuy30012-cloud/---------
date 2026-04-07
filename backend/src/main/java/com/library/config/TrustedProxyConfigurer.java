package com.library.config;

import com.library.util.ClientIpResolver;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.BindResult;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class TrustedProxyConfigurer {

    private static final List<String> DEFAULT_TRUSTED_PROXIES = List.of(
            "127.0.0.1/32",
            "::1/128"
    );

    private final Environment environment;

    public TrustedProxyConfigurer(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    public void configureTrustedProxies() {
        Binder binder = Binder.get(environment);
        BindResult<List<String>> antiCrawlerBinding = binder.bind("anti-crawler.trusted-proxies", Bindable.listOf(String.class));
        BindResult<List<String>> legacyBinding = binder.bind("security.trusted-proxies", Bindable.listOf(String.class));

        List<String> configuredProxies;
        if (antiCrawlerBinding.isBound()) {
            configuredProxies = antiCrawlerBinding.get();
            log.info("Configured trusted proxy ranges from anti-crawler.trusted-proxies");
        } else if (legacyBinding.isBound()) {
            configuredProxies = legacyBinding.get();
            log.info("Configured trusted proxy ranges from legacy security.trusted-proxies");
        } else {
            configuredProxies = DEFAULT_TRUSTED_PROXIES;
            log.info("Using default trusted proxy ranges");
        }

        ClientIpResolver.setTrustedProxyCidrs(configuredProxies);
    }
}
