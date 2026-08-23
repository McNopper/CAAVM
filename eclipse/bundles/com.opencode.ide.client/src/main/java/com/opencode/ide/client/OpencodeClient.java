package com.opencode.ide.client;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import com.opencode.ide.client.model.Agent;
import com.opencode.ide.client.model.ChatEntry;
import com.opencode.ide.client.model.CommandInfo;
import com.opencode.ide.client.model.ConfigInfo;
import com.opencode.ide.client.model.FileDiff;
import com.opencode.ide.client.model.FileNode;
import com.opencode.ide.client.model.HealthStatus;
import com.opencode.ide.client.model.McpServerInfo;
import com.opencode.ide.client.model.ProjectSummary;
import com.opencode.ide.client.model.ProviderList;
import com.opencode.ide.client.model.SearchMatch;
import com.opencode.ide.client.model.SkillInfo;
import com.opencode.ide.client.model.Session;
import com.opencode.ide.client.model.SessionStatus;
import com.opencode.ide.client.model.SessionTodo;
import com.opencode.ide.client.model.SymbolResult;
import com.opencode.ide.client.model.VcsInfo;

/**
 * Client for an opencode server ({@code opencode serve}). All methods perform
 * synchronous HTTP and throw {@link OpencodeException} on failure.
 *
 * <p>Methods mirror the documented REST surface (see the opencode "Server" docs
 * and the server's OpenAPI spec at {@code /doc}).</p>
 */
public interface OpencodeClient {

    /** {@code GET /global/health} - server health and version. */
    HealthStatus getHealth() throws OpencodeException;

    /** {@code GET /agent} - all available agent definitions. */
    List<Agent> getAgents() throws OpencodeException;

    /** {@code GET /config/providers} - providers, their models, and the defaults. */
    ProviderList getProviders() throws OpencodeException;

    /** {@code GET /config} - server config (default model etc.). */
    ConfigInfo getConfig() throws OpencodeException;

    /** {@code GET /session} - all sessions (running agent instances), including subagent children. */
    List<Session> getSessions() throws OpencodeException;

    /** {@code GET /session/status} - per-session status ({@code idle}/{@code busy}/{@code retry}). */
    Map<String, SessionStatus> getSessionStatus() throws OpencodeException;

    /**
     * {@code POST /session} - create a new session.
     *
     * @param title optional title (may be {@code null})
     */
    default Session createSession(String title) throws OpencodeException {
        return createSession(title, null);
    }

    /**
     * {@code POST /session?directory=…} - create a session scoped to a project
     * directory (e.g. a git worktree an agent should work in).
     *
     * @param title     optional title (may be {@code null})
     * @param directory working directory the session operates in
     *                  (may be {@code null} = server default)
     */
    Session createSession(String title, Path directory) throws OpencodeException;

    /**
     * {@code POST /mcp} - register an MCP server with the opencode server so its
     * tools become available to agents.
     *
     * @param name   the MCP server name (e.g. {@code "eclipse-build"})
     * @param config the endpoint to register
     */
    void registerMcp(String name, McpServerConfig config) throws OpencodeException;

    /**
     * {@code GET /mcp} - the MCP servers registered with the opencode server
     * (their ids and transport types). Default returns empty so test fakes and
     * partial implementations stay compiling.
     */
    default List<McpServerInfo> getMcpServers() throws OpencodeException {
        return List.of();
    }

    /**
     * {@code GET /skill} - the skills loaded from the working directory's
     * {@code .opencode/skills/}. Default returns empty so test fakes and
     * partial implementations stay compiling.
     */
    default List<SkillInfo> getSkills() throws OpencodeException {
        return List.of();
    }

    /** {@code GET /session/:id/message} - the message history of a session. */
    List<ChatEntry> getMessages(String sessionId) throws OpencodeException;

    /**
     * {@code GET /session/:id/todo} - the session's todo list (opencode v1.18).
     * Default returns empty so test fakes and partial implementations stay
     * compiling (same pattern as {@link #abortSession(String)}).
     */
    default List<SessionTodo> getSessionTodos(String sessionId) throws OpencodeException {
        return List.of();
    }

    /**
     * {@code POST /session/:id/message} - send a user prompt and wait for the
     * assistant reply. Streaming display should be driven by the {@code /event}
     * SSE stream; this call's result is the final, complete reply.
     *
     * @param request model/agent/variant/system + the prompt (see {@link ChatRequest})
     */
    ChatEntry sendMessage(ChatRequest request) throws OpencodeException;

    /**
     * {@code POST /session/:id/abort} - abort the running agent in a session.
     * A 4xx reply (typically "session is already idle") is tolerated and
     * logged, not surfaced; server (>= 5xx) and transport errors raise
     * {@link OpencodeException}.
     *
     * <p>Default implementation throws {@link UnsupportedOperationException} so
     * in-repo test fakes that never abort stay source-compatible;
     * {@code HttpOpencodeClient} overrides it.</p>
     */
    default void abortSession(String sessionId) throws OpencodeException {
        throw new UnsupportedOperationException("abortSession");
    }

    /**
     * {@code POST /log} - write an entry into the opencode server log.
     *
     * @param service free-form service identifier (e.g. {@code "opencode-eclipse"})
     * @param level   one of {@code DEBUG}, {@code INFO}, {@code WARN}, {@code ERROR}
     * @param message the log message
     * @param extra   optional structured payload (may be {@code null})
     */
    void log(String service, String level, String message, Map<String, Object> extra) throws OpencodeException;

    // ---------- H5 surface (opencode v1.18.x): all defaulted so existing fakes stay compiling ----------

    /**
     * {@code GET /session/:id/diff} - the session's file diffs (authoritative,
     * server-side; works for taken-over and external sessions too).
     *
     * @param sessionId the session
     * @param messageId optional message id to diff up to (may be {@code null})
     */
    default List<FileDiff> getSessionDiff(String sessionId, String messageId) throws OpencodeException {
        return List.of();
    }

    /** {@code POST /session/:id/fork} - fork a session at a message (explore a variant). */
    default Session forkSession(String sessionId, String messageId) throws OpencodeException {
        throw new UnsupportedOperationException("forkSession");
    }

    /**
     * {@code POST /session/:id/revert} - revert the conversation to before a
     * message (optionally a single part).
     */
    default boolean revertMessage(String sessionId, String messageId, String partId) throws OpencodeException {
        throw new UnsupportedOperationException("revertMessage");
    }

    /** {@code POST /session/:id/unrevert} - restore all reverted messages. */
    default boolean unrevertSession(String sessionId) throws OpencodeException {
        throw new UnsupportedOperationException("unrevertSession");
    }

    /**
     * {@code POST /session/:id/summarize} - compact a long session (provider/
     * model pick; the server then maintains the summary itself).
     */
    default boolean summarizeSession(String sessionId, String providerId, String modelId)
            throws OpencodeException {
        throw new UnsupportedOperationException("summarizeSession");
    }

    /** {@code POST /session/:id/share} - publish a read-only share link (opt-in). */
    default Session shareSession(String sessionId) throws OpencodeException {
        throw new UnsupportedOperationException("shareSession");
    }

    /** {@code DELETE /session/:id/share} - withdraw a share link. */
    default Session unshareSession(String sessionId) throws OpencodeException {
        throw new UnsupportedOperationException("unshareSession");
    }

    /**
     * {@code POST /session/:id/permissions/:permissionID} - answer a permission
     * request an unattended session raised.
     *
     * @param response     {@code "once"}, {@code "always"} or {@code "reject"}
     * @param remember     persist the decision as a rule in the session config
     */
    default boolean respondToPermission(String sessionId, String permissionId, String response, boolean remember)
            throws OpencodeException {
        throw new UnsupportedOperationException("respondToPermission");
    }

    /** {@code GET /command} - the project's custom slash commands ({@code .opencode/command/}). */
    default List<CommandInfo> getCommands() throws OpencodeException {
        return List.of();
    }

    /**
     * {@code POST /session/:id/command} - execute a custom slash command in a
     * session and wait for the reply.
     */
    default ChatEntry runCommand(String sessionId, String command, List<String> arguments)
            throws OpencodeException {
        throw new UnsupportedOperationException("runCommand");
    }

    /** {@code GET /project} - all projects the server knows (worktree + VCS position). */
    default List<ProjectSummary> getProjects() throws OpencodeException {
        return List.of();
    }

    /** {@code GET /vcs} - VCS state of the current project (branch, remote). */
    default VcsInfo getVcsInfo() throws OpencodeException {
        return new VcsInfo(null, null);
    }

    /**
     * {@code GET /file?path=…} - one level of the workspace file tree
     * (empty path or {@code "."} = the project root).
     */
    default List<FileNode> listFiles(String path) throws OpencodeException {
        return List.of();
    }

    /** {@code GET /find?pattern=…} - text search across workspace files. */
    default List<SearchMatch> findText(String pattern) throws OpencodeException {
        return List.of();
    }

    /** {@code GET /find/file?query=…} - fuzzy file-name search (paths). */
    default List<String> findFiles(String query) throws OpencodeException {
        return List.of();
    }

    /** {@code GET /find/symbol?query=…} - workspace symbol search. */
    default List<SymbolResult> findSymbols(String query) throws OpencodeException {
        return List.of();
    }

    /**
     * {@code PATCH /config} - apply partial config changes (e.g. switch the
     * default model) and get the updated config back.
     *
     * @param changes JSON-serializable partial config (e.g. {@code {"model":"x/y"}})
     */
    default ConfigInfo patchConfig(Map<String, Object> changes) throws OpencodeException {
        throw new UnsupportedOperationException("patchConfig");
    }

    /**
     * {@code POST /tui/<action>} - drive the opencode TUI (the official IDE
     * take-over mechanism): {@code append-prompt}, {@code submit-prompt},
     * {@code clear-prompt}, {@code open-models}, {@code open-help},
     * {@code open-sessions}, {@code open-themes}, {@code execute-command},
     * {@code show-toast}.
     *
     * @param action the TUI action (without the {@code /tui/} prefix)
     * @param body   JSON-serializable payload (may be {@code null} for no-arg actions)
     */
    default boolean tuiAction(String action, Map<String, Object> body) throws OpencodeException {
        throw new UnsupportedOperationException("tuiAction");
    }
}
