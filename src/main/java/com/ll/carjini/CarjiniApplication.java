package com.ll.carjini;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableCaching
@EnableJpaAuditing
@EnableScheduling
@SpringBootApplication
@EnableMongoAuditing
public class CarjiniApplication {

    public static void main(String[] args) {
        SpringApplication.run(CarjiniApplication.class, args);
    }
}
