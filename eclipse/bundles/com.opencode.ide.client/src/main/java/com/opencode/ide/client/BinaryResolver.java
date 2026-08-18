package com.opencode.ide.client;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Resolves the {@code opencode} executable. The pure logic is split out (with
 * the environment read separately) so resolution can be unit-tested with a
 * controlled PATH and OS flag.
 */
public final class BinaryResolver {

    private BinaryResolver() {
    }

    /** Resolves using the real environment: an explicit path, else the {@code PATH}. */
    public static Path resolveBinary(String configured) {
        return resolveBinary(configured, System.getenv("PATH"), isWindows());
    }

    /**
     * @param configured an explicit binary path chosen by the user (may be {@code null}/blank)
     * @param pathEnv    the {@code PATH} string (entries split by {@link java.io.File#pathSeparator})
     * @param windows    whether to look for {@code .cmd/.exe/.bat} shims (true) or bare/{@code .sh} (false)
     * @return the resolved binary, or {@code null} if none found
     */
    public static Path resolveBinary(String configured, String pathEnv, boolean windows) {
        if (configured != null && !configured.isBlank()) {
            Path explicit = Path.of(configured.trim());
            if (Files.isRegularFile(explicit)) {
                return explicit;
            }
        }
        if (pathEnv == null || pathEnv.isEmpty()) {
            return null;
        }
        List<String> suffixes = windows
                ? List.of(".cmd", ".exe", ".bat", "")
                : List.of("", ".sh");
        for (String dir : pathEnv.split(java.io.File.pathSeparator)) {
            if (dir.isBlank()) {
                continue;
            }
            for (String ext : suffixes) {
                Path candidate = Path.of(dir, "opencode" + ext);
                if (Files.isRegularFile(candidate)) {
                    return candidate;
                }
            }
        }
        return null;
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }
}
