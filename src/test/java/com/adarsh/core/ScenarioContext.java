package com.adarsh.core;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Per-scenario scratch space, so one step can hand a value to the next without any step class
 * holding a field.
 *
 * <p>Step classes with instance fields are the usual reason a suite cannot run in parallel and
 * why scenarios start depending on each other. The state lives here instead, keyed per thread and
 * wiped by the hooks after every scenario.
 */
public final class ScenarioContext {

    /** Keys are an enum rather than loose strings, so a typo is a compile error. */
    public enum Key {
        LOGGED_IN_USERNAME,
        LOGGED_IN_PASSWORD,
        NEW_CUSTOMER,
        SOURCE_ACCOUNT,
        TARGET_ACCOUNT,
        NEW_ACCOUNT_NUMBER,
        BALANCE_BEFORE,
        TRANSFER_AMOUNT,
        PAYEE,
        LOAN_OUTCOME,
        PROFILE_BEFORE
    }

    private static final ThreadLocal<Map<Key, Object>> STORE = ThreadLocal.withInitial(HashMap::new);

    private ScenarioContext() {
        // utility holder
    }

    public static void put(Key key, Object value) {
        STORE.get().put(key, value);
    }

    /** Reads a value, failing loudly when a step depends on one an earlier step never set. */
    public static <T> T get(Key key, Class<T> type) {
        var value = STORE.get().get(key);
        if (value == null) {
            throw new IllegalStateException(
                    "Nothing stored under " + key + ". A step that sets it must run first.");
        }
        return type.cast(value);
    }

    public static <T> Optional<T> find(Key key, Class<T> type) {
        return Optional.ofNullable(STORE.get().get(key)).map(type::cast);
    }

    public static boolean has(Key key) {
        return STORE.get().containsKey(key);
    }

    /** Called by the hooks after every scenario so nothing leaks into the next one. */
    public static void clear() {
        STORE.get().clear();
        STORE.remove();
    }
}
