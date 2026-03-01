package ru.kolivim.document.workflow.service;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;

import javax.sql.DataSource;


//@Configuration
public class TestConfiguration {

    @Bean(initMethod = "start", destroyMethod = "stop")
    public PostgreSQLContainer<?> postgreSQLContainer(){
        return new PostgreSQLContainer<>("postgres:17.6")
                .withDatabaseName("doc_workflow")
                .withUsername("docWf")
                .withPassword("docWf");
    }


    @Bean
    public DataSource dataSource(){
        var hikariDataSource = new HikariDataSource();
        hikariDataSource.setJdbcUrl(postgreSQLContainer().getJdbcUrl());
        hikariDataSource.setUsername(postgreSQLContainer().getUsername());
        hikariDataSource.setPassword(postgreSQLContainer().getPassword());
        return hikariDataSource;
    }

}
