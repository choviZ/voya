package com.zcw.voya.ai;

import com.zcw.voya.ai.model.HtmlCodeResult;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AiCodeGeneratorServiceTest {

    @Resource
    private AiCodeGeneratorService aiCodeGeneratorService;

    @Test
    void generateHtmlCode() {
        HtmlCodeResult result = aiCodeGeneratorService.generateHtmlCode("做一个菜谱网页,不超过50行代码");
        Assertions.assertNotNull(result);
    }

    @Test
    void generateMultiFileCode() {
        String result = String.valueOf(aiCodeGeneratorService.generateHtmlCode("做一个菜谱网页"));
        Assertions.assertNotNull(result);
    }
}