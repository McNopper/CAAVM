package com.opencode.ide.ui;

import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

/**
 * The "OpenCode" perspective: Server view to the left (per-server explorer with
 * agents + running sessions), Providers view at the bottom. Editor area in the
 * center/top-right.
 */
public class OpencodePerspective implements IPerspectiveFactory {

    public static final String ID = "com.opencode.ide.ui.perspective";

    @Override
    public void createInitialLayout(IPageLayout layout) {
        String editorArea = layout.getEditorArea();

        // Views are added via addView (never addStandaloneView with showTitle=false):
        // regular views come with a title bar, so they are closeable (X),
        // detachable/floatable (drag the title bar), movable/dockable, and reopenable
        // via Window -> Show View.
        layout.addView(
                com.opencode.ide.ui.views.ServerView.ID,
                IPageLayout.LEFT, 0.32f, editorArea);

        // Chat view (contributed by com.opencode.ide.chat - referenced by ID so this
        // bundle has no compile dependency on it; silently skipped if not installed).
        layout.addView("com.opencode.ide.chat.views.ChatView", IPageLayout.RIGHT, 0.55f, editorArea);

        layout.addView(
                com.opencode.ide.ui.views.ProvidersView.ID,
                IPageLayout.BOTTOM, 0.50f, editorArea);

        layout.addShowViewShortcut(com.opencode.ide.ui.views.ServerView.ID);
        layout.addShowViewShortcut("com.opencode.ide.chat.views.ChatView");
        layout.addShowViewShortcut(com.opencode.ide.ui.views.ProvidersView.ID);
        layout.addPerspectiveShortcut(ID);
    }
}
