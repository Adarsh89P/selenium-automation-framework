package com.adarsh.config;

import java.time.Duration;
import org.aeonbits.owner.ConfigCache;
import org.aeonbits.owner.ConfigFactory;

/**
 * Single access point to {@link FrameworkConfig}.
 *
 * <p>Everything that needs a setting reads it from here, so no other class ever touches
 * {@code System.getProperty} or loads a properties file itself.
 */
public final class ConfigReader {

    static {
        // Owner expands ${env} inside @Sources. Seed a default so the suite still starts
        // when nobody passed -Denv, and let an explicit -Denv win.
        ConfigFactory.setProperty("env", System.getProperty("env", "dev"));
    }

    private ConfigReader() {
        // utility holder
    }

    public static FrameworkConfig get() {
        return ConfigCache.getOrCreate(FrameworkConfig.class);
    }

    // Convenience wrappers for the values that are read most often, so callers deal in
    // Duration rather than raw ints.

    public static Duration explicitTimeout() {
        return Duration.ofSeconds(get().explicitTimeout());
    }

    public static Duration pageLoadTimeout() {
        return Duration.ofSeconds(get().pageLoadTimeout());
    }

    public static Duration pollingInterval() {
        return Duration.ofMillis(get().pollingIntervalMillis());
    }

    /** Joins the configured base URL to a relative ParaBank path. */
    public static String url(String relativePath) {
        var base = get().baseUrl();
        if (base.endsWith("/") && relativePath.startsWith("/")) {
            return base + relativePath.substring(1);
        }
        if (!base.endsWith("/") && !relativePath.startsWith("/")) {
            return base + "/" + relativePath;
        }
        return base + relativePath;
    }
}
