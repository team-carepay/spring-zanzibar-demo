package com.carepay.demo.configuration;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.carepay.demo.security.ZanzibarPermissionEvaluator;
import com.fasterxml.jackson.datatype.hibernate6.Hibernate6Module;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.graphql.GraphQlSourceBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
@EnableWebSecurity
@Slf4j
public class SecurityConfiguration {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // @formatter:off
        http
                .csrf(CsrfConfigurer::disable)
                .sessionManagement(sessionManagementConfigurer -> sessionManagementConfigurer.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.GET, "*.html", "*.css", "*.js").permitAll()
                        .requestMatchers("/graphiql").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oAuth2ResourceServerConfigurer -> oAuth2ResourceServerConfigurer.jwt(Customizer.withDefaults()))
                ;
        // @formatter:on
        return http.build();
    }

    @Bean
    DefaultMethodSecurityExpressionHandler expressionHandler(ZanzibarPermissionEvaluator zanzibarPermissionEvaluator) {
        var expressionHandler = new DefaultMethodSecurityExpressionHandler();
        expressionHandler.setPermissionEvaluator(zanzibarPermissionEvaluator);
        return expressionHandler;
    }

    //region customizations
    @Bean
    GraphQlSourceBuilderCustomizer inspectionCustomizer() {
        return source -> source.inspectSchemaMappings(report -> log.info(report.toString()));
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        var c = new JwtAuthenticationConverter();
        c.setJwtGrantedAuthoritiesConverter(jwt -> jwt.<Map<String, List<String>>>getClaim("realm_access").get("roles").stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList()));
        return c;
    }

    @Bean
    public Hibernate6Module hibernate6Module() {
        return new Hibernate6Module();
    }
    //endregion
}