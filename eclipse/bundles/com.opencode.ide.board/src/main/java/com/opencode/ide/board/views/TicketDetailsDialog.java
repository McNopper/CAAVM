package com.opencode.ide.board.views;

import java.nio.file.Path;
import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import com.opencode.ide.tasks.Task;

/**
 * Read-only ticket details (SWT widgets only, no browser): metadata line,
 * description, acceptance-criteria checklist, todos, artifacts with an
 * {@code Open} action, and recent comments.
 */
final class TicketDetailsDialog extends Dialog {

    private static final int RECENT_COMMENTS = 5;

    private final Task task;
    private final Path repoRoot;

    TicketDetailsDialog(Shell parentShell, Task task, Path repoRoot) {
        super(parentShell);
        this.task = task;
        this.repoRoot = repoRoot;
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText((task.id == null ? "Ticket" : task.id) + " — " + safe(task.title));
    }

    @Override
    protected boolean isResizable() {
        return true;
    }

    @Override
    protected Point getInitialSize() {
        return new Point(760, 640);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite body = (Composite) super.createDialogArea(parent);
        body.setLayoutData(new GridData(GridData.FILL_BOTH));

        ScrolledComposite scroll = new ScrolledComposite(body, SWT.V_SCROLL | SWT.H_SCROLL);
        scroll.setExpandHorizontal(true);
        scroll.setExpandVertical(true);
        scroll.setLayoutData(new GridData(GridData.FILL_BOTH));

        Composite inner = new Composite(scroll, SWT.NONE);
        GridLayout layout = new GridLayout(1, false);
        layout.marginHeight = 0;
        inner.setLayout(layout);

        section(inner, metaLine());
        wrapText(inner, description());

        section(inner, "Acceptance criteria");
        checklist(inner, task.acceptanceCriteria, false);

        section(inner, "Todos");
        Table todos = checklist(inner,
                task.todos.stream().map(t -> t.text() == null ? "" : t.text()).toList(), true);
        for (int i = 0; i < task.todos.size(); i++) {
            todos.getItem(i).setChecked(task.todos.get(i).done());
        }

        section(inner, "Artifacts");
        Table artifacts = artifacts(inner);
        Button open = new Button(inner, SWT.PUSH);
        open.setText("Open artifact");
        open.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
        open.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                int index = artifacts.getSelectionIndex();
                if (index >= 0 && index < task.artifacts.size()) {
                    openArtifact(task.artifacts.get(index));
                } else {
                    info("Select an artifact first.");
                }
            }
        });

        section(inner, "Comments");
        wrapText(inner, comments());

        scroll.setContent(inner);
        scroll.setMinSize(inner.computeSize(SWT.DEFAULT, SWT.DEFAULT));
        return body;
    }

    private String metaLine() {
        StringBuilder sb = new StringBuilder();
        sb.append(safe(task.status));
        if (task.sprint != null) {
            sb.append("  •  sprint ").append(task.sprint);
        }
        if (task.role != null) {
            sb.append("  •  ").append(task.role);
        }
        sb.append("  •  ").append(task.storyPoints).append(" pts");
        if (task.assignee != null) {
            sb.append("  •  @").append(task.assignee);
        }
        if (task.blocked) {
            sb.append("  •  BLOCKED: ").append(safe(task.blocker));
        }
        return sb.toString();
    }

    private String description() {
        String text = task.description;
        return text == null || text.isBlank() ? "(no description)" : text;
    }

    private String comments() {
        if (task.comments.isEmpty()) {
            return "(no comments)";
        }
        StringBuilder sb = new StringBuilder();
        int from = Math.max(0, task.comments.size() - RECENT_COMMENTS);
        for (int i = task.comments.size() - 1; i >= from; i--) {
            Task.Comment c = task.comments.get(i);
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append('[').append(Task.formatTs(c.ts())).append("] ")
                    .append(c.by() == null ? "?" : c.by()).append(": ")
                    .append(safe(c.text()));
        }
        return sb.toString();
    }

    private void openArtifact(Task.Artifact artifact) {
        String kind = artifact.kind() == null ? "" : artifact.kind();
        String ref = artifact.ref() == null ? "" : artifact.ref();
        if (ref.isBlank()) {
            info("Artifact has no reference.");
            return;
        }
        switch (kind) {
            case "file", "path" -> {
                com.opencode.ide.board.model.ArtifactResolver.Result result =
                        com.opencode.ide.board.model.ArtifactResolver.resolve(repoRoot, ref);
                if (result.openable()) {
                    Program.launch(result.path().toString());
                } else {
                    info(result.refusal());
                }
            }
            case "url", "doc", "git" -> copyToClipboard(ref);
            default -> info("Unsupported artifact kind: " + kind);
        }
    }

    private void copyToClipboard(String text) {
        Clipboard clipboard = new Clipboard(getShell().getDisplay());
        try {
            clipboard.setContents(new Object[] { text }, new Transfer[] { TextTransfer.getInstance() });
            info("Copied to clipboard:\n" + text);
        } finally {
            clipboard.dispose();
        }
    }

    private Label section(Composite parent, String title) {
        Label label = new Label(parent, SWT.NONE);
        label.setText(title);
        label.setFont(JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT));
        label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
        return label;
    }

    private Text wrapText(Composite parent, String content) {
        Text text = new Text(parent, SWT.READ_ONLY | SWT.MULTI | SWT.WRAP | SWT.BORDER);
        text.setText(content);
        text.setBackground(text.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
        text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        return text;
    }

    private Table checklist(Composite parent, List<String> items, boolean emptyAsNone) {
        Table table = new Table(parent, SWT.CHECK | SWT.BORDER | SWT.V_SCROLL);
        GridData data = new GridData(SWT.FILL, SWT.CENTER, true, false);
        data.heightHint = 60;
        table.setLayoutData(data);
        if (items.isEmpty() && emptyAsNone) {
            new TableItem(table, SWT.NONE).setText("(none)");
        }
        for (String item : items) {
            new TableItem(table, SWT.NONE).setText(item == null ? "" : item);
        }
        return table;
    }

    private Table artifacts(Composite parent) {
        Table table = new Table(parent, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
        table.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        if (task.artifacts.isEmpty()) {
            new TableItem(table, SWT.NONE).setText("(no artifacts)");
        }
        for (Task.Artifact artifact : task.artifacts) {
            String line = safe(artifact.kind()) + ":" + safe(artifact.ref());
            if (artifact.note() != null && !artifact.note().isBlank()) {
                line += " — " + artifact.note();
            }
            new TableItem(table, SWT.NONE).setText(line);
        }
        table.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                int index = table.getSelectionIndex();
                if (index >= 0 && index < task.artifacts.size()) {
                    openArtifact(task.artifacts.get(index));
                }
            }
        });
        return table;
    }

    private void info(String message) {
        MessageDialog.openInformation(getShell(), "Ticket " + safe(task.id), message);
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}
