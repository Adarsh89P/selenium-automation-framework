package com.adarsh.core;

/** Browsers this framework can drive, selected with {@code -Dbrowser=...}. */
public enum BrowserType {
    CHROME,
    FIREFOX,
    EDGE;

    public static BrowserType from(String value) {
        return switch (value == null ? "" : value.trim().toLowerCase()) {
            case "firefox" -> FIREFOX;
            case "edge" -> EDGE;
            case "chrome", "" -> CHROME;
            default -> throw new IllegalArgumentException(
                    "Unsupported browser '" + value + "'. Use chrome, firefox or edge.");
        };
    }
}
