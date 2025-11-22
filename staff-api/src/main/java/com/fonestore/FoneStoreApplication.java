package com.fonestore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
@SpringBootApplication
@ComponentScan(basePackages = "com.fonestore")

@EnableJpaRepositories(basePackages = {
    "com.fonestore.user_api.repository",
    "com.fonestore.staff_api.repository"
})
@EntityScan(basePackages = {
    "com.fonestore.user_api.entity",
    "com.fonestore.staff_api.entity"
})
public class FoneStoreApplication {
    public static void main(String[] args) {
        SpringApplication.run(FoneStoreApplication.class, args);
    }
}
