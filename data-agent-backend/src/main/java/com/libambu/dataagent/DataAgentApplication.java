package com.libambu.dataagent;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.libambu.dataagent.mapper")
public class DataAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(DataAgentApplication.class, args);
    }

}
