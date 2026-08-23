package com.opencode.ide.client.model;

/**
 * One text-search match of {@code GET /find?pattern=…}: the file path, the
 * matched line(s) and the 1-based line number.
 */
public record SearchMatch(
        String path,
        String lines,
        Integer line_number) {

    /** @return the 1-based line number, or {@code 0} when the server omitted it. */
    public int line() {
        return line_number == null ? 0 : line_number;
    }
}
