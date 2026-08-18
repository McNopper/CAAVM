package com.opencode.ide.tools;

import java.util.List;

import com.google.gson.JsonObject;

/**
 * SPI for a language-specific tool pack of the {@code eclipse-build} MCP
 * server (C/C++ today, further languages later). The {@link McpDispatcher}
 * unions {@link #tools()} for tools/list and routes each tools/call to the
 * provider that declared the tool name, so a pack fully owns its tool names;
 * keep them distinct across packs (e.g. prefix by language domain).
 *
 * <p>Wiring is a plain constructor-injected list -
 * {@code new McpDispatcher(List.of(new CppToolProvider(), ...))}, assembled
 * in the MCP bundle's {@code McpServiceComponent}. A future
 * {@code PythonToolProvider} plugs in by (a) implementing this interface in
 * its own pack (e.g. {@code com.opencode.ide.tools.python}), (b) appending
 * it to the provider list where the MCP bundle builds the dispatcher - or,
 * once several packs ship, by replacing that list with
 * {@code ServiceLoader.load(ToolProvider.class, getClass().getClassLoader())}
 * plus a {@code META-INF/services} registration in each pack.</p>
 *
 * <p>The SPI and the JSON-RPC dispatch engine are exported from this
 * Eclipse-free tools bundle, so a pack may live in any bundle that depends
 * on it.</p>
 *
 * <p>Implementations raise {@link ParamError} for structurally invalid
 * arguments (mapped by the dispatcher to JSON-RPC -32602) and return
 * {@link McpToolResult#error(String)} for machine-dependent problems such as
 * a missing linter binary or a bad path.</p>
 */
public interface ToolProvider {

    /** @return the language identifier of the pack, e.g. {@code "cpp"} or {@code "python"}. */
    String language();

    /** @return the tools contributed to tools/list (union across all providers). */
    List<McpTool> tools();

    /**
     * Executes one tool declared by this pack.
     *
     * @param toolName one of the names returned by {@link #tools()}
     * @param arguments the tools/call arguments object (never {@code null}, may be empty)
     * @return the MCP tool-call outcome (text content + isError flag)
     */
    McpToolResult call(String toolName, JsonObject arguments);
}
