package com.opencode.ide.client.activity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import com.opencode.ide.client.model.OpencodeEvent;

/**
 * Derives live fleet activity from the opencode {@code /event} SSE stream:
 * per-session running/thinking state with tool invocations, and the set of
 * files agents currently work on. Feed every event through {@link #apply};
 * read state via {@link #snapshot()}; observe changes via {@link #addListener}.
 *
 * <p>Thread-safe; listeners run on the calling thread of {@link #apply}.</p>
 */
public final class ActivityTracker {

    private static final class MutableSession {
        volatile boolean running;
        volatile boolean thinking;
        final Map<String, ToolActivity> tools = new ConcurrentHashMap<>();
    }

    private final Map<String, MutableSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, FileActivity> files = new ConcurrentHashMap<>();
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    /** Registers a change notification (runs on whichever thread applies events). */
    public void addListener(Runnable listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void removeListener(Runnable listener) {
        listeners.remove(listener);
    }

    /** Feeds one event; updates derived state and notifies listeners on change. */
    public void apply(OpencodeEvent event) {
        if (event == null || event.type() == null) {
            return;
        }
        boolean changed = false;
        switch (event.type()) {
            case "session.status" -> changed = applyStatus(event);
            case "session.idle" -> changed = applyIdle(event);
            case "session.deleted" -> changed = applyDeleted(event);
            case "message.part.updated" -> changed = applyPart(event);
            default -> {
            }
        }
        if (changed) {
            for (Runnable listener : listeners) {
                listener.run();
            }
        }
    }

    private boolean applyStatus(OpencodeEvent event) {
        String sessionId = event.string("sessionID");
        if (sessionId == null) {
            return false;
        }
        String status = event.string("status");
        boolean running = "busy".equals(status) || "retry".equals(status);
        MutableSession session = sessions.get(sessionId);
        if (session == null) {
            if (!running) {
                return false;
            }
            MutableSession created = new MutableSession();
            created.running = true;
            sessions.put(sessionId, created);
            return true;
        }
        if (session.running != running) {
            session.running = running;
            return true;
        }
        return false;
    }

    private boolean applyIdle(OpencodeEvent event) {
        String sessionId = event.string("sessionID");
        if (sessionId == null) {
            return false;
        }
        MutableSession session = sessions.get(sessionId);
        if (session != null && session.running) {
            session.running = false;
            return true;
        }
        return false;
    }

    private boolean applyDeleted(OpencodeEvent event) {
        String sessionId = event.string("sessionID");
        if (sessionId == null || sessions.remove(sessionId) == null) {
            return false;
        }
        files.values().removeIf(f -> sessionId.equals(f.sessionId()));
        return true;
    }

    private boolean applyPart(OpencodeEvent event) {
        String sessionId = event.string("sessionID");
        String partType = event.at("part.type");
        if (sessionId == null || partType == null) {
            return false;
        }
        if ("tool".equals(partType)) {
            return applyToolPart(event, sessionId);
        }
        boolean thinking = "reasoning".equals(partType);
        MutableSession session = sessions.get(sessionId);
        if (session == null) {
            if (!thinking) {
                return false;
            }
            session = sessions.computeIfAbsent(sessionId, id -> new MutableSession());
        }
        if (session.thinking != thinking) {
            session.thinking = thinking;
            return true;
        }
        return false;
    }

    private boolean applyToolPart(OpencodeEvent event, String sessionId) {
        String tool = event.at("part.tool");
        String state = event.at("part.state.status");
        if (tool == null && state == null) {
            return false;
        }
        String file = firstNonNull(event.at("part.input.filePath"), event.at("part.input.path"),
                event.at("part.input.file"), event.at("part.input.absolutePath"));
        ToolActivity.State parsed = parseState(state);
        ToolActivity activity = new ToolActivity(tool, file, parsed);
        MutableSession session = sessions.computeIfAbsent(sessionId, id -> new MutableSession());
        boolean changed = false;
        ToolActivity previous = session.tools.put(tool + "|" + file, activity);
        if (previous == null || !previous.equals(activity)) {
            changed = true;
        }
        if (file != null) {
            if (parsed == ToolActivity.State.RUNNING) {
                FileActivity entry = new FileActivity(sessionId, tool, file);
                FileActivity previousEntry = files.put(file, entry);
                if (previousEntry == null || !previousEntry.equals(entry)) {
                    changed = true;
                }
            } else if (files.remove(file) != null) {
                changed = true;
            }
        }
        if (session.thinking) {
            session.thinking = false;
            changed = true;
        }
        return changed;
    }

    private static ToolActivity.State parseState(String state) {
        if (state == null) {
            return ToolActivity.State.RUNNING;
        }
        return switch (state) {
            case "completed" -> ToolActivity.State.COMPLETED;
            case "error" -> ToolActivity.State.ERROR;
            default -> ToolActivity.State.RUNNING;
        };
    }

    private static String firstNonNull(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    /** Drops all derived state for a session (e.g. after it ended). */
    public void sessionEnded(String sessionId) {
        if (sessionId == null) {
            return;
        }
        if (sessions.remove(sessionId) != null) {
            files.values().removeIf(f -> sessionId.equals(f.sessionId()));
            for (Runnable listener : listeners) {
                listener.run();
            }
        }
    }

    /** @return an immutable copy of the current derived activity. */
    public ActivitySnapshot snapshot() {
        Map<String, SessionActivity> copy = new java.util.LinkedHashMap<>();
        sessions.forEach((id, session) -> copy.put(id,
                new SessionActivity(id, session.running, session.thinking,
                        new ArrayList<>(session.tools.values()))));
        return new ActivitySnapshot(copy, new java.util.LinkedHashMap<>(files));
    }
}
