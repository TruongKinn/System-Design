package com.dataflow.export;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ExportServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ExportServiceApplication.class, args);
    }
}
