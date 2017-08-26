package com.kiwi;

import com.linecorp.bot.spring.boot.annotation.LineMessageHandler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@LineMessageHandler
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}