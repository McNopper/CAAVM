package com.opencode.ide.fleet;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import com.opencode.ide.tools.McpDispatcher;

/**
 * Standalone stdio entry point for the fleet tools (chat-first control): a
 * minimal line-based MCP JSON-RPC loop over stdin/stdout, modeled on
 * {@code TasksStdioMain} and reusing the same {@link McpDispatcher}. The
 * {@code eclipse/fleet-tools.ps1} launcher resolves the jars (fleet, client,
 * git, tasks, tools, gson); the default store root is
 * {@code .opencode/tasks} relative to the working directory, so an opencode
 * session started in a repository controls that repo's fleet. The loop speaks
 * the same subset as the tasks launcher (initialize, ping, tools/list,
 * tools/call); notifications produce no output.
 *
 * <p>A shutdown hook closes the {@link FleetControl}, killing the spawned
 * {@code opencode serve} process when opencode closes stdin.</p>
 */
public final class FleetStdioMain {

    private FleetStdioMain() {
    }

    /** Runs the stdio loop; returns when stdin closes. */
    public static void main(String[] args) throws Exception {
        Path root = Path.of(".opencode", "tasks");
        for (int i = 0; i < args.length - 1; i++) {
            if ("--root".equals(args[i])) {
                root = Path.of(args[i + 1]);
                break;
            }
        }
        FleetToolProvider provider = new FleetToolProvider(root);
        Runtime.getRuntime().addShutdownHook(new Thread(provider::close, "fleet-shutdown"));
        McpDispatcher dispatcher = new McpDispatcher(provider);
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        PrintStream out = new PrintStream(System.out, true, StandardCharsets.UTF_8);
        String line;
        while ((line = in.readLine()) != null) {
            if (line.isBlank()) {
                continue;
            }
            String response = dispatcher.handle(line);
            if (response != null) {
                out.println(response);
                out.flush();
            }
        }
    }
}
