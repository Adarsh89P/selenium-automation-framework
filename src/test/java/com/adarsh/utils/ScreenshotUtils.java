package com.adarsh.utils;

import com.adarsh.core.DriverManager;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Screenshot capture for failure diagnosis.
 *
 * <p>Every method here is best-effort: a browser that has already crashed cannot produce a
 * screenshot, and losing the picture must never replace the real assertion failure with a
 * confusing WebDriver error. Failures to capture are logged and swallowed.
 */
public final class ScreenshotUtils {

    private static final Logger log = LoggerFactory.getLogger(ScreenshotUtils.class);
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");
    private static final Path SCREENSHOT_DIR = Path.of("target", "screenshots");

    private ScreenshotUtils() {
        // utility holder
    }

    /** Full-viewport screenshot as bytes, ready to attach to a report. */
    public static Optional<byte[]> capture() {
        if (!DriverManager.hasDriver()) {
            return Optional.empty();
        }
        try {
            var driver = DriverManager.getDriver();
            return Optional.of(((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES));
        } catch (RuntimeException e) {
            log.warn("Could not capture screenshot: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /** Writes a screenshot to {@code target/screenshots} and returns the file it created. */
    public static Optional<Path> saveToDisk(String name) {
        return capture().flatMap(bytes -> write(name, bytes));
    }

    private static Optional<Path> write(String name, byte[] bytes) {
        try {
            Files.createDirectories(SCREENSHOT_DIR);
            var file = SCREENSHOT_DIR.resolve(sanitise(name) + "-" + LocalDateTime.now().format(STAMP) + ".png");
            Files.write(file, bytes);
            log.info("Screenshot written to {}", file.toAbsolutePath());
            return Optional.of(file);
        } catch (IOException e) {
            log.warn("Could not write screenshot for '{}': {}", name, e.getMessage());
            return Optional.empty();
        }
    }

    /** The page source at the moment of failure, which often explains more than the picture. */
    public static Optional<String> pageSource() {
        if (!DriverManager.hasDriver()) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(DriverManager.getDriver().getPageSource());
        } catch (RuntimeException e) {
            log.warn("Could not capture page source: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public static Optional<String> currentUrl() {
        if (!DriverManager.hasDriver()) {
            return Optional.empty();
        }
        try {
            WebDriver driver = DriverManager.getDriver();
            return Optional.ofNullable(driver.getCurrentUrl());
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    private static String sanitise(String name) {
        return name.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
