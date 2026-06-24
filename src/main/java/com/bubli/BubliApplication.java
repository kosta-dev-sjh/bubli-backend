package com.bubli;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BubliApplication {

    public static void main(String[] args) {
        SpringApplication.run(BubliApplication.class, args);
    }

}
