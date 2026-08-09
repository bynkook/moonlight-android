package com.limelight.binding.video;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

final class LowLatencyProfilePlanner {
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

    static List<Profile> deduplicateProfiles(List<Profile> profiles) {
        LinkedHashSet<Profile> uniqueProfiles = new LinkedHashSet<>(profiles);
        return Collections.unmodifiableList(new ArrayList<>(uniqueProfiles));
    }

    private LowLatencyProfilePlanner() {
    }
}
