package io.feeflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@ComponentScan(basePackages = {"io.feeflow", "auth", "common", "students"})
@EntityScan(basePackages = {"auth.entity"})
@EnableJpaRepositories(basePackages = {"auth.repository"})
@EnableCaching
public class FeeflowBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(FeeflowBackendApplication.class, args);
    }

}
