package com.vigilai.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;
import java.net.URI;

/**
 * Ensures reliable database connection when deployed to managed cloud platforms (Railway, Render).
 * Automatically translates raw 'mysql://' URIs injected by platform environment variables
 * into valid JDBC URLs that HikariCP and MySQL Connector/J can connect to without crashing.
 */
@Configuration
@Profile("prod")
public class ProdDataSourceConfig {

    private static final Logger log = LoggerFactory.getLogger(ProdDataSourceConfig.class);

    @Value("${spring.datasource.url:#{null}}")
    private String configuredUrl;

    @Value("${spring.datasource.username:#{null}}")
    private String configuredUsername;

    @Value("${spring.datasource.password:#{null}}")
    private String configuredPassword;

    @Bean
    @Primary
    public DataSource prodDataSource() {
        String finalJdbcUrl = configuredUrl;
        String finalUser = configuredUsername;
        String finalPass = configuredPassword;

        // Check if platform provided raw mysql:// or database url
        String envUrl = System.getenv("MYSQL_URL");
        if (envUrl == null || envUrl.isBlank()) {
            envUrl = System.getenv("DATABASE_URL");
        }

        if (envUrl != null && !envUrl.isBlank() && envUrl.startsWith("mysql://")) {
            try {
                URI uri = new URI(envUrl);
                String host = uri.getHost();
                int port = uri.getPort() == -1 ? 3306 : uri.getPort();
                String path = uri.getPath();
                String dbName = (path != null && path.length() > 1) ? path.substring(1) : "railway";

                if (uri.getUserInfo() != null) {
                    String[] userParts = uri.getUserInfo().split(":");
                    finalUser = userParts[0];
                    if (userParts.length > 1) {
                        finalPass = userParts[1];
                    }
                }
                finalJdbcUrl = "jdbc:mysql://" + host + ":" + port + "/" + dbName +
                        "?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
                log.info("Auto-configured JDBC URL from cloud platform MYSQL_URL: host={}, port={}, db={}", host, port, dbName);
            } catch (Exception e) {
                log.warn("Failed to parse MYSQL_URL URI: {}", e.getMessage());
            }
        } else if (finalJdbcUrl == null || finalJdbcUrl.isBlank() || finalJdbcUrl.startsWith("mysql://")) {
            String host = System.getenv().getOrDefault("MYSQLHOST", "localhost");
            String port = System.getenv().getOrDefault("MYSQLPORT", "3306");
            String db = System.getenv().getOrDefault("MYSQLDATABASE", "railway");
            finalUser = System.getenv().getOrDefault("MYSQLUSER", finalUser != null ? finalUser : "root");
            finalPass = System.getenv().getOrDefault("MYSQLPASSWORD", finalPass != null ? finalPass : "");

            finalJdbcUrl = "jdbc:mysql://" + host + ":" + port + "/" + db +
                    "?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
            log.info("Auto-configured JDBC URL from individual environment variables: host={}, port={}, db={}", host, port, db);
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(finalJdbcUrl);
        config.setUsername(finalUser);
        config.setPassword(finalPass);
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);

        return new HikariDataSource(config);
    }
}
