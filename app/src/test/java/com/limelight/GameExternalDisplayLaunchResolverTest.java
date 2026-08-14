package com.limelight;

import static org.junit.Assert.assertEquals;

import android.view.Display;

import org.junit.Test;

public class GameExternalDisplayLaunchResolverTest {
    @Test
    public void requestedSecondaryMatchingActualDisplayUsesDirectExternalMode() {
        assertEquals(Game.ExternalDisplayLaunchMode.DIRECT,
                Game.resolveExternalDisplayLaunch(7, 7, true, -1));
    }

    @Test
    public void requestedSecondaryDifferentFromActualDisplayUsesPresentationFallback() {
        assertEquals(Game.ExternalDisplayLaunchMode.PRESENTATION,
                Game.resolveExternalDisplayLaunch(7, Display.DEFAULT_DISPLAY, true, -1));
    }

    @Test
    public void unavailableRequestedTargetAbortsBeforeConnection() {
        assertEquals(Game.ExternalDisplayLaunchMode.ABORT,
                Game.resolveExternalDisplayLaunch(7, Display.DEFAULT_DISPLAY, false, -1));
    }

    @Test
    public void explicitPresentationTargetTakesPrecedenceOverDirectPlacement() {
        assertEquals(Game.ExternalDisplayLaunchMode.PRESENTATION,
                Game.resolveExternalDisplayLaunch(7, 7, true, 7));
    }

    @Test
    public void controllerFallbackReasonTakesPrecedenceOverPlacementMismatch() {
        assertEquals("not-allowed", Game.resolveExternalDisplayFallbackReason(
                Game.ExternalDisplayLaunchMode.PRESENTATION, "not-allowed"));
        assertEquals("launch-exception", Game.resolveExternalDisplayFallbackReason(
                Game.ExternalDisplayLaunchMode.PRESENTATION, "launch-exception"));
        assertEquals("display-mismatch", Game.resolveExternalDisplayFallbackReason(
                Game.ExternalDisplayLaunchMode.PRESENTATION, null));
    }

    @Test
    public void unavailableTargetTakesPrecedenceOverStaleControllerFallbackReason() {
        assertEquals("target-unavailable", Game.resolveExternalDisplayFallbackReason(
                Game.ExternalDisplayLaunchMode.ABORT, "not-allowed"));
    }
}
