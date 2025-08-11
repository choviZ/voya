package com.zcw.voya;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.zcw.voya.mapper")
public class VoyaApplication {

    public static void main(String[] args) {
        SpringApplication.run(VoyaApplication.class, args);
    }

}
