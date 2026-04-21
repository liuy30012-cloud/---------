package com.library.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@Component
@ConfigurationProperties(prefix = "app.demo-books")
public class DemoBooksProperties {

    private boolean enabled = false;

    private boolean seedIfEmptyOnly = true;

    private Resource resource;

    private Resource chineseShowcaseResource;

    @Min(1)
    @Max(1_000_000)
    private int targetCount = 100_000;

    private boolean disableEsListenerDuringSeed = true;
}
