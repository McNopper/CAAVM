package com.opencode.ide.mcp.internal;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.opencode.ide.tools.McpDispatcher;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/**
 * The MCP Streamable HTTP endpoint: single path {@code /mcp}, POST only,
 * JSON responses (no SSE), loopback only, no sessions. Mirrors the embedded
 * {@code com.sun.net.httpserver} approach of the chat bundle's ChatWebServer,
 * but with a thread pool so a long cmake build cannot block the endpoint.
 */
public final class McpHttpServer {

    private final HttpServer server;
    private final ExecutorService executor;
    private final McpDispatcher dispatcher;

    private McpHttpServer(HttpServer server, ExecutorService executor, McpDispatcher dispatcher) {
        this.server = server;
        this.executor = executor;
        this.dispatcher = dispatcher;
    }

    public static McpHttpServer start(McpDispatcher dispatcher) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        ExecutorService executor = Executors.newCachedThreadPool(r -> {
            Thread thread = new Thread(r, "mcp-http");
            thread.setDaemon(true);
            return thread;
        });
        server.setExecutor(executor);
        McpHttpServer mcp = new McpHttpServer(server, executor, dispatcher);
        server.createContext("/mcp", mcp::handle);
        server.start();
        return mcp;
    }

    public int port() {
        return server.getAddress().getPort();
    }

    public String endpointUrl() {
        return "http://127.0.0.1:" + port() + "/mcp";
    }

    public void stop() {
        server.stop(0);
        executor.shutdownNow();
    }

    private void handle(HttpExchange exchange) throws IOException {
        try {
            if (!"POST".equals(exchange.getRequestMethod())) {
                exchange.getResponseHeaders().set("Allow", "POST");
                respond(exchange, 405, "method not allowed: POST only".getBytes(StandardCharsets.UTF_8),
                        "text/plain; charset=utf-8");
                return;
            }
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            String response = dispatcher.handle(body);
            if (response == null) {
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(202, -1);
                exchange.close();
                return;
            }
            respond(exchange, 200, response.getBytes(StandardCharsets.UTF_8), "application/json");
        } catch (RuntimeException e) {
            String error = "{\"jsonrpc\":\"2.0\",\"id\":null,"
                    + "\"error\":{\"code\":-32603,\"message\":\"internal error\"}}";
            try {
                respond(exchange, 200, error.getBytes(StandardCharsets.UTF_8), "application/json");
            } catch (IOException ignored) {
                // headers may already be sent; nothing more we can do
            }
        }
    }

    private static void respond(HttpExchange exchange, int status, byte[] body, String contentType)
            throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(status, body.length == 0 ? -1 : body.length);
        if (body.length == 0) {
            exchange.close();
            return;
        }
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(body);
        }
    }
}
