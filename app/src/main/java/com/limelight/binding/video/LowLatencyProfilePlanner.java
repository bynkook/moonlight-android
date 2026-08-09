package com.limelight.binding.video;

import android.media.MediaFormat;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

final class LowLatencyProfilePlanner {
    static final String VDEC_LOW_LATENCY = "vdec-lowlatency";

    static final String QTI_CORE = "vendor.qti-ext-dec-low-latency.enable";
    static final String QTI_PICTURE_ORDER = "vendor.qti-ext-dec-picture-order.enable";
    static final String QTI_SOFTWARE_FENCE =
            "vendor.qti-ext-output-sw-fence-enable.value";
    static final String QTI_OUTPUT_FENCE_ENABLE = "vendor.qti-ext-output-fence.enable";
    static final String QTI_OUTPUT_FENCE_TYPE = "vendor.qti-ext-output-fence.fence_type";

    enum DecoderFamily {
        QUALCOMM,
        MEDIATEK,
        OTHER
    }

    enum PlanPurpose {
        INITIAL_CONFIGURATION,
        RUNTIME_RECOVERY
    }

    static final class Capabilities {
        final DecoderFamily family;
        final String decoderName;
        final int sdkInt;
        final boolean androidLowLatencySupported;
        final boolean maxOperatingRateSupported;
        final boolean legacyVdecLowLatencyAllowed;
        final boolean vendorParameterEnumerationAvailable;
        final Set<String> supportedVendorParameters;

        Capabilities(DecoderFamily family,
                     String decoderName,
                     int sdkInt,
                     boolean androidLowLatencySupported,
                     boolean maxOperatingRateSupported,
                     boolean legacyVdecLowLatencyAllowed,
                     boolean vendorParameterEnumerationAvailable,
                     Set<String> supportedVendorParameters) {
            this.family = Objects.requireNonNull(family);
            this.decoderName = Objects.requireNonNull(decoderName);
            this.sdkInt = sdkInt;
            this.androidLowLatencySupported = androidLowLatencySupported;
            this.maxOperatingRateSupported = maxOperatingRateSupported;
            this.legacyVdecLowLatencyAllowed = legacyVdecLowLatencyAllowed;
            this.vendorParameterEnumerationAvailable = vendorParameterEnumerationAvailable;

            TreeSet<String> normalizedParameters = new TreeSet<>();
            for (String parameter : supportedVendorParameters) {
                if (parameter != null) {
                    normalizedParameters.add(parameter.toLowerCase(Locale.US));
                }
            }
            this.supportedVendorParameters = Collections.unmodifiableSet(
                    new LinkedHashSet<>(normalizedParameters));
        }
    }

    static final class Profile {
        final String name;
        final Map<String, Integer> integerOptions;

        private final List<Map.Entry<String, Integer>> orderedOptions;

        Profile(String name, Map<String, Integer> integerOptions) {
            this.name = Objects.requireNonNull(name);

            LinkedHashMap<String, Integer> optionsCopy = new LinkedHashMap<>(integerOptions);
            this.integerOptions = Collections.unmodifiableMap(optionsCopy);

            List<Map.Entry<String, Integer>> entries = new ArrayList<>(optionsCopy.size());
            for (Map.Entry<String, Integer> entry : optionsCopy.entrySet()) {
                entries.add(new AbstractMap.SimpleImmutableEntry<>(entry));
            }
            this.orderedOptions = Collections.unmodifiableList(entries);
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof Profile)) {
                return false;
            }

            Profile otherProfile = (Profile) other;
            return orderedOptions.equals(otherProfile.orderedOptions);
        }

        @Override
        public int hashCode() {
            return orderedOptions.hashCode();
        }
    }

    static List<Profile> plan(Capabilities capabilities,
                              boolean ultraLowLatency,
                              PlanPurpose purpose) {
        Objects.requireNonNull(capabilities);
        Objects.requireNonNull(purpose);

        if (purpose == PlanPurpose.RUNTIME_RECOVERY) {
            List<Profile> recoveryProfiles = new ArrayList<>();
            addProfile(recoveryProfiles, "android-standard", standardOptions(capabilities));
            return deduplicateProfiles(recoveryProfiles);
        }

        if (capabilities.family == DecoderFamily.QUALCOMM) {
            return planQualcomm(capabilities, ultraLowLatency);
        }

        List<Profile> profiles = new ArrayList<>();
        addProfile(profiles, "android-standard", standardOptions(capabilities));
        return deduplicateProfiles(profiles);
    }

    static List<Profile> deduplicateProfiles(List<Profile> profiles) {
        LinkedHashSet<Profile> uniqueProfiles = new LinkedHashSet<>(profiles);
        return Collections.unmodifiableList(new ArrayList<>(uniqueProfiles));
    }

    private static List<Profile> planQualcomm(Capabilities capabilities,
                                               boolean ultraLowLatency) {
        boolean legacyQualcomm = capabilities.sdkInt >= 26 && capabilities.sdkInt < 31;
        boolean qtiCore = legacyQualcomm || supportsVendorParameter(capabilities, QTI_CORE);
        boolean pictureOrder = legacyQualcomm ||
                supportsVendorParameter(capabilities, QTI_PICTURE_ORDER);

        List<Profile> profiles = new ArrayList<>();
        if (!ultraLowLatency) {
            addProfile(profiles, "android-standard", standardOptions(capabilities));
            addQtiCorePictureProfile(profiles, capabilities, qtiCore, pictureOrder);
            addQtiCoreOnlyProfile(profiles, qtiCore);
            return deduplicateProfiles(profiles);
        }

        boolean outputFence = supportsVendorParameter(capabilities, QTI_OUTPUT_FENCE_ENABLE) &&
                supportsVendorParameter(capabilities, QTI_OUTPUT_FENCE_TYPE);
        boolean softwareFence = supportsVendorParameter(capabilities, QTI_SOFTWARE_FENCE);

        LinkedHashMap<String, Integer> full = qtiBaseOptions(
                capabilities, qtiCore, pictureOrder);
        addPreferredFence(full, outputFence, softwareFence);
        addScheduler(full, capabilities);
        addProfile(profiles, "qti-experimental-full", full);

        if (outputFence && softwareFence) {
            LinkedHashMap<String, Integer> alternateFence = qtiBaseOptions(
                    capabilities, qtiCore, pictureOrder);
            alternateFence.put(QTI_SOFTWARE_FENCE, 1);
            addScheduler(alternateFence, capabilities);
            addProfile(profiles, "qti-experimental-alt-fence", alternateFence);
        }

        LinkedHashMap<String, Integer> noScheduler = qtiBaseOptions(
                capabilities, qtiCore, pictureOrder);
        addPreferredFence(noScheduler, outputFence, softwareFence);
        addProfile(profiles, "qti-experimental-no-scheduler", noScheduler);

        addProfile(profiles, "qti-no-fence",
                qtiBaseOptions(capabilities, qtiCore, pictureOrder));
        addProfile(profiles, "android-standard", standardOptions(capabilities));
        addQtiCorePictureProfile(profiles, capabilities, qtiCore, pictureOrder);
        addQtiCoreOnlyProfile(profiles, qtiCore);

        return deduplicateProfiles(profiles);
    }

    private static LinkedHashMap<String, Integer> qtiBaseOptions(
            Capabilities capabilities,
            boolean qtiCore,
            boolean pictureOrder) {
        LinkedHashMap<String, Integer> options = standardOptions(capabilities);
        if (qtiCore) {
            options.put(QTI_CORE, 1);
        }
        if (pictureOrder) {
            options.put(QTI_PICTURE_ORDER,
                    capabilities.decoderName.toLowerCase(Locale.US).startsWith("omx.qcom") ? 0 : 1);
        }
        return options;
    }

    private static void addQtiCorePictureProfile(List<Profile> profiles,
                                                  Capabilities capabilities,
                                                  boolean qtiCore,
                                                  boolean pictureOrder) {
        if (!qtiCore || !pictureOrder) {
            return;
        }

        LinkedHashMap<String, Integer> options = new LinkedHashMap<>();
        options.put(QTI_CORE, 1);
        options.put(QTI_PICTURE_ORDER,
                capabilities.decoderName.toLowerCase(Locale.US).startsWith("omx.qcom") ? 0 : 1);
        addProfile(profiles, "qti-core-picture", options);
    }

    private static void addQtiCoreOnlyProfile(List<Profile> profiles, boolean qtiCore) {
        if (!qtiCore) {
            return;
        }

        LinkedHashMap<String, Integer> options = new LinkedHashMap<>();
        options.put(QTI_CORE, 1);
        addProfile(profiles, "qti-core-only", options);
    }

    private static LinkedHashMap<String, Integer> standardOptions(Capabilities capabilities) {
        LinkedHashMap<String, Integer> options = new LinkedHashMap<>();
        if (capabilities.androidLowLatencySupported) {
            options.put(MediaFormat.KEY_LOW_LATENCY, 1);
        }
        return options;
    }

    private static void addPreferredFence(LinkedHashMap<String, Integer> options,
                                          boolean outputFence,
                                          boolean softwareFence) {
        if (outputFence) {
            options.put(QTI_OUTPUT_FENCE_ENABLE, 1);
            options.put(QTI_OUTPUT_FENCE_TYPE, 1);
        }
        else if (softwareFence) {
            options.put(QTI_SOFTWARE_FENCE, 1);
        }
    }

    private static void addScheduler(LinkedHashMap<String, Integer> options,
                                     Capabilities capabilities) {
        if (capabilities.maxOperatingRateSupported) {
            options.put(MediaFormat.KEY_OPERATING_RATE, (int) Short.MAX_VALUE);
        }
        else if (capabilities.sdkInt >= 23) {
            options.put(MediaFormat.KEY_PRIORITY, 0);
        }
    }

    private static boolean supportsVendorParameter(Capabilities capabilities, String parameter) {
        return capabilities.sdkInt >= 31 &&
                capabilities.vendorParameterEnumerationAvailable &&
                capabilities.supportedVendorParameters.contains(parameter.toLowerCase(Locale.US));
    }

    private static void addProfile(List<Profile> profiles,
                                   String name,
                                   LinkedHashMap<String, Integer> options) {
        if (!options.isEmpty()) {
            profiles.add(new Profile(name, options));
        }
    }

    private LowLatencyProfilePlanner() {
    }
}
