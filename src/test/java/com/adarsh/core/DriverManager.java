package com.adarsh.core;

import org.openqa.selenium.WebDriver;

/**
 * Holds the WebDriver for the current thread.
 *
 * <p>There is deliberately no {@code static WebDriver} field and no setter that takes a driver
 * from outside the framework: parallel scenarios each get their own browser, and a scenario can
 * never reach into another scenario's session.
 */
public final class DriverManager {

    private static final ThreadLocal<WebDriver> DRIVER = new ThreadLocal<>();

    private DriverManager() {
        // utility holder
    }

    /** Creates a browser for this thread. Called once per scenario by the Cucumber hooks. */
    public static void startDriver() {
        if (DRIVER.get() != null) {
            // A leaked driver would silently leak a browser process; fail loudly instead.
            throw new IllegalStateException(
                    "A driver is already active on thread " + Thread.currentThread().getName()
                            + ". quitDriver() was not called after the previous scenario.");
        }
        DRIVER.set(DriverFactory.create());
    }

    public static WebDriver getDriver() {
        var driver = DRIVER.get();
        if (driver == null) {
            throw new IllegalStateException(
                    "No driver on thread " + Thread.currentThread().getName()
                            + ". startDriver() must run before any page interaction.");
        }
        return driver;
    }

    public static boolean hasDriver() {
        return DRIVER.get() != null;
    }

    /** Quits the browser and clears the slot. Safe to call when no driver was ever started. */
    public static void quitDriver() {
        var driver = DRIVER.get();
        if (driver != null) {
            try {
                driver.quit();
            } finally {
                // remove() matters: without it the thread-pool thread keeps a dead driver
                // and the next scenario on that thread fails in a confusing way.
                DRIVER.remove();
            }
        }
    }
}
