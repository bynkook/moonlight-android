package com.limelight.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import com.limelight.Game;
import com.limelight.utils.ExternalDisplayControlActivity.PresentationState;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowDisplayManager;

import java.util.ArrayList;
import java.util.List;

@Config(sdk = {33}, shadows = {com.limelight.shadows.ShadowMoonBridge.class, com.limelight.shadows.ShadowGameManager.class})
@RunWith(RobolectricTestRunner.class)
public class ExternalDisplayDirectLaunchTest {
    @Before
    public void setUp() {
        ShadowDisplayManager.reset();
        Game.instance = null;
        Shadows.shadowOf(RuntimeEnvironment.getApplication().getPackageManager()).setSystemFeature(
                PackageManager.FEATURE_ACTIVITIES_ON_SECONDARY_DISPLAYS, true);
    }

    @Test
    public void eligibleDirectLaunchUsesRequestedDisplayInActivityOptions() {
        int displayId = ShadowDisplayManager.addDisplay("w1920dp-h1080dp");
        RecordingExternalDisplayControlActivity activity = startActivity(displayId, true, true, false);

        assertEquals(1, activity.successfulStarts.size());
        StartRecord started = activity.successfulStarts.get(0);
        assertNotNull(started.options);
        assertEquals(displayId, started.options.getInt("android.activity.launchDisplayId", -1));
        assertEquals(displayId, started.intent.getIntExtra(Game.EXTRA_DISPLAY_ID, -1));
        assertFalse(started.intent.hasExtra(Game.EXTRA_PRESENTATION_DISPLAY_ID));
    }

    @Test
    public void directLaunchExceptionUsesOneDefaultDisplayPresentationFallbackWithoutMutatingSource() {
        int displayId = ShadowDisplayManager.addDisplay("w1920dp-h1080dp");
        RecordingExternalDisplayControlActivity activity = startActivity(displayId, true, true, true);

        assertEquals(2, activity.attemptedStarts.size());
        assertEquals(1, activity.successfulStarts.size());
        StartRecord fallback = activity.successfulStarts.get(0);
        assertNull(fallback.options);
        assertEquals(0, fallback.intent.getIntExtra(Game.EXTRA_DISPLAY_ID, -1));
        assertEquals(displayId, fallback.intent.getIntExtra(Game.EXTRA_PRESENTATION_DISPLAY_ID, -1));
        assertEquals("launch-exception", fallback.intent.getStringExtra(
                Game.EXTRA_EXTERNAL_DISPLAY_FALLBACK_REASON));
        assertFalse(activity.sourceIntent.hasExtra(Game.EXTRA_PRESENTATION_DISPLAY_ID));
    }

    @Test
    public void unsupportedFeatureLaunchesOneIsolatedPresentationFallback() {
        int displayId = ShadowDisplayManager.addDisplay("w1920dp-h1080dp");
        RecordingExternalDisplayControlActivity activity = startActivity(displayId, false, true, false);

        assertOnePresentationFallback(activity, displayId, "not-supported");
    }

    @Test
    public void disallowedSecondaryLaunchesOneIsolatedPresentationFallback() {
        int displayId = ShadowDisplayManager.addDisplay("w1920dp-h1080dp");
        RecordingExternalDisplayControlActivity activity = startActivity(displayId, true, false, false);

        assertOnePresentationFallback(activity, displayId, "not-allowed");
    }

    @Test
    public void directLaunchEligibilityRequiresPlatformFeatureTargetAndAllowance() {
        assertTrue(ExternalDisplayControlActivity.shouldAttemptDirectExternalLaunch(
                30, true, true, true, false));
        assertFalse(ExternalDisplayControlActivity.shouldAttemptDirectExternalLaunch(
                29, true, true, true, false));
        assertFalse(ExternalDisplayControlActivity.shouldAttemptDirectExternalLaunch(
                30, false, true, true, false));
        assertFalse(ExternalDisplayControlActivity.shouldAttemptDirectExternalLaunch(
                30, true, false, true, false));
        assertFalse(ExternalDisplayControlActivity.shouldAttemptDirectExternalLaunch(
                30, true, true, false, false));
    }

    @Test
    public void unavailableTargetAbortsWithoutLaunchingPresentation() {
        RecordingExternalDisplayControlActivity activity = startActivity(42, true, true, false);

        assertTrue(activity.attemptedStarts.isEmpty());
        assertTrue(activity.isFinishing());
    }

    @Test
    public void controllerGameReadinessUsesDirectFocusOnlyForDirectMode() {
        int displayId = ShadowDisplayManager.addDisplay("w1920dp-h1080dp");
        RecordingExternalDisplayControlActivity directActivity = startActivity(displayId, true, true, false);
        Game directGame = mock(Game.class);
        when(directGame.isInPresentationMode()).thenReturn(false);

        assertEquals(PresentationState.IDLE,
                directActivity.gameReadyForTest(directGame));
        assertEquals(1, directActivity.gameFocusRequests);

        RecordingExternalDisplayControlActivity presentationActivity = startActivity(
                displayId, true, false, false);
        Game presentationGame = mock(Game.class);
        when(presentationGame.isInPresentationMode()).thenReturn(true);

        assertEquals(PresentationState.LAUNCHING_GAME,
                presentationActivity.gameReadyForTest(presentationGame));
        assertEquals(0, presentationActivity.gameFocusRequests);
    }

    private void assertOnePresentationFallback(RecordingExternalDisplayControlActivity activity,
                                               int displayId,
                                               String fallbackReason) {
        assertEquals(1, activity.attemptedStarts.size());
        assertEquals(1, activity.successfulStarts.size());
        StartRecord fallback = activity.successfulStarts.get(0);
        assertNull(fallback.options);
        assertEquals(0, fallback.intent.getIntExtra(Game.EXTRA_DISPLAY_ID, -1));
        assertEquals(displayId, fallback.intent.getIntExtra(Game.EXTRA_PRESENTATION_DISPLAY_ID, -1));
        assertEquals(fallbackReason, fallback.intent.getStringExtra(
                Game.EXTRA_EXTERNAL_DISPLAY_FALLBACK_REASON));
        assertFalse(activity.sourceIntent.hasExtra(Game.EXTRA_PRESENTATION_DISPLAY_ID));
        assertFalse(activity.sourceIntent.hasExtra(Game.EXTRA_EXTERNAL_DISPLAY_FALLBACK_REASON));
    }

    private RecordingExternalDisplayControlActivity startActivity(int displayId,
                                                                    boolean featureSupported,
                                                                    boolean activityStartAllowed,
                                                                    boolean throwDirectLaunch) {
        Intent gameIntent = new Intent();
        gameIntent.setClassName("com.limelight", Game.class.getName());
        gameIntent.putExtra(Game.EXTRA_DISPLAY_ID, displayId);

        Intent wrapperIntent = new Intent();
        wrapperIntent.putExtra(ExternalDisplayControlActivity.EXTRA_LAUNCH_INTENT, gameIntent);

        Shadows.shadowOf(RuntimeEnvironment.getApplication().getPackageManager()).setSystemFeature(
                PackageManager.FEATURE_ACTIVITIES_ON_SECONDARY_DISPLAYS, featureSupported);
        RecordingExternalDisplayControlActivity.activityManager = mock(ActivityManager.class);
        when(RecordingExternalDisplayControlActivity.activityManager.isActivityStartAllowedOnDisplay(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq(displayId),
                org.mockito.ArgumentMatchers.any())).thenReturn(activityStartAllowed);
        RecordingExternalDisplayControlActivity.throwDirectLaunch = throwDirectLaunch;
        RecordingExternalDisplayControlActivity activity = Robolectric
                .buildActivity(RecordingExternalDisplayControlActivity.class, wrapperIntent)
                .create()
                .get();
        activity.sourceIntent = gameIntent;
        return activity;
    }

    public static class RecordingExternalDisplayControlActivity extends ExternalDisplayControlActivity {
        static ActivityManager activityManager;
        static boolean throwDirectLaunch;
        final List<StartRecord> attemptedStarts = new ArrayList<>();
        final List<StartRecord> successfulStarts = new ArrayList<>();
        Intent sourceIntent;
        int gameFocusRequests;

        @Override
        public Object getSystemService(String name) {
            if (Context.ACTIVITY_SERVICE.equals(name)) {
                return activityManager;
            }
            return super.getSystemService(name);
        }

        PresentationState gameReadyForTest(Game game) {
            return onGameReadyForController(game);
        }

        @Override
        protected void requestGameFocus() {
            gameFocusRequests++;
        }

        @Override
        public void startActivity(Intent intent, Bundle options) {
            StartRecord record = new StartRecord(new Intent(intent), options == null ? null : new Bundle(options));
            attemptedStarts.add(record);
            if (throwDirectLaunch && options != null) {
                throw new IllegalStateException("direct launch rejected");
            }
            successfulStarts.add(record);
        }
    }

    private static final class StartRecord {
        final Intent intent;
        final Bundle options;

        StartRecord(Intent intent, Bundle options) {
            this.intent = intent;
            this.options = options;
        }
    }
}
