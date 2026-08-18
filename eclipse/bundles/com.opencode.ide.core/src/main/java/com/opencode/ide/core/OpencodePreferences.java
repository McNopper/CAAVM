package com.opencode.ide.core;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.osgi.service.prefs.BackingStoreException;

import com.opencode.ide.client.ConnectionConfig;

/**
 * Public facade over the core connection preferences (node {@value #NODE_ID}).
 *
 * <p>This is the only place the connection settings are read or written; both
 * the core {@link OpencodeConnection} and the UI preference page use it.</p>
 *
 * <p>Note: the password is stored as plain text in instance preferences for now.
 * This is acceptable because the opencode server runs locally; switch to
 * {@code org.eclipse.equinox.security} secure storage for remote servers.</p>
 */
public final class OpencodePreferences {

    public static final String NODE_ID = "com.opencode.ide.core";

    public static final String KEY_MODE = "mode";
    public static final String KEY_SERVER_URL = "serverUrl";
    public static final String KEY_USERNAME = "username";
    public static final String KEY_PASSWORD = "password";
    public static final String KEY_OPENCODE_BINARY = "opencodeBinary";
    public static final String KEY_SPAWN_HOSTNAME = "spawnHostname";
    public static final String KEY_SPAWN_PORT = "spawnPort";
    public static final String KEY_ADVERTISE_RENDERING = "advertiseRendering";

    public static final String MODE_CONNECT = "CONNECT";
    public static final String MODE_SPAWN = "SPAWN";

    private static final String DEFAULT_URL = "http://127.0.0.1:4096";
    private static final String DEFAULT_HOST = "127.0.0.1";

    private final IEclipsePreferences prefs;

    public OpencodePreferences() {
        this.prefs = InstanceScope.INSTANCE.getNode(NODE_ID);
    }

    public IEclipsePreferences raw() {
        return prefs;
    }

    public String getMode() {
        return prefs.get(KEY_MODE, MODE_SPAWN);
    }

    public void setMode(String mode) {
        prefs.put(KEY_MODE, mode);
    }

    public boolean isConnectMode() {
        return MODE_CONNECT.equals(getMode());
    }

    public String getServerUrl() {
        return prefs.get(KEY_SERVER_URL, DEFAULT_URL);
    }

    public void setServerUrl(String url) {
        prefs.put(KEY_SERVER_URL, url);
    }

    public String getUsername() {
        return prefs.get(KEY_USERNAME, "opencode");
    }

    public void setUsername(String username) {
        if (username == null || username.isEmpty()) {
            prefs.remove(KEY_USERNAME);
        } else {
            prefs.put(KEY_USERNAME, username);
        }
    }

    public String getPassword() {
        return prefs.get(KEY_PASSWORD, "");
    }

    public void setPassword(String password) {
        if (password == null || password.isEmpty()) {
            prefs.remove(KEY_PASSWORD);
        } else {
            prefs.put(KEY_PASSWORD, password);
        }
    }

    public String getOpencodeBinary() {
        return prefs.get(KEY_OPENCODE_BINARY, "");
    }

    public void setOpencodeBinary(String path) {
        prefs.put(KEY_OPENCODE_BINARY, path == null ? "" : path);
    }

    public String getSpawnHostname() {
        return prefs.get(KEY_SPAWN_HOSTNAME, DEFAULT_HOST);
    }

    public void setSpawnHostname(String hostname) {
        prefs.put(KEY_SPAWN_HOSTNAME, hostname);
    }

    public int getSpawnPort() {
        return prefs.getInt(KEY_SPAWN_PORT, 0);
    }

    public void setSpawnPort(int port) {
        prefs.putInt(KEY_SPAWN_PORT, port);
    }

    /**
     * Whether every chat message advertises what the chat view can render
     * (markdown, KaTeX math, mermaid, highlighted code) via the request's
     * {@code system} field. On by default: without it models answer in plain
     * terminal style and math/diagrams have to be requested manually.
     */
    public boolean isAdvertiseRendering() {
        return prefs.getBoolean(KEY_ADVERTISE_RENDERING, true);
    }

    public void setAdvertiseRendering(boolean enabled) {
        prefs.putBoolean(KEY_ADVERTISE_RENDERING, enabled);
    }

    /** Builds the {@link ConnectionConfig} used for {@code connect} mode. */
    public ConnectionConfig toConnectConfig() {
        String url = getServerUrl();
        String user = getUsername();
        String pw = getPassword();
        return new ConnectionConfig(
                java.net.URI.create(url),
                user.isEmpty() ? null : user,
                pw.isEmpty() ? null : pw);
    }

    public void save() throws BackingStoreException {
        prefs.flush();
    }
}
