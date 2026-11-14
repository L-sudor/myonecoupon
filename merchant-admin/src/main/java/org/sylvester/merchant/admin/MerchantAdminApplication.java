package org.sylvester.merchant.admin;

import com.mzt.logapi.starter.annotation.EnableLogRecord;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableLogRecord(tenant = "MerchantAdmin")
@MapperScan("org.sylvester.merchant.admin.dao.mapper")
public class MerchantAdminApplication {

    public static void main(String[] args) {
        org.springframework.boot.SpringApplication.run(MerchantAdminApplication.class, args);
    }
}
