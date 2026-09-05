package com.adarsh.hooks;

import com.adarsh.config.ConfigReader;
import com.adarsh.core.DriverManager;
import com.adarsh.core.ScenarioContext;
import com.adarsh.listeners.RetryAnalyzer;
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

    /**
     * Records, from inside the scenario, that this run is a retry of one that already failed.
     *
     * <h4>What could not be made to work, so it is not attempted here</h4>
     *
     * <p>Three routes to stamping the Allure test case were tried and all three are dead ends with
     * {@code allure-cucumber7-jvm}, each failing silently rather than erroring:
     *
     * <ul>
     *   <li>An {@code ITestListener} runs too late - Allure brackets the method from
     *       {@code IInvokedMethodListener}, so the case is already written.
     *   <li>{@code updateTestCase(...)} from this hook does nothing: the plugin manages its test
     *       case by uuid and never binds it to the Allure thread context, so there is no "current"
     *       test case for a hook to modify. Verified - neither status details nor labels survived.
     *   <li>{@code scenario.attach(...)} from a hook is dropped: the plugin converts only
     *       step-phase attachments, and it does not model Before hooks as steps.
     * </ul>
     *
     * <h4>Why nothing further is needed</h4>
     *
     * <p>Allure already solves this on its own. Both attempts are written with the same
     * {@code historyId}, so the report groups them and shows the failed attempt, with its reason,
     * under the test case's Retries tab. A scenario that only passed on the second go is therefore
     * visibly distinct from one that passed first time without any help from this framework.
     * Verified against a real run: one {@code historyId}, two results - {@code broken} carrying the
     * failure message, then {@code passed}.
     *
     * <p>What is added here is the human-readable line, which lands in the Cucumber HTML report and
     * the console where someone reading a build log will actually see it. The machine-readable
     * record is {@code target/flaky-report.txt}, written by {@code TestListener}.
     *
     * <p>Order 2 puts this after the driver exists, so a failure to annotate can never stop a
     * browser from starting.
     */
    @Before(order = 2)
    public void flagRetriedScenario(Scenario scenario) {
        var reason = RetryAnalyzer.retryReasonForName(scenario.getName());
        if (reason.isEmpty()) {
            return;
        }
        var attempt = RetryAnalyzer.retriesUsedForName(scenario.getName()) + 1;
        log.warn("Attempt {} of '{}' - previous attempt failed with {}",
                attempt, scenario.getName(), reason.get());
        scenario.log("FLAKY: this is attempt " + attempt
                + ". The previous attempt failed with: " + reason.get());
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
