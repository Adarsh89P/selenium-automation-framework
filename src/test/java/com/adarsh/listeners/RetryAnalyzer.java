package com.adarsh.listeners;

import com.adarsh.config.ConfigReader;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

/**
 * Re-runs a failed scenario up to {@code retry.max} times.
 *
 * <p>Applied to every test method by {@link RetryTransformer}; no test carries a
 * {@code @Test(retryAnalyzer = ...)} annotation, so a new scenario cannot be written without
 * retry protection and, equally, cannot opt out of the reporting that comes with it.
 *
 * <h2>Why the attempt counter is a map and not a field</h2>
 *
 * <p>The obvious implementation - {@code private int attempts} - is wrong here, and wrong in a way
 * that stays hidden until the suite runs in parallel.
 *
 * <p>TestNG creates one retry analyzer per {@code ITestNGMethod}, not per invocation. Every
 * Cucumber scenario in a runner arrives through the single method
 * {@code AbstractTestNGCucumberTests#runScenario}, fed by a data provider. All of them therefore
 * share one analyzer instance. With a plain field, two unrelated scenarios failing on two threads
 * would decrement the same budget: the second scenario would get one retry instead of two, or none
 * at all, and the increments would race. The count is keyed per invocation instead, in a
 * concurrent map.
 *
 * <h2>What is deliberately not retried</h2>
 *
 * <p>By default an {@link AssertionError} is treated as a verdict, not a glitch. A deterministic
 * assertion failure will fail identically on the retry, so retrying only slows the build down; and
 * where the failure is an intermittent <em>product</em> bug, a retry is exactly the mechanism that
 * hides it. Infrastructure failures - a stale element, a socket reset from the grid, a timeout
 * waiting for a page - are what retries are for. Set
 * {@code -Dretry.only.infrastructure.failures=false} to retry everything.
 */
public class RetryAnalyzer implements IRetryAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(RetryAnalyzer.class);

    /** Attempts already made, keyed per invocation. Shared across analyzer instances. */
    private static final Map<String, Integer> ATTEMPTS = new ConcurrentHashMap<>();

    /** Why each retried invocation was retried, so the report can say more than "flaky". */
    private static final Map<String, String> REASONS = new ConcurrentHashMap<>();

    /**
     * The same reasons, keyed by display name instead of by invocation key.
     *
     * <p>This exists because the Allure test case cannot be marked from a TestNG listener. Allure
     * opens and closes its test case inside {@code IInvokedMethodListener}, which brackets the
     * method more tightly than {@code ITestListener} does, so by the time {@code onTestSuccess}
     * runs the case is already written to disk and an update finds nothing to change. The only
     * reliable place to stamp the marker is inside the running test - for Cucumber, the
     * {@code @Before} hook - and that context knows the scenario by name and nothing else.
     */
    private static final Map<String, String> REASONS_BY_NAME = new ConcurrentHashMap<>();

    /** Attempts consumed, keyed by display name. Same reason as {@link #REASONS_BY_NAME}. */
    private static final Map<String, Integer> ATTEMPTS_BY_NAME = new ConcurrentHashMap<>();

    @Override
    public boolean retry(ITestResult result) {
        var max = ConfigReader.get().retryMax();
        if (max <= 0) {
            return false;
        }

        var failure = result.getThrowable();
        if (!isRetryable(failure)) {
            log.info("Not retrying '{}': {} is a verdict, not an infrastructure failure",
                    describe(result), failure.getClass().getSimpleName());
            return false;
        }

        var key = keyOf(result);
        // merge() is the atomic read-modify-write; get()-then-put() would race between threads
        // that happen to fail at the same moment.
        var attempt = ATTEMPTS.merge(key, 1, Integer::sum);

        if (attempt > max) {
            log.error("'{}' failed {} times and is now a hard failure", describe(result), attempt);
            return false;
        }

        var reason = summarise(failure);
        REASONS.put(key, reason);
        REASONS_BY_NAME.put(describe(result), reason);
        ATTEMPTS_BY_NAME.put(describe(result), attempt);
        log.warn("Retrying '{}' - attempt {} of {} - after {}",
                describe(result), attempt + 1, max + 1, summarise(failure));
        return true;
    }

    // ===== Read side, used by TestListener to mark the result flaky in Allure =====

    /** How many retries this invocation consumed. Zero means it passed first time. */
    public static int retriesUsed(ITestResult result) {
        return ATTEMPTS.getOrDefault(keyOf(result), 0);
    }

    /** The failure that triggered the last retry, present only if the invocation was retried. */
    public static Optional<String> retryReason(ITestResult result) {
        return Optional.ofNullable(REASONS.get(keyOf(result)));
    }

    /**
     * The failure that caused this scenario to be re-run, looked up by the name the test reports
     * itself under.
     *
     * <p>Called from the Cucumber {@code @Before} hook: if a reason is present, the scenario about
     * to start is a retry of one that already failed, and the hook stamps the Allure test case
     * accordingly while it is still open.
     */
    public static Optional<String> retryReasonForName(String displayName) {
        return Optional.ofNullable(REASONS_BY_NAME.get(displayName));
    }

    /** Retries consumed so far by the test reporting under this name. */
    public static int retriesUsedForName(String displayName) {
        return ATTEMPTS_BY_NAME.getOrDefault(displayName, 0);
    }

    /**
     * Drops the bookkeeping for a finished invocation.
     *
     * <p>Called by {@code TestListener} once it has read the counts, so a long nightly run does not
     * accumulate an entry per scenario for the life of the JVM.
     */
    public static void forget(ITestResult result) {
        var key = keyOf(result);
        ATTEMPTS.remove(key);
        REASONS.remove(key);
        ATTEMPTS_BY_NAME.remove(describe(result));
        REASONS_BY_NAME.remove(describe(result));
    }

    // ===== Internals =====

    private static boolean isRetryable(Throwable failure) {
        if (failure == null) {
            // No throwable means TestNG failed the test for a lifecycle reason of its own; there
            // is nothing to diagnose, so treat it as retryable rather than swallowing it.
            return true;
        }
        if (!ConfigReader.get().retryOnlyInfrastructureFailures()) {
            return true;
        }
        // Walk the cause chain: Cucumber rethrows the step's throwable, but a step that wraps its
        // own failure would otherwise hide the AssertionError from this check.
        for (var t = failure; t != null; t = t.getCause()) {
            if (t instanceof AssertionError) {
                return false;
            }
            if (t.getCause() == t) {
                break; // self-referential cause; stop rather than loop forever
            }
        }
        return true;
    }

    /**
     * Identifies one invocation.
     *
     * <p>For Cucumber this resolves to the method plus the scenario name and feature name, which
     * is what the data-provider parameters stringify to. It stays generic on purpose so the
     * Excel- and JSON-driven data providers get the same per-row budget.
     *
     * <p>Known limit: two scenarios sharing a name inside the same feature share a key and
     * therefore a retry budget. Duplicate scenario names inside one feature are a smell in their
     * own right, so this is not worth a Cucumber-specific dependency here.
     */
    private static String keyOf(ITestResult result) {
        var parameters = Arrays.stream(result.getParameters())
                .map(String::valueOf)
                .collect(Collectors.joining("|"));
        return result.getMethod().getQualifiedName() + "(" + parameters + ")";
    }

    /**
     * The name this invocation reports itself under, matched against what the test itself can see.
     *
     * <p>Cucumber's {@code PickleWrapper.toString()} wraps the scenario name in double quotes.
     * {@code Scenario.getName()} inside a hook does not, so the quotes have to come off here or the
     * name-keyed lookup silently never matches and the flaky marker is never applied - a failure
     * mode that looks exactly like "nothing was flaky".
     */
    private static String describe(ITestResult result) {
        var parameters = result.getParameters();
        var raw = parameters.length > 0 ? String.valueOf(parameters[0]) : result.getName();
        if (raw.length() > 1 && raw.startsWith("\"") && raw.endsWith("\"")) {
            return raw.substring(1, raw.length() - 1);
        }
        return raw;
    }

    private static String summarise(Throwable failure) {
        if (failure == null) {
            return "no throwable reported";
        }
        var message = failure.getMessage();
        if (message == null || message.isBlank()) {
            return failure.getClass().getName();
        }
        // Selenium's messages carry a build-info banner and a full DOM dump; the first line is
        // the part a human reads in a report.
        var firstLine = message.lines().findFirst().orElse(message);
        return failure.getClass().getSimpleName() + ": " + firstLine.strip();
    }
}
