package com.zst.zsearch;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@MapperScan({"com.zst.zsearch.biz.mapper", "com.zst.zsearch.auth.mapper"})
public class ZSearchApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZSearchApplication.class, args);
    }
}
