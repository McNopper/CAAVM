package com.opencode.ide.git;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
 * End-to-end tests against real git repos in a temp dir (one bare origin plus
 * clones for the ahead/behind cases). Skipped when git is not available.
 */
public class StoreGitStatusTest {

    private Path base;

    @Before
    public void setUp() throws Exception {
        Assume.assumeTrue("git not available", gitAvailable());
        base = Files.createTempDirectory("opencode-store-status").toAbsolutePath().normalize();
    }

    @After
    public void tearDown() {
        if (base != null && Files.isDirectory(base)) {
            deleteRecursively(base);
        }
    }

    @Test
    public void cleanCloneWithUpstreamIsAllZero() throws Exception {
        Path clone = cloneOfOrigin("clone");
        StoreGitStatus st = StoreGitStatus.load(clone);
        assertTrue(st.exists());
        assertEquals("main", st.branch());
        assertEquals(0, st.ahead());
        assertEquals(0, st.behind());
        assertEquals(0, st.changed());
        assertFalse(st.detached());
        assertEquals("main", st.summary());
    }

    @Test
    public void noUpstreamReportsBranchWithoutDivergence() throws Exception {
        Path repo = newRepo("repo");
        StoreGitStatus st = StoreGitStatus.load(repo);
        assertTrue(st.exists());
        assertEquals("main", st.branch());
        assertEquals(0, st.ahead());
        assertEquals(0, st.behind());
        assertEquals("main", st.summary());
    }

    @Test
    public void localCommitsCountAsAhead() throws Exception {
        Path repo = repoWithUpstream("repo");
        write(repo, "file.txt", "two\n");
        commit(repo, "second");
        write(repo, "file.txt", "three\n");
        commit(repo, "third");
        StoreGitStatus st = StoreGitStatus.load(repo);
        assertEquals(2, st.ahead());
        assertEquals(0, st.behind());
        assertEquals("main · ahead 2", st.summary());
    }

    @Test
    public void remoteCommitsCountAsBehind() throws Exception {
        Path origin = sharedOrigin();
        Path repo = cloneOf("repo", origin);
        Path other = cloneOf("other", origin);
        write(other, "other.txt", "other\n");
        commit(other, "other change");
        git(other, "push");
        git(repo, "fetch");
        StoreGitStatus st = StoreGitStatus.load(repo);
        assertEquals(0, st.ahead());
        assertEquals(1, st.behind());
        assertEquals("main · behind 1", st.summary());
    }

    @Test
    public void divergedHistoryCountsAheadAndBehind() throws Exception {
        Path origin = sharedOrigin();
        Path repo = cloneOf("repo", origin);
        Path other = cloneOf("other", origin);
        write(repo, "file.txt", "local\n");
        commit(repo, "local change");
        write(other, "other.txt", "other\n");
        commit(other, "other change");
        git(other, "push");
        git(repo, "fetch");
        StoreGitStatus st = StoreGitStatus.load(repo);
        assertEquals(1, st.ahead());
        assertEquals(1, st.behind());
        assertEquals("main · ahead 1 · behind 1", st.summary());
    }

    @Test
    public void detachedHeadIsFlaggedWithNullBranch() throws Exception {
        Path repo = newRepo("repo");
        git(repo, "checkout", "--detach", "HEAD");
        write(repo, "extra.txt", "extra\n");
        StoreGitStatus st = StoreGitStatus.load(repo);
        assertTrue(st.exists());
        assertTrue(st.detached());
        assertNull(st.branch());
        assertEquals(1, st.changed());
        assertTrue(st.summary(), st.summary().startsWith("detached"));
        assertEquals("detached · 1 changed", st.summary());
    }

    @Test
    public void dirtyFilesAreCounted() throws Exception {
        Path repo = newRepo("repo");
        write(repo, "file.txt", "changed\n");
        write(repo, "untracked.txt", "new\n");
        StoreGitStatus st = StoreGitStatus.load(repo);
        assertEquals(2, st.changed());
        assertEquals("main · 2 changed", st.summary());
    }

    @Test
    public void summaryOmitsZeroParts() throws Exception {
        Path repo = repoWithUpstream("repo");
        write(repo, "file.txt", "two\n");
        commit(repo, "second");
        write(repo, "file.txt", "three\n");
        commit(repo, "third");
        write(repo, "file.txt", "dirty\n");
        write(repo, "untracked.txt", "new\n");
        StoreGitStatus st = StoreGitStatus.load(repo);
        assertEquals("main · ahead 2 · 2 changed", st.summary());
    }

    @Test
    public void nonRepoDirLoadsNone() throws Exception {
        Path dir = base.resolve("not-a-repo");
        Files.createDirectories(dir);
        StoreGitStatus st = StoreGitStatus.load(dir);
        assertEquals(StoreGitStatus.NONE, st);
        assertFalse(st.exists());
        assertNull(st.branch());
        assertFalse(st.detached());
        assertEquals(0, st.changed());
        assertEquals("", st.summary());
    }

    @Test
    public void midRebaseShowsDetachedAndConflicts() throws Exception {
        Path origin = sharedOrigin();
        Path repo = cloneOf("repo", origin);
        Path other = cloneOf("other", origin);
        write(other, "file.txt", "other\n");
        commit(other, "other change");
        git(other, "push");
        write(repo, "file.txt", "local\n");
        commit(repo, "local change");
        try {
            git(repo, "pull", "--rebase");
            fail("expected pull --rebase to fail");
        } catch (IllegalStateException expected) {
            // conflicted
        }
        StoreGitStatus st = StoreGitStatus.load(repo);
        assertTrue(st.exists());
        assertTrue(st.detached());
        assertNull(st.branch());
        assertTrue(st.summary(), st.summary().startsWith("detached · "));
        assertTrue("expected conflicted file to count as changed: " + st.changed(), st.changed() >= 1);
    }

    @Test
    public void unbornBranchStillReportsBranchName() throws Exception {
        Path repo = base.resolve("unborn");
        Files.createDirectories(repo);
        git(repo, "init", "-b", "main");
        write(repo, "task.md", "task\n");
        StoreGitStatus st = StoreGitStatus.load(repo);
        assertTrue(st.exists());
        assertFalse(st.detached());
        assertEquals("main", st.branch());
        assertEquals(1, st.changed());
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

    private Path repoWithUpstream(String name) throws Exception {
        Path origin = sharedOrigin();
        Path repo = cloneOf(name, origin);
        return repo;
    }

    private Path cloneOfOrigin(String name) throws Exception {
        return cloneOf(name, sharedOrigin());
    }

    private Path cloneOf(String name, Path origin) throws Exception {
        Path clone = base.resolve(name);
        git(base, "clone", origin.toString(), clone.toString());
        git(clone, "config", "user.email", "a@b.c");
        git(clone, "config", "user.name", "Test");
        return clone;
    }

    private static void write(Path repo, String file, String content) throws Exception {
        Files.writeString(repo.resolve(file), content, StandardCharsets.UTF_8);
    }

    private static void commit(Path repo, String message) throws Exception {
        git(repo, "add", ".");
        git(repo, "commit", "-m", message);
    }

    private static void git(Path dir, String... args) throws Exception {
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
