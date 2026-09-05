package com.adarsh.listeners;

import com.adarsh.core.DriverManager;
import com.adarsh.utils.ScreenshotUtils;
import io.qameta.allure.Allure;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import org.openqa.selenium.logging.LogType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

/**
 * Turns a retried test into a visible flaky result, and captures failure evidence for tests that
 * still hold a live browser when they fail.
 *
 * <h2>The reporting problem this exists to solve</h2>
 *
 * <p>Note what this class does <em>not</em> do: it does not mark the Allure test case. Allure
 * opens and closes its test case from {@code IInvokedMethodListener}, which brackets a method more
 * tightly than {@code ITestListener} does, so by the time any method here runs the case is already
 * written and an update finds nothing to modify. The Allure marker is applied from inside the
 * running test - see the Cucumber {@code Hooks} - using the name-keyed lookup on
 * {@link RetryAnalyzer}. This class owns the log record, the run summary and the evidence bundle.
 *
 * <p>When {@link RetryAnalyzer} retries a failure, TestNG records the failed attempt as
 * <em>skipped</em> and the successful attempt as <em>passed</em>. A suite that needed two goes to
 * get green therefore reports {@code Failures: 0} and looks identical to one that passed first
 * time. That is the outcome retries are usually criticised for, and it is a reporting choice
 * rather than an inevitability: this listener records the retry count and the reason, so a flaky
 * pass is distinguishable from a clean one.
 *
 * <h2>Where the failure evidence actually comes from</h2>
 *
 * <p>For Cucumber scenarios, not from here. The Cucumber {@code @After} hook quits the browser
 * before TestNG's {@code onTestFailure} runs, so a screenshot taken at this point would be an
 * exception rather than a picture - which is exactly why {@code Hooks} captures evidence while the
 * session is still alive. The capture below serves the TestNG-native data-driven tests, which have
 * no Cucumber lifecycle wrapped around them and are still holding a driver when they fail.
 *
 * <p>The guard is {@code DriverManager.hasDriver()} rather than a flag, so neither path has to
 * know which kind of test is running.
 */
public class TestListener implements ITestListener {

    private static final Logger log = LoggerFactory.getLogger(TestListener.class);

    /** Tests that only went green after a retry, reported as a block in the suite summary. */
    private static final List<String> FLAKY = new CopyOnWriteArrayList<>();

    @Override
    public void onTestStart(ITestResult result) {
        log.debug("Starting {}", name(result));
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        var retries = RetryAnalyzer.retriesUsed(result);
        if (retries > 0) {
            var reason = RetryAnalyzer.retryReason(result).orElse("reason not recorded");
            FLAKY.add(name(result) + " (passed on attempt " + (retries + 1) + ")");
            log.warn("FLAKY: {} passed only after {} retry(ies). First failure: {}",
                    name(result), retries, reason);
        }
        RetryAnalyzer.forget(result);
    }

    @Override
    public void onTestFailure(ITestResult result) {
        log.error("FAILED: {}", name(result), result.getThrowable());
        captureEvidence();
        RetryAnalyzer.forget(result);
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        // A skip here is almost always a retried attempt rather than a genuine skip. The retry
        // bookkeeping is left alone: the final attempt's success or failure handler clears it.
        log.debug("Skipped {} (likely a retried attempt)", name(result));
    }

    @Override
    public void onFinish(ITestContext context) {
        if (FLAKY.isEmpty()) {
            return;
        }
        // Printed as a block at the end, because a warning logged forty minutes into a nightly
        // run is a warning nobody reads.
        log.warn("=== {} flaky test(s) in this run - green, but not trustworthy ===", FLAKY.size());
        FLAKY.forEach(entry -> log.warn("  - {}", entry));
        writeFlakyReport();
    }

    /**
     * Writes the flaky list to a file rather than to Allure.
     *
     * <p>{@code Allure.addAttachment} needs a test case to attach to, and by {@code onFinish}
     * every test case is closed - the call would be a silent no-op. A file is something CI can
     * publish as an artifact and a human can diff between runs.
     */
    private static void writeFlakyReport() {
        var report = Path.of("target", "flaky-report.txt");
        try {
            Files.createDirectories(report.getParent());
            Files.write(report, FLAKY, StandardCharsets.UTF_8);
            log.info("Flaky test report written to {}", report.toAbsolutePath());
        } catch (IOException e) {
            // Reporting must never be the reason a suite fails.
            log.warn("Could not write the flaky report: {}", e.getMessage());
        }
    }

    // ===== Internals =====

    /**
     * Attaches whatever the live session can still say about the failure.
     *
     * <p>Every capture is best-effort and separately guarded: a test that failed because the
     * session died would otherwise throw in here and replace a useful test failure with a
     * confusing listener failure.
     */
    private static void captureEvidence() {
        if (!DriverManager.hasDriver()) {
            // Cucumber scenario: Hooks has already captured evidence and quit the browser.
            return;
        }

        ScreenshotUtils.capture().ifPresent(bytes ->
                Allure.addAttachment("Screenshot on failure", new ByteArrayInputStream(bytes)));

        ScreenshotUtils.pageSource().ifPresent(html ->
                Allure.addAttachment("Page source on failure", "text/html", html, ".html"));

        ScreenshotUtils.currentUrl().ifPresent(url ->
                Allure.addAttachment("URL on failure", url));

        browserConsoleLog().ifPresent(entries ->
                Allure.addAttachment("Browser console log", entries));
    }

    /**
     * Reads the browser's own console output.
     *
     * <p>Enabled by the {@code goog:loggingPrefs} capability set in {@code DriverFactory}. Without
     * that capability this returns an empty list rather than failing, so the capability and this
     * reader have to stay in step or the report silently gains a blank section. Chromium only:
     * Firefox exposes no console log over W3C WebDriver.
     */
    private static Optional<String> browserConsoleLog() {
        try {
            var entries = DriverManager.getDriver().manage().logs().get(LogType.BROWSER);
            var text = entries.getAll().stream()
                    .map(entry -> entry.getLevel() + " " + entry.getMessage())
                    .collect(Collectors.joining(System.lineSeparator()));
            return text.isBlank() ? Optional.empty() : Optional.of(text);
        } catch (RuntimeException e) {
            // Firefox, a dead session, or a grid that does not proxy logs. Not worth failing over.
            log.debug("Browser console log unavailable: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private static String name(ITestResult result) {
        var parameters = result.getParameters();
        return parameters.length > 0
                ? String.valueOf(parameters[0])
                : result.getMethod().getQualifiedName();
    }
}
