package com.demo.orders;

import com.demo.orders.config.SqsProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
@EnableConfigurationProperties(SqsProperties.class)
public class ModernOrderApplication {

    public static void main(String[] args) {
        SpringApplication.run(ModernOrderApplication.class, args);
    }
}
