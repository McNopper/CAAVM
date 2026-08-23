package com.opencode.ide.ui.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.opencode.ide.client.model.SearchMatch;
import com.opencode.ide.client.model.SymbolResult;

/**
 * Pure (SWT-free) formatting of the Repo view's search results: the client's
 * {@code findFiles}/{@code findSymbols}/{@code findText} lists merged into
 * uniform display {@link Row}s (kind, label, {@code path:line} location),
 * with deduplication and a result cap.
 *
 * <p>Also owns the view's search-mode convention: a plain query means fuzzy
 * file search, an {@code @} prefix switches to symbol search and a
 * {@code /} prefix to text search ({@link #parse(String)}).</p>
 */
public final class SearchResults {

    /** Results are capped after dedup so a huge workspace cannot flood the view. */
    public static final int DEFAULT_CAP = 50;

    /** Long text-match lines are truncated to keep rows readable. */
    public static final int LINE_LABEL_MAX = 120;

    /** The search modes selectable via the query prefix. */
    public enum Mode {
        /** Fuzzy file-name search (plain query). */
        FILE,
        /** Workspace symbol search ({@code @} prefix). */
        SYMBOL,
        /** Workspace text search ({@code /} prefix). */
        TEXT
    }

    /** A parsed search box input: the mode plus the prefix-stripped query text. */
    public record Query(Mode mode, String text) {
        /** @return true when there is nothing to search for. */
        public boolean isEmpty() {
            return text == null || text.isBlank();
        }
    }

    /**
     * One display row: {@code kind} ("file", the symbol's kind with
     * "symbol" as fallback, or "text"), a human label, the location
     * ({@code path} or {@code path:line}) plus the raw path/line for actions.
     */
    public record Row(String kind, String label, String location, String path, int lineNumber) {
    }

    private SearchResults() {
    }

    // ---------- query parsing (the search-mode mechanism) ----------

    /**
     * Parses the search box input: {@code null}/blank &rarr; an empty FILE
     * query; an {@code @} prefix &rarr; SYMBOL; a {@code /} prefix &rarr;
     * TEXT; anything else &rarr; FILE. The prefix is stripped and the
     * remaining text trimmed.
     */
    public static Query parse(String raw) {
        Mode mode = Mode.FILE;
        String text = raw == null ? "" : raw.trim();
        if (text.startsWith("@")) {
            mode = Mode.SYMBOL;
            text = text.substring(1).trim();
        } else if (text.startsWith("/")) {
            mode = Mode.TEXT;
            text = text.substring(1).trim();
        }
        return new Query(mode, text);
    }

    // ---------- row derivation ----------

    /** Maps fuzzy file-search paths to rows (dedup by path, capped). */
    public static List<Row> fromFiles(List<String> paths) {
        if (paths == null || paths.isEmpty()) {
            return List.of();
        }
        Set<String> seen = new HashSet<>();
        List<Row> rows = new ArrayList<>();
        for (String path : paths) {
            if (path == null || path.isBlank() || !seen.add(path)) {
                continue;
            }
            rows.add(new Row("file", fileNameOf(path), path, path, 0));
            if (rows.size() >= DEFAULT_CAP) {
                break;
            }
        }
        return rows;
    }

    /** Maps symbol results to rows (dedup by name/path/line, capped). */
    public static List<Row> fromSymbols(List<SymbolResult> symbols) {
        if (symbols == null || symbols.isEmpty()) {
            return List.of();
        }
        Set<String> seen = new HashSet<>();
        List<Row> rows = new ArrayList<>();
        for (SymbolResult symbol : symbols) {
            if (symbol == null || symbol.path() == null || symbol.path().isBlank()) {
                continue;
            }
            String key = symbol.name() + "|" + symbol.path() + "|" + symbol.lineNumber();
            if (!seen.add(key)) {
                continue;
            }
            String name = symbol.name() == null || symbol.name().isBlank()
                    ? "(unnamed)"
                    : symbol.name();
            String kind = symbol.kind() == null || symbol.kind().isBlank()
                    ? "symbol"
                    : symbol.kind();
            rows.add(new Row(kind, name, locationOf(symbol.path(), symbol.lineNumber()),
                    symbol.path(), symbol.lineNumber()));
            if (rows.size() >= DEFAULT_CAP) {
                break;
            }
        }
        return rows;
    }

    /** Maps text-search matches to rows (dedup by path/line, label collapsed and capped). */
    public static List<Row> fromText(List<SearchMatch> matches) {
        if (matches == null || matches.isEmpty()) {
            return List.of();
        }
        Set<String> seen = new HashSet<>();
        List<Row> rows = new ArrayList<>();
        for (SearchMatch match : matches) {
            if (match == null || match.path() == null || match.path().isBlank()) {
                continue;
            }
            String key = match.path() + "|" + match.line();
            if (!seen.add(key)) {
                continue;
            }
            rows.add(new Row("text", lineLabel(match.lines()),
                    locationOf(match.path(), match.line()), match.path(), match.line()));
            if (rows.size() >= DEFAULT_CAP) {
                break;
            }
        }
        return rows;
    }

    // ---------- helpers ----------

    /** @return the last segment of the path (the file name), or the whole path when flat. */
    static String fileNameOf(String path) {
        String p = RepoTree.normalize(path);
        if (p.isEmpty()) {
            return "(root)";
        }
        int idx = p.lastIndexOf('/');
        return idx >= 0 ? p.substring(idx + 1) : p;
    }

    /** @return {@code path:line} when the line is known (&gt; 0), else the bare path. */
    static String locationOf(String path, int line) {
        return line > 0 ? path + ":" + line : path;
    }

    /** Collapses the matched line(s) into one display label, capped at {@link #LINE_LABEL_MAX}. */
    static String lineLabel(String lines) {
        String label = lines == null ? "" : lines.replaceAll("\\s+", " ").trim();
        if (label.isEmpty()) {
            return "(no preview)";
        }
        return label.length() <= LINE_LABEL_MAX ? label : label.substring(0, LINE_LABEL_MAX) + "…";
    }
}
