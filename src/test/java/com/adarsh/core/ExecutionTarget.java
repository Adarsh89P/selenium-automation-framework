package com.adarsh.core;

/** Where the browser actually runs, selected with {@code -Dexecution=...}. */
public enum ExecutionTarget {
    LOCAL,
    GRID,
    CLOUD;

    public static ExecutionTarget from(String value) {
        return switch (value == null ? "" : value.trim().toLowerCase()) {
            case "grid" -> GRID;
            case "cloud" -> CLOUD;
            case "local", "" -> LOCAL;
            default -> throw new IllegalArgumentException(
                    "Unsupported execution target '" + value + "'. Use local, grid or cloud.");
        };
    }
}
