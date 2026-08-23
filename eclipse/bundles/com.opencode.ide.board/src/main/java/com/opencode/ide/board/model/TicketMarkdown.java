package com.opencode.ide.board.model;

import java.util.List;

import com.opencode.ide.tasks.Task;

/**
 * Composes one ticket into a single markdown document for the Board's details
 * view (rendered by the chat web component, so tables, math and mermaid
 * diagrams stored in tickets render as diagrams).
 *
 * <p>Sections: description, acceptance criteria, todos, artifacts, comments.
 * Checkmark items are plain literal {@code [x]}/{@code [ ]} text (the renderer
 * has no task-list plugin) — honest for a read-only, agent-owned store.
 * Comments render as top-level markdown so a diagram stored in a comment
 * renders as a diagram.</p>
 */
public final class TicketMarkdown {

    private TicketMarkdown() {
    }

    /** @return the full ticket document (description first, newest comment last). */
    public static String document(Task task) {
        StringBuilder sb = new StringBuilder();
        sb.append(safe(task.description == null || task.description.isBlank()
                ? "_no description_" : task.description.trim()));

        section(sb, "Acceptance criteria", task.acceptanceCriteria.isEmpty());
        for (String item : task.acceptanceCriteria) {
            sb.append("- [ ] ").append(safe(item)).append('\n');
        }

        section(sb, "Todos", task.todos.isEmpty());
        for (Task.Todo todo : task.todos) {
            sb.append("- ").append(todo.done() ? "[x]" : "[ ]")
                    .append(' ').append(safe(todo.text())).append('\n');
        }

        section(sb, "Artifacts", task.artifacts.isEmpty());
        for (Task.Artifact artifact : task.artifacts) {
            sb.append("- `").append(safe(artifact.kind())).append("` `").append(safe(artifact.ref()))
                    .append('`');
            if (artifact.note() != null && !artifact.note().isBlank()) {
                sb.append(" — ").append(safe(artifact.note()));
            }
            sb.append('\n');
        }

        section(sb, "Comments", task.comments.isEmpty());
        List<Task.Comment> comments = task.comments;
        int from = Math.max(0, comments.size() - 10); // newest last, capped
        for (int i = from; i < comments.size(); i++) {
            Task.Comment c = comments.get(i);
            if (i > from) {
                sb.append('\n');
            }
            sb.append("**[").append(Task.formatTs(c.ts())).append("] ")
                    .append(c.by() == null ? "?" : c.by()).append(":**\n\n")
                    .append(safe(c.text())).append('\n');
        }
        return sb.toString();
    }

    private static void section(StringBuilder sb, String title, boolean empty) {
        sb.append("\n## ").append(title).append('\n');
        if (empty) {
            sb.append("_(none)_\n");
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}
