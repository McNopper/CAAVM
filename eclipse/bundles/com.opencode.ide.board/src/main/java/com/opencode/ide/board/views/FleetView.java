package com.opencode.ide.board.views;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.part.ViewPart;

import com.opencode.ide.board.fleet.FleetJobHandle;
import com.opencode.ide.board.fleet.TaskFleetLauncher;
import com.opencode.ide.board.internal.GitCli;
import com.opencode.ide.board.model.DiffSource;
import com.opencode.ide.board.model.FleetJobsModel;
import com.opencode.ide.board.model.SessionDiffText;
import com.opencode.ide.client.OpencodeException;
import com.opencode.ide.client.model.FileDiff;
import com.opencode.ide.core.OpencodeConnection;

/**
 * The Fleet view: one row per fleet job in the shared {@link FleetJobsModel}
 * (fed by the Board view's "Launch task"), with state coloring and
 * Open diff / Open folder / Take over actions. Refreshes automatically on
 * model changes ({@code asyncExec} from the model listener). "Open diff"
 * shows the server's authoritative session diff when the job carries a
 * session id ({@link DiffSource}), falling back to the local git branch
 * diff; both run on a background thread (the client may spawn/wait for the
 * server, git can block for up to a minute) and open the dialog from
 * {@code asyncExec}; the action stays disabled while a diff is running.
 *
 * <p>The "Permissions (n)" toolbar action (enabled when n &gt; 0, count kept
 * live via the shared permission queue's listener) opens
 * {@link FleetPermissionsDialog} where unattended sessions' permission
 * requests are answered (approve once / always / reject).</p>
 */
public class FleetView extends ViewPart {

    public static final String ID = "com.opencode.ide.board.views.FleetView";

    private static final String EMPTY_STATE = "No fleet jobs launched yet — use the Board view.";

    private TableViewer viewer;
    private Composite tableComposite;
    private Label emptyLabel;
    private Action openDiffAction;
    private Action openFolderAction;
    private Action takeOverAction;
    private Action permissionsAction;
    /** Single daemon thread for git diff processes (OSGi-light, never blocks the UI thread). */
    private ExecutorService diffExecutor;
    private final AtomicBoolean diffRunning = new AtomicBoolean();
    private final Runnable modelListener = () -> {
        Display display = Display.getDefault();
        if (display != null && !display.isDisposed()) {
            display.asyncExec(this::refreshFromModel);
        }
    };
    /** Live pending-count hint: the permission queue notifies on every change (SSE thread). */
    private final Runnable permissionsListener = () -> {
        Display display = Display.getDefault();
        if (display != null && !display.isDisposed()) {
            display.asyncExec(this::updatePermissionsAction);
        }
    };

    @Override
    public void createPartControl(Composite parent) {
        diffExecutor = Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(task, "board-fleet-diff");
            thread.setDaemon(true);
            return thread;
        });

        Composite outer = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout(1, false);
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        outer.setLayout(layout);

        tableComposite = new Composite(outer, SWT.NONE);
        TableColumnLayout tableLayout = new TableColumnLayout();
        tableComposite.setLayout(tableLayout);
        tableComposite.setLayoutData(new GridData(GridData.FILL_BOTH));

        viewer = new TableViewer(tableComposite,
                SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER);
        viewer.getTable().setHeaderVisible(true);
        viewer.getTable().setLinesVisible(true);
        viewer.setContentProvider(ArrayContentProvider.getInstance());
        viewer.addSelectionChangedListener((ISelectionChangedListener) e -> updateActionEnablement());
        createColumn("Task", 40, row -> row.taskId(), false);
        createColumn("Session", 40, row -> row.sessionId(), false);
        createColumn("Worktree", 120, row -> row.worktree(), false);
        createColumn("State", 30, row -> row.state() == null ? "" : row.state().toString(), true);
        createColumn("Detail", 120, row -> row.detail(), false);

        emptyLabel = new Label(outer, SWT.CENTER | SWT.WRAP);
        emptyLabel.setText(EMPTY_STATE);
        GridData emptyData = new GridData(GridData.FILL_BOTH);
        emptyLabel.setLayoutData(emptyData);

        contributeActions();
        FleetJobsModel.getDefault().addListener(modelListener);
        TaskFleetLauncher.permissions().addListener(permissionsListener);
        refreshFromModel();
        updatePermissionsAction();
    }

    private interface RowText {
        String text(FleetJobHandle row);
    }

    /** @param stateColumn true only for the State column — keys the coloring on the column, not its header text */
    private void createColumn(String title, int weight, RowText value, boolean stateColumn) {
        TableViewerColumn column = new TableViewerColumn(viewer, SWT.NONE);
        column.getColumn().setText(title);
        column.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                FleetJobHandle row = asRow(element);
                return row == null ? "" : value.text(row);
            }

            @Override
            public Color getForeground(Object element) {
                FleetJobHandle row = asRow(element);
                if (row == null || row.state() == null || !stateColumn) {
                    return null;
                }
                Display display = viewer.getControl().getDisplay();
                return switch (row.state()) {
                    case FAILED -> display.getSystemColor(SWT.COLOR_RED);
                    case MERGED -> display.getSystemColor(SWT.COLOR_DARK_GREEN);
                    case RUNNING -> display.getSystemColor(SWT.COLOR_DARK_BLUE);
                    case COMPLETED -> null;
                };
            }
        });
        ((TableColumnLayout) tableComposite.getLayout())
                .setColumnData(column.getColumn(), new ColumnWeightData(weight, 60, true));
    }

    private static FleetJobHandle asRow(Object element) {
        return element instanceof FleetJobHandle row ? row : null;
    }

    private void contributeActions() {
        openDiffAction = new Action("Open diff") {
            @Override
            public void run() {
                openDiff();
            }
        };
        openDiffAction.setToolTipText(
                "Session diff from the opencode server; falls back to a git diff of the task branch");
        openDiffAction.setEnabled(false);

        openFolderAction = new Action("Open folder") {
            @Override
            public void run() {
                openFolder();
            }
        };
        openFolderAction.setToolTipText("Open the job's worktree in the file explorer");
        openFolderAction.setEnabled(false);

        takeOverAction = new Action("Take over") {
            @Override
            public void run() {
                takeOver();
            }
        };
        takeOverAction.setToolTipText("Open the worktree and mark the job as taken over");
        takeOverAction.setEnabled(false);

        permissionsAction = new Action("Permissions") {
            @Override
            public void run() {
                new FleetPermissionsDialog(getSite().getShell(),
                        TaskFleetLauncher.permissions()).open();
            }
        };
        permissionsAction.setToolTipText(
                "Pending permission requests of unattended fleet sessions (approve once / always / reject)");
        permissionsAction.setEnabled(false);

        IToolBarManager toolbar = getViewSite().getActionBars().getToolBarManager();
        toolbar.add(permissionsAction);
        toolbar.add(openDiffAction);
        toolbar.add(openFolderAction);
        toolbar.add(takeOverAction);
    }

    /** Refreshes the "Permissions (n)" hint from the queue (UI thread). */
    private void updatePermissionsAction() {
        if (permissionsAction == null || viewer == null || viewer.getControl().isDisposed()) {
            return;
        }
        int pending = TaskFleetLauncher.permissions().pendingCount();
        permissionsAction.setText(pending == 0 ? "Permissions" : "Permissions (" + pending + ")");
        permissionsAction.setEnabled(pending > 0);
    }

    private FleetJobHandle selectedRow() {
        IStructuredSelection selection = viewer.getStructuredSelection();
        if (selection == null || selection.isEmpty()
                || !(selection.getFirstElement() instanceof FleetJobHandle row)) {
            return null;
        }
        return row;
    }

    private void updateActionEnablement() {
        boolean hasSelection = selectedRow() != null;
        openDiffAction.setEnabled(hasSelection && !diffRunning.get());
        openFolderAction.setEnabled(hasSelection);
        takeOverAction.setEnabled(hasSelection);
    }

    private void refreshFromModel() {
        if (viewer == null || viewer.getControl().isDisposed()) {
            return;
        }
        List<FleetJobHandle> jobs = FleetJobsModel.getDefault().jobs();
        viewer.setInput(jobs);
        boolean empty = jobs.isEmpty();
        setLaidOut(tableComposite, !empty);
        setLaidOut(emptyLabel, empty);
        viewer.getControl().getParent().layout();
        updateActionEnablement();
        updatePermissionsAction();
    }

    private static void setLaidOut(Control control, boolean visible) {
        control.setVisible(visible);
        GridData data = (GridData) control.getLayoutData();
        data.exclude = !visible;
    }

    /**
     * Runs the diff off the UI thread and opens the dialog via asyncExec.
     * Server-first: a job with a session id tries the authoritative
     * {@code GET /session/:id/diff} via the shared connection (whose client
     * may block while spawning the server — hence the background thread); an
     * empty or failing server diff falls back to the local git branch diff.
     */
    private void openDiff() {
        FleetJobHandle row = selectedRow();
        if (row == null || diffRunning.get()) {
            return;
        }
        boolean preferServer = DiffSource.of(row) == DiffSource.Source.SERVER;
        Path repo = repoRootOf(row);
        if (!preferServer && (repo == null || !Files.isDirectory(repo))) {
            MessageDialog.openInformation(getSite().getShell(), "Open diff",
                    "No main repo known for " + row.taskId()
                            + " (the job has no fleet worktree yet).");
            return;
        }
        diffRunning.set(true);
        updateActionEnablement();
        String taskId = row.taskId();
        String sessionId = row.sessionId();
        Path repoRoot = repo;
        ExecutorService executor = diffExecutor;
        if (executor == null) {
            diffRunning.set(false);
            return;
        }
        executor.execute(() -> {
            String title = null;
            String text = null;
            String failure = null;
            if (preferServer) {
                try {
                    List<FileDiff> diffs = OpencodeConnection.getInstance().getClient()
                            .getSessionDiff(sessionId, null);
                    if (!diffs.isEmpty()) {
                        title = "Diff " + taskId + " (session " + sessionId + ")";
                        text = SessionDiffText.format(diffs);
                    }
                } catch (OpencodeException | RuntimeException e) {
                    // no authoritative diff (server down, unknown session) — try git
                }
            }
            if (text == null) {
                if (repoRoot == null || !Files.isDirectory(repoRoot)) {
                    failure = "No server diff for session " + sessionId
                            + " and no fleet worktree to run a local git diff against.";
                } else {
                    title = "Diff " + taskId + " ("
                            + com.opencode.ide.git.FleetGit.branchFor(taskId) + ")";
                    try {
                        String diff = GitCli.diff(repoRoot, taskId);
                        text = diff.isBlank() ? "(no differences)" : diff;
                    } catch (RuntimeException e) {
                        failure = e.getMessage();
                    }
                }
            }
            String dialogTitle = title;
            String result = text;
            String error = failure;
            Display display = Display.getDefault();
            if (display == null || display.isDisposed()) {
                return;
            }
            display.asyncExec(() -> {
                if (viewer == null || viewer.getControl().isDisposed()) {
                    return;
                }
                diffRunning.set(false);
                updateActionEnablement();
                if (error != null) {
                    MessageDialog.openError(getSite().getShell(), "Open diff", error);
                } else {
                    new TextDialog(getSite().getShell(), dialogTitle, result).open();
                }
            });
        });
    }

    private void openFolder() {
        FleetJobHandle row = selectedRow();
        if (row == null) {
            return;
        }
        Path worktree = worktreeOf(row);
        if (worktree != null && Files.isDirectory(worktree)) {
            Program.launch(worktree.toString());
        } else {
            MessageDialog.openInformation(getSite().getShell(), "Open folder",
                    "Worktree not found: " + (worktree == null ? "(none)" : worktree.toString()));
        }
    }

    private void takeOver() {
        FleetJobHandle row = selectedRow();
        if (row == null) {
            return;
        }
        Path worktree = worktreeOf(row);
        if (worktree != null && Files.isDirectory(worktree)) {
            Program.launch(worktree.toString());
            FleetJobsModel.getDefault().update(new FleetJobHandle(row.taskId(), row.sessionId(),
                    row.worktree(), row.state(), "taken over by user"));
        } else {
            MessageDialog.openInformation(getSite().getShell(), "Take over",
                    "Worktree not found: " + (worktree == null ? "(none)" : worktree.toString()));
        }
    }

    private static Path worktreeOf(FleetJobHandle row) {
        String worktree = row.worktree();
        return worktree == null || worktree.isBlank() ? null : Path.of(worktree);
    }

    private static Path repoRootOf(FleetJobHandle row) {
        Path worktree = worktreeOf(row);
        if (worktree == null) {
            return null;
        }
        Path fleetDir = worktree.getParent();
        Path gitDir = fleetDir == null ? null : fleetDir.getParent();
        return gitDir == null ? null : gitDir.getParent();
    }

    @Override
    public void setFocus() {
        if (viewer != null && !viewer.getControl().isDisposed()) {
            viewer.getControl().setFocus();
        }
    }

    @Override
    public void dispose() {
        FleetJobsModel.getDefault().removeListener(modelListener);
        TaskFleetLauncher.permissions().removeListener(permissionsListener);
        if (diffExecutor != null) {
            diffExecutor.shutdown();
            diffExecutor = null;
        }
        super.dispose();
    }
}
