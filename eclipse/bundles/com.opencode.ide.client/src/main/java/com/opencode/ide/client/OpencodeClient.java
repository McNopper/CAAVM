package com.opencode.ide.client;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import com.opencode.ide.client.model.Agent;
import com.opencode.ide.client.model.ChatEntry;
import com.opencode.ide.client.model.ConfigInfo;
import com.opencode.ide.client.model.HealthStatus;
import com.opencode.ide.client.model.McpServerInfo;
import com.opencode.ide.client.model.ProviderList;
import com.opencode.ide.client.model.SkillInfo;
import com.opencode.ide.client.model.Session;
import com.opencode.ide.client.model.SessionStatus;
import com.opencode.ide.client.model.SessionTodo;

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
}
