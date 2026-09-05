package com.adarsh.listeners;

import com.adarsh.core.BrowserType;
import com.adarsh.core.DriverFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.IInvokedMethod;
import org.testng.IInvokedMethodListener;
import org.testng.ITestResult;

/**
 * Lets a suite XML choose the browser per {@code <test>} block.
 *
 * <h2>The problem</h2>
 *
 * <p>A cross-browser suite is conventionally written as one {@code <test>} block per browser, each
 * passing {@code <parameter name="browser" value="..."/>}. That does not work on its own here.
 * {@code DriverFactory} reads the browser from {@code ConfigReader}, which resolves a single
 * JVM-wide {@code -Dbrowser} value, and a TestNG parameter never reaches it - so all three blocks
 * would quietly run Chrome and the suite would report cross-browser coverage it never had. A
 * silently wrong green is worse than a missing feature, which is why this class exists rather than
 * the suite XML alone.
 *
 * <h2>Why an IInvokedMethodListener specifically</h2>
 *
 * <p>The override has to be a {@code ThreadLocal}, because scenarios run concurrently and each one
 * needs its own answer. That rules out anything that runs on a different thread from the scenario:
 *
 * <ul>
 *   <li>{@code ISuiteListener} runs once, on the main thread, before any pool thread exists.
 *   <li>{@code ITestListener.onTestStart} is closer but is not guaranteed to share the thread that
 *       later executes the method.
 * </ul>
 *
 * <p>{@code beforeInvocation} runs on the very thread that is about to execute the test method, and
 * therefore the thread on which the Cucumber {@code @Before} hook will ask for a driver. It also
 * has access to the {@code XmlTest} the method belongs to, which is where the parameter lives.
 *
 * <p>Registered through {@code META-INF/services/org.testng.ITestNGListener} like the others.
 * Harmless when absent: a suite with no {@code browser} parameter falls straight back to the
 * configured value, so the single-browser suites are unaffected.
 */
public class BrowserParameterListener implements IInvokedMethodListener {

    private static final Logger log = LoggerFactory.getLogger(BrowserParameterListener.class);

    /** Name of the suite-XML parameter this reads. Matches the {@code -Dbrowser} property name. */
    private static final String BROWSER_PARAMETER = "browser";

    @Override
    public void beforeInvocation(IInvokedMethod method, ITestResult testResult) {
        // Configuration methods (@BeforeClass and friends) do not create browsers; only the test
        // method itself needs the pin, and setting it twice would be harmless but misleading.
        if (!method.isTestMethod()) {
            return;
        }

        var xmlTest = testResult.getTestContext().getCurrentXmlTest();
        var value = xmlTest == null ? null : xmlTest.getParameter(BROWSER_PARAMETER);
        if (value == null || value.isBlank()) {
            // No parameter: the suite is single-browser and the configured value stands.
            return;
        }

        // Deliberately not caught. A typo in a suite XML should stop the run at the first scenario
        // with a message naming the bad value, not start Chrome and report a Firefox run.
        var browser = BrowserType.from(value);
        DriverFactory.overrideBrowser(browser);
        log.debug("Pinned '{}' to {} from suite parameter", xmlTest.getName(), browser);
    }

    @Override
    public void afterInvocation(IInvokedMethod method, ITestResult testResult) {
        if (!method.isTestMethod()) {
            return;
        }
        // Unconditional: TestNG hands this pool thread to the next scenario, which may belong to a
        // different <test> block and a different browser.
        DriverFactory.clearBrowserOverride();
    }
}
