package com.innowise.internship.config.db;


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

@Configuration
@EnableJpaRepositories(
        basePackages = "com.innowise.internship.dao_order",
        entityManagerFactoryRef = "orderEntityManagerFactory",
        transactionManagerRef = "orderTransactionManager"
)
@EnableTransactionManagement
public class OrderDataSourceConfig {

    @Bean
    @ConfigurationProperties("spring.datasource.order")
    public DataSource orderDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean orderEntityManagerFactory(
            EntityManagerFactoryBuilder builder) {
        return builder
                .dataSource(orderDataSource())
                .packages("com.innowise.internship.entity")
                .persistenceUnit("order")
                .build();
    }

    @Bean
    public PlatformTransactionManager orderTransactionManager(
            EntityManagerFactoryBuilder builder) {
        return new JpaTransactionManager(orderEntityManagerFactory(builder).getObject());
    }

}
