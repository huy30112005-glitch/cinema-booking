package com.cinema.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String PROPERTY_SOURCE_NAME = "normalizedDatabaseUrl";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (hasText(environment.getProperty("spring.datasource.url"))) {
            return;
        }

        String databaseUrl = firstNonBlank(
                environment.getProperty("DB_URL"),
                environment.getProperty("DATABASE_URL")
        );

        if (!hasText(databaseUrl)) {
            return;
        }

        Map<String, Object> properties = new HashMap<>();
        String jdbcUrl = toJdbcUrl(databaseUrl, properties);

        properties.put("spring.datasource.url", jdbcUrl);
        environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, properties));
    }

    private String toJdbcUrl(String databaseUrl, Map<String, Object> properties) {
        if (databaseUrl.startsWith("jdbc:postgresql:")) {
            return withSslMode(databaseUrl);
        }

        if (!databaseUrl.startsWith("postgres://") && !databaseUrl.startsWith("postgresql://")) {
            return databaseUrl;
        }

        URI uri = URI.create(databaseUrl);
        String userInfo = uri.getRawUserInfo();

        if (hasText(userInfo)) {
            String[] credentials = userInfo.split(":", 2);
            properties.putIfAbsent("spring.datasource.username", decode(credentials[0]));

            if (credentials.length > 1) {
                properties.putIfAbsent("spring.datasource.password", decode(credentials[1]));
            }
        }

        StringBuilder jdbcUrl = new StringBuilder("jdbc:postgresql://")
                .append(uri.getHost());

        if (uri.getPort() > 0) {
            jdbcUrl.append(":").append(uri.getPort());
        }

        jdbcUrl.append(uri.getRawPath());

        if (hasText(uri.getRawQuery())) {
            jdbcUrl.append("?").append(uri.getRawQuery());
        }

        return withSslMode(jdbcUrl.toString());
    }

    private String withSslMode(String jdbcUrl) {
        if (jdbcUrl.contains("sslmode=") || jdbcUrl.contains("ssl=true")) {
            return jdbcUrl;
        }

        return jdbcUrl + (jdbcUrl.contains("?") ? "&" : "?") + "sslmode=require";
    }

    private String firstNonBlank(String first, String second) {
        return hasText(first) ? first : second;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
