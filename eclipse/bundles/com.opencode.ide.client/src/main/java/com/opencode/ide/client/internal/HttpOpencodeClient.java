package com.opencode.ide.client.internal;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import com.opencode.ide.client.ChatRequest;
import com.opencode.ide.client.ChatRequests;
import com.opencode.ide.client.ConnectionConfig;
import com.opencode.ide.client.McpRequests;
import com.opencode.ide.client.McpServerConfig;
import com.opencode.ide.client.OpencodeClient;
import com.opencode.ide.client.OpencodeConnectionException;
import com.opencode.ide.client.OpencodeException;
import com.opencode.ide.client.model.Agent;
import com.opencode.ide.client.model.ChatEntry;
import com.opencode.ide.client.model.ConfigInfo;
import com.opencode.ide.client.model.HealthStatus;
import com.opencode.ide.client.model.ProviderList;
import com.opencode.ide.client.model.Session;
import com.opencode.ide.client.model.SessionStatus;

/**
 * {@link OpencodeClient} backed by {@code java.net.http.HttpClient} + Gson.
 *
 * <p>Internal implementation detail: the {@code internal} package is not
 * exported (visible only to the client's own tests), so
 * {@code OpencodeClients.http(...)} is the only way other layers obtain an
 * {@link OpencodeClient}.</p>
 */
public final class HttpOpencodeClient implements OpencodeClient {

    private static final Gson GSON = new Gson();

    private final HttpClient http;
    private final URI baseUri;
    private final String authHeader;

    public HttpOpencodeClient(ConnectionConfig config) {
        this.baseUri = config.baseUrl();
        this.authHeader = Auth.basicHeader(config.username(), config.password());
        this.http = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1) // the opencode server hangs on h2c-upgrade POSTs
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public HealthStatus getHealth() throws OpencodeException {
        return get("/global/health", HealthStatus.class);
    }

    @Override
    public List<Agent> getAgents() throws OpencodeException {
        return getList("/agent", Agent.class);
    }

    @Override
    public ProviderList getProviders() throws OpencodeException {
        return get("/config/providers", ProviderList.class);
    }

    @Override
    public List<Session> getSessions() throws OpencodeException {
        return getList("/session", Session.class);
    }

    @Override
    public Map<String, SessionStatus> getSessionStatus() throws OpencodeException {
        return parseBody("GET", "/session/status", request("GET", "/session/status", null),
                TypeToken.getParameterized(Map.class, String.class, SessionStatus.class).getType());
    }

    @Override
    public ConfigInfo getConfig() throws OpencodeException {
        return get("/config", ConfigInfo.class);
    }

    @Override
    public Session createSession(String title) throws OpencodeException {
        return createSession(title, null);
    }

    @Override
    public Session createSession(String title, Path directory) throws OpencodeException {
        JsonObject body = new JsonObject();
        if (title != null && !title.isBlank()) {
            body.addProperty("title", title);
        }
        String path = "/session";
        if (directory != null) {
            String encoded = URLEncoder.encode(directory.toString(), StandardCharsets.UTF_8)
                    .replace("+", "%20");
            path = path + "?directory=" + encoded;
        }
        return parseBody("POST", path, request("POST", path, body.toString()), Session.class);
    }

    @Override
    public void registerMcp(String name, McpServerConfig config) throws OpencodeException {
        request("POST", "/mcp", McpRequests.registerBody(name, config));
    }

    @Override
    public List<ChatEntry> getMessages(String sessionId) throws OpencodeException {
        return getList("/session/" + sessionId + "/message", ChatEntry.class);
    }

    @Override
    public ChatEntry sendMessage(ChatRequest chatRequest) throws OpencodeException {
        String body = ChatRequests.messageBody(chatRequest);
        String path = "/session/" + chatRequest.sessionId() + "/message";
        // a chat reply can legitimately take minutes (LLM generation) - allow 5
        return parseBody("POST", path, request("POST", path, body, Duration.ofMinutes(5)),
                ChatEntry.class);
    }

    @Override
    public void log(String service, String level, String message, Map<String, Object> extra) throws OpencodeException {
        JsonObject body = new JsonObject();
        body.addProperty("service", service);
        body.addProperty("level", level);
        body.addProperty("message", message);
        if (extra != null) {
            body.add("extra", GSON.toJsonTree(extra));
        }
        request("POST", "/log", GSON.toJson(body));
    }

    private <T> T get(String path, Class<T> type) throws OpencodeException {
        return parseBody("GET", path, request("GET", path, null), type);
    }

    private <T> List<T> getList(String path, Class<T> elementType) throws OpencodeException {
        return parseBody("GET", path, request("GET", path, null),
                TypeToken.getParameterized(List.class, elementType).getType());
    }

    private static <T> T parseBody(String method, String path, HttpResponse<String> response, Type type)
            throws OpencodeException {
        int status = response.statusCode();
        String body = response.body();
        if (body == null || body.isBlank()) {
            throw new OpencodeException("opencode " + method + " " + path + " failed: HTTP " + status
                    + " - empty response body where JSON was expected");
        }
        try {
            T value = GSON.fromJson(body, type);
            if (value == null) {
                throw new OpencodeException("opencode " + method + " " + path + " failed: HTTP " + status
                        + " - JSON null response: " + truncate(body, 300));
            }
            return value;
        } catch (JsonParseException e) {
            throw new OpencodeException("opencode " + method + " " + path + " failed: HTTP " + status
                    + " - malformed response body: " + truncate(body, 300), e);
        }
    }

    private HttpResponse<String> request(String method, String path, String body) throws OpencodeException {
        return request(method, path, body, Duration.ofSeconds(30));
    }

    private HttpResponse<String> request(String method, String path, String body, Duration timeout)
            throws OpencodeException {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(baseUri.resolve(path))
                .timeout(timeout)
                .header("Accept", "application/json");
        if (authHeader != null) {
            builder.header("Authorization", authHeader);
        }
        if (body != null) {
            builder.header("Content-Type", "application/json");
            builder.method(method, HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
        } else {
            builder.method(method, HttpRequest.BodyPublishers.noBody());
        }

        HttpResponse<String> response;
        try {
            response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new OpencodeConnectionException("Interrupted while calling " + path, e);
        } catch (IOException e) {
            throw new OpencodeConnectionException(
                    "Cannot reach opencode server at " + baseUri + " (" + method + " " + path + ")", e);
        }

        int status = response.statusCode();
        if (status >= 200 && status < 300) {
            return response;
        }
        throw new OpencodeException("opencode " + method + " " + path + " failed: HTTP " + status
                + " - " + truncate(response.body(), 500));
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, max) + "...";
    }
}
