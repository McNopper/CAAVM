package com.opencode.ide.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.opencode.ide.client.ServerVersionPin.Verdict;
import com.opencode.ide.client.model.HealthStatus;

/**
 * Unit tests for {@link ServerVersionPin}: the pinned-version verdict over
 * one {@link HealthStatus} — matching (whitespace tolerated), mismatch
 * (healthy flag irrelevant, warning naming both versions and the remedy),
 * and the unknown cases (null snapshot, null or blank version) — plus the
 * never-throws contract.
 */
public class ServerVersionPinTest {

    @Test
    public void pinnedVersionIsMatching() {
        Verdict verdict = ServerVersionPin.evaluate(new HealthStatus(true, ServerVersionPin.PINNED_VERSION));
        assertEquals(Verdict.Kind.MATCHING, verdict.kind());
        assertEquals(ServerVersionPin.PINNED_VERSION, verdict.serverVersion());
        assertNull(verdict.warning());
    }

    @Test
    public void surroundingWhitespaceStillMatchesAndIsNormalized() {
        Verdict verdict = ServerVersionPin.evaluate(new HealthStatus(true, "  " + ServerVersionPin.PINNED_VERSION + " "));
        assertEquals(Verdict.Kind.MATCHING, verdict.kind());
        assertEquals(ServerVersionPin.PINNED_VERSION, verdict.serverVersion());
    }

    @Test
    public void otherVersionIsMismatchCarryingTheSeenVersion() {
        Verdict verdict = ServerVersionPin.evaluate(new HealthStatus(true, "1.19.0"));
        assertTrue(verdict.isMismatch());
        assertEquals(Verdict.Kind.MISMATCH, verdict.kind());
        assertEquals("1.19.0", verdict.serverVersion());
    }

    @Test
    public void theHealthyFlagIsNotPartOfTheVerdict() {
        assertEquals(Verdict.Kind.MISMATCH,
                ServerVersionPin.evaluate(new HealthStatus(false, "1.19.0")).kind());
        assertEquals(Verdict.Kind.MATCHING,
                ServerVersionPin.evaluate(new HealthStatus(false, ServerVersionPin.PINNED_VERSION)).kind());
    }

    @Test
    public void mismatchWarningNamesBothVersionsAndTheRemedy() {
        String warning = ServerVersionPin.evaluate(new HealthStatus(true, "1.19.0")).warning();
        assertEquals("opencode server 1.19.0 != pinned " + ServerVersionPin.PINNED_VERSION
                + " - the endpoint cross-check may have rotted; rerun the endpoint smoke", warning);
    }

    @Test
    public void nullVersionIsUnknown() {
        Verdict verdict = ServerVersionPin.evaluate(new HealthStatus(true, null));
        assertEquals(Verdict.Kind.UNKNOWN, verdict.kind());
        assertNull(verdict.serverVersion());
        assertNull(verdict.warning());
    }

    @Test
    public void blankVersionIsUnknown() {
        for (String blank : new String[] { "", "   ", "\t" }) {
            Verdict verdict = ServerVersionPin.evaluate(new HealthStatus(true, blank));
            assertEquals("blank reads as UNKNOWN: '" + blank + "'", Verdict.Kind.UNKNOWN, verdict.kind());
            assertNull("no warning without a version", verdict.warning());
        }
    }

    @Test
    public void nullSnapshotIsUnknownAndNeverThrows() {
        Verdict verdict = ServerVersionPin.evaluate(null);
        assertEquals(Verdict.Kind.UNKNOWN, verdict.kind());
        assertNull(verdict.serverVersion());
        assertNull(verdict.warning());
    }
}
