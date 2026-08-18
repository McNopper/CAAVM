package com.opencode.ide.ui.preferences;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.opencode.ide.core.OpencodeConnection;
import com.opencode.ide.core.OpencodePreferences;

/**
 * Preferences for the connection to the opencode server.
 *
 * <p>Phase 1/3: only {@code connect} mode is wired end-to-end. {@code spawn}
 * mode (managed {@code opencode serve} child process) is added in a later phase;
 * the option is already selectable so the preference shape is stable.</p>
 */
public class ConnectionPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

    private OpencodePreferences prefs;

    private Combo modeCombo;
    private Text urlText;
    private Text userText;
    private Text passwordText;

    @Override
    public void init(IWorkbench workbench) {
        // no-op
    }

    @Override
    protected Control createContents(Composite parent) {
        prefs = new OpencodePreferences();

        Composite composite = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout(1, false);
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        composite.setLayout(layout);
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));

        Group connectGroup = new Group(composite, SWT.NONE);
        connectGroup.setText("opencode server");
        connectGroup.setLayout(new GridLayout(2, false));
        connectGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Label modeLabel = new Label(connectGroup, SWT.NONE);
        modeLabel.setText("Mode:");
        modeCombo = new Combo(connectGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
        modeCombo.add(OpencodePreferences.MODE_CONNECT);
        modeCombo.add(OpencodePreferences.MODE_SPAWN);
        modeCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Label urlLabel = new Label(connectGroup, SWT.NONE);
        urlLabel.setText("Server URL:");
        urlText = new Text(connectGroup, SWT.BORDER);
        urlText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Label userLabel = new Label(connectGroup, SWT.NONE);
        userLabel.setText("Username:");
        userText = new Text(connectGroup, SWT.BORDER);
        userText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Label passwordLabel = new Label(connectGroup, SWT.NONE);
        passwordLabel.setText("Password:");
        passwordText = new Text(connectGroup, SWT.BORDER | SWT.PASSWORD);
        passwordText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Label hint = new Label(connectGroup, SWT.WRAP);
        hint.setText("Spawn mode (default): the plugin starts and manages its own "
                + "'opencode serve' process automatically when you open the OpenCode "
                + "perspective. Switch to Connect mode to use a server you started "
                + "yourself (then set Server URL/Username/Password).");
        hint.setLayoutData(span(2));

        load();
        return composite;
    }

    private static GridData span(int horizontalSpan) {
        GridData data = new GridData(GridData.FILL_HORIZONTAL);
        data.horizontalSpan = horizontalSpan;
        data.widthHint = 200;
        return data;
    }

    private void load() {
        modeCombo.setText(prefs.getMode());
        urlText.setText(prefs.getServerUrl());
        userText.setText(prefs.getUsername());
        passwordText.setText(prefs.getPassword());
    }

    @Override
    protected void performDefaults() {
        modeCombo.setText(OpencodePreferences.MODE_SPAWN);
        urlText.setText("http://127.0.0.1:4096");
        userText.setText("opencode");
        passwordText.setText("");
    }

    @Override
    public boolean performOk() {
        prefs.setMode(modeCombo.getText());
        prefs.setServerUrl(urlText.getText().trim());
        prefs.setUsername(userText.getText().trim());
        prefs.setPassword(passwordText.getText());
        try {
            prefs.save();
        } catch (org.osgi.service.prefs.BackingStoreException e) {
            com.opencode.ide.ui.internal.UiActivator.getDefault().getLog().log(
                    new org.eclipse.core.runtime.Status(
                            org.eclipse.core.runtime.Status.ERROR,
                            com.opencode.ide.ui.internal.UiActivator.PLUGIN_ID,
                            "Could not save opencode preferences", e));
            return false;
        }
        OpencodeConnection.getInstance().refresh();
        return true;
    }
}
