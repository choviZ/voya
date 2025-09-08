package com.zcw.voya.utils;

import com.zcw.voya.util.WebScreenShotUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class WebScreenShotTest {

    @Test
    void saveWebPageScreenshot() {
        String testUrl = "https://www.mianshiya.com/";
        String webPageScreenshot = WebScreenShotUtils.saveWebScreenShot(testUrl);
        Assertions.assertNotNull(webPageScreenshot);
    }
}

