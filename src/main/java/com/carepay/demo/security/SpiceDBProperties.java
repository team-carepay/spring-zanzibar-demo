package com.carepay.demo.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

@ConfigurationProperties(prefix = "spicedb")
public record SpiceDBProperties(String host, boolean useSsl, String token, String userSubjectType) {
    @ConstructorBinding
    public SpiceDBProperties {
        // compact constructor for spring binding
    }
}
