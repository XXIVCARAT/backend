package com.example.badminton.config;

import java.net.URI;
import javax.sql.DataSource;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;

@Configuration
public class DatabaseConfig {
    @Bean
    @Primary
    public DataSource dataSource(Environment env) {
        String dbUrl = env.getProperty("DB_URL");
        String dbUser = env.getProperty("DB_USER");
        String dbPassword = env.getProperty("DB_PASSWORD");
        String databaseUrl = env.getProperty("DATABASE_URL");

        if (dbUrl != null && !dbUrl.isBlank()) {
            return build(dbUrl, dbUser, dbPassword);
        }

        if (databaseUrl != null && !databaseUrl.isBlank()) {
            return buildFromDatabaseUrl(databaseUrl);
        }

        return build("jdbc:postgresql://localhost:5432/badminton", "badminton", "badminton");
    }

    private DataSource buildFromDatabaseUrl(String databaseUrl) {
        URI uri = URI.create(databaseUrl);
        String[] userInfo = uri.getUserInfo() != null ? uri.getUserInfo().split(":", 2) : new String[0];
        String username = userInfo.length > 0 ? userInfo[0] : null;
        String password = userInfo.length > 1 ? userInfo[1] : null;

        int port = uri.getPort() == -1 ? 5432 : uri.getPort();
        String jdbcUrl = "jdbc:postgresql://" + uri.getHost() + ":" + port + uri.getPath();
        if (uri.getQuery() != null && !uri.getQuery().isBlank()) {
            jdbcUrl += "?" + uri.getQuery();
        }

        return build(jdbcUrl, username, password);
    }

    private DataSource build(String url, String username, String password) {
        DataSourceBuilder<?> builder = DataSourceBuilder.create().url(url);
        if (username != null && !username.isBlank()) {
            builder.username(username);
        }
        if (password != null && !password.isBlank()) {
            builder.password(password);
        }
        return builder.build();
    }
}
