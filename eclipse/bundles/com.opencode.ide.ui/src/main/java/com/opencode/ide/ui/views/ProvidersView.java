package com.opencode.ide.ui.views;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.Parameterization;
import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.ILazyContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.part.ViewPart;

import com.opencode.ide.client.OpencodeClient;
import com.opencode.ide.client.OpencodeException;
import com.opencode.ide.client.model.HealthStatus;
import com.opencode.ide.client.model.ProviderList;
import com.opencode.ide.core.ConnectionsManager;
import com.opencode.ide.core.ManagedConnection;
import com.opencode.ide.core.OpencodeConnection;
import com.opencode.ide.ui.internal.ProviderIcons;
import com.opencode.ide.ui.internal.ProviderLogos;
import com.opencode.ide.ui.internal.Refreshable;
import com.opencode.ide.ui.internal.UiActivator;
import com.opencode.ide.ui.internal.ViewLoadSupport;
import com.opencode.ide.ui.model.ModelComparator;
import com.opencode.ide.ui.model.ModelFilter;
import com.opencode.ide.ui.model.ModelRow;
import com.opencode.ide.ui.model.ModelRows;

/**
 * Models from {@code GET /config/providers}, shown as a flat, sortable table.
 *
 * <p>Provider and Model are kept as <b>separate columns</b> (one row per model) so
 * that sorting by either works. A text filter and a "server" indicator (which
 * server these providers came from) are provided.</p>
 *
 * <p>The table is {@code SWT.VIRTUAL} with an {@link ILazyContentProvider}: rows
 * are materialized on demand from the element array. Filtering and sorting
 * operate on the source array (no per-element {@code ViewerFilter} callbacks),
 * and the data comes from the primary connection's cached providers via the
 * {@link ConnectionsManager} (30s TTL / invalidated on disconnect).</p>
 */
public class ProvidersView extends ViewPart implements Refreshable {

    public static final String ID = "com.opencode.ide.ui.views.ProvidersView";

    private TableViewer viewer;
    private Text filterText;
    private ModelFilter filter;
    private ModelComparator comparator;
    private Action refreshAction;

    /** All rows from the last load (the filter/sort source array). */
    private volatile ModelRow[] source = new ModelRow[0];

    private record ProvidersLoad(List<ModelRow> rows, String server, String healthTag) {
    }

    @Override
    public void createPartControl(Composite parent) {
        Composite outer = new Composite(parent, SWT.NONE);
        GridLayout outerLayout = new GridLayout(1, false);
        outerLayout.marginWidth = 0;
        outerLayout.marginHeight = 0;
        outerLayout.verticalSpacing = 2;
        outer.setLayout(outerLayout);
        outer.setLayoutData(new GridData(GridData.FILL_BOTH));

        filterText = new Text(outer, SWT.SEARCH | SWT.ICON_CANCEL | SWT.BORDER);
        filterText.setMessage("Filter by provider / model / id / status...");
        filterText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        Composite tableComposite = new Composite(outer, SWT.NONE);
        TableColumnLayout layout = new TableColumnLayout();
        tableComposite.setLayout(layout);
        tableComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        // SWT.VIRTUAL + ILazyContentProvider: rows are only materialized when
        // scrolled into view; the element array is swapped on filter/sort/load.
        viewer = new TableViewer(tableComposite,
                SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER | SWT.VIRTUAL);
        viewer.setUseHashlookup(true);
        viewer.getTable().setHeaderVisible(true);
        viewer.getTable().setLinesVisible(true);
        viewer.setContentProvider(new LazyModelContentProvider());

        filter = new ModelFilter();
        comparator = new ModelComparator();

        createColumn(layout, "Provider", 2, ModelRow::providerName, 0);
        createColumn(layout, "Model", 3, r -> r.model() == null ? "" : (r.model().name() == null ? r.model().id() : r.model().name()), 1);
        createColumn(layout, "ID", 2, r -> r.model() == null ? "" : r.model().id(), 2);
        createColumn(layout, "Status", 1, r -> r.model() == null || r.model().status() == null ? "" : r.model().status(), 3);
        createColumn(layout, "Capabilities", 1, r -> ModelRows.capabilities(r.model()), 4);
        createColumn(layout, "Context", 1, r -> ModelRows.context(r.model()), 5);
        createColumn(layout, "Default", 1, r -> r.defaultModel() ? "yes" : "", 6);

        viewer.getTable().setSortColumn(viewer.getTable().getColumn(0));
        viewer.getTable().setSortDirection(comparator.isAscending() ? SWT.UP : SWT.DOWN);

        filterText.addModifyListener(e -> {
            filter.setFilter(filterText.getText());
            applyFilterAndSort();
        });

        // double-click a model row -> open the chat with that model preselected
        viewer.addDoubleClickListener((DoubleClickEvent e) ->
                openChatWithSelection((IStructuredSelection) e.getSelection()));

        // context menu with the same action
        Action chatAction = new Action("Chat with This Model") {
            @Override
            public void run() {
                openChatWithSelection(viewer.getStructuredSelection());
            }
        };
        MenuManager menuManager = new MenuManager();
        menuManager.add(chatAction);
        Menu menu = menuManager.createContextMenu(viewer.getControl());
        viewer.getControl().setMenu(menu);

        contributeActions();
        refresh();
    }

    /** Launches the chat (via the openChat command) with the selected model. */
    private void openChatWithSelection(IStructuredSelection selection) {
        if (selection == null || selection.isEmpty()) {
            return;
        }
        Object first = selection.getFirstElement();
        if (!(first instanceof ModelRow row) || row.model() == null || row.model().id() == null) {
            return;
        }
        try {
            ICommandService commands = getViewSite().getService(ICommandService.class);
            var command = commands.getCommand("com.opencode.ide.chat.openChat");
            if (!command.isDefined()) {
                MessageDialog.openInformation(viewer.getControl().getShell(),
                        "Chat", "The opencode Chat view is not installed.");
                return;
            }
            ParameterizedCommand parameterized = new ParameterizedCommand(command, new Parameterization[] {
                    new Parameterization(command.getParameter("com.opencode.ide.chat.openChat.providerId"),
                            row.providerId()),
                    new Parameterization(command.getParameter("com.opencode.ide.chat.openChat.modelId"),
                            row.model().id()) });
            IHandlerService handlers = getViewSite().getService(IHandlerService.class);
            handlers.executeCommand(parameterized, null);
        } catch (Exception e) {
            UiActivator.getDefault().getLog().log(
                    new Status(Status.ERROR, UiActivator.PLUGIN_ID, "Failed to open chat", e));
        }
    }

    private void contributeActions() {
        refreshAction = new Action("Refresh") {
            @Override
            public void run() {
                refresh();
            }
        };
        refreshAction.setToolTipText("Refresh providers from the opencode server");
        IToolBarManager toolBar = getViewSite().getActionBars().getToolBarManager();
        toolBar.add(refreshAction);
    }

    @Override
    public void refresh() {
        setContentDescription("Loading...");
        ViewLoadSupport.load("Loading opencode providers", () -> {
            ConnectionsManager manager = ConnectionsManager.getDefault();
            OpencodeClient client = primaryClient(manager);
            String healthTag = healthTag(client.getHealth()); // also ensures spawned/connected
            ProviderList list = manager.providers(manager.primaryConnection());
            String server = OpencodeConnection.getInstance().getConnectConfig().baseUrl().toString();
            return new ProvidersLoad(ModelRows.toRows(list), server, healthTag);
        }, result -> showRows(result.rows(), result.server(), result.healthTag()), this::showError);
    }

    private static OpencodeClient primaryClient(ConnectionsManager manager) throws OpencodeException {
        ManagedConnection primary = manager.primaryConnection();
        if (primary != null) {
            return primary.client();
        }
        return OpencodeConnection.getInstance().getClient();
    }

    private static String healthTag(HealthStatus health) {
        return ((health != null && health.healthy()) ? "healthy" : "unhealthy")
                + (health != null && health.version() != null ? ", v" + health.version() : "");
    }

    private void showRows(List<ModelRow> rows, String server, String healthTag) {
        if (viewer.getControl().isDisposed()) {
            return;
        }
        source = rows.toArray(new ModelRow[0]);
        applyFilterAndSort();
        setContentDescription("Server: " + server + "  •  " + healthTag + "  •  " + rows.size() + " models");
    }

    /** Recomputes the visible array from the source (filter + sort) and pushes it into the viewer. */
    private void applyFilterAndSort() {
        if (viewer.getControl().isDisposed()) {
            return;
        }
        ModelRow[] visible = filterAndSort(source);
        viewer.setInput(visible);
        viewer.setItemCount(visible.length);
    }

    private ModelRow[] filterAndSort(ModelRow[] input) {
        List<ModelRow> rows = new ArrayList<>(input.length);
        for (ModelRow row : input) {
            if (filter.accepts(row)) {
                rows.add(row);
            }
        }
        rows.sort(comparator);
        return rows.toArray(new ModelRow[0]);
    }

    private void showError(Throwable e) {
        if (viewer.getControl().isDisposed()) {
            return;
        }
        viewer.setInput(java.util.Collections.emptyList());
        viewer.setItemCount(0);
        setContentDescription("Error: " + ViewLoadSupport.message(e));
        UiActivator.getDefault().getLog().log(
                new Status(Status.ERROR, UiActivator.PLUGIN_ID, "Failed to load providers", e));
    }

    private void createColumn(TableColumnLayout layout, String title, int weight,
            java.util.function.Function<ModelRow, String> value, int index) {
        TableViewerColumn column = new TableViewerColumn(viewer, SWT.NONE);
        column.getColumn().setText(title);
        column.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                return value.apply((ModelRow) element);
            }

            @Override
            public Image getImage(Object element) {
                // a distinct icon per provider on the leading (Provider) column:
                // the vendored logo when available, the colored badge otherwise
                if (index != 0) {
                    return null;
                }
                ModelRow row = (ModelRow) element;
                Image logo = ProviderLogos.imageFor(row.providerId());
                return logo != null ? logo : ProviderIcons.imageFor(row.providerId(), row.providerName());
            }
        });
        layout.setColumnData(column.getColumn(), new ColumnWeightData(weight, 60, true));
        column.getColumn().addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                comparator.applySort(index);
                viewer.getTable().setSortColumn(column.getColumn());
                viewer.getTable().setSortDirection(comparator.isAscending() ? SWT.UP : SWT.DOWN);
                applyFilterAndSort();
            }
        });
    }

    /** Serves rows by index for the virtual table; the array is swapped on every filter/sort/load. */
    private final class LazyModelContentProvider implements ILazyContentProvider {
        private ModelRow[] rows = new ModelRow[0];

        @Override
        public void updateElement(int index) {
            ModelRow[] snapshot = rows;
            if (index >= 0 && index < snapshot.length) {
                viewer.replace(snapshot[index], index);
            }
        }

        @Override
        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
            rows = newInput instanceof ModelRow[] array ? array : new ModelRow[0];
        }

        @Override
        public void dispose() {
            rows = new ModelRow[0];
        }
    }

    @Override
    public void setFocus() {
        if (filterText != null && !filterText.isDisposed()) {
            filterText.setFocus();
        } else if (viewer != null && !viewer.getControl().isDisposed()) {
            viewer.getControl().setFocus();
        }
    }
}
