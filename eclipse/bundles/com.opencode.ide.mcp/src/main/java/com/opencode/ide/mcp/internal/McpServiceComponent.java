package com.opencode.ide.mcp.internal;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.opencode.ide.mcp.McpInfo;
import com.opencode.ide.tools.McpDispatcher;
import com.opencode.ide.tools.cpp.CppToolProvider;

/**
 * OSGi Declarative Services component (immediate) that owns the
 * {@code eclipse-build} MCP server lifecycle: starts the Streamable HTTP
 * endpoint on an ephemeral loopback port on activation, stops it on
 * deactivation, and publishes {@link McpInfo} as an OSGi service while
 * running. The port is backed by {@link McpState}.
 */
public class McpServiceComponent implements McpInfo {

    private static final Logger LOG = Logger.getLogger(McpServiceComponent.class.getName());

    private McpHttpServer server;

    protected void activate() {
        try {
            server = McpHttpServer.start(new McpDispatcher(new CppToolProvider()));
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "failed to start eclipse-build MCP server", e);
            throw new IllegalStateException("eclipse-build MCP server failed to start", e);
        }
        McpState.setPort(server.port());
        LOG.info("eclipse-build MCP listening on http://127.0.0.1:" + server.port() + "/mcp");
    }

    protected void deactivate() {
        McpState.setPort(-1);
        McpHttpServer current = server;
        server = null;
        if (current != null) {
            current.stop();
        }
    }

    @Override
    public int getPort() {
        return McpState.port;
    }

    @Override
    public boolean isRunning() {
        return McpState.port > 0;
    }

    @Override
    public String getEndpointUrl() {
        return isRunning() ? "http://127.0.0.1:" + McpState.port + "/mcp" : null;
    }
}
