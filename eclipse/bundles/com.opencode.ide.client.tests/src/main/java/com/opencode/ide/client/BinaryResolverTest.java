package com.opencode.ide.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Test;

/**
 * Unit tests for {@link BinaryResolver#resolveBinary(String, String, boolean)} -
 * the pure (no {@code System.getenv}) resolution logic. Uses a temp dir with a
 * fake shim so no real opencode install is required.
 */
public class BinaryResolverTest {

    @Test
    public void resolvesCmdShimOnWindowsPath() throws IOException {
        Path dir = Files.createTempDirectory("opencode-bin-test");
        Files.createFile(dir.resolve("opencode.cmd"));
        try {
            Path resolved = BinaryResolver.resolveBinary(null, dir.toString(), true);
            assertEquals(dir.resolve("opencode.cmd"), resolved);
        } finally {
            Files.deleteIfExists(dir.resolve("opencode.cmd"));
            Files.deleteIfExists(dir);
        }
    }

    @Test
    public void resolvesBareBinaryOnPosixPath() throws IOException {
        Path dir = Files.createTempDirectory("opencode-bin-test");
        Files.createFile(dir.resolve("opencode"));
        try {
            Path resolved = BinaryResolver.resolveBinary(null, dir.toString(), false);
            assertEquals(dir.resolve("opencode"), resolved);
        } finally {
            Files.deleteIfExists(dir.resolve("opencode"));
            Files.deleteIfExists(dir);
        }
    }

    @Test
    public void explicitConfiguredPathWins() throws IOException {
        Path dir = Files.createTempDirectory("opencode-bin-test");
        Path explicit = dir.resolve("custom-opencode.exe");
        Files.createFile(explicit);
        try {
            Path resolved = BinaryResolver.resolveBinary(explicit.toString(), "", true);
            assertEquals(explicit, resolved);
        } finally {
            Files.deleteIfExists(explicit);
            Files.deleteIfExists(dir);
        }
    }

    @Test
    public void returnsNullWhenNotFound() {
        assertNull(BinaryResolver.resolveBinary(null, "", true));
        assertNull(BinaryResolver.resolveBinary(null, "C:\\does-not-exist-12345", true));
    }

    @Test
    public void windowsPrefersCmdOverExeOrder() throws IOException {
        Path dir = Files.createTempDirectory("opencode-bin-test");
        Files.createFile(dir.resolve("opencode.exe"));
        try {
            Path resolved = BinaryResolver.resolveBinary(null, dir.toString(), true);
            // .cmd is tried before .exe
            assertEquals(dir.resolve("opencode.exe"), resolved);
        } finally {
            Files.deleteIfExists(dir.resolve("opencode.exe"));
            Files.deleteIfExists(dir);
        }
    }
}
