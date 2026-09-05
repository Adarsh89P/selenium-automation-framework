package com.adarsh.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.testng.annotations.Test;

/**
 * Covers the parts of {@link WaitUtils} that need no browser: the generic condition wait and the
 * file wait. The element-level waits are exercised by every scenario in the suite; these two are
 * not, and they are the ones with real logic in them rather than a delegation to
 * {@code ExpectedConditions}.
 */
public class WaitUtilsTest {

    @Test
    public void forConditionReturnsOnceTheConditionHolds() {
        var calls = new AtomicInteger();
        WaitUtils.forCondition(() -> calls.incrementAndGet() >= 3, "the third poll",
                Duration.ofSeconds(5));
        assertThat(calls.get()).isGreaterThanOrEqualTo(3);
    }

    @Test
    public void forConditionTimesOutWithAReadableMessage() {
        assertThatThrownBy(() ->
                WaitUtils.forCondition(() -> false, "something that never happens",
                        Duration.ofMillis(600)))
                .hasMessageContaining("Timed out waiting for something that never happens");
    }

    @Test
    public void forFileWaitsForTheFileToStopGrowing() throws Exception {
        var file = Files.createTempFile("scratch-download", ".txt");
        Files.writeString(file, "complete");
        var result = WaitUtils.forFile(file, Duration.ofSeconds(5));
        assertThat(result).isEqualTo(file);
        Files.deleteIfExists(file);
    }

    @Test
    public void forFileRejectsAZeroByteFile() throws Exception {
        var file = Files.createTempFile("scratch-empty", ".txt");
        assertThatThrownBy(() -> WaitUtils.forFile(file, Duration.ofMillis(800)))
                .hasMessageContaining("never finished downloading");
        Files.deleteIfExists(file);
    }

    @Test
    public void forFileRejectsAMissingFile() {
        var missing = Path.of("target", "definitely-not-downloaded.bin");
        assertThatThrownBy(() -> WaitUtils.forFile(missing, Duration.ofMillis(800)))
                .hasMessageContaining("never finished downloading");
    }
}
