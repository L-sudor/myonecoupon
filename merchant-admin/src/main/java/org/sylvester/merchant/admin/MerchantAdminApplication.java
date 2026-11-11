package org.sylvester.merchant.admin;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("org.sylvester.merchant.admin.dao.mapper")
public class MerchantAdminApplication {

    public static void main(String[] args) {
        org.springframework.boot.SpringApplication.run(MerchantAdminApplication.class, args);
    }
}
