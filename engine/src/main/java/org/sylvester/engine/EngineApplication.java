package org.sylvester.engine;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("org.sylvester.engine.dao.mapper")
public class EngineApplication {

    public static void main(String[] args) {
        org.springframework.boot.SpringApplication.run(EngineApplication.class, args);
    }
}
