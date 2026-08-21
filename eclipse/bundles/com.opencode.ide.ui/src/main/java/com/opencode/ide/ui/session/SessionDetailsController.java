package com.opencode.ide.ui.session;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

import com.opencode.ide.client.OpencodeClient;
import com.opencode.ide.client.model.ChatEntry;
import com.opencode.ide.client.model.ChatMessageInfo;
import com.opencode.ide.client.model.ChatPart;
import com.opencode.ide.client.model.Session;

/**
 * SWT-free controller behind the Session Details view: loads the message
 * history of one session ({@code GET /session/:id/message}) plus the session
 * list (for the title) and maps it into an immutable
 * {@link SessionDetails} snapshot the view can render.
 *
 * <p>{@link #load()} never throws: supplier/client failures (including
 * {@link com.opencode.ide.client.OpencodeException} wrapped by the supplier in
 * a runtime exception) are converted into a snapshot whose {@code errorNote}
 * carries the message. Blocking is fine here — the view wraps {@code load()}
 * in a background job via {@code ViewLoadSupport}.</p>
 */
public final class SessionDetailsController {

    /** Shown as the error note when the session has no messages at all. */
    public static final String EMPTY_NOTE = "No messages in this session yet.";

    private final String sessionId;
    private final Supplier<OpencodeClient> clientSupplier;

    public SessionDetailsController(String sessionId, Supplier<OpencodeClient> clientSupplier) {
        this.sessionId = sessionId;
        this.clientSupplier = clientSupplier;
    }

    public String sessionId() {
        return sessionId;
    }

    /**
     * @return the snapshot for this session; on any failure a snapshot with an
     *         {@code errorNote} (never {@code null}, never throws).
     */
    public SessionDetails load() {
        try {
            OpencodeClient client = clientSupplier.get();
            List<ChatEntry> messages = client.getMessages(sessionId);
            List<Session> sessions = client.getSessions();
            return build(sessions, messages);
        } catch (Exception e) {
            return new SessionDetails(sessionId, null, null, null, null, List.of(), message(e));
        }
    }

    private SessionDetails build(List<Session> sessions, List<ChatEntry> messages) {
        String title = findTitle(sessions);
        List<MessageRow> rows = new ArrayList<>();
        String lastAssistantModel = null;
        Double totalCost = null;
        TokenTotals totals = null;
        for (ChatEntry entry : messages == null ? List.<ChatEntry>of() : messages) {
            if (entry == null) {
                continue;
            }
            rows.add(toRow(entry));
            ChatMessageInfo info = entry.info();
            if (info != null && "assistant".equals(info.role())) {
                if (!info.modelLabel().isEmpty()) {
                    lastAssistantModel = info.modelLabel();
                }
                if (info.cost() != null) {
                    totalCost = (totalCost == null ? 0.0 : totalCost) + info.cost();
                }
                if (info.tokens() != null) {
                    totals = TokenTotals.add(totals, info.tokens());
                }
            }
        }
        String note = rows.isEmpty() ? EMPTY_NOTE : null;
        return new SessionDetails(sessionId, title, lastAssistantModel, totalCost, totals,
                List.copyOf(rows), note);
    }

    private String findTitle(List<Session> sessions) {
        if (sessions == null) {
            return null;
        }
        for (Session session : sessions) {
            if (session != null && sessionId != null && sessionId.equals(session.id())) {
                return session.title();
            }
        }
        return null;
    }

    private static MessageRow toRow(ChatEntry entry) {
        ChatMessageInfo info = entry.info();
        List<ToolLine> tools = new ArrayList<>();
        for (ChatPart part : entry.parts()) {
            if (part != null && part.isTool() && part.tool() != null) {
                tools.add(new ToolLine(part.tool(), part.stateName()));
            }
        }
        return new MessageRow(
                info == null ? null : info.role(),
                info == null ? null : info.agent(),
                info == null ? "" : info.modelLabel(),
                timeLabel(info == null ? null : info.time()),
                entry.text(),
                entry.reasoning(),
                List.copyOf(tools));
    }

    /** Deterministic, locale-free label: ISO-8601 instant, seconds precision ("" when unknown). */
    static String timeLabel(Session.Time time) {
        if (time == null) {
            return "";
        }
        long millis = time.created() > 0 ? time.created() : time.updated();
        return millis > 0 ? Instant.ofEpochMilli(millis).truncatedTo(ChronoUnit.SECONDS).toString() : "";
    }

    /** Deepest available message (unwraps supplier-wrapped {@code OpencodeException}s). */
    private static String message(Throwable t) {
        Throwable cause = t;
        while (cause != null && cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        String m = cause == null ? null : cause.getMessage();
        return (m == null || m.isBlank()) ? (cause == null ? "unknown error" : cause.getClass().getSimpleName()) : m;
    }

    // ---------- snapshot records (immutable; the view renders, never mutates) ----------

    /**
     * Immutable snapshot: header aggregates plus the ordered message rows, or
     * an {@code errorNote} when loading failed / the history is empty.
     */
    public record SessionDetails(String sessionId, String title, String modelLabel, Double totalCost,
            TokenTotals tokens, List<MessageRow> rows, String errorNote) {

        public SessionDetails {
            rows = (rows == null) ? List.of() : List.copyOf(rows);
        }
    }

    /** One rendered message: text/reasoning plus its tool calls. */
    public record MessageRow(String role, String agent, String modelLabel, String timeLabel,
            String text, String reasoning, List<ToolLine> tools) {

        public MessageRow {
            tools = (tools == null) ? List.of() : List.copyOf(tools);
        }
    }

    /** One tool call part ({@code name} never {@code null}; {@code state} may be). */
    public record ToolLine(String name, String state) {
    }

    /** Per-field token sums ({@code null} fields were never reported by any message). */
    public record TokenTotals(Long input, Long output, Long reasoning, Long cacheRead, Long cacheWrite) {

        public boolean isEmpty() {
            return input == null && output == null && reasoning == null
                    && cacheRead == null && cacheWrite == null;
        }

        /** Human-readable summary for the view header, e.g. {@code "in 1,234 • out 567 • reasoning 12"}. */
        public String summary() {
            StringBuilder sb = new StringBuilder();
            append(sb, "in ", input);
            append(sb, " • out ", output);
            append(sb, " • reasoning ", reasoning);
            if (cacheRead != null || cacheWrite != null) {
                sb.append(" • cache ");
                if (cacheRead != null) {
                    sb.append(String.format(Locale.ROOT, "%,dr", cacheRead));
                }
                if (cacheRead != null && cacheWrite != null) {
                    sb.append('/');
                }
                if (cacheWrite != null) {
                    sb.append(String.format(Locale.ROOT, "%,dw", cacheWrite));
                }
            }
            return sb.toString();
        }

        private static void append(StringBuilder sb, String label, Long value) {
            if (value != null) {
                sb.append(label).append(String.format(Locale.ROOT, "%,d", value));
            }
        }

        static TokenTotals add(TokenTotals acc, Session.Tokens tokens) {
            if (tokens == null) {
                return acc;
            }
            return new TokenTotals(
                    plus(acc == null ? null : acc.input(), tokens.input()),
                    plus(acc == null ? null : acc.output(), tokens.output()),
                    plus(acc == null ? null : acc.reasoning(), tokens.reasoning()),
                    tokens.cache() == null
                            ? (acc == null ? null : acc.cacheRead())
                            : plus(acc == null ? null : acc.cacheRead(), tokens.cache().read()),
                    tokens.cache() == null
                            ? (acc == null ? null : acc.cacheWrite())
                            : plus(acc == null ? null : acc.cacheWrite(), tokens.cache().write()));
        }

        private static Long plus(Long acc, long value) {
            return (acc == null ? 0L : acc) + value;
        }
    }
}
