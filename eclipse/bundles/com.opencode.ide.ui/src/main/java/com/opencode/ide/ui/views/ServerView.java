package com.opencode.ide.ui.views;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.part.ViewPart;

import com.opencode.ide.client.OpencodeEventListener;
import com.opencode.ide.client.activity.ActivityTracker;
import com.opencode.ide.client.activity.FileActivity;
import com.opencode.ide.client.activity.SessionActivity;
import com.opencode.ide.client.activity.ToolActivity;
import com.opencode.ide.client.model.Agent;
import com.opencode.ide.client.model.HealthStatus;
import com.opencode.ide.client.model.OpencodeEvent;
import com.opencode.ide.client.model.Session;
import com.opencode.ide.client.model.SessionStatus;
import com.opencode.ide.core.OpencodeConnection;
import com.opencode.ide.ui.internal.Refreshable;
import com.opencode.ide.ui.internal.UiActivator;
import com.opencode.ide.ui.internal.ViewLoadSupport;

/**
 * Per-server explorer: the connection (server) as root, with categories for the
 * available agents and the running sessions (nested by {@code parentID}, so
 * subagents appear under the agent that spawned them). Future categories
 * (MCP, tools, ...) slot in alongside Agents/Sessions.
 *
 * <p>Replaces the earlier separate Connections + Agents views.</p>
 */
public class ServerView extends ViewPart implements Refreshable {

    public static final String ID = "com.opencode.ide.ui.views.ServerView";

    private TreeViewer viewer;
    private Action refreshAction;
    private Action reconnectAction;

    private volatile ServerNode current;
    private OpencodeEventListener eventListener;
    private Runnable trackerListener;
    private boolean refreshPending;

    private final ActivityTracker tracker = new ActivityTracker();

    enum CategoryKind { AGENTS, SESSIONS, ACTIVE_FILES }

    static final class ServerNode {
        final String mode;
        final String url;
        final boolean healthy;
        final String version;
        final Long pid;
        final List<Agent> agents;
        final List<Session> sessions;          // mutable: updated live from /event
        final Map<String, SessionStatus> statuses;   // mutable
        final Map<String, String> activity;    // sessionId -> live label ("thinking"/"running tool"/...)
        final CategoryNode agentsCategory;
        final CategoryNode sessionsCategory;
        final CategoryNode filesCategory;

        ServerNode(String mode, String url, boolean healthy, String version, Long pid,
                List<Agent> agents, List<Session> sessions, Map<String, SessionStatus> statuses) {
            this.mode = mode;
            this.url = url;
            this.healthy = healthy;
            this.version = version;
            this.pid = pid;
            this.agents = agents;
            this.sessions = new ArrayList<>(sessions);
            this.statuses = new HashMap<>(statuses);
            this.activity = new HashMap<>();
            this.agentsCategory = new CategoryNode("Agents", CategoryKind.AGENTS, this);
            this.sessionsCategory = new CategoryNode("Sessions", CategoryKind.SESSIONS, this);
            this.filesCategory = new CategoryNode("Active files", CategoryKind.ACTIVE_FILES, this);
        }
    }

    static final class CategoryNode {
        final String label;
        final CategoryKind kind;
        final ServerNode server;

        CategoryNode(String label, CategoryKind kind, ServerNode server) {
            this.label = label;
            this.kind = kind;
            this.server = server;
        }
    }

    @Override
    public void createPartControl(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        TreeColumnLayout layout = new TreeColumnLayout();
        composite.setLayout(layout);

        viewer = new TreeViewer(composite,
                SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER);
        viewer.getTree().setHeaderVisible(true);
        viewer.getTree().setLinesVisible(true);
        viewer.setContentProvider(new TreeContentProvider(tracker));

        TreeViewerColumn nameCol = new TreeViewerColumn(viewer, SWT.NONE);
        nameCol.getColumn().setText("Name");
        nameCol.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                return name(element);
            }

            @Override
            public Image getImage(Object element) {
                return icon(element);
            }
        });

        TreeViewerColumn detailCol = new TreeViewerColumn(viewer, SWT.NONE);
        detailCol.getColumn().setText("Details");
        detailCol.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                return detail(element);
            }
        });

        layout.setColumnData(nameCol.getColumn(), new ColumnWeightData(2, 140, true));
        layout.setColumnData(detailCol.getColumn(), new ColumnWeightData(3, 220, true));

        // double-click a session -> resume it in a chat window
        viewer.addDoubleClickListener(e -> {
            Object selection = e.getSelection();
            Object first = (selection instanceof org.eclipse.jface.viewers.IStructuredSelection structured)
                    ? structured.getFirstElement()
                    : null;
            if (first instanceof Session s && s.id() != null) {
                openChatForSession(s.id());
            }
        });

        contributeActions();
        refresh();
    }

    private void contributeActions() {
        refreshAction = new Action("Refresh") {            @Override
            public void run() {
                refresh();
            }
        };
        refreshAction.setToolTipText("Refresh server, agents and sessions");
        reconnectAction = new Action("Reconnect") {
            @Override
            public void run() {
                reconnect();
            }
        };
        reconnectAction.setToolTipText("Drop the client/server and reconnect");

        Action expandAllAction = new Action("Expand All") {
            @Override
            public void run() {
                if (viewer != null && !viewer.getControl().isDisposed()) {
                    viewer.expandAll();
                }
            }
        };
        expandAllAction.setToolTipText("Expand all sessions (incl. subagents)");
        Action collapseAllAction = new Action("Collapse All") {
            @Override
            public void run() {
                if (viewer != null && !viewer.getControl().isDisposed()) {
                    viewer.collapseAll();
                }
            }
        };
        collapseAllAction.setToolTipText("Collapse all categories and sessions");

        IToolBarManager toolBar = getViewSite().getActionBars().getToolBarManager();
        toolBar.add(refreshAction);
        toolBar.add(reconnectAction);
        toolBar.add(expandAllAction);
        toolBar.add(collapseAllAction);
    }

    /** Resumes the given session in a chat window (via the openChat command - no chat-bundle dependency). */
    private void openChatForSession(String sessionId) {
        try {
            var commands = getViewSite().getService(org.eclipse.ui.commands.ICommandService.class);
            var command = commands.getCommand("com.opencode.ide.chat.openChat");
            if (!command.isDefined()) {
                return; // chat bundle not installed
            }
            var parameterized = new org.eclipse.core.commands.ParameterizedCommand(command,
                    new org.eclipse.core.commands.Parameterization[] {
                            new org.eclipse.core.commands.Parameterization(
                                    command.getParameter("com.opencode.ide.chat.openChat.sessionId"), sessionId) });
            var handlers = getViewSite().getService(org.eclipse.ui.handlers.IHandlerService.class);
            handlers.executeCommand(parameterized, null);
        } catch (Exception e) {
            UiActivator.getDefault().getLog().log(
                    new org.eclipse.core.runtime.Status(org.eclipse.core.runtime.Status.ERROR,
                            UiActivator.PLUGIN_ID, "Failed to open chat for session " + sessionId, e));
        }
    }

    @Override
    public void refresh() {
        setContentDescription("Loading...");
        ViewLoadSupport.load("Loading opencode server", () -> {
            OpencodeConnection connection = OpencodeConnection.getInstance();
            connection.getClient(); // ensure spawned/connected
            HealthStatus health = connection.getClient().getHealth();
            List<Agent> agents = connection.getClient().getAgents();
            List<Session> sessions = connection.getClient().getSessions();
            Map<String, SessionStatus> statuses = connection.getClient().getSessionStatus();
            String mode = connection.getMode();
            String url = connection.getConnectConfig().baseUrl().toString();
            Long pid = connection.getSpawnedProcessId();
            return new ServerNode(mode, url, health.healthy(), health.version(), pid,
                    agents == null ? Collections.emptyList() : agents,
                    sessions == null ? Collections.emptyList() : sessions,
                    statuses == null ? Collections.emptyMap() : statuses);
        }, this::showNode, this::showError);
    }

    private void reconnect() {
        setContentDescription("Reconnecting...");
        ViewLoadSupport.load("Reconnecting opencode", () -> {
            OpencodeConnection.getInstance().refresh();
            return null;
        }, ignored -> refresh(), this::showError);
    }

    private void showNode(ServerNode node) {
        if (viewer.getControl().isDisposed()) {
            return;
        }
        current = node;
        // Subscribe once to live server events so the Sessions tree self-updates.
        if (eventListener == null) {
            eventListener = this::onEvent;
            OpencodeConnection.getInstance().addEventListener(eventListener);
        }
        if (trackerListener == null) {
            trackerListener = this::onTrackerChanged;
            tracker.addListener(trackerListener);
        }
        // Pass a list as input (NOT the ServerNode itself): if the input equals a
        // tree element, the TreeViewer's expansion logic can misbehave.
        viewer.setInput(java.util.List.of(node));
        // Default: server + categories expanded so agents and the top-level sessions
        // are visible, but the subagent children stay collapsed (use Expand All to open them).
        viewer.setExpandedElements(
                new Object[] { node, node.agentsCategory, node.sessionsCategory, node.filesCategory });
        updateContentDescription();
    }

    // ---------- live updates (driven by /event SSE via core) ----------

    /** Called on the SSE thread; feeds the tracker, then hops to the UI thread to mutate the viewer's model. */
    private void onEvent(OpencodeEvent event) {
        tracker.apply(event);
        Display display = Display.getDefault();
        if (display != null && !display.isDisposed()) {
            display.asyncExec(() -> handleEvent(event));
        }
    }

    /** Called on the SSE thread when derived activity changes; hop to the UI thread for a coalesced refresh. */
    private void onTrackerChanged() {
        Display display = Display.getDefault();
        if (display != null && !display.isDisposed()) {
            display.asyncExec(() -> {
                if (viewer != null && !viewer.getControl().isDisposed()) {
                    scheduleRefresh();
                }
            });
        }
    }

    private void handleEvent(OpencodeEvent event) {
        ServerNode node = current;
        if (node == null || event == null || event.type() == null) {
            return;
        }
        switch (event.type()) {
            case "session.created", "session.updated" -> {
                Session s = extractSession(event);
                if (s != null) {
                    upsertSession(node, s);
                    scheduleRefresh();
                }
            }
            case "session.deleted" -> {
                Session s = extractSession(event);
                if (s != null && s.id() != null) {
                    node.sessions.removeIf(x -> s.id().equals(x.id()));
                    node.statuses.remove(s.id());
                    node.activity.remove(s.id());
                    scheduleRefresh();
                }
            }
            case "session.status" -> {
                String sid = event.string("sessionID");
                String statusType = extractStatusType(event);
                if (sid != null) {
                    if (statusType != null) {
                        node.statuses.put(sid, new SessionStatus(statusType));
                    }
                    if (!"busy".equalsIgnoreCase(statusType) && !"retry".equalsIgnoreCase(statusType)) {
                        node.activity.remove(sid); // idle -> stop showing thinking/running
                    }
                    scheduleRefresh();
                }
            }
            case "session.idle" -> {
                String sid = event.string("sessionID");
                if (sid != null) {
                    node.statuses.put(sid, new SessionStatus("idle"));
                    node.activity.remove(sid);
                    scheduleRefresh();
                }
            }
            case "message.updated" -> {
                // defense in depth: a completed assistant message means the turn is over,
                // even if a session.status/session.idle event was missed
                String sid = event.string("sessionID");
                if (sid != null
                        && "assistant".equals(event.at("info.role"))
                        && event.at("info.time.completed") != null) {
                    node.statuses.put(sid, new SessionStatus("idle"));
                    node.activity.remove(sid);
                    scheduleRefresh();
                }
            }
            case "message.part.updated" -> {
                String sid = partSessionId(event);
                String label = partActivityLabel(event);
                if (sid != null && label != null) {
                    node.activity.put(sid, label);
                    scheduleRefresh();
                }
            }
            default -> {
                // other event types ignored for now
            }
        }
    }

    private Session extractSession(OpencodeEvent event) {
        return event.as("info", Session.class);
    }

    /** Reads the status type from {@code session.status}, tolerating string or object form. */
    private static String extractStatusType(OpencodeEvent event) {
        String flat = event.string("status");
        return flat != null ? flat : event.at("status.type");
    }

    private static String partSessionId(OpencodeEvent event) {
        return event.at("part.sessionID");
    }

    /** Maps a streamed part to a live activity label ("thinking" / "running tool" / "responding"). */
    private static String partActivityLabel(OpencodeEvent event) {
        String partType = event.at("part.type");
        if (partType == null) {
            return null;
        }
        return switch (partType) {
            case "reasoning" -> "thinking";
            case "tool" -> "running".equals(event.at("part.state.status")) ? "running tool" : null;
            case "text" -> "responding";
            default -> null;
        };
    }

    private static void upsertSession(ServerNode node, Session session) {
        if (session == null || session.id() == null) {
            return;
        }
        for (int i = 0; i < node.sessions.size(); i++) {
            if (session.id().equals(node.sessions.get(i).id())) {
                node.sessions.set(i, session);
                return;
            }
        }
        node.sessions.add(session);
    }

    /** Coalesce rapid events into one viewer refresh (~3/sec). */
    private void scheduleRefresh() {
        if (viewer == null || viewer.getControl().isDisposed()) {
            return;
        }
        if (refreshPending) {
            return;
        }
        refreshPending = true;
        Display.getDefault().timerExec(300, () -> {
            refreshPending = false;
            if (viewer == null || viewer.getControl().isDisposed()) {
                return;
            }
            viewer.refresh();
            updateContentDescription();
        });
    }

    private void updateContentDescription() {
        ServerNode node = current;
        if (node == null) {
            return;
        }
        long active = node.activity.size();
        setContentDescription((node.healthy ? "Connected" : "Unreachable") + ": " + node.url
                + "  •  live  •  " + node.agents.size() + " agents, " + node.sessions.size() + " sessions"
                + (active > 0 ? "  •  " + active + " active" : ""));
    }

    private void showError(Throwable e) {
        if (viewer.getControl().isDisposed()) {
            return;
        }
        viewer.setInput(null);
        setContentDescription("Error: " + ViewLoadSupport.message(e));
        UiActivator.getDefault().getLog().log(
                new Status(Status.ERROR, UiActivator.PLUGIN_ID, "Failed to load opencode server", e));
    }

    // ---------- label helpers ----------

    String name(Object element) {
        if (element instanceof ServerNode) {
            return "opencode server";
        }
        if (element instanceof CategoryNode c) {
            int count = switch (c.kind) {
                case AGENTS -> c.server.agents.size();
                case SESSIONS -> c.server.sessions.size();
                case ACTIVE_FILES -> tracker.snapshot().files().size();
            };
            return c.label + " (" + count + ")";
        }
        if (element instanceof FileActivity f) {
            return f.file() + " \u2014 " + f.tool() + " (" + shortId(f.sessionId()) + ")";
        }
        if (element instanceof Agent a) {
            return a.name();
        }
        if (element instanceof Session s) {
            String title = (s.title() != null && !s.title().isEmpty())
                    ? s.title()
                    : (s.slug() == null ? s.id() : s.slug());
            // surface the running agent identity (v1.18.x has no parentID, so no nesting)
            if (s.agent() != null && !s.agent().isEmpty()) {
                return s.agent() + " \u2014 " + title;
            }
            return title;
        }
        return String.valueOf(element);
    }

    String detail(Object element) {
        if (element instanceof ServerNode sn) {
            StringBuilder sb = new StringBuilder(sn.healthy ? "healthy" : "unhealthy");
            if (sn.version != null) {
                sb.append(" • v").append(sn.version);
            }
            if (sn.mode != null) {
                sb.append(" • ").append(sn.mode);
            }
            if (sn.url != null) {
                sb.append(" • ").append(sn.url);
            }
            if (sn.pid != null) {
                sb.append(" • pid ").append(sn.pid);
            }
            return sb.toString();
        }
        if (element instanceof CategoryNode c) {
            if (c.kind == CategoryKind.SESSIONS && !c.server.sessions.isEmpty()) {
                long busy = c.server.sessions.stream().filter(s -> isBusy(c.server, s)).count();
                long roots = c.server.sessions.stream().filter(s -> s.parentID() == null).count();
                return c.server.sessions.size() + " total, " + roots + " top-level"
                        + (busy > 0 ? " • " + busy + " busy" : "");
            }
            return "";
        }
        if (element instanceof Agent a) {
            StringBuilder sb = new StringBuilder();
            if (a.mode() != null) {
                sb.append(a.mode());
            }
            if (a.isNative()) {
                sb.append(" • native");
            }
            if (a.description() != null && !a.description().isEmpty()) {
                sb.append(" — ").append(a.description());
            }
            return sb.toString();
        }
        if (element instanceof Session s) {
            // prefer the derived activity label (thinking… / tool: name — file), then the legacy part label
            String live = trackerLabel(s.id());
            if (live == null && current != null) {
                live = current.activity.get(s.id());
            }
            StringBuilder sb = new StringBuilder(live != null ? live : statusOf(s));
            if (s.parentID() != null) {
                sb.append(" • subagent");
            }
            if (s.time() != null && s.time().updated() > 0) {
                sb.append(" • updated ").append(relative(s.time().updated()));
            }
            return sb.toString();
        }
        return "";
    }

    Image icon(Object element) {
        if (element instanceof ServerNode) {
            return UiActivator.image(UiActivator.ICON_SERVER);
        }
        if (element instanceof CategoryNode) {
            return UiActivator.image(UiActivator.ICON_CATEGORY);
        }
        if (element instanceof Session s) {
            // busy/thinking sessions get a distinct (orange) icon so changes are visible at a glance
            ServerNode node = current;
            boolean active = isBusy(node, s)
                    || (node != null && node.activity.containsKey(s.id()))
                    || sessionActive(s.id());
            return UiActivator.image(active ? UiActivator.ICON_AGENT_BUSY : UiActivator.ICON_AGENT);
        }
        if (element instanceof Agent) {
            return UiActivator.image(UiActivator.ICON_AGENT);
        }
        return null;
    }

    private String statusOf(Session s) {
        ServerNode node = current;
        if (node == null || node.statuses == null) {
            return "idle";
        }
        SessionStatus status = node.statuses.get(s.id());
        String type = (status == null || status.type() == null) ? "idle" : status.type();
        return type;
    }

    /** Derived live label from the tracker: thinking, else the first still-running tool. */
    private String trackerLabel(String sessionId) {
        SessionActivity session = tracker.snapshot().sessions().get(sessionId);
        if (session == null) {
            return null;
        }
        if (session.thinking()) {
            return "thinking\u2026";
        }
        ToolActivity running = session.activity().stream()
                .filter(t -> t.state() == ToolActivity.State.RUNNING)
                .findFirst()
                .orElse(null);
        if (running == null) {
            return null;
        }
        return running.file() != null
                ? "tool: " + running.tool() + " \u2014 " + running.file()
                : "tool: " + running.tool();
    }

    /** Whether the tracker currently sees activity (running/thinking/tool) for the session. */
    private boolean sessionActive(String sessionId) {
        SessionActivity session = tracker.snapshot().sessions().get(sessionId);
        if (session == null) {
            return false;
        }
        return session.running() || session.thinking()
                || session.activity().stream().anyMatch(t -> t.state() == ToolActivity.State.RUNNING);
    }

    private static String shortId(String sessionId) {
        if (sessionId == null) {
            return "";
        }
        return sessionId.length() <= 8 ? sessionId : sessionId.substring(0, 8);
    }

    private static boolean isBusy(ServerNode node, Session s) {
        if (node == null || node.statuses == null) {
            return false;
        }
        SessionStatus status = node.statuses.get(s.id());
        if (status == null || status.type() == null) {
            return false;
        }
        return "busy".equalsIgnoreCase(status.type()) || "retry".equalsIgnoreCase(status.type());
    }

    private static String relative(long epochMillis) {
        long ago = Instant.now().toEpochMilli() - epochMillis;
        if (ago < 0) {
            ago = 0;
        }
        long minutes = ago / 60_000L;
        if (minutes < 1) {
            return "just now";
        }
        if (minutes < 60) {
            return minutes + "m ago";
        }
        long hours = minutes / 60;
        if (hours < 24) {
            return hours + "h ago";
        }
        return (hours / 24) + "d ago";
    }

    /** Tree content provider: server -> categories -> items; sessions nest by parentID. */
    private static final class TreeContentProvider implements ITreeContentProvider {
        private final ActivityTracker tracker;
        private ServerNode root;

        TreeContentProvider(ActivityTracker tracker) {
            this.tracker = tracker;
        }

        @Override
        public Object[] getElements(Object input) {
            if (input instanceof ServerNode node) {
                return new Object[] { node };
            }
            if (input instanceof Object[] array) {
                return array;
            }
            if (input instanceof java.util.Collection<?> collection) {
                return collection.toArray();
            }
            return new Object[0];
        }

        @Override
        public Object[] getChildren(Object parent) {
            if (parent instanceof ServerNode node) {
                List<Object> children = new ArrayList<>();
                children.add(node.agentsCategory);
                children.add(node.sessionsCategory);
                if (!tracker.snapshot().files().isEmpty()) {
                    children.add(node.filesCategory); // hidden while nothing is being worked on
                }
                return children.toArray();
            }
            if (parent instanceof CategoryNode category) {
                if (category.kind == CategoryKind.AGENTS) {
                    return category.server.agents.toArray();
                }
                if (category.kind == CategoryKind.SESSIONS) {
                    return topLevelSessions(category.server).toArray();
                }
                if (category.kind == CategoryKind.ACTIVE_FILES) {
                    return activeFiles();
                }
                return new Object[0];
            }
            if (parent instanceof Session session && root != null) {
                return childrenOf(root, session).toArray();
            }
            return new Object[0];
        }

        @Override
        public Object getParent(Object element) {
            return null;
        }

        @Override
        public boolean hasChildren(Object parent) {
            if (parent instanceof ServerNode) {
                return true;
            }
            if (parent instanceof CategoryNode category) {
                if (category.kind == CategoryKind.AGENTS) {
                    return !category.server.agents.isEmpty();
                }
                if (category.kind == CategoryKind.SESSIONS) {
                    return category.server.sessions.stream().anyMatch(s -> s.parentID() == null);
                }
                if (category.kind == CategoryKind.ACTIVE_FILES) {
                    return !tracker.snapshot().files().isEmpty();
                }
            }
            if (parent instanceof Session session && root != null) {
                return root.sessions.stream().anyMatch(s -> session.id() != null && session.id().equals(s.parentID()));
            }
            return false;
        }

        @Override
        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
            // Input is a List containing a single ServerNode (see ServerView.showNode).
            // Resolve it back to the ServerNode so session->subagent nesting works.
            if (newInput instanceof ServerNode node) {
                root = node;
            } else if (newInput instanceof java.util.Collection<?> collection && !collection.isEmpty()) {
                Object first = collection.iterator().next();
                root = (first instanceof ServerNode) ? (ServerNode) first : null;
            } else {
                root = null;
            }
        }

        @Override
        public void dispose() {
            // stateless beyond root
        }

        private static List<Session> topLevelSessions(ServerNode node) {
            return node.sessions.stream()
                    .filter(s -> s.parentID() == null)
                    .sorted(com.opencode.ide.client.SessionOrder.MOST_RECENT_FIRST) // most current on top
                    .collect(Collectors.toList());
        }

        private Object[] activeFiles() {
            return tracker.snapshot().files().values().stream()
                    .sorted(Comparator.comparing(FileActivity::file))
                    .toArray();
        }

        private static List<Session> childrenOf(ServerNode node, Session parent) {
            return node.sessions.stream()
                    .filter(s -> parent.id() != null && parent.id().equals(s.parentID()))
                    .sorted(com.opencode.ide.client.SessionOrder.MOST_RECENT_FIRST)
                    .collect(Collectors.toList());
        }
    }

    @Override
    public void dispose() {
        if (trackerListener != null) {
            tracker.removeListener(trackerListener);
            trackerListener = null;
        }
        if (eventListener != null) {
            try {
                OpencodeConnection.getInstance().removeEventListener(eventListener);
            } catch (Throwable ignored) {
                // best-effort during dispose
            }
            eventListener = null;
        }
        super.dispose();
    }

    @Override
    public void setFocus() {
        if (viewer != null && !viewer.getControl().isDisposed()) {
            viewer.getControl().setFocus();
        }
    }
}
