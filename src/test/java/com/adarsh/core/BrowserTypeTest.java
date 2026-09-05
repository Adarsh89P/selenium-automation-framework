package com.adarsh.core;

import org.testng.Assert;
import org.testng.annotations.Test;

public class BrowserTypeTest {

    @Test
    public void mapsSupportedBrowsersCaseInsensitively() {
        Assert.assertEquals(BrowserType.from("chrome"), BrowserType.CHROME);
        Assert.assertEquals(BrowserType.from(" FIREFOX "), BrowserType.FIREFOX);
        Assert.assertEquals(BrowserType.from("Edge"), BrowserType.EDGE);
    }

    @Test
    public void missingBrowserDefaultsToChrome() {
        Assert.assertEquals(BrowserType.from(null), BrowserType.CHROME);
        Assert.assertEquals(BrowserType.from(""), BrowserType.CHROME);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void rejectsUnsupportedBrowser() {
        BrowserType.from("safari");
    }
}
