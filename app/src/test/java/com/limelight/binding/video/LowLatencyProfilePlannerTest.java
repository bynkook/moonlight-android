package com.limelight.binding.video;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.media.MediaFormat;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class LowLatencyProfilePlannerTest {
    private static final Set<String> S25_VENDOR_PARAMETERS = parameters(
            LowLatencyProfilePlanner.QTI_PICTURE_ORDER,
            LowLatencyProfilePlanner.QTI_OUTPUT_FENCE_ENABLE,
            LowLatencyProfilePlanner.QTI_OUTPUT_FENCE_TYPE);

    @Test
    public void profileOptionsAreOrderedAndImmutable() {
        LinkedHashMap<String, Integer> options = new LinkedHashMap<>();
        options.put("low-latency", 1);
        options.put("vendor.test.option", 2);

        LowLatencyProfilePlanner.Profile profile =
                new LowLatencyProfilePlanner.Profile("test-profile", options);
        options.put("added-after-construction", 3);

        assertEquals(
                Arrays.asList("low-latency", "vendor.test.option"),
                new ArrayList<>(profile.integerOptions.keySet()));

        try {
            profile.integerOptions.put("mutation", 4);
            fail("Profile options must be immutable");
        }
        catch (UnsupportedOperationException expected) {
            // Expected.
        }
    }

    @Test
    public void duplicateOrderedOptionsAreRemoved() {
        LowLatencyProfilePlanner.Profile first = profile(
                "first", "low-latency", 1, "vendor.test.option", 2);
        LowLatencyProfilePlanner.Profile duplicate = profile(
                "duplicate", "low-latency", 1, "vendor.test.option", 2);
        LowLatencyProfilePlanner.Profile reverseOrder = profile(
                "reverse-order", "vendor.test.option", 2, "low-latency", 1);

        List<LowLatencyProfilePlanner.Profile> profiles =
                LowLatencyProfilePlanner.deduplicateProfiles(
                        Arrays.asList(first, duplicate, reverseOrder));

        assertEquals(2, profiles.size());
        assertEquals("first", profiles.get(0).name);
        assertEquals("reverse-order", profiles.get(1).name);
    }

    @Test
    public void standardOffReturnsAndroidStandardOptionProfileOnly() {
        List<LowLatencyProfilePlanner.Profile> profiles = LowLatencyProfilePlanner.plan(
                capabilities("vendor.decoder", 36, true, false, true, Collections.emptySet()),
                false,
                LowLatencyProfilePlanner.PlanPurpose.INITIAL_CONFIGURATION);

        assertEquals(1, profiles.size());
        assertEquals("android-standard", profiles.get(0).name);
        assertEquals(singleOption(MediaFormat.KEY_LOW_LATENCY, 1), profiles.get(0).integerOptions);
    }

    @Test
    public void runtimeRecoveryReturnsOnlyStandardWhenSupported() {
        List<LowLatencyProfilePlanner.Profile> profiles = LowLatencyProfilePlanner.plan(
                capabilities("c2.qti.avc.decoder", 36, true, true, true, S25_VENDOR_PARAMETERS),
                true,
                LowLatencyProfilePlanner.PlanPurpose.RUNTIME_RECOVERY);

        assertEquals(1, profiles.size());
        assertEquals("android-standard", profiles.get(0).name);
        assertEquals(singleOption(MediaFormat.KEY_LOW_LATENCY, 1), profiles.get(0).integerOptions);
    }

    @Test
    public void capabilitiesNormalizeAndDefensivelyCopyVendorParameters() {
        Set<String> parameters = parameters("Vendor.QTI-Ext-Dec-Low-Latency.Enable");
        LowLatencyProfilePlanner.Capabilities capabilities = capabilities(
                "c2.qti.avc.decoder", 36, true, true, true, parameters);
        parameters.add("vendor.added.after.construction");

        assertEquals(parameters(LowLatencyProfilePlanner.QTI_CORE),
                capabilities.supportedVendorParameters);
        try {
            capabilities.supportedVendorParameters.add("mutation");
            fail("Capabilities must be immutable");
        }
        catch (UnsupportedOperationException expected) {
            // Expected.
        }
    }

    @Test
    public void qtiS25UsesOnlyAdvertisedOutputFenceFamily() {
        List<LowLatencyProfilePlanner.Profile> profiles = LowLatencyProfilePlanner.plan(
                capabilities("c2.qti.hevc.decoder.low_latency", 36, true, true, true,
                        S25_VENDOR_PARAMETERS),
                true,
                LowLatencyProfilePlanner.PlanPurpose.INITIAL_CONFIGURATION);

        assertEquals(Arrays.asList(
                        "qti-experimental-full",
                        "qti-experimental-no-scheduler",
                        "qti-no-fence",
                        "android-standard"),
                profileNames(profiles));

        Map<String, Integer> full = profiles.get(0).integerOptions;
        assertFalse(full.containsKey(LowLatencyProfilePlanner.VDEC_LOW_LATENCY));
        assertFalse(full.containsKey(LowLatencyProfilePlanner.QTI_CORE));
        assertFalse(full.containsKey(LowLatencyProfilePlanner.QTI_SOFTWARE_FENCE));
        assertEquals(1, full.get(LowLatencyProfilePlanner.QTI_OUTPUT_FENCE_ENABLE).intValue());
        assertEquals(1, full.get(LowLatencyProfilePlanner.QTI_OUTPUT_FENCE_TYPE).intValue());
        assertEquals(Arrays.asList(
                        MediaFormat.KEY_LOW_LATENCY,
                        LowLatencyProfilePlanner.QTI_PICTURE_ORDER,
                        LowLatencyProfilePlanner.QTI_OUTPUT_FENCE_ENABLE,
                        LowLatencyProfilePlanner.QTI_OUTPUT_FENCE_TYPE,
                        MediaFormat.KEY_OPERATING_RATE),
                new ArrayList<>(full.keySet()));
    }

    @Test
    public void qtiAdvertisedCoreIncludesTrueCoreOnlyFallback() {
        List<LowLatencyProfilePlanner.Profile> profiles = LowLatencyProfilePlanner.plan(
                capabilities("c2.qti.avc.decoder", 36, true, true, true,
                        parameters(LowLatencyProfilePlanner.QTI_CORE,
                                LowLatencyProfilePlanner.QTI_PICTURE_ORDER)),
                false,
                LowLatencyProfilePlanner.PlanPurpose.INITIAL_CONFIGURATION);

        assertEquals(Arrays.asList("android-standard", "qti-core-picture", "qti-core-only"),
                profileNames(profiles));
        assertEquals(singleOption(LowLatencyProfilePlanner.QTI_CORE, 1),
                profiles.get(2).integerOptions);
    }

    @Test
    public void qtiNeverMixesSoftwareAndOutputFenceFamilies() {
        Set<String> advertised = parameters(
                LowLatencyProfilePlanner.QTI_CORE,
                LowLatencyProfilePlanner.QTI_PICTURE_ORDER,
                LowLatencyProfilePlanner.QTI_OUTPUT_FENCE_ENABLE,
                LowLatencyProfilePlanner.QTI_OUTPUT_FENCE_TYPE,
                LowLatencyProfilePlanner.QTI_SOFTWARE_FENCE);

        List<LowLatencyProfilePlanner.Profile> profiles = LowLatencyProfilePlanner.plan(
                capabilities("c2.qti.avc.decoder", 36, true, true, true, advertised),
                true,
                LowLatencyProfilePlanner.PlanPurpose.INITIAL_CONFIGURATION);

        assertEquals("qti-experimental-full", profiles.get(0).name);
        assertEquals("qti-experimental-alt-fence", profiles.get(1).name);
        assertTrue(profiles.get(0).integerOptions.containsKey(
                LowLatencyProfilePlanner.QTI_OUTPUT_FENCE_ENABLE));
        assertFalse(profiles.get(0).integerOptions.containsKey(
                LowLatencyProfilePlanner.QTI_SOFTWARE_FENCE));
        assertTrue(profiles.get(1).integerOptions.containsKey(
                LowLatencyProfilePlanner.QTI_SOFTWARE_FENCE));
        assertFalse(profiles.get(1).integerOptions.containsKey(
                LowLatencyProfilePlanner.QTI_OUTPUT_FENCE_ENABLE));

        for (LowLatencyProfilePlanner.Profile profile : profiles) {
            assertFalse(profile.integerOptions.containsKey(
                            LowLatencyProfilePlanner.QTI_SOFTWARE_FENCE) &&
                    profile.integerOptions.containsKey(
                            LowLatencyProfilePlanner.QTI_OUTPUT_FENCE_ENABLE));
        }
    }

    @Test
    public void qtiPartialOutputFencePairIsNotUsed() {
        for (String partialKey : Arrays.asList(
                LowLatencyProfilePlanner.QTI_OUTPUT_FENCE_ENABLE,
                LowLatencyProfilePlanner.QTI_OUTPUT_FENCE_TYPE)) {
            List<LowLatencyProfilePlanner.Profile> profiles = LowLatencyProfilePlanner.plan(
                    capabilities("c2.qti.avc.decoder", 36, true, true, true,
                            parameters(partialKey)),
                    true,
                    LowLatencyProfilePlanner.PlanPurpose.INITIAL_CONFIGURATION);

            for (LowLatencyProfilePlanner.Profile profile : profiles) {
                assertFalse(profile.integerOptions.containsKey(
                        LowLatencyProfilePlanner.QTI_OUTPUT_FENCE_ENABLE));
                assertFalse(profile.integerOptions.containsKey(
                        LowLatencyProfilePlanner.QTI_OUTPUT_FENCE_TYPE));
            }
        }
    }

    @Test
    public void qtiPartialOutputFenceFallsBackToAdvertisedSoftwareFence() {
        List<LowLatencyProfilePlanner.Profile> profiles = LowLatencyProfilePlanner.plan(
                capabilities("c2.qti.avc.decoder", 36, true, true, true,
                        parameters(LowLatencyProfilePlanner.QTI_OUTPUT_FENCE_ENABLE,
                                LowLatencyProfilePlanner.QTI_SOFTWARE_FENCE)),
                true,
                LowLatencyProfilePlanner.PlanPurpose.INITIAL_CONFIGURATION);

        assertTrue(profiles.get(0).integerOptions.containsKey(
                LowLatencyProfilePlanner.QTI_SOFTWARE_FENCE));
        assertFalse(profiles.get(0).integerOptions.containsKey(
                LowLatencyProfilePlanner.QTI_OUTPUT_FENCE_ENABLE));
        assertFalse(profiles.get(0).integerOptions.containsKey(
                LowLatencyProfilePlanner.QTI_OUTPUT_FENCE_TYPE));
    }

    @Test
    public void qtiApi31EmptyOrFailedEnumerationUsesNoVendorKeys() {
        for (boolean enumerationAvailable : Arrays.asList(true, false)) {
            List<LowLatencyProfilePlanner.Profile> profiles = LowLatencyProfilePlanner.plan(
                    capabilities("c2.qti.avc.decoder", 31, true, true,
                            enumerationAvailable, Collections.emptySet()),
                    true,
                    LowLatencyProfilePlanner.PlanPurpose.INITIAL_CONFIGURATION);

            for (LowLatencyProfilePlanner.Profile profile : profiles) {
                for (String key : profile.integerOptions.keySet()) {
                    assertFalse(key.startsWith("vendor.qti-"));
                }
            }
        }
    }

    @Test
    public void qtiLegacyApi26To30UsesCoreAndPictureButNeverFences() {
        for (int sdkInt : Arrays.asList(26, 30)) {
            List<LowLatencyProfilePlanner.Profile> profiles = LowLatencyProfilePlanner.plan(
                    capabilities("c2.qti.avc.decoder", sdkInt, false, false,
                            false, Collections.emptySet()),
                    false,
                    LowLatencyProfilePlanner.PlanPurpose.INITIAL_CONFIGURATION);

            assertEquals(Arrays.asList("qti-core-picture", "qti-core-only"),
                    profileNames(profiles));
            assertTrue(profiles.get(0).integerOptions.containsKey(
                    LowLatencyProfilePlanner.QTI_CORE));
            assertTrue(profiles.get(0).integerOptions.containsKey(
                    LowLatencyProfilePlanner.QTI_PICTURE_ORDER));
            assertNoFenceKeys(profiles);
        }
    }

    @Test
    public void qtiPictureOrderMatchesOmxAndCodec2Contracts() {
        assertEquals(0, pictureOrderFor("OMX.qcom.video.decoder.avc"));
        assertEquals(1, pictureOrderFor("c2.qti.avc.decoder"));
        assertEquals(1, pictureOrderFor("c2.qcom.avc.decoder"));
    }

    @Test
    public void qtiNeverUsesVdecLowLatency() {
        List<LowLatencyProfilePlanner.Capabilities> fixtures = Arrays.asList(
                capabilities("OMX.qcom.video.decoder.avc", 26, false, true,
                        false, Collections.emptySet()),
                capabilities("c2.qti.avc.decoder", 36, true, true, true,
                        parameters(LowLatencyProfilePlanner.QTI_CORE,
                                LowLatencyProfilePlanner.QTI_PICTURE_ORDER,
                                LowLatencyProfilePlanner.QTI_SOFTWARE_FENCE)));

        for (LowLatencyProfilePlanner.Capabilities fixture : fixtures) {
            for (boolean ultraLowLatency : Arrays.asList(false, true)) {
                List<LowLatencyProfilePlanner.Profile> profiles = LowLatencyProfilePlanner.plan(
                        fixture,
                        ultraLowLatency,
                        LowLatencyProfilePlanner.PlanPurpose.INITIAL_CONFIGURATION);
                for (LowLatencyProfilePlanner.Profile profile : profiles) {
                    assertFalse(profile.integerOptions.containsKey(
                            LowLatencyProfilePlanner.VDEC_LOW_LATENCY));
                }
            }
        }
    }

    @Test
    public void qtiSchedulerUsesOperatingRateOrPriorityButNeverBoth() {
        List<LowLatencyProfilePlanner.Profile> operatingRateProfiles =
                LowLatencyProfilePlanner.plan(
                        capabilities("c2.qti.avc.decoder", 36, true, true, true,
                                S25_VENDOR_PARAMETERS),
                        true,
                        LowLatencyProfilePlanner.PlanPurpose.INITIAL_CONFIGURATION);
        Map<String, Integer> operatingRate = operatingRateProfiles.get(0).integerOptions;
        assertEquals((int) Short.MAX_VALUE,
                operatingRate.get(MediaFormat.KEY_OPERATING_RATE).intValue());
        assertFalse(operatingRate.containsKey(MediaFormat.KEY_PRIORITY));

        List<LowLatencyProfilePlanner.Profile> priorityProfiles =
                LowLatencyProfilePlanner.plan(
                        capabilities("c2.qti.avc.decoder", 36, true, false, true,
                                S25_VENDOR_PARAMETERS),
                        true,
                        LowLatencyProfilePlanner.PlanPurpose.INITIAL_CONFIGURATION);
        Map<String, Integer> priority = priorityProfiles.get(0).integerOptions;
        assertEquals(0, priority.get(MediaFormat.KEY_PRIORITY).intValue());
        assertFalse(priority.containsKey(MediaFormat.KEY_OPERATING_RATE));
    }

    @Test
    public void plannerNeverReturnsEmptyBaseProfile() {
        List<LowLatencyProfilePlanner.Profile> profiles = LowLatencyProfilePlanner.plan(
                capabilities("c2.qti.avc.decoder", 36, false, false,
                        true, Collections.emptySet()),
                false,
                LowLatencyProfilePlanner.PlanPurpose.INITIAL_CONFIGURATION);

        assertTrue(profiles.isEmpty());
    }

    private static LowLatencyProfilePlanner.Capabilities capabilities(
            String decoderName,
            int sdkInt,
            boolean androidLowLatencySupported,
            boolean maxOperatingRateSupported,
            boolean vendorParameterEnumerationAvailable,
            Set<String> supportedVendorParameters) {
        return new LowLatencyProfilePlanner.Capabilities(
                LowLatencyProfilePlanner.DecoderFamily.QUALCOMM,
                decoderName,
                sdkInt,
                androidLowLatencySupported,
                maxOperatingRateSupported,
                false,
                vendorParameterEnumerationAvailable,
                supportedVendorParameters);
    }

    private static int pictureOrderFor(String decoderName) {
        List<LowLatencyProfilePlanner.Profile> profiles = LowLatencyProfilePlanner.plan(
                capabilities(decoderName, 30, false, false,
                        false, Collections.emptySet()),
                false,
                LowLatencyProfilePlanner.PlanPurpose.INITIAL_CONFIGURATION);
        return profiles.get(0).integerOptions.get(
                LowLatencyProfilePlanner.QTI_PICTURE_ORDER);
    }

    private static void assertNoFenceKeys(List<LowLatencyProfilePlanner.Profile> profiles) {
        for (LowLatencyProfilePlanner.Profile profile : profiles) {
            assertFalse(profile.integerOptions.containsKey(
                    LowLatencyProfilePlanner.QTI_SOFTWARE_FENCE));
            assertFalse(profile.integerOptions.containsKey(
                    LowLatencyProfilePlanner.QTI_OUTPUT_FENCE_ENABLE));
            assertFalse(profile.integerOptions.containsKey(
                    LowLatencyProfilePlanner.QTI_OUTPUT_FENCE_TYPE));
        }
    }

    private static List<String> profileNames(List<LowLatencyProfilePlanner.Profile> profiles) {
        List<String> names = new ArrayList<>();
        for (LowLatencyProfilePlanner.Profile profile : profiles) {
            names.add(profile.name);
        }
        return names;
    }

    private static Set<String> parameters(String... values) {
        return new LinkedHashSet<>(Arrays.asList(values));
    }

    private static Map<String, Integer> singleOption(String key, int value) {
        LinkedHashMap<String, Integer> options = new LinkedHashMap<>();
        options.put(key, value);
        return options;
    }

    private static LowLatencyProfilePlanner.Profile profile(
            String name,
            String firstKey,
            int firstValue,
            String secondKey,
            int secondValue) {
        LinkedHashMap<String, Integer> options = new LinkedHashMap<>();
        options.put(firstKey, firstValue);
        options.put(secondKey, secondValue);
        return new LowLatencyProfilePlanner.Profile(name, options);
    }
}
