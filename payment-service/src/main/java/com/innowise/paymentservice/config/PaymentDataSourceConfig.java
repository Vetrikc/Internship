package com.innowise.paymentservice.config;


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
        basePackages = "com.innowise.paymentservice.dao",
        entityManagerFactoryRef = "orderEntityManagerFactory",
        transactionManagerRef = "orderTransactionManager"
)
@EnableTransactionManagement
public class PaymentDataSourceConfig {

    @Bean
    @ConfigurationProperties("spring.datasource.payment")
    public DataSource orderDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean orderEntityManagerFactory(
            EntityManagerFactoryBuilder builder) {
        return builder
                .dataSource(orderDataSource())
                .packages("com.innowise.paymentservice.entity")
                .persistenceUnit("payment")
                .build();
    }

    @Bean
    public PlatformTransactionManager orderTransactionManager(
            EntityManagerFactoryBuilder builder) {
        return new JpaTransactionManager(orderEntityManagerFactory(builder).getObject());
    }

}
