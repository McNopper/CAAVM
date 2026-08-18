package com.opencode.ide.tools.cpp;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Detects the C/C++ toolchains available on this machine, in default order
 * MSVC first, then the MSYS2 environments clang64, mingw64, ucrt64. Also
 * resolves the lint/format tools: clang-tidy and clang-format per toolchain
 * (MSYS2 env bin, standalone LLVM, then PATH) and cppcheck as a standalone
 * lint tool. Detection shells out to vswhere / cmake --help and is cached for
 * the lifetime of the JVM (machine facts do not change mid-run).
 */
public final class ToolchainRegistry {

    /** One detected toolchain. Absent optionals mean the tool is not available; tools must fail with a clear message. */
    public record Toolchain(String id, String displayName, Optional<Path> cmake, Optional<Path> ninja,
            Optional<Path> compiler, Optional<Path> ctest, Optional<Path> gdb, Optional<Path> clangTidy,
            Optional<Path> clangFormat, Optional<String> generator, List<Path> pathPrepend) {
    }

    private static final Path MSYS2_ROOT = Paths.get("C:\\msys64");
    private static final Path VSWHERE =
            Paths.get("C:\\Program Files (x86)\\Microsoft Visual Studio\\Installer\\vswhere.exe");
    private static final Path STANDALONE_CMAKE = Paths.get("C:\\Program Files\\CMake\\bin\\cmake.exe");
    private static final Path STANDALONE_LLVM_BIN = Paths.get("C:\\Program Files\\LLVM\\bin");
    private static final Path STANDALONE_CPPCHECK = Paths.get("C:\\Program Files\\Cppcheck\\cppcheck.exe");
    private static final List<String> MSYS_ENV_IDS = List.of("clang64", "mingw64", "ucrt64");
    private static final java.util.Map<String, List<String>> MSYS_COMPILERS = java.util.Map.of(
            "clang64", List.of("clang++", "clang"),
            "mingw64", List.of("clang++", "g++", "gcc"),
            "ucrt64", List.of("g++", "gcc"));
    private static final Pattern ANSI = Pattern.compile("\u001B\\[[0-9;]*m");
    private static final Pattern VS_GENERATOR = Pattern.compile("Visual Studio (\\d+) (\\d{4})");

    private static volatile List<Toolchain> cached;
    private static volatile Optional<Path> cachedCppcheck;

    private ToolchainRegistry() {
    }

    /** @return the cached detection result, detecting on first use. Ordered: msvc, clang64, mingw64, ucrt64. */
    public static List<Toolchain> detected() {
        List<Toolchain> result = cached;
        if (result == null) {
            synchronized (ToolchainRegistry.class) {
                result = cached;
                if (result == null) {
                    result = detect();
                    cached = result;
                }
            }
        }
        return result;
    }

    /** Runs the actual detection (vswhere, filesystem probes, cmake --help); no caching. */
    public static List<Toolchain> detect() {
        List<Toolchain> list = new ArrayList<>();
        detectMsvc().ifPresent(list::add);
        for (String id : MSYS_ENV_IDS) {
            detectMsysEnv(id).ifPresent(list::add);
        }
        return List.copyOf(list);
    }

    /** @return the toolchain with the given id, if detected. */
    public static Optional<Toolchain> byId(String id) {
        return detected().stream().filter(t -> t.id().equals(id)).findFirst();
    }

    /** @return the default toolchain (first detected: msvc, clang64, mingw64, ucrt64). */
    public static Optional<Toolchain> defaultToolchain() {
        List<Toolchain> all = detected();
        return all.isEmpty() ? Optional.empty() : Optional.of(all.get(0));
    }

    /**
     * Standalone lint tool, not tied to any toolchain: the Cppcheck installer
     * location first, then PATH. Cached like the toolchain detection.
     *
     * @return the cppcheck.exe path if present on this machine
     */
    public static Optional<Path> cppcheck() {
        Optional<Path> result = cachedCppcheck;
        if (result == null) {
            synchronized (ToolchainRegistry.class) {
                result = cachedCppcheck;
                if (result == null) {
                    result = exe(STANDALONE_CPPCHECK).or(() -> which("cppcheck"));
                    cachedCppcheck = result;
                }
            }
        }
        return result;
    }

    private static Optional<Toolchain> detectMsysEnv(String id) {
        Path bin = MSYS2_ROOT.resolve(id).resolve("bin");
        if (!Files.isDirectory(bin)) {
            return Optional.empty();
        }
        Optional<Path> cmake = exe(bin.resolve("cmake.exe"));
        Optional<Path> ninja = exe(bin.resolve("ninja.exe"));
        Optional<Path> compiler = MSYS_COMPILERS.get(id).stream()
                .map(name -> bin.resolve(name + ".exe"))
                .filter(Files::isRegularFile)
                .findFirst();
        if (cmake.isEmpty() && compiler.isEmpty()) {
            return Optional.empty();
        }
        Optional<Path> ctest = exe(bin.resolve("ctest.exe"));
        Optional<Path> gdb = exe(bin.resolve("gdb.exe")).or(() -> which("gdb"));
        Optional<Path> clangTidy = exe(bin.resolve("clang-tidy.exe")).or(() -> which("clang-tidy"));
        Optional<Path> clangFormat = exe(bin.resolve("clang-format.exe")).or(() -> which("clang-format"));
        Optional<String> generator = ninja.isPresent() ? Optional.of("Ninja") : Optional.empty();
        String compilerName = compiler.map(p -> p.getFileName().toString().replace(".exe", ""))
                .orElse("no compiler");
        String displayName = "MSYS2 " + id + " (" + compilerName + (generator.isPresent() ? " + Ninja)" : ")");
        return Optional.of(new Toolchain(id, displayName, cmake, ninja, compiler, ctest, gdb, clangTidy,
                clangFormat, generator, List.of(bin)));
    }

    private static Optional<Toolchain> detectMsvc() {
        if (!Files.isRegularFile(VSWHERE)) {
            return Optional.empty();
        }
        String installPath = capture(VSWHERE, List.of("-latest", "-products", "*", "-property", "installationPath"));
        if (installPath == null || installPath.isBlank()) {
            return Optional.empty();
        }
        Path vsDir = Paths.get(installPath.trim());
        if (!Files.isDirectory(vsDir)) {
            return Optional.empty();
        }
        String version = capture(VSWHERE,
                List.of("-latest", "-products", "*", "-property", "installationVersion"));
        int major = parseMajor(version);
        Optional<Path> cmake = exe(STANDALONE_CMAKE).or(() -> which("cmake"));
        Optional<Path> ctest = cmake.map(p -> p.resolveSibling("ctest.exe"))
                .filter(Files::isRegularFile);
        Optional<Path> ninja = which("ninja");
        Optional<Path> gdb = which("gdb");
        Optional<Path> clangTidy = exe(STANDALONE_LLVM_BIN.resolve("clang-tidy.exe"))
                .or(() -> which("clang-tidy"));
        Optional<Path> clangFormat = exe(STANDALONE_LLVM_BIN.resolve("clang-format.exe"))
                .or(() -> which("clang-format"));
        Optional<Path> compiler = findCl(vsDir);
        Optional<String> generator = msvcGenerator(major, cmake);
        String displayName = generator.map(g -> "MSVC (" + g + " generator)").orElse("MSVC");
        return Optional.of(new Toolchain("msvc", displayName, cmake, ninja, compiler, ctest, gdb, clangTidy,
                clangFormat, generator, List.of()));
    }

    /** Picks the matching "Visual Studio <major> <year>" generator from cmake --help, with a static fallback. */
    private static Optional<String> msvcGenerator(int major, Optional<Path> cmake) {
        if (cmake.isPresent() && major > 0) {
            String help = capture(cmake.get(), List.of("--help"));
            if (help != null) {
                String clean = ANSI.matcher(help).replaceAll("");
                Matcher m = VS_GENERATOR.matcher(clean);
                while (m.find()) {
                    if (Integer.parseInt(m.group(1)) == major) {
                        return Optional.of(m.group(0));
                    }
                }
            }
        }
        return switch (major) {
            case 17 -> Optional.of("Visual Studio 17 2022");
            default -> Optional.of("Visual Studio 18 2026");
        };
    }

    /** Newest cl.exe under the VS installation's VC Tools (bin/Hostx64/x64). */
    private static Optional<Path> findCl(Path vsDir) {
        Path base = vsDir.resolve("VC").resolve("Tools").resolve("MSVC");
        if (!Files.isDirectory(base)) {
            return Optional.empty();
        }
        try (Stream<Path> versions = Files.list(base)) {
            return versions.filter(Files::isDirectory)
                    .filter(v -> v.getFileName() != null)
                    .max(Comparator.comparing(v -> v.getFileName().toString(),
                            ToolchainRegistry::compareVersions))
                    .map(v -> v.resolve("bin").resolve("Hostx64").resolve("x64").resolve("cl.exe"))
                    .filter(Files::isRegularFile);
        } catch (java.io.IOException e) {
            return Optional.empty();
        }
    }

    private static int compareVersions(String a, String b) {
        int[] va = versionSegments(a);
        int[] vb = versionSegments(b);
        for (int i = 0; i < Math.max(va.length, vb.length); i++) {
            int x = i < va.length ? va[i] : 0;
            int y = i < vb.length ? vb[i] : 0;
            if (x != y) {
                return Integer.compare(x, y);
            }
        }
        return 0;
    }

    private static int[] versionSegments(String version) {
        String[] parts = version.split("\\.");
        int[] segments = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try {
                segments[i] = Integer.parseInt(parts[i]);
            } catch (NumberFormatException e) {
                segments[i] = 0;
            }
        }
        return segments;
    }

    private static int parseMajor(String version) {
        if (version == null) {
            return -1;
        }
        String trimmed = version.trim();
        int dot = trimmed.indexOf('.');
        String major = dot > 0 ? trimmed.substring(0, dot) : trimmed;
        try {
            return Integer.parseInt(major);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /** Runs a helper executable (30s cap) and returns its stdout, or null on failure. */
    private static String capture(Path exe, List<String> args) {
        List<String> command = new ArrayList<>();
        command.add(exe.toString());
        command.addAll(args);
        BuildRunner.ToolResult result = BuildRunner.run(command, List.of(), null, Duration.ofSeconds(30));
        return result.exitCode() == 0 ? result.output() : null;
    }

    private static Optional<Path> which(String tool) {
        String path = System.getenv("PATH");
        if (path == null || path.isEmpty()) {
            return Optional.empty();
        }
        for (String dir : path.split(Pattern.quote(File.pathSeparator))) {
            if (dir.isBlank()) {
                continue;
            }
            Path withExe = Paths.get(dir).resolve(tool + ".exe");
            if (Files.isRegularFile(withExe)) {
                return Optional.of(withExe);
            }
            Path bare = Paths.get(dir).resolve(tool);
            if (Files.isRegularFile(bare)) {
                return Optional.of(bare);
            }
        }
        return Optional.empty();
    }

    private static Optional<Path> exe(Path path) {
        return Files.isRegularFile(path) ? Optional.of(path) : Optional.empty();
    }
}
