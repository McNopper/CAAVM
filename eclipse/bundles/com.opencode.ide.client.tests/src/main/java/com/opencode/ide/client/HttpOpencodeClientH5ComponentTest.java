package com.opencode.ide.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.opencode.ide.client.model.ChatEntry;
import com.opencode.ide.client.model.CommandInfo;
import com.opencode.ide.client.model.ConfigInfo;
import com.opencode.ide.client.model.FileDiff;
import com.opencode.ide.client.model.FileNode;
import com.opencode.ide.client.model.ProjectSummary;
import com.opencode.ide.client.model.SearchMatch;
import com.opencode.ide.client.model.Session;
import com.opencode.ide.client.model.SymbolResult;
import com.opencode.ide.client.model.VcsInfo;

/**
 * Component test for the H5 client surface (session lifecycle, diff,
 * permissions, commands, project/vcs, file/find, tui, config patch): real
 * {@code HttpOpencodeClient} over real HTTP against a local stub server,
 * verifying paths, methods, bodies and parsing. No Eclipse, no opencode.
 */
public class HttpOpencodeClientH5ComponentTest {

    private static com.sun.net.httpserver.HttpServer server;
    private static OpencodeClient client;

    private static final AtomicReference<String> lastMethod = new AtomicReference<>();
    private static final AtomicReference<String> lastPath = new AtomicReference<>();
    private static final AtomicReference<String> lastQuery = new AtomicReference<>();
    private static final AtomicReference<String> lastBody = new AtomicReference<>();

    @BeforeClass
    public static void startStub() throws IOException {
        server = com.sun.net.httpserver.HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            lastMethod.set(exchange.getRequestMethod());
            lastPath.set(exchange.getRequestURI().getPath());
            lastQuery.set(exchange.getRequestURI().getRawQuery());
            lastBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] bytes = respond(exchange.getRequestURI().getPath());
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        });
        server.start();
        int port = server.getAddress().getPort();
        client = new com.opencode.ide.client.internal.HttpOpencodeClient(
                new ConnectionConfig(URI.create("http://127.0.0.1:" + port), null, null));
    }

    private static byte[] respond(String path) {
        String json = switch (path) {
            case "/session/ses_1/diff" -> """
                    [{"path":"src/a.cpp","before":"HEAD","after":"opencode/ses_1",
                      "content":"--- a/src/a.cpp\\n+++ b/src/a.cpp\\n@@ -1 +1 @@\\n-x\\n+y"}]
                    """;
            case "/session/ses_1/fork" -> "{\"id\":\"ses_fork\",\"time\":{\"created\":9,\"updated\":9}}";
            case "/session/ses_1/revert", "/session/ses_1/unrevert", "/session/ses_1/summarize",
                 "/session/ses_1/permissions/perm_7" -> "true";
            case "/session/ses_1/share" -> "{\"id\":\"ses_1\",\"share\":{\"url\":\"https://x/sh/s1\"}}";
            case "/command" -> """
                    [{"name":"review","description":"request a code review"},
                     {"name":"ship"}]
                    """;
            case "/session/ses_1/command" -> """
                    {"info":{"id":"msg_c1","sessionID":"ses_1","role":"assistant","time":{"created":3}},
                     "parts":[{"type":"text","text":"command ran"}]}
                    """;
            case "/project" -> """
                    [{"worktree":"C:/repo","vcs":{"branch":"main","repository":"git@github.com:o/r.git"}}]
                    """;
            case "/vcs" -> "{\"branch\":\"main\",\"repository\":\"git@github.com:o/r.git\"}";
            case "/file" -> """
                    [{"name":"src","path":"src","type":"directory"},
                     {"name":"CMakeLists.txt","path":"CMakeLists.txt","type":"file"}]
                    """;
            case "/find" -> """
                    [{"path":"src/a.cpp","lines":"int add(int a, int b)","line_number":3}]
                    """;
            case "/find/file" -> "[\"src/a.cpp\",\"src/b.cpp\"]";
            case "/find/symbol" -> """
                    [{"name":"add","kind":"function","path":"src/a.cpp","line":3}]
                    """;
            case "/config" -> "{\"model\":\"prov/m2\",\"small_model\":null}";
            default -> "{}";
        };
        return json.getBytes(StandardCharsets.UTF_8);
    }

    @AfterClass
    public static void stopStub() {
        server.stop(0);
    }

    @Test
    public void sessionDiffParsesFilesAndPatch() throws Exception {
        List<FileDiff> diffs = client.getSessionDiff("ses_1", null);
        assertEquals(1, diffs.size());
        assertEquals("src/a.cpp", diffs.get(0).path());
        assertTrue(diffs.get(0).content().contains("+y"));
        assertEquals("GET", lastMethod.get());
        assertEquals("/session/ses_1/diff", lastPath.get());
    }

    @Test
    public void forkPostsAndParsesSession() throws Exception {
        Session fork = client.forkSession("ses_1", "msg_2");
        assertEquals("ses_fork", fork.id());
        assertEquals("POST", lastMethod.get());
        assertTrue(lastBody.get().contains("\"messageID\":\"msg_2\""));
    }

    @Test
    public void revertUnrevertSummarizeReturnBoolean() throws Exception {
        assertTrue(client.revertMessage("ses_1", "msg_2", null));
        assertTrue(lastBody.get().contains("\"messageID\":\"msg_2\""));
        assertTrue(client.unrevertSession("ses_1"));
        assertTrue(client.summarizeSession("ses_1", "prov", "m1"));
        assertTrue(lastBody.get().contains("\"providerID\":\"prov\""));
    }

    @Test
    public void shareReturnsSessionWithSharePayload() throws Exception {
        Session shared = client.shareSession("ses_1");
        assertEquals("ses_1", shared.id());
        Session unshared = client.unshareSession("ses_1");
        assertEquals("DELETE", lastMethod.get());
        assertNotNull(unshared);
    }

    @Test
    public void permissionResponsePostsBody() throws Exception {
        assertTrue(client.respondToPermission("ses_1", "perm_7", "once", false));
        assertEquals("/session/ses_1/permissions/perm_7", lastPath.get());
        assertTrue(lastBody.get().contains("\"response\":\"once\""));
        assertTrue(lastBody.get().contains("\"remember\":false"));
    }

    @Test
    public void commandsListAndRun() throws Exception {
        List<CommandInfo> commands = client.getCommands();
        assertEquals(2, commands.size());
        assertEquals("review", commands.get(0).name());

        ChatEntry reply = client.runCommand("ses_1", "review", List.of("src/a.cpp"));
        assertEquals("command ran", reply.text());
        assertEquals("POST", lastMethod.get());
        assertTrue(lastBody.get().contains("\"command\":\"review\""));
        assertTrue(lastBody.get().contains("src/a.cpp"));
    }

    @Test
    public void projectAndVcsParseLeniently() throws Exception {
        List<ProjectSummary> projects = client.getProjects();
        assertEquals(1, projects.size());
        assertEquals("C:/repo", projects.get(0).worktree());
        assertEquals("main", projects.get(0).branch());

        VcsInfo vcs = client.getVcsInfo();
        assertEquals("main", vcs.branch());
        assertEquals("git@github.com:o/r.git", vcs.repository());
    }

    @Test
    public void fileTreeFindAndSymbolsParse() throws Exception {
        List<FileNode> nodes = client.listFiles(null);
        assertEquals(2, nodes.size());
        assertTrue(nodes.get(0).isDirectory());
        assertFalse(nodes.get(1).isDirectory());

        List<SearchMatch> matches = client.findText("add");
        assertEquals(1, matches.size());
        assertEquals(3, matches.get(0).line());

        List<String> files = client.findFiles("a.cpp");
        assertEquals(List.of("src/a.cpp", "src/b.cpp"), files);

        List<SymbolResult> symbols = client.findSymbols("add");
        assertEquals(1, symbols.size());
        assertEquals("function", symbols.get(0).kind());
        assertEquals(3, symbols.get(0).lineNumber());
    }

    /**
     * The server rejects {@code GET /file} without a {@code path} key with
     * HTTP 400 ({@code Missing key at ["path"]}), which used to break the Repo
     * view's very first (root) load. Every listing must carry the key.
     */
    @Test
    public void listFilesAlwaysSendsThePathQueryKey() throws Exception {
        client.listFiles(null);
        assertEquals("/file", lastPath.get());
        assertEquals("path=.", lastQuery.get());

        client.listFiles("");
        assertEquals("path=.", lastQuery.get());

        client.listFiles("  ");
        assertEquals("path=.", lastQuery.get());

        client.listFiles(".");
        assertEquals("path=.", lastQuery.get());

        client.listFiles("cpp/src");
        assertEquals("path=cpp%2Fsrc", lastQuery.get());

        // the server answers with Windows separators; they must survive encoding
        client.listFiles("cpp\\src\\");
        assertEquals("path=cpp%5Csrc%5C", lastQuery.get());
    }

    @Test
    public void configPatchSendsChangesAndParses() throws Exception {
        ConfigInfo config = client.patchConfig(Map.of("model", "prov/m2"));
        assertEquals("PATCH", lastMethod.get());
        assertEquals("/config", lastPath.get());
        assertTrue(lastBody.get().contains("\"model\":\"prov/m2\""));
        assertNotNull(config);
    }

    @Test
    public void tuiActionPostsAndReturnsTrue() throws Exception {
        assertTrue(client.tuiAction("append-prompt", Map.of("text", "hello")));
        assertEquals("/tui/append-prompt", lastPath.get());
        assertTrue(lastBody.get().contains("hello"));

        assertTrue(client.tuiAction("open-models", null)); // no-arg action: no body
        assertEquals("", lastBody.get());
    }
}
