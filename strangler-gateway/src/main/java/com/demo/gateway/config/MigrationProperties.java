package com.demo.gateway.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "migration")
public class MigrationProperties {
    private Toggle orders = new Toggle();
    private Toggle inventory = new Toggle();

    @Getter
    @Setter
    public static class Toggle {
        private boolean enabled;
    }
}
