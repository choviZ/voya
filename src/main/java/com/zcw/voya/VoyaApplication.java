package com.zcw.voya;

import dev.langchain4j.community.store.embedding.redis.spring.RedisEmbeddingStoreAutoConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(exclude = {RedisEmbeddingStoreAutoConfiguration.class})
@MapperScan("com.zcw.voya.mapper")
public class VoyaApplication {

    public static void main(String[] args) {
        SpringApplication.run(VoyaApplication.class, args);
    }

}
