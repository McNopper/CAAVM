package com.opencode.ide.core.internal;

import java.util.logging.Level;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;

import com.opencode.ide.client.ClientLog;
import com.opencode.ide.core.ConnectionsManager;
import com.opencode.ide.core.OpencodeConnection;
import com.opencode.ide.core.OpencodePreferences;
import com.opencode.ide.core.context.ProjectContext;

/**
 * Bundle activator for {@code com.opencode.ide.core}. Owns the singleton
 * {@link OpencodeConnection} lifecycle, tracks the optional
 * {@link ProjectContext} service (implemented by the CDT bundle), and provides
 * simple logging helpers.
 */
public class CoreActivator extends Plugin {

    public static final String PLUGIN_ID = "com.opencode.ide.core";

    private static CoreActivator instance;

    private ServiceTracker<ProjectContext, ProjectContext> projectContextTracker;
    private ServiceRegistration<OpencodeConnection> connectionService;
    private ServiceRegistration<ConnectionsManager> connectionsService;
    private Thread shutdownHook;

    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        instance = this;
        // Bridge the tasksRoot preference into the system property the
        // eclipse-build MCP endpoint reads when its DS component activates
        // (service-driven, i.e. strictly AFTER this activator ran) — one task
        // store for the Board view, the fleet and the in-session task_* tools.
        try {
            String configured = new OpencodePreferences().getTasksRoot();
            if (configured != null && !configured.isBlank()) {
                System.setProperty("opencode.tasks.root", configured.trim());
            }
        } catch (RuntimeException e) {
            logWarning("cannot bridge tasksRoot preference into opencode.tasks.root: " + e.getMessage());
        }
        ClientLog.install((level, message, cause) -> {
            if (Level.WARNING.equals(level)) {
                logWarning(message);
            } else {
                logError(message, cause);
            }
        });
        connectionService = context.registerService(
                OpencodeConnection.class, OpencodeConnection.getInstance(), null);
        connectionsService = context.registerService(
                ConnectionsManager.class, ConnectionsManager.getDefault(), null);
        projectContextTracker = new ServiceTracker<ProjectContext, ProjectContext>(
                context, ProjectContext.class.getName(), null) {
            @Override
            public ProjectContext addingService(ServiceReference<ProjectContext> reference) {
                ProjectContext service = context.getService(reference);
                // a different project context may now be available - drop cached client
                OpencodeConnection.getInstance().refresh();
                return service;
            }

            @Override
            public void removedService(ServiceReference<ProjectContext> reference, ProjectContext service) {
                context.ungetService(reference);
            }
        };
        projectContextTracker.open();

        // Safety net: if bundle stop() does not run (abnormal Eclipse/JVM exit),
        // the JVM shutdown hook still tears down any spawned opencode server so it
        // is never left orphaned when Eclipse closes.
        shutdownHook = new Thread(() -> {
            try {
                ConnectionsManager.getDefault().dispose();
            } catch (Throwable t) {
                // best-effort during shutdown; do not propagate
            }
            try {
                OpencodeConnection.getInstance().dispose();
            } catch (Throwable t) {
                // best-effort during shutdown; do not propagate
            }
        }, "opencode-eclipse-shutdown");
        try {
            Runtime.getRuntime().addShutdownHook(shutdownHook);
        } catch (IllegalStateException ignored) {
            // JVM already shutting down - nothing to register
        }
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        try {
            if (connectionService != null) {
                connectionService.unregister();
                connectionService = null;
            }
            if (connectionsService != null) {
                connectionsService.unregister();
                connectionsService = null;
            }
            if (projectContextTracker != null) {
                projectContextTracker.close();
                projectContextTracker = null;
            }
            ConnectionsManager.getDefault().dispose();
            OpencodeConnection.getInstance().dispose();
            if (shutdownHook != null) {
                try {
                    Runtime.getRuntime().removeShutdownHook(shutdownHook);
                } catch (IllegalStateException ignored) {
                    // JVM already shutting down - that's fine, the hook will run
                }
                shutdownHook = null;
            }
        } finally {
            ClientLog.install(null);
            instance = null;
            super.stop(context);
        }
    }

    public static CoreActivator getDefault() {
        return instance;
    }

    /** @return the currently registered {@link ProjectContext} service, or {@code null}. */
    public static ProjectContext getProjectContext() {
        CoreActivator a = instance;
        return (a != null && a.projectContextTracker != null) ? a.projectContextTracker.getService() : null;
    }

    public static void logError(String message, Throwable cause) {
        CoreActivator a = instance;
        if (a != null) {
            a.getLog().log(new Status(IStatus.ERROR, PLUGIN_ID, message, cause));
        }
    }

    public static void logWarning(String message) {
        CoreActivator a = instance;
        if (a != null) {
            a.getLog().log(new Status(IStatus.WARNING, PLUGIN_ID, message));
        }
    }
}
