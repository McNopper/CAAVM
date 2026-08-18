package com.opencode.ide.ui.views;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.eclipse.core.commands.Parameterization;
import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerFilter;
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

import com.opencode.ide.client.OpencodeException;
import com.opencode.ide.client.model.HealthStatus;
import com.opencode.ide.client.model.Model;
import com.opencode.ide.client.model.Provider;
import com.opencode.ide.client.model.ProviderList;
import com.opencode.ide.core.OpencodeConnection;
import com.opencode.ide.ui.internal.Refreshable;
import com.opencode.ide.ui.internal.UiActivator;
import com.opencode.ide.ui.internal.ViewLoadSupport;

/**
 * Models from {@code GET /config/providers}, shown as a flat, sortable table.
 *
 * <p>Provider and Model are kept as <b>separate columns</b> (one row per model) so
 * that sorting by either works. A text filter and a "server" indicator (which
 * server these providers came from) are provided.</p>
 */
public class ProvidersView extends ViewPart implements Refreshable {

    public static final String ID = "com.opencode.ide.ui.views.ProvidersView";

    private TableViewer viewer;
    private Text filterText;
    private ModelFilter filter;
    private ModelComparator comparator;
    private Action refreshAction;

    /** One row per model, carrying its provider + whether it is the provider's default. */
    public record ModelRow(String providerName, String providerId, Model model, boolean defaultModel) {
    }

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

        viewer = new TableViewer(tableComposite,
                SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER);
        viewer.getTable().setHeaderVisible(true);
        viewer.getTable().setLinesVisible(true);
        viewer.setContentProvider(ArrayContentProvider.getInstance());

        comparator = new ModelComparator();
        viewer.setComparator(comparator);
        filter = new ModelFilter();
        viewer.addFilter(filter);

        createColumn(layout, "Provider", 2, ModelRow::providerName, 0);
        createColumn(layout, "Model", 3, r -> r.model() == null ? "" : (r.model().name() == null ? r.model().id() : r.model().name()), 1);
        createColumn(layout, "ID", 2, r -> r.model() == null ? "" : r.model().id(), 2);
        createColumn(layout, "Status", 1, r -> r.model() == null || r.model().status() == null ? "" : r.model().status(), 3);
        createColumn(layout, "Capabilities", 1, r -> capabilities(r.model()), 4);
        createColumn(layout, "Context", 1, r -> context(r.model()), 5);
        createColumn(layout, "Default", 1, r -> r.defaultModel() ? "yes" : "", 6);

        viewer.getTable().setSortColumn(viewer.getTable().getColumn(0));
        viewer.getTable().setSortDirection(comparator.getDirection());

        filterText.addModifyListener(e -> {
            filter.setFilter(filterText.getText());
            viewer.refresh(false);
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
            OpencodeConnection connection = OpencodeConnection.getInstance();
            connection.getClient();
            ProviderList list = connection.getClient().getProviders();
            String server = connection.getConnectConfig().baseUrl().toString();
            String healthTag = healthTag(connection);
            return new ProvidersLoad(toRows(list), server, healthTag);
        }, result -> showRows(result.rows(), result.server(), result.healthTag()), this::showError);
    }

    private static String healthTag(OpencodeConnection connection) {
        try {
            HealthStatus h = connection.getClient().getHealth();
            return (h.healthy() ? "healthy" : "unhealthy") + ", v" + h.version();
        } catch (OpencodeException ignored) {
            return "unreachable";
        }
    }

    private static List<ModelRow> toRows(ProviderList list) {
        List<ModelRow> rows = new ArrayList<>();
        if (list == null || list.providers() == null) {
            return rows;
        }
        for (Provider provider : list.providers()) {
            if (provider == null || provider.models() == null) {
                continue;
            }
            String pName = provider.name() == null ? provider.id() : provider.name();
            String defaultModel = list.defaults() == null ? null : list.defaults().get(provider.id());
            provider.models().values().stream()
                    .filter(m -> m != null)
                    .forEach(m -> rows.add(new ModelRow(pName, provider.id(), m, m.id() != null && m.id().equals(defaultModel))));
        }
        return rows;
    }

    private void showRows(List<ModelRow> rows, String server, String healthTag) {
        if (viewer.getControl().isDisposed()) {
            return;
        }
        viewer.setInput(rows);
        setContentDescription("Server: " + server + "  •  " + healthTag + "  •  " + rows.size() + " models");
    }

    private void showError(Throwable e) {
        if (viewer.getControl().isDisposed()) {
            return;
        }
        viewer.setInput(java.util.Collections.emptyList());
        setContentDescription("Error: " + ViewLoadSupport.message(e));
        UiActivator.getDefault().getLog().log(
                new Status(Status.ERROR, UiActivator.PLUGIN_ID, "Failed to load providers", e));
    }

    private static String capabilities(Model m) {
        if (m == null || m.capabilities() == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        if (m.capabilities().reasoning()) {
            sb.append("R");
        }
        if (m.capabilities().attachment()) {
            sb.append("A");
        }
        if (m.capabilities().toolcall()) {
            sb.append("T");
        }
        return sb.toString();
    }

    private static String context(Model m) {
        if (m == null || m.limit() == null) {
            return "";
        }
        long ctx = m.limit().context();
        return ctx <= 0 ? "" : (ctx / 1000) + "k";
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
                // a distinct colored badge per provider on the leading (Provider) column
                if (index != 0) {
                    return null;
                }
                ModelRow row = (ModelRow) element;
                return com.opencode.ide.ui.internal.ProviderIcons.imageFor(row.providerId(), row.providerName());
            }
        });
        layout.setColumnData(column.getColumn(), new ColumnWeightData(weight, 60, true));
        column.getColumn().addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                comparator.applySort(index);
                viewer.getTable().setSortColumn(column.getColumn());
                viewer.getTable().setSortDirection(comparator.getDirection());
                viewer.refresh();
            }
        });
    }

    /** Case-insensitive substring filter across provider and model fields. */
    private static final class ModelFilter extends ViewerFilter {
        private String f = "";

        void setFilter(String text) {
            this.f = (text == null) ? "" : text.trim().toLowerCase(Locale.ROOT);
        }

        @Override
        public boolean select(org.eclipse.jface.viewers.Viewer viewer, Object parentElement, Object element) {
            if (f.isEmpty()) {
                return true;
            }
            ModelRow r = (ModelRow) element;
            Model m = r.model();
            return matches(r.providerName()) || matches(r.providerId())
                    || matches(m == null ? null : m.id())
                    || matches(m == null ? null : m.name())
                    || matches(m == null ? null : m.status());
        }

        private boolean matches(String value) {
            return value != null && value.toLowerCase(Locale.ROOT).contains(f);
        }
    }

    /** Sorts by a chosen column; Context numeric, others by text. */
    private static final class ModelComparator extends ViewerComparator {
        private static final Comparator<String> TEXT = Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER);
        private int column = 0;
        private int direction = SWT.UP;

        void applySort(int clicked) {
            if (clicked == column) {
                direction = (direction == SWT.UP) ? SWT.DOWN : SWT.UP;
            } else {
                column = clicked;
                direction = SWT.UP;
            }
        }

        int getDirection() {
            return direction;
        }

        @Override
        public int compare(org.eclipse.jface.viewers.Viewer viewer, Object a, Object b) {
            ModelRow x = (ModelRow) a;
            ModelRow y = (ModelRow) b;
            int cmp;
            switch (column) {
                case 0:
                    cmp = TEXT.compare(x.providerName(), y.providerName());
                    break;
                case 1:
                    cmp = TEXT.compare(modelName(x), modelName(y));
                    break;
                case 2:
                    cmp = TEXT.compare(modelId(x), modelId(y));
                    break;
                case 3:
                    cmp = TEXT.compare(modelStatus(x), modelStatus(y));
                    break;
                case 4:
                    cmp = TEXT.compare(capabilities(x.model()), capabilities(y.model()));
                    break;
                case 5:
                    cmp = Long.compare(contextValue(x.model()), contextValue(y.model()));
                    break;
                case 6:
                    cmp = Boolean.compare(x.defaultModel(), y.defaultModel());
                    break;
                default:
                    cmp = 0;
            }
            if (cmp == 0) {
                cmp = TEXT.compare(x.providerName(), y.providerName());
                if (cmp == 0) {
                    cmp = TEXT.compare(modelName(x), modelName(y));
                }
            }
            return direction == SWT.DOWN ? -cmp : cmp;
        }

        private static String modelName(ModelRow r) {
            Model m = r.model();
            return m == null ? "" : (m.name() == null ? m.id() : m.name());
        }

        private static String modelId(ModelRow r) {
            Model m = r.model();
            return m == null ? "" : (m.id() == null ? "" : m.id());
        }

        private static String modelStatus(ModelRow r) {
            Model m = r.model();
            return m == null || m.status() == null ? "" : m.status();
        }

        private static long contextValue(Model m) {
            return (m == null || m.limit() == null) ? 0L : m.limit().context();
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
