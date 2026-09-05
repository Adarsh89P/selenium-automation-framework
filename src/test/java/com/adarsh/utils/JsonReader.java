package com.adarsh.utils;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reads fixed test data from {@code src/test/resources/testdata}.
 *
 * <p>Files are cached after the first read: a parallel suite would otherwise re-parse the same
 * JSON once per scenario, and the cache also guarantees every thread sees identical data.
 */
public final class JsonReader {

    private static final ObjectMapper MAPPER =
            new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private static final Map<String, Object> CACHE = new ConcurrentHashMap<>();

    private JsonReader() {
        // utility holder
    }

    /** Reads one object from {@code testdata/<fileName>}. */
    public static <T> T read(String fileName, Class<T> type) {
        var key = fileName + "#" + type.getName();
        @SuppressWarnings("unchecked")
        var cached = (T) CACHE.computeIfAbsent(key, ignored -> parse(fileName, type));
        return cached;
    }

    /** Reads an array from {@code testdata/<fileName>} into a list. */
    public static <T> List<T> readList(String fileName, Class<T> type) {
        var key = fileName + "#list#" + type.getName();
        @SuppressWarnings("unchecked")
        var cached = (List<T>) CACHE.computeIfAbsent(key, ignored -> parseList(fileName, type));
        return cached;
    }

    private static <T> T parse(String fileName, Class<T> type) {
        try (var stream = open(fileName)) {
            return MAPPER.readValue(stream, type);
        } catch (IOException e) {
            throw new IllegalStateException("Could not parse test data file: " + fileName, e);
        }
    }

    private static <T> List<T> parseList(String fileName, Class<T> type) {
        try (var stream = open(fileName)) {
            CollectionType listType =
                    MAPPER.getTypeFactory().constructCollectionType(List.class, type);
            return MAPPER.readValue(stream, listType);
        } catch (IOException e) {
            throw new IllegalStateException("Could not parse test data file: " + fileName, e);
        }
    }

    private static InputStream open(String fileName) {
        var path = fileName.startsWith("testdata/") ? fileName : "testdata/" + fileName;
        var stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
        if (stream == null) {
            throw new IllegalArgumentException(
                    "Test data file not found on the classpath: " + path
                            + ". Expected it under src/test/resources/testdata.");
        }
        return stream;
    }
}
