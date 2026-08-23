package com.opencode.ide.ui.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import com.opencode.ide.client.model.SearchMatch;
import com.opencode.ide.client.model.SymbolResult;

/**
 * Unit tests for the SWT-free {@link SearchResults} behind the Repo view:
 * prefix-based mode parsing plus the dedup/cap/format rules of the display
 * rows. No SWT, no JFace, no Display.
 */
public class SearchResultsTest {

    // ---------- parse (the search-mode mechanism) ----------

    @Test
    public void plainQueryIsFileSearch() {
        assertEquals(SearchResults.Mode.FILE, SearchResults.parse("readme").mode());
        assertEquals("readme", SearchResults.parse("readme").text());
        assertEquals(SearchResults.Mode.FILE, SearchResults.parse("  readme  ").mode());
        assertEquals("readme", SearchResults.parse("  readme  ").text());
    }

    @Test
    public void atPrefixIsSymbolSearchAndSlashPrefixIsTextSearch() {
        assertEquals(SearchResults.Mode.SYMBOL, SearchResults.parse("@RepoView").mode());
        assertEquals("RepoView", SearchResults.parse("@RepoView").text());
        assertEquals(SearchResults.Mode.SYMBOL, SearchResults.parse("@ spaced ").mode());
        assertEquals("spaced", SearchResults.parse("@ spaced ").text());
        assertEquals(SearchResults.Mode.TEXT, SearchResults.parse("/TODO").mode());
        assertEquals("TODO", SearchResults.parse("/TODO").text());
    }

    @Test
    public void nullAndBlankQueriesAreEmptyFileQueries() {
        assertEquals(SearchResults.Mode.FILE, SearchResults.parse(null).mode());
        assertEquals("", SearchResults.parse(null).text());
        assertTrue(SearchResults.parse(null).isEmpty());
        assertTrue(SearchResults.parse("").isEmpty());
        assertTrue(SearchResults.parse("   ").isEmpty());
        assertTrue(SearchResults.parse("@").isEmpty()); // prefix without text
        assertTrue(SearchResults.parse("/").isEmpty());
    }

    // ---------- fromFiles ----------

    @Test
    public void fromFilesLabelsByLastSegmentAndDedups() {
        // Arrays.asList (not List.of): the fixture intentionally contains nulls
        List<SearchResults.Row> rows = SearchResults.fromFiles(
                java.util.Arrays.asList("docs/readme.md", "readme.md", "docs/readme.md", null, " "));

        assertEquals(2, rows.size());
        assertEquals("readme.md", rows.get(0).label());
        assertEquals("docs/readme.md", rows.get(0).location());
        assertEquals("docs/readme.md", rows.get(0).path());
        assertEquals("file", rows.get(0).kind());
        assertEquals("readme.md", rows.get(1).location());
    }

    @Test
    public void fromFilesCapsAtDefaultCapAfterDedup() {
        // 60 unique paths but the first path repeats later: still 50 rows
        java.util.List<String> paths = new java.util.ArrayList<>();
        for (int i = 0; i < 60; i++) {
            paths.add("f" + i + ".txt");
        }
        paths.add("f0.txt"); // duplicate of the first

        List<SearchResults.Row> rows = SearchResults.fromFiles(paths);

        assertEquals(SearchResults.DEFAULT_CAP, rows.size());
        assertEquals("f49.txt", rows.get(49).label()); // cap keeps the first 50
    }

    @Test
    public void fromFilesToleratesNullList() {
        assertEquals(List.of(), SearchResults.fromFiles(null));
        assertEquals(List.of(), SearchResults.fromFiles(List.of()));
    }

    // ---------- fromSymbols ----------

    @Test
    public void fromSymbolsFormatsKindLabelAndLocation() {
        List<SearchResults.Row> rows = SearchResults.fromSymbols(List.of(
                new SymbolResult("refresh", "function", "ui/RepoView.java", 120),
                new SymbolResult("RepoView", null, "ui/RepoView.java", null)));

        assertEquals(2, rows.size());
        assertEquals("function", rows.get(0).kind());
        assertEquals("refresh", rows.get(0).label());
        assertEquals("ui/RepoView.java:120", rows.get(0).location());
        assertEquals(120, rows.get(0).lineNumber());
        // missing kind -> "symbol"; missing line -> bare path in the location
        assertEquals("symbol", rows.get(1).kind());
        assertEquals("RepoView", rows.get(1).label());
        assertEquals("ui/RepoView.java", rows.get(1).location());
        assertEquals(0, rows.get(1).lineNumber());
    }

    @Test
    public void fromSymbolsDedupsAndCaps() {
        java.util.List<SymbolResult> symbols = new java.util.ArrayList<>();
        symbols.add(new SymbolResult("sym0", "function", "F0.java", 0));
        symbols.add(new SymbolResult("sym1", "function", "F1.java", 1));
        symbols.add(new SymbolResult("sym1", "function", "F1.java", 1)); // exact duplicate
        for (int i = 2; i < SearchResults.DEFAULT_CAP + 10; i++) {
            symbols.add(new SymbolResult("sym" + i, "function", "F" + i + ".java", i));
        }

        List<SearchResults.Row> rows = SearchResults.fromSymbols(symbols);

        assertEquals(SearchResults.DEFAULT_CAP, rows.size()); // dup dropped, then capped
        assertEquals("F0.java", rows.get(0).location()); // line 0 -> bare path
    }

    @Test
    public void fromSymbolsSkipsNullsAndPathlessEntriesAndToleratesNullList() {
        assertEquals(List.of(), SearchResults.fromSymbols(null));
        assertEquals(List.of(), SearchResults.fromSymbols(List.of()));
        // Arrays.asList (not List.of): the fixture intentionally contains nulls
        assertEquals(0, SearchResults.fromSymbols(
                java.util.Arrays.asList(null, new SymbolResult("x", "class", null, 1),
                        new SymbolResult("y", "class", " ", 2)))
                .size());
    }

    // ---------- fromText ----------

    @Test
    public void fromTextCollapsesAndCapsTheLineLabel() {
        List<SearchResults.Row> rows = SearchResults.fromText(List.of(
                new SearchMatch("a/b.txt", "  hello   \n world  ", 7)));

        assertEquals(1, rows.size());
        assertEquals("text", rows.get(0).kind());
        assertEquals("hello world", rows.get(0).label());
        assertEquals("a/b.txt:7", rows.get(0).location());

        String longLine = "x".repeat(200);
        List<SearchResults.Row> capped = SearchResults.fromText(
                List.of(new SearchMatch("a.txt", longLine, 1)));
        assertEquals(SearchResults.LINE_LABEL_MAX + 1, capped.get(0).label().length()); // + ellipsis
        assertTrue(capped.get(0).label().endsWith("…"));
    }

    @Test
    public void fromTextDedupsByPathAndLineAndDefaultsMissingPreview() {
        List<SearchResults.Row> rows = SearchResults.fromText(List.of(
                new SearchMatch("a.txt", "one", 3),
                new SearchMatch("a.txt", "one", 3),   // exact dup
                new SearchMatch("a.txt", "two", 4),   // same path, other line: kept
                new SearchMatch("b.txt", null, null))); // no line -> 0, no preview

        assertEquals(3, rows.size());
        assertEquals("b.txt", rows.get(2).location()); // line 0 -> bare path
        assertEquals("(no preview)", rows.get(2).label());
    }

    @Test
    public void fromTextToleratesNullList() {
        assertEquals(List.of(), SearchResults.fromText(null));
        assertEquals(List.of(), SearchResults.fromText(List.of()));
    }
}
