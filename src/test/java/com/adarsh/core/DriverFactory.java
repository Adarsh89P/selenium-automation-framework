package com.adarsh.core;

import com.adarsh.config.ConfigReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.util.logging.Level;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.logging.LoggingPreferences;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The only place in the framework that constructs a WebDriver.
 *
 * <p>Driver binaries are resolved by Selenium Manager, which ships inside Selenium 4.6+. There is
 * no {@code System.setProperty("webdriver.*.driver", ...)} and no WebDriverManager dependency.
 */
public final class DriverFactory {

    private static final Logger log = LoggerFactory.getLogger(DriverFactory.class);

    /**
     * Browser chosen for this thread, overriding the configured one.
     *
     * <p>Exists so a suite can run the same scenarios against several browsers at once. The
     * configured {@code -Dbrowser} is a single JVM-wide value, so a suite XML with one
     * {@code <test>} per browser would otherwise run the same browser three times and look like it
     * worked. {@code BrowserParameterListener} seeds this from the {@code <test>} block's
     * {@code browser} parameter on the thread that is about to run the scenario.
     */
    private static final ThreadLocal<BrowserType> BROWSER_OVERRIDE = new ThreadLocal<>();

    private DriverFactory() {
        // utility holder
    }

    /** Pins this thread to a browser for the scenario it is about to run. */
    public static void overrideBrowser(BrowserType browser) {
        BROWSER_OVERRIDE.set(browser);
    }

    /**
     * Releases the pin.
     *
     * <p>Must be called when the invocation ends: TestNG reuses pool threads, so a leftover value
     * would hand the wrong browser to whatever scenario lands on this thread next.
     */
    public static void clearBrowserOverride() {
        BROWSER_OVERRIDE.remove();
    }

    static WebDriver create() {
        var config = ConfigReader.get();
        var override = BROWSER_OVERRIDE.get();
        var browser = override != null ? override : BrowserType.from(config.browser());
        var target = ExecutionTarget.from(config.execution());

        log.info("Creating {} driver for execution target {}", browser, target);

        WebDriver driver =
                switch (target) {
                    case LOCAL -> createLocal(browser);
                    case GRID -> createRemote(browser, config.gridUrl());
                    case CLOUD -> createCloud(browser);
                };

        applyCommonSettings(driver);
        return driver;
    }

    private static WebDriver createLocal(BrowserType browser) {
        return switch (browser) {
            case CHROME -> new ChromeDriver(chromeOptions());
            case FIREFOX -> new FirefoxDriver(firefoxOptions());
            case EDGE -> new EdgeDriver(edgeOptions());
        };
    }

    private static WebDriver createRemote(BrowserType browser, String remoteUrl) {
        var driver = new RemoteWebDriver(toUrl(remoteUrl), optionsFor(browser));
        // Without this, uploading a file to a Grid node silently fails.
        driver.setFileDetector(new org.openqa.selenium.remote.LocalFileDetector());
        return driver;
    }

    private static WebDriver createCloud(BrowserType browser) {
        var config = ConfigReader.get();
        if (config.cloudUrl().isBlank()) {
            throw new IllegalStateException(
                    "-Dexecution=cloud requires cloud.url, cloud.username and cloud.accesskey. "
                            + "Supply them as environment variables, never in a properties file.");
        }
        var options = optionsFor(browser);
        // TODO: confirm the vendor capability namespace once a cloud account is available.
        // BrowserStack uses "bstack:options", LambdaTest uses "LT:Options".
        var vendorOptions = new java.util.HashMap<String, Object>();
        vendorOptions.put("userName", config.cloudUsername());
        vendorOptions.put("accessKey", config.cloudAccessKey());
        vendorOptions.put("sessionName", "ParaBank regression");
        options.setCapability("bstack:options", vendorOptions);
        return new RemoteWebDriver(toUrl(config.cloudUrl()), options);
    }

    private static MutableCapabilities optionsFor(BrowserType browser) {
        return switch (browser) {
            case CHROME -> chromeOptions();
            case FIREFOX -> firefoxOptions();
            case EDGE -> edgeOptions();
        };
    }

    // ===== Browser options. DesiredCapabilities is removed from Selenium 4 and is not used. =====

    private static ChromeOptions chromeOptions() {
        var options = new ChromeOptions();
        if (ConfigReader.get().headless()) {
            options.addArguments("--headless=new");
        }
        options.addArguments("--disable-gpu");
        options.addArguments("--no-sandbox");
        // Containers default to a 64MB /dev/shm, which crashes Chrome on heavier pages.
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--remote-allow-origins=*");
        options.addArguments("--disable-notifications");
        options.setPageLoadTimeout(ConfigReader.pageLoadTimeout());
        enableBrowserLogging(options);
        return options;
    }

    private static FirefoxOptions firefoxOptions() {
        var options = new FirefoxOptions();
        if (ConfigReader.get().headless()) {
            options.addArguments("-headless");
        }
        options.setPageLoadTimeout(ConfigReader.pageLoadTimeout());
        return options;
    }

    private static EdgeOptions edgeOptions() {
        var options = new EdgeOptions();
        if (ConfigReader.get().headless()) {
            options.addArguments("--headless=new");
        }
        options.addArguments("--disable-gpu");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.setPageLoadTimeout(ConfigReader.pageLoadTimeout());
        enableBrowserLogging(options);
        return options;
    }

    /**
     * Turns on browser-side console logging so a failure can be diagnosed with the JavaScript
     * errors the page actually produced, not just a screenshot of the aftermath.
     *
     * <p>Chromium only. Without the {@code goog:loggingPrefs} capability set here,
     * {@code driver.manage().logs().get(LogType.BROWSER)} returns an empty list rather than
     * failing, so the evidence in the report would silently be blank. Firefox exposes no
     * equivalent over W3C WebDriver - console capture there needs BiDi, which is why the
     * console-log scenarios are tagged for a Chromium browser.
     */
    private static void enableBrowserLogging(MutableCapabilities options) {
        var logging = new LoggingPreferences();
        logging.enable(LogType.BROWSER, Level.ALL);
        options.setCapability("goog:loggingPrefs", logging);
    }

    private static void applyCommonSettings(WebDriver driver) {
        var config = ConfigReader.get();
        // Fixed window size instead of maximize(): headless and Grid sessions then produce
        // screenshots of identical dimensions, which makes failures comparable.
        driver.manage().window().setSize(new Dimension(config.windowWidth(), config.windowHeight()));
        // No implicitlyWait(): mixing implicit and explicit waits makes timeouts unpredictable.
        driver.manage().timeouts().pageLoadTimeout(ConfigReader.pageLoadTimeout());
        driver.manage().timeouts().scriptTimeout(Duration.ofSeconds(config.explicitTimeout()));
    }

    private static URL toUrl(String value) {
        try {
            return URI.create(value).toURL();
        } catch (MalformedURLException | IllegalArgumentException e) {
            throw new IllegalStateException("Not a usable remote WebDriver URL: " + value, e);
        }
    }
}
