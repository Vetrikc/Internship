package com.innowise.internship.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

// src/main/java/com/innowise/internship/config/MainDataSourceConfig.java
@Configuration
@EnableJpaRepositories(
    basePackages = "com.innowise.internship.dao",  // твои обычные репозитории (UserRepository и т.д.)
    entityManagerFactoryRef = "mainEntityManagerFactory",
    transactionManagerRef = "mainTransactionManager"
)
@EnableTransactionManagement
public class MainDataSourceConfig {

    @Bean
    @Primary  // Важно! Это основной DataSource
    @ConfigurationProperties("spring.datasource.main")
    public DataSource mainDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean
    @Primary
    public LocalContainerEntityManagerFactoryBean mainEntityManagerFactory(
            EntityManagerFactoryBuilder builder) {
        return builder
                .dataSource(mainDataSource())
                .packages("com.innowise.internship.entity")  // пакет с User, PaymentCard и т.д.
                .persistenceUnit("main")
                .build();
    }

    @Bean
    @Primary
    public PlatformTransactionManager mainTransactionManager(
            EntityManagerFactoryBuilder builder) {
        return new JpaTransactionManager(mainEntityManagerFactory(builder).getObject());
    }
}