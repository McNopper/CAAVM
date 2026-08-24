package com.opencode.ide.git;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

/**
 * End-to-end tests of the pull→commit→push discipline against real git repos
 * in a temp dir, with bare clones as origins. Skipped when git is not
 * available.
 */
public class StoreSyncTest {

    private Path base;

    @Before
    public void setUp() throws Exception {
        Assume.assumeTrue("git not available", gitAvailable());
        base = Files.createTempDirectory("opencode-store-sync").toAbsolutePath().normalize();
    }

    @After
    public void tearDown() {
        if (base != null && Files.isDirectory(base)) {
            deleteRecursively(base);
        }
    }

    @Test
    public void nothingToDoIsUpToDate() throws Exception {
        Path clone = cloneOf("clone", sharedOrigin());
        assertEquals(StoreSync.Outcome.UP_TO_DATE, StoreSync.sync(clone, "sync"));
        assertEquals(0, StoreGitStatus.load(clone).changed());
    }

    @Test
    public void newStoreFileIsCommittedAndPushed() throws Exception {
        Path origin = sharedOrigin();
        Path clone = cloneOf("clone", origin);
        write(clone, "project/T-001.md", "task\n");
        assertEquals(StoreSync.Outcome.PUSHED, StoreSync.sync(clone, "claim T-001"));
        assertEquals("claim T-001", gitOut(origin, "log", "-1", "--format=%s").trim());
        assertEquals(StoreSync.Outcome.UP_TO_DATE, StoreSync.sync(clone, "sync"));
        assertEquals(StoreGitStatus.load(clone).summary(), "main", StoreGitStatus.load(clone).summary());
    }

    @Test
    public void divergedClonesReportPullConflict() throws Exception {
        Path origin = sharedOrigin();
        Path a = cloneOf("a", origin);
        Path b = cloneOf("b", origin);
        write(a, "file.txt", "a\n");
        commit(a, "a change");
        assertEquals(StoreSync.Outcome.PUSHED, StoreSync.sync(a, "a sync"));
        write(b, "file.txt", "b\n");
        commit(b, "b change");
        assertEquals(StoreSync.Outcome.PULL_CONFLICT, StoreSync.sync(b, "b sync"));
        assertTrue(StoreGitStatus.load(b).detached());
    }

    @Test
    public void recoverCleansStateThenSyncProceedsAfterManualResolution() throws Exception {
        Path origin = sharedOrigin();
        Path a = cloneOf("a", origin);
        Path b = cloneOf("b", origin);
        write(a, "file.txt", "a\n");
        commit(a, "a change");
        assertEquals(StoreSync.Outcome.PUSHED, StoreSync.sync(a, "a sync"));
        write(b, "file.txt", "b\n");
        commit(b, "b change");
        assertEquals(StoreSync.Outcome.PULL_CONFLICT, StoreSync.sync(b, "b sync"));
        assertEquals(StoreSync.Outcome.FAILED, StoreSync.recover(b));
        StoreGitStatus st = StoreGitStatus.load(b);
        assertFalse("expected to be back on the branch after recover", st.detached());
        assertEquals("main", st.branch());
        assertEquals(0, st.changed());
        git(b, "reset", "--hard", "origin/main");
        assertEquals(StoreSync.Outcome.UP_TO_DATE, StoreSync.sync(b, "b sync"));
        assertEquals("a", Files.readString(b.resolve("file.txt")).replace("\r", "").replace("\n", ""));
    }

    @Test
    public void rejectedPushIsReported() throws Exception {
        Path origin = sharedOrigin();
        Path rejecting = base.resolve("rejecting.git");
        git(base, "clone", "--bare", origin.toString(), rejecting.toString());
        Path a = cloneOf("a", rejecting);
        write(a, "a.txt", "a\n");
        commit(a, "a change");
        git(a, "push", rejecting.toString(), "main");
        Path b = cloneOf("b", origin);
        git(b, "remote", "set-url", "--push", "origin", rejecting.toString());
        write(b, "b.txt", "b\n");
        assertEquals(StoreSync.Outcome.PUSH_REJECTED, StoreSync.sync(b, "b sync"));
    }

    @Test
    public void nonRepoIsNotARepo() throws Exception {
        Path dir = base.resolve("not-a-repo");
        Files.createDirectories(dir);
        assertEquals(StoreSync.Outcome.NOT_A_REPO, StoreSync.sync(dir, "sync"));
        assertEquals(StoreSync.Outcome.NOT_A_REPO, StoreSync.recover(dir));
    }

    @Test
    public void noUpstreamPullFailsButKeepsLocalCommit() throws Exception {
        Path repo = newRepo("repo");
        write(repo, "file.txt", "two\n");
        assertEquals(StoreSync.Outcome.FAILED, StoreSync.sync(repo, "sync"));
        assertEquals("sync", gitOut(repo, "log", "-1", "--format=%s").trim());
        assertEquals("main", StoreGitStatus.load(repo).summary());
    }

    private Path newRepo(String name) throws Exception {
        Path repo = base.resolve(name);
        Files.createDirectories(repo);
        git(repo, "init");
        git(repo, "config", "user.email", "a@b.c");
        git(repo, "config", "user.name", "Test");
        write(repo, "file.txt", "one\n");
        git(repo, "add", ".");
        git(repo, "commit", "-m", "initial");
        git(repo, "branch", "-M", "main");
        return repo;
    }

    private Path sharedOrigin() throws Exception {
        Path seed = newRepo("seed");
        Path origin = base.resolve("origin.git");
        git(base, "clone", "--bare", seed.toString(), origin.toString());
        deleteRecursively(seed);
        return origin;
    }

    private Path cloneOf(String name, Path origin) throws Exception {
        Path clone = base.resolve(name);
        git(base, "clone", origin.toString(), clone.toString());
        git(clone, "config", "user.email", "a@b.c");
        git(clone, "config", "user.name", "Test");
        return clone;
    }

    private static void write(Path repo, String file, String content) throws Exception {
        Files.createDirectories(repo.resolve(file).getParent());
        Files.writeString(repo.resolve(file), content, StandardCharsets.UTF_8);
    }

    private static void commit(Path repo, String message) throws Exception {
        git(repo, "add", ".");
        git(repo, "commit", "-m", message);
    }

    private static String gitOut(Path dir, String... args) throws Exception {
        List<String> command = new ArrayList<>();
        command.add("git");
        command.add("-C");
        command.add(dir.toString());
        command.addAll(List.of(args));
        Process p = new ProcessBuilder(command).start();
        String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        String err = new String(p.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
        int code = p.waitFor();
        if (code != 0) {
            throw new IllegalStateException("git " + List.of(args) + " failed (exit " + code + "): " + err);
        }
        return out;
    }

    private static void git(Path dir, String... args) throws Exception {
        gitOut(dir, args);
    }

    private static boolean gitAvailable() {
        try {
            Process p = new ProcessBuilder("git", "--version").start();
            p.getErrorStream().readAllBytes();
            return p.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private static void deleteRecursively(Path root) {
        try (var walk = Files.walk(root)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    p.toFile().setWritable(true);
                    Files.deleteIfExists(p);
                } catch (IOException ignored) {
                }
            });
        } catch (IOException ignored) {
        }
    }
}
