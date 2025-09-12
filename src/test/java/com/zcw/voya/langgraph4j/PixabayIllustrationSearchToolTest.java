package com.zcw.voya.langgraph4j;

import com.zcw.voya.langgraph4j.model.ImageResource;
import com.zcw.voya.langgraph4j.model.enums.ImageCategoryEnum;
import com.zcw.voya.langgraph4j.tools.PixabayIllustrationSearchTool;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class PixabayIllustrationSearchToolTest {

    @Resource
    private PixabayIllustrationSearchTool pixabayillustrationSearch;

    @Test
    void testSearchIllustrations() {
        // 测试正常搜索插画
        List<ImageResource> illustrations = pixabayillustrationSearch.searchIllustrations("happy");
        assertNotNull(illustrations);
        // 验证返回的插画资源
        ImageResource firstIllustration = illustrations.get(0);
        assertEquals(ImageCategoryEnum.ILLUSTRATION, firstIllustration.getCategory());
        assertNotNull(firstIllustration.getDescription());
        assertNotNull(firstIllustration.getUrl());
        assertTrue(firstIllustration.getUrl().startsWith("http"));
        System.out.println("搜索到 " + illustrations.size() + " 张插画");
        illustrations.forEach(illustration ->
                System.out.println("插画: " + illustration.getDescription() + " - " + illustration.getUrl())
        );
    }
}
