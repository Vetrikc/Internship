package com.innowise.internship.config.db;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;


@Configuration
@EnableJpaRepositories(
    basePackages = "com.innowise.internship.dao_auth",  // или где у тебя AuthUserRepository
    entityManagerFactoryRef = "authEntityManagerFactory",
    transactionManagerRef = "authTransactionManager"
)
@EnableTransactionManagement
public class AuthDataSourceConfig {

    @Bean
    @ConfigurationProperties("spring.datasource.auth")
    public DataSource authDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean authEntityManagerFactory(
            EntityManagerFactoryBuilder builder) {
        return builder
                .dataSource(authDataSource())
                .packages("com.innowise.internship.entity")  // пакет с AuthUser
                .persistenceUnit("auth")
                .build();
    }

    @Bean
    public PlatformTransactionManager authTransactionManager(
            EntityManagerFactoryBuilder builder) {
        return new JpaTransactionManager(authEntityManagerFactory(builder).getObject());
    }
}