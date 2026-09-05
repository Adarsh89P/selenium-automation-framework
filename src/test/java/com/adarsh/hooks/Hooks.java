package com.adarsh.hooks;

import com.adarsh.config.ConfigReader;
import com.adarsh.core.DriverManager;
import com.adarsh.core.ScenarioContext;
import com.adarsh.utils.ScreenshotUtils;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.qameta.allure.Allure;
import java.io.ByteArrayInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * Scenario lifecycle: one browser per scenario, torn down whatever happens.
 *
 * <p>The teardown hook runs at the lowest order so it is the last thing to execute, which means a
 * browser is still alive while the failure hook takes its screenshot.
 */
public class Hooks {

    private static final Logger log = LoggerFactory.getLogger(Hooks.class);

    @Before(order = 0)
    public void startBrowser(Scenario scenario) {
        // MDC gives log4j2 a per-scenario routing key, so parallel runs produce one log file
        // per scenario instead of a single interleaved mess.
        MDC.put("scenario", safeName(scenario));
        log.info("=== START {} ===", scenario.getName());
        DriverManager.startDriver();
    }

    @Before(order = 1)
    public void openApplication() {
        DriverManager.getDriver().get(ConfigReader.url("index.htm"));
    }

    @After(order = 1)
    public void captureEvidenceOnFailure(Scenario scenario) {
        if (!scenario.isFailed()) {
            return;
        }
        log.error("=== FAILED {} at {} ===", scenario.getName(),
                ScreenshotUtils.currentUrl().orElse("<no url>"));

        ScreenshotUtils.capture()
                .ifPresent(bytes -> {
                    // Attach to both sinks: Cucumber's own report and Allure.
                    scenario.attach(bytes, "image/png", "screenshot-on-failure");
                    Allure.addAttachment("Screenshot on failure", new ByteArrayInputStream(bytes));
                });

        ScreenshotUtils.pageSource()
                .ifPresent(html -> Allure.addAttachment("Page source on failure", "text/html", html, ".html"));

        ScreenshotUtils.currentUrl()
                .ifPresent(url -> Allure.addAttachment("URL on failure", url));
    }

    @After(order = 0)
    public void stopBrowser(Scenario scenario) {
        try {
            DriverManager.quitDriver();
        } finally {
            // Clearing the context matters even on failure: a thread-pool thread is reused by
            // the next scenario and must start empty.
            ScenarioContext.clear();
            log.info("=== END {} [{}] ===", scenario.getName(), scenario.getStatus());
            MDC.remove("scenario");
        }
    }

    private static String safeName(Scenario scenario) {
        return scenario.getName().replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
