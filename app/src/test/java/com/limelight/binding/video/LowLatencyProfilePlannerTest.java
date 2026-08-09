package com.limelight.binding.video;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

public class LowLatencyProfilePlannerTest {
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
