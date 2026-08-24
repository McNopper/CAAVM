package com.opencode.ide.chat.views;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;

import com.opencode.ide.chat.internal.ChatLog;
import com.opencode.ide.chat.internal.ChatPage;
import com.opencode.ide.chat.internal.ChatServerConnection;
import com.opencode.ide.chat.internal.ChatSessionController;
import com.opencode.ide.chat.internal.CommandComposer;
import com.opencode.ide.client.ChatCapabilities;
import com.opencode.ide.client.OpencodeClient;
import com.opencode.ide.client.OpencodeEventListener;
import com.opencode.ide.client.OpencodeException;
import com.opencode.ide.client.model.Agent;
import com.opencode.ide.client.model.CommandInfo;
import com.opencode.ide.client.model.Provider;
import com.opencode.ide.client.model.ProviderList;
import com.opencode.ide.core.OpencodeConnection;
import com.opencode.ide.core.OpencodePreferences;

/**
 * Native chat view into the opencode server: markdown (code blocks, tables),
 * LaTeX math ($…$, $$…$$) and mermaid diagrams rendered by an embedded SWT
 * Browser (WebView2/Edge) whose assets are served by a local
 * {@link com.opencode.ide.chat.internal.ChatWebServer} (see there for why
 * file:// URLs do not work for jar'd bundles). The browser side lives in
 * {@link ChatPage}, the session logic in {@link ChatSessionController}; this
 * view is the SWT wiring (layout, selectors, input, toolbar, lifecycle).
 *
 * <p>Multiple instances are supported ({@code allowMultiple=true}); the Eclipse
 * secondary id carries the session to resume ({@code ses_…}) or is unique for a
 * fresh window. Reply text streams in via {@code message.part.delta} events and
 * is finalized with the authoritative render from the completed
 * {@code POST /session/:id/message} reply.</p>
 */
public class ChatView extends ViewPart {

    public static final String ID = "com.opencode.ide.chat.views.ChatView";

    private static final AtomicLong FRESH_COUNTER = new AtomicLong();

    /** "(default)" entry of the variant combo - means "do not send a variant". */
    private static final String VARIANT_DEFAULT = "(default)";

    /** Visible rows of the slash-command picker (it shows up to 8 proposals). */
    private static final int PICKER_ROWS = 5;

    private ChatPage page;
    private ChatSessionController controller;
    private CommandComposer composer;
    private Text input;
    private Button sendButton;
    private Action abortAction;
    private Combo agentCombo;
    private Combo modelCombo;
    private Combo variantCombo;
    private org.eclipse.swt.widgets.List commandPicker;

    /** Current picker proposals (empty = picker hidden). */
    private List<CommandInfo> pickerMatches = List.of();

    /** Escape dismissed the picker until the input text changes again. */
    private boolean pickerDismissed;

    /** provider/model -> its variant names, for the variant combo. */
    private final Map<String, List<String>> variantsByModel = new HashMap<>();

    /** Ambient services for the controller: background jobs, UI dispatch, logging, status. */
    private final ChatSessionController.Host host = new ChatSessionController.Host() {
        @Override
        public void runInBackground(String jobName, Runnable task) {
            Job job = Job.create(jobName, monitor -> {
                task.run();
                return Status.OK_STATUS;
            });
            job.setSystem(true);
            job.schedule();
        }

        @Override
        public void runOnUi(Runnable task) {
            Display.getDefault().asyncExec(task);
        }

        @Override
        public void info(String message) {
            ChatLog.info(message);
        }

        @Override
        public void error(String message, Throwable throwable) {
            ChatLog.error(message, throwable);
        }

        @Override
        public void statusChanged(String description) {
            setContentDescription(description);
        }

        @Override
        public void sendingChanged(boolean sending) {
            if (sendButton != null && !sendButton.isDisposed()) {
                sendButton.setEnabled(!sending);
            }
            if (abortAction != null) {
                abortAction.setEnabled(sending);
            }
        }
    };

    /** The connection adapter over the core singleton (client + SSE events). */
    private final ChatServerConnection connection = new ChatServerConnection() {
        @Override
        public OpencodeClient getClient() throws OpencodeException {
            return OpencodeConnection.getInstance().getClient();
        }

        @Override
        public void addEventListener(OpencodeEventListener listener) {
            OpencodeConnection.getInstance().addEventListener(listener);
        }

        @Override
        public void removeEventListener(OpencodeEventListener listener) {
            OpencodeConnection.getInstance().removeEventListener(listener);
        }
    };

    @Override
    public void createPartControl(Composite parent) {
        Composite outer = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout(1, false);
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        layout.verticalSpacing = 3;
        outer.setLayout(layout);

        page = ChatPage.create(outer);
        if (page == null) {
            return; // fallback label already shown; no chat UI without the browser
        }
        controller = new ChatSessionController(connection, page, host);
        composer = new CommandComposer(connection);
        ChatLog.info("chat view created (browser: " + page.browserType() + ", secondary id: "
                + getViewSite().getSecondaryId() + ")");

        // row 1: agent + model selectors
        Composite selectorRow = new Composite(outer, SWT.NONE);
        // one row: agent | model | variant  (the variant belongs directly after the
        // model it applies to, as in opencode)
        GridLayout selectorLayout = new GridLayout(3, false);
        selectorLayout.marginWidth = 0;
        selectorLayout.marginHeight = 0;
        selectorRow.setLayout(selectorLayout);
        selectorRow.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false));

        agentCombo = new Combo(selectorRow, SWT.DROP_DOWN | SWT.READ_ONLY);
        agentCombo.setToolTipText("Agent");
        agentCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
        modelCombo = new Combo(selectorRow, SWT.DROP_DOWN | SWT.READ_ONLY);
        modelCombo.setToolTipText("Model (provider/model) - pre-set to your preferred default (Preferences → OpenCode)");
        modelCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        modelCombo.addListener(SWT.Selection, e -> fillVariants());
        variantCombo = new Combo(selectorRow, SWT.DROP_DOWN | SWT.READ_ONLY);
        variantCombo.setToolTipText("Reasoning effort (model variant) - as in opencode");
        variantCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));

        // row 1.5: slash-command proposals (inline above the input; excluded
        // from the layout until a "/" trigger shows it)
        commandPicker = new org.eclipse.swt.widgets.List(outer, SWT.BORDER | SWT.V_SCROLL);
        GridData pickerData = new GridData(GridData.FILL, GridData.CENTER, true, false);
        pickerData.exclude = true;
        commandPicker.setLayoutData(pickerData);
        commandPicker.setVisible(false);

        // row 2: prompt input + send button (separate row below the transcript)
        Composite inputRow = new Composite(outer, SWT.NONE);
        GridLayout inputLayout = new GridLayout(2, false);
        inputLayout.marginWidth = 0;
        inputLayout.marginHeight = 0;
        inputRow.setLayout(inputLayout);
        inputRow.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false));

        input = new Text(inputRow, SWT.MULTI | SWT.WRAP | SWT.BORDER);
        input.setToolTipText("Prompt (ENTER sends, Shift+ENTER newline, / commands)");
        GridData inputData = new GridData(SWT.FILL, SWT.CENTER, true, false);
        inputData.heightHint = 52;
        input.setLayoutData(inputData);
        input.addModifyListener(e -> {
            pickerDismissed = false;
            updateCommandPicker();
        });
        input.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                boolean plainEnter = e.character == SWT.CR && (e.stateMask & SWT.SHIFT) == 0;
                if (plainEnter && pickerHasMatches()) {
                    e.doit = false;
                    sendSelection(composer.select(pickerMatches.get(0), input.getText()));
                } else if (e.character == SWT.TAB && pickerHasMatches()) {
                    e.doit = false;
                    completeTopMatch();
                } else if (e.character == SWT.ESC && pickerHasMatches()) {
                    e.doit = false;
                    pickerDismissed = true;
                    updateCommandPicker();
                } else if (plainEnter) {
                    e.doit = false;
                    send();
                }
            }
        });

        sendButton = new Button(inputRow, SWT.PUSH);
        sendButton.setText("Send");
        sendButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
        sendButton.addListener(SWT.Selection, e -> send());

        contributeActions();
        page.load();
        controller.subscribe();
        loadSelectors();
        host.runInBackground("Loading opencode commands", composer::loadCommands);
        maybeResumeFromSecondaryId();
    }

    private void contributeActions() {
        Action newSessionAction = new Action("New Session") {
            @Override
            public void run() {
                controller.startNewSession();
            }
        };
        newSessionAction.setToolTipText("Start a fresh chat session");

        abortAction = new Action("Abort") {
            @Override
            public void run() {
                abortRequested();
            }
        };
        abortAction.setToolTipText("Abort the reply currently being generated (Ctrl+Alt+Shift+A)");
        abortAction.setEnabled(false); // enabled while a send is in flight

        IToolBarManager toolBar = getViewSite().getActionBars().getToolBarManager();
        toolBar.add(newSessionAction);
        toolBar.add(abortAction);
    }

    /**
     * Aborts the in-flight reply (toolbar Stop action and the
     * {@code com.opencode.ide.chat.abort} key binding). The controller posts the
     * abort on a background thread - never the UI thread.
     */
    public void abortRequested() {
        if (controller != null) {
            controller.abort();
        }
    }

    /** @return true while a reply is being generated (used by the abort handler). */
    public boolean isGenerating() {
        return controller != null && controller.isSending();
    }

    // ---------- selectors ----------

    private void loadSelectors() {
        controller.loadSelectorData(new ChatSessionController.SelectorDataListener() {
            @Override
            public void loaded(List<Agent> agents, ProviderList providers, String[] defaultModel) {
                fillSelectors(agents, providers, defaultModel);
            }

            @Override
            public void failed(OpencodeException error) {
                setContentDescription("Error: " + error.getMessage());
            }
        });
    }

    private void fillSelectors(List<Agent> agents, ProviderList providers, String[] fallback) {
        if (agentCombo.isDisposed()) {
            return;
        }
        agentCombo.removeAll();
        int selectAt = 0;
        if (agents != null) {
            for (Agent a : agents) {
                if (!a.isPrimary()) {
                    continue;
                }
                agentCombo.add(a.name());
                if ("build".equals(a.name())) {
                    selectAt = agentCombo.getItemCount() - 1;
                }
            }
        }
        if (agentCombo.getItemCount() > 0) {
            agentCombo.select(selectAt);
        }

        modelCombo.removeAll();
        modelCombo.add("");
        variantsByModel.clear();
        if (providers != null && providers.providers() != null) {
            for (Provider provider : providers.providers()) {
                if (provider.models() == null) {
                    continue;
                }
                for (var model : provider.models().values()) {
                    if (model != null && model.id() != null) {
                        String combined = provider.id() + "/" + model.id();
                        modelCombo.add(combined);
                        List<String> variants = model.variantNames();
                        if (!variants.isEmpty()) {
                            variantsByModel.put(combined, variants);
                        }
                    }
                }
            }
        }
        // preferred default (Preferences → OpenCode) wins when it exists on the live
        // server; otherwise the server's /config default (validated by DefaultModels)
        OpencodePreferences prefs = new OpencodePreferences();
        String[] preferred = prefs.getDefaultModelParts();
        String[] effective = fallback;
        if (preferred != null && modelCombo.indexOf(preferred[0] + "/" + preferred[1]) >= 0) {
            effective = preferred;
        }
        controller.setDefaultModel(effective != null ? effective[0] : null,
                effective != null ? effective[1] : null);
        if (effective != null) {
            String combined = effective[0] + "/" + effective[1];
            int idx = modelCombo.indexOf(combined);
            if (idx < 0) {
                modelCombo.add(combined, 1);
                idx = 1;
            }
            modelCombo.select(idx);
        } else {
            modelCombo.select(0);
        }
        fillVariants();
        // preselect the preferred reasoning variant when the selected model exposes it
        String preferredVariant = prefs.getDefaultVariant();
        if (preferredVariant != null && !preferredVariant.isBlank() && !variantCombo.isDisposed()) {
            int variantIndex = variantCombo.indexOf(preferredVariant);
            if (variantIndex > 0) {
                variantCombo.select(variantIndex);
            }
        }
    }

    /**
     * Populates the variant combo from the selected model (opencode's
     * reasoning-effort variants, e.g. {@code none/low/medium/high/xhigh/max} or
     * {@code none/thinking}). Disabled for models that expose none.
     */
    private void fillVariants() {
        if (variantCombo == null || variantCombo.isDisposed()) {
            return;
        }
        String previous = selectedVariant();
        List<String> variants = variantsByModel.getOrDefault(selectedModel(), List.of());
        variantCombo.removeAll();
        variantCombo.add(VARIANT_DEFAULT);
        for (String variant : variants) {
            variantCombo.add(variant);
        }
        variantCombo.setEnabled(!variants.isEmpty());
        int keep = (previous != null) ? variantCombo.indexOf(previous) : -1;
        variantCombo.select(keep > 0 ? keep : 0);
        variantCombo.setToolTipText(variants.isEmpty()
                ? "This model has no reasoning variants"
                : "Reasoning effort (model variant): " + String.join(", ", variants));
    }

    /** @return the selected {@code provider/model}, or {@code ""}. */
    private String selectedModel() {
        if (modelCombo == null || modelCombo.isDisposed() || modelCombo.getSelectionIndex() < 0) {
            return "";
        }
        return modelCombo.getItem(modelCombo.getSelectionIndex());
    }

    /** @return the selected variant, or {@code null} for the model default. */
    private String selectedVariant() {
        if (variantCombo == null || variantCombo.isDisposed() || variantCombo.getSelectionIndex() <= 0) {
            return null;
        }
        return variantCombo.getItem(variantCombo.getSelectionIndex());
    }

    /** Preselect a model. Used by the openChat command (new chat for a model). */
    public void preselectModel(String providerId, String modelId) {
        if (providerId == null || modelId == null || modelCombo == null || modelCombo.isDisposed()) {
            return;
        }
        String combined = providerId + "/" + modelId;
        int idx = modelCombo.indexOf(combined);
        if (idx < 0) {
            modelCombo.add(combined, 1);
            idx = 1;
        }
        modelCombo.select(idx);
        controller.setDefaultModel(providerId, modelId);
    }

    // ---------- session resume / multiple windows ----------

    /** Secondary id convention: {@code ses_…} resumes that session; anything else = fresh. */
    private void maybeResumeFromSecondaryId() {
        String secondary = getViewSite().getSecondaryId();
        if (secondary == null || !secondary.startsWith("ses_")) {
            return;
        }
        setContentDescription("Resuming " + secondary);
        controller.resume(secondary); // already URL-decoded by the workbench
    }

    /** Opens a NEW chat window (fresh session) with an optional preselected model. */
    public static ChatView openNew(IWorkbenchPage page, String providerId, String modelId) {
        return open(page, "fresh-" + FRESH_COUNTER.incrementAndGet()
                + "-" + Long.toString(System.currentTimeMillis(), 36), providerId, modelId);
    }

    /** Opens (or focuses) the chat window RESUMING the given session. */
    public static ChatView openResume(IWorkbenchPage page, String sessionId) {
        try {
            return open(page, java.net.URLEncoder.encode(sessionId, java.nio.charset.StandardCharsets.UTF_8),
                    null, null);
        } catch (Exception e) {
            return null;
        }
    }

    private static ChatView open(IWorkbenchPage page, String secondaryId, String providerId, String modelId) {
        if (page == null) {
            return null;
        }
        try {
            IViewPart part = page.showView(ID, secondaryId, IWorkbenchPage.VIEW_ACTIVATE);
            if (part instanceof ChatView chat) {
                if (providerId != null && modelId != null) {
                    chat.preselectModel(providerId, modelId);
                }
                chat.setFocus();
                return chat;
            }
        } catch (PartInitException e) {
            ChatLog.error("Failed to open chat view", e);
        }
        return null;
    }

    // ---------- slash-command picker ----------

    /** @return true while the picker shows proposals. */
    private boolean pickerHasMatches() {
        return !pickerMatches.isEmpty();
    }

    /** Recomputes the proposals for the current input text and shows/hides the picker. */
    private void updateCommandPicker() {
        if (composer == null || commandPicker == null || commandPicker.isDisposed()
                || input == null || input.isDisposed()) {
            return;
        }
        String text = input.getText();
        pickerMatches = !pickerDismissed && composer.isPickerTrigger(text)
                ? composer.matches(text)
                : List.of();
        boolean show = !pickerMatches.isEmpty();
        if (show) {
            commandPicker.removeAll();
            for (CommandInfo match : pickerMatches) {
                String description = match.description();
                commandPicker.add("/" + match.name()
                        + (description == null || description.isBlank() ? "" : " - " + description));
            }
            commandPicker.setSelection(0);
            int itemHeight = Math.max(commandPicker.getItemHeight(), 18);
            ((GridData) commandPicker.getLayoutData()).heightHint =
                    Math.min(pickerMatches.size(), PICKER_ROWS) * itemHeight + 4;
        }
        if (show == commandPicker.isVisible()) {
            return;
        }
        commandPicker.setVisible(show);
        ((GridData) commandPicker.getLayoutData()).exclude = !show;
        commandPicker.getParent().layout(true);
    }

    /** Tab: completes the input to the top match and puts the caret after it. */
    private void completeTopMatch() {
        CommandInfo top = pickerMatches.get(0);
        input.setText("/" + top.name() + " ");
        input.setSelection(input.getText().length());
    }

    // ---------- sending ----------

    private void send() {
        if (controller.isSending()) {
            return;
        }
        String text = input.getText().trim();
        if (text.isEmpty()) {
            return;
        }
        sendSelection(composer.resolve(text));
    }

    /** Clears the input and routes one resolved submission: command or message. */
    private void sendSelection(CommandComposer.CommandSelection selection) {
        if (controller.isSending() || selection == null) {
            return;
        }
        input.setText(""); // fires the modify listener, hiding the picker
        if (selection.kind() == CommandComposer.Kind.COMMAND) {
            controller.sendCommand(selection);
        } else {
            controller.send(outgoingMessage(selection.message()));
        }
    }

    private ChatSessionController.OutgoingMessage outgoingMessage(String text) {
        final String agent = (agentCombo.getSelectionIndex() >= 0)
                ? agentCombo.getItem(agentCombo.getSelectionIndex())
                : null;
        String selection = selectedModel();
        final String pickedVariant = selectedVariant();
        final String pickedProvider;
        final String pickedModel;
        int slash = selection.indexOf('/');
        if (slash > 0 && slash < selection.length() - 1) {
            pickedProvider = selection.substring(0, slash);
            pickedModel = selection.substring(slash + 1);
        } else {
            pickedProvider = null;
            pickedModel = null;
        }
        // tells the model what this view renders (math, mermaid, highlighted code)
        final String system = new OpencodePreferences().isAdvertiseRendering()
                ? ChatCapabilities.RENDERER_SYSTEM_PROMPT
                : null;

        return new ChatSessionController.OutgoingMessage(agent, pickedProvider, pickedModel,
                pickedVariant, system, text);
    }

    @Override
    public void dispose() {
        if (controller != null) {
            controller.dispose(); // unsubscribe the SSE event listener
        }
        if (page != null) {
            page.dispose();
        }
        super.dispose();
    }

    @Override
    public void setFocus() {
        if (input != null && !input.isDisposed()) {
            input.setFocus();
        }
    }
}
