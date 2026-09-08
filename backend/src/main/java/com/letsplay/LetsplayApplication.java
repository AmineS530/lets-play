package com.letsplay;

import com.letsplay.config.DotenvLoader;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class LetsplayApplication {

    public static void main(String[] args) {
        DotenvLoader.load();
        SpringApplication.run(LetsplayApplication.class, args);
    }
}
