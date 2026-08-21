package com.opencode.ide.board.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.opencode.ide.tasks.Task;

/**
 * Immutable result of {@link BoardModel#refresh()}: the five kanban columns
 * (all statuses always present, in store order), the selected sprint's goal,
 * totals, and an error/notice string ({@code null} when the board loaded
 * fine — e.g. "task store not found at ..." when the store root is missing).
 *
 * <p>In {@link BoardModel.BoardMode#PIPELINE} the snapshot additionally
 * carries a {@link PipelineSnapshot} (all ten V stages plus the trailing
 * untracked group); in {@link BoardModel.BoardMode#FLAT} {@link #pipeline()}
 * is {@code null} and the flat columns are the authoritative grouping.</p>
 */
public record BoardSnapshot(Map<String, List<TicketRow>> columns, String sprintGoal,
        int total, int blockedCount, String error, PipelineSnapshot pipeline) {

    /** FLAT-mode shape: no pipeline grouping. */
    public BoardSnapshot(Map<String, List<TicketRow>> columns, String sprintGoal,
            int total, int blockedCount, String error) {
        this(columns, sprintGoal, total, blockedCount, error, null);
    }

    /** An all-empty board carrying an error/notice message. */
    public static BoardSnapshot empty(String error) {
        Map<String, List<TicketRow>> columns = new LinkedHashMap<>();
        for (String status : Task.VALID_STATUSES) {
            columns.put(status, new ArrayList<>());
        }
        return new BoardSnapshot(columns, "", 0, 0, error);
    }

    /** One column's rows; never {@code null}, missing statuses read as empty. */
    public List<TicketRow> column(String status) {
        List<TicketRow> rows = columns.get(status);
        return rows == null ? List.of() : rows;
    }
}
