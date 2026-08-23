package com.opencode.ide.client.model;

/**
 * One node of the workspace file tree ({@code GET /file?path=…}): a file or
 * directory with its project-relative path and name.
 */
public record FileNode(
        String name,
        String path,
        String type) {

    /** @return true when this node is a directory (server sends {@code "directory"}). */
    public boolean isDirectory() {
        return "directory".equalsIgnoreCase(type);
    }
}
