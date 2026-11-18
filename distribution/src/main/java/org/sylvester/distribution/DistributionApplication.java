package org.sylvester.distribution;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("org.sylvester.distribution.dao.mapper")
public class DistributionApplication {

    public static void main(String[] args) {
        org.springframework.boot.SpringApplication.run(DistributionApplication.class, args);
    }
}
