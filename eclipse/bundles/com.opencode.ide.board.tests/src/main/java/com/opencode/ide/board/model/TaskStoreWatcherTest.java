package com.opencode.ide.board.model;

import static org.junit.Assert.assertTrue;

import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.opencode.ide.tasks.TaskStore;

/**
 * Unit tests for {@link TaskStoreWatcher}: it must fire (within ~3 s) when the
 * store writes a ticket through its atomic tmp-rename pattern, and via the
 * poll fallback when the watched project directory appears only after start.
 */
public class TaskStoreWatcherTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void firesWhenTaskStoreWrites() throws Exception {
        Path root = tmp.newFolder().toPath();
        TaskStore store = new TaskStore(root);
        store.create("p", TaskStore.CreateSpec.of("seed"));

        CountDownLatch fired = new CountDownLatch(1);
        TaskStoreWatcher watcher = new TaskStoreWatcher(root.resolve("p"), fired::countDown);
        watcher.start();
        try {
            store.create("p", TaskStore.CreateSpec.of("second"));
            assertTrue("watcher should fire within 3s of a TaskStore write",
                    fired.await(3, TimeUnit.SECONDS));
        } finally {
            watcher.stop();
        }
    }

    @Test
    public void firesViaPollWhenProjectDirAppears() throws Exception {
        Path root = tmp.newFolder().toPath().resolve("tasks");

        CountDownLatch fired = new CountDownLatch(1);
        TaskStoreWatcher watcher = new TaskStoreWatcher(root.resolve("p"), fired::countDown);
        watcher.start();
        try {
            new TaskStore(root).create("p", TaskStore.CreateSpec.of("first"));
            assertTrue("watcher should fire via the poll fallback when the dir appears",
                    fired.await(8, TimeUnit.SECONDS));
        } finally {
            watcher.stop();
        }
    }

    @Test
    public void stopIsIdempotentAndRestartable() throws Exception {
        Path root = tmp.newFolder().toPath();
        TaskStore store = new TaskStore(root);
        store.create("p", TaskStore.CreateSpec.of("seed"));

        CountDownLatch fired = new CountDownLatch(1);
        TaskStoreWatcher watcher = new TaskStoreWatcher(root.resolve("p"), fired::countDown);
        watcher.start();
        watcher.stop();
        watcher.stop();

        watcher.start();
        try {
            store.create("p", TaskStore.CreateSpec.of("after restart"));
            assertTrue(fired.await(3, TimeUnit.SECONDS));
        } finally {
            watcher.stop();
        }
    }
}
