package com.zcw.voya.core;

import com.zcw.voya.ai.model.enums.CodeGenTypeEnum;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AiCodeGeneratorFacadeTest {

    @Resource
    private AiCodeGeneratorFacade aiCodeGeneratorFacade;

    @Test
    void generateCode() {
        File file = aiCodeGeneratorFacade.generateCode("做一个课程表网页，不超过50行", CodeGenTypeEnum.HTML);
        Assertions.assertNotNull(file);
    }

    @Test
    void generateCodeStream() {
        Flux<String> stream = aiCodeGeneratorFacade.generateCodeStream("做一个课程表网页，不超过50行", CodeGenTypeEnum.HTML);
        // 阻塞等待所有数据收集完成
        List<String> block = stream.collectList().block();
        Assertions.assertNotNull(block);
    }
}