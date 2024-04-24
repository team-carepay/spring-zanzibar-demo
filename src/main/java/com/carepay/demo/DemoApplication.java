package com.carepay.demo;

import com.carepay.demo.security.SpiceDBProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.modulith.events.config.EnablePersistentDomainEvents;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableConfigurationProperties(SpiceDBProperties.class)
@EnableAsync
@EnablePersistentDomainEvents
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}
