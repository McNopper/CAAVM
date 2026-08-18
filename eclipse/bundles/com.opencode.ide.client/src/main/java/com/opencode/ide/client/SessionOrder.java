package com.opencode.ide.client;

import java.util.Comparator;

import com.opencode.ide.client.model.Session;

/**
 * Session comparators. Pure - unit-testable.
 */
public final class SessionOrder {

    /** Most recently updated first; sessions without a time sort last. */
    public static final Comparator<Session> MOST_RECENT_FIRST =
            Comparator.comparingLong((Session s) -> s.time() == null ? 0L : s.time().updated())
                    .reversed();

    private SessionOrder() {
    }
}
