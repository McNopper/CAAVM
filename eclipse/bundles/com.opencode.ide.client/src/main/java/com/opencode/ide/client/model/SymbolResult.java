package com.opencode.ide.client.model;

/**
 * One workspace symbol of {@code GET /find/symbol?query=…}: symbol name, its
 * kind (function/class/…), file path and 1-based line.
 */
public record SymbolResult(
        String name,
        String kind,
        String path,
        Integer line) {

    /** @return the 1-based line number, or {@code 0} when omitted. */
    public int lineNumber() {
        return line == null ? 0 : line;
    }
}
