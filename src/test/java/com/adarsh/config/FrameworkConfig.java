package com.adarsh.config;

import org.aeonbits.owner.Config;

/**
 * Typed view over the framework's configuration.
 *
 * <p>{@code LoadType.MERGE} matters: with Owner's default {@code FIRST} policy the loader stops
 * at the first source that opens, and {@code system:properties} always opens, so no properties
 * file would ever be read. Under MERGE every source is loaded and the first one holding a given
 * key wins, which gives this precedence chain:
 *
 * <ol>
 *   <li>{@code -Dkey=value} on the command line
 *   <li>environment variables (how CI supplies secrets)
 *   <li>the per-environment file selected by {@code -Denv=dev|stage}
 *   <li>{@code config.properties} as the shared baseline
 * </ol>
 *
 * <p>Credentials therefore never need to live in a file: exporting {@code APP_PASSWORD}
 * overrides whatever the properties file says.
 */
@Config.LoadPolicy(Config.LoadType.MERGE)
@Config.Sources({
    "system:properties",
    "system:env",
    "classpath:config/${env}.properties",
    "classpath:config/config.properties"
})
public interface FrameworkConfig extends Config {

    // ===== Application under test =====

    @Key("base.url")
    String baseUrl();

    @Key("app.username")
    String username();

    @Key("app.password")
    String password();

    // ===== Execution =====

    @Key("browser")
    @DefaultValue("chrome")
    String browser();

    @Key("execution")
    @DefaultValue("local")
    String execution();

    @Key("headless")
    @DefaultValue("true")
    boolean headless();

    @Key("grid.url")
    @DefaultValue("http://localhost:4444/wd/hub")
    String gridUrl();

    /** Window size applied to every browser so screenshots are comparable across runs. */
    @Key("window.width")
    @DefaultValue("1920")
    int windowWidth();

    @Key("window.height")
    @DefaultValue("1080")
    int windowHeight();

    // ===== Timeouts (seconds) =====

    /** Upper bound for every explicit wait. There is no implicit wait anywhere by design. */
    @Key("explicit.timeout")
    @DefaultValue("20")
    int explicitTimeout();

    @Key("page.load.timeout")
    @DefaultValue("40")
    int pageLoadTimeout();

    @Key("polling.interval.millis")
    @DefaultValue("250")
    int pollingIntervalMillis();

    // ===== Cloud grid (optional, only read when -Dexecution=cloud) =====

    @Key("cloud.url")
    @DefaultValue("")
    String cloudUrl();

    @Key("cloud.username")
    @DefaultValue("")
    String cloudUsername();

    @Key("cloud.accesskey")
    @DefaultValue("")
    String cloudAccessKey();

    // ===== Stability =====

    /**
     * How many times a failed scenario is retried before the failure stands. Two is the ceiling
     * on purpose: a test that needs three attempts is not flaky, it is broken.
     */
    @Key("retry.max")
    @DefaultValue("2")
    int retryMax();

    /**
     * When true (the default) an {@code AssertionError} is never retried. A deterministic
     * assertion failure does not heal on a second run - retrying it only buys a slower red build
     * and, worse, occasionally turns a real intermittent product bug green.
     */
    @Key("retry.only.infrastructure.failures")
    @DefaultValue("true")
    boolean retryOnlyInfrastructureFailures();
}
