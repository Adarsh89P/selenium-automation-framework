package com.adarsh.runners;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;
import org.testng.annotations.DataProvider;

/**
 * Shared Cucumber wiring. Concrete runners only pick the tag filter.
 *
 * <p>{@code parallel = true} on the data provider is what lets TestNG run scenarios concurrently;
 * combined with the ThreadLocal driver, each scenario gets its own browser.
 */
@CucumberOptions(
        features = "src/test/resources/features",
        glue = {"com.adarsh.steps", "com.adarsh.hooks"},
        plugin = {
            "pretty",
            "summary",
            "timeline:target/cucumber-timeline",
            "html:target/cucumber-report.html",
            "json:target/cucumber.json",
            "com.aventstack.extentreports.cucumber.adapter.ExtentCucumberAdapter:",
            "io.qameta.allure.cucumber7jvm.AllureCucumber7Jvm"
        },
        monochrome = true,
        // Never upload results to the public Cucumber reports service.
        publish = false)
public abstract class BaseCucumberRunner extends AbstractTestNGCucumberTests {

    @Override
    @DataProvider(parallel = true)
    public Object[][] scenarios() {
        return super.scenarios();
    }
}
