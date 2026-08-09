package com.limelight.binding.video;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class MediaCodecLowLatencyCapabilityTest {
    @Test
    public void advertisedParametersAreNormalizedImmutableCachedAndReleased() {
        AtomicInteger openCount = new AtomicInteger();
        AtomicInteger releaseCount = new AtomicInteger();
        MediaCodecHelper.VendorParameterCapabilityStore store =
                new MediaCodecHelper.VendorParameterCapabilityStore(decoderName -> {
                    openCount.incrementAndGet();
                    return session(Arrays.asList(
                            "Vendor.QTI-Ext-Output-Fence.Enable",
                            "vendor.qti-ext-dec-picture-order.enable"), releaseCount);
                });

        MediaCodecHelper.VendorParameterSnapshot first =
                store.getSnapshot("C2.QTI.AVC.Decoder", 31);
        MediaCodecHelper.VendorParameterSnapshot cached =
                store.getSnapshot("c2.qti.avc.decoder", 36);

        assertTrue(first.enumerationAvailable);
        assertEquals(Arrays.asList(
                        "vendor.qti-ext-dec-picture-order.enable",
                        "vendor.qti-ext-output-fence.enable"),
                Arrays.asList(first.supportedParameters.toArray(new String[0])));
        assertEquals(first.supportedParameters, cached.supportedParameters);
        assertEquals(1, openCount.get());
        assertEquals(1, releaseCount.get());

        try {
            first.supportedParameters.add("mutation");
            fail("Vendor parameter snapshot must be immutable");
        }
        catch (UnsupportedOperationException expected) {
            // Expected.
        }
    }

    @Test
    public void successfulEmptyEnumerationIsAvailableAndCached() {
        AtomicInteger openCount = new AtomicInteger();
        AtomicInteger releaseCount = new AtomicInteger();
        MediaCodecHelper.VendorParameterCapabilityStore store =
                new MediaCodecHelper.VendorParameterCapabilityStore(decoderName -> {
                    openCount.incrementAndGet();
                    return session(Collections.emptyList(), releaseCount);
                });

        MediaCodecHelper.VendorParameterSnapshot first =
                store.getSnapshot("c2.qti.hevc.decoder", 31);
        MediaCodecHelper.VendorParameterSnapshot second =
                store.getSnapshot("C2.QTI.HEVC.DECODER", 31);

        assertTrue(first.enumerationAvailable);
        assertTrue(first.supportedParameters.isEmpty());
        assertTrue(second.enumerationAvailable);
        assertEquals(1, openCount.get());
        assertEquals(1, releaseCount.get());
    }

    @Test
    public void failedEnumerationIsUnavailableReleasedAndNotCached() {
        AtomicInteger openCount = new AtomicInteger();
        AtomicInteger releaseCount = new AtomicInteger();
        MediaCodecHelper.VendorParameterCapabilityStore store =
                new MediaCodecHelper.VendorParameterCapabilityStore(decoderName -> {
                    openCount.incrementAndGet();
                    return new MediaCodecHelper.VendorParameterSession() {
                        @Override
                        public List<String> getSupportedVendorParameters() throws Exception {
                            throw new Exception("codec query failed");
                        }

                        @Override
                        public void release() {
                            releaseCount.incrementAndGet();
                        }
                    };
                });

        MediaCodecHelper.VendorParameterSnapshot first =
                store.getSnapshot("c2.qti.av1.decoder", 31);
        MediaCodecHelper.VendorParameterSnapshot retried =
                store.getSnapshot("C2.QTI.AV1.DECODER", 31);

        assertFalse(first.enumerationAvailable);
        assertTrue(first.supportedParameters.isEmpty());
        assertFalse(retried.enumerationAvailable);
        assertEquals(2, openCount.get());
        assertEquals(2, releaseCount.get());
    }

    @Test
    public void api26And30NeverOpenProvider() {
        AtomicInteger openCount = new AtomicInteger();
        MediaCodecHelper.VendorParameterCapabilityStore store =
                new MediaCodecHelper.VendorParameterCapabilityStore(decoderName -> {
                    openCount.incrementAndGet();
                    throw new AssertionError("Provider must not be opened before API 31");
                });

        for (int sdkInt : Arrays.asList(26, 30)) {
            MediaCodecHelper.VendorParameterSnapshot snapshot =
                    store.getSnapshot("c2.qti.avc.decoder", sdkInt);
            assertFalse(snapshot.enumerationAvailable);
            assertTrue(snapshot.supportedParameters.isEmpty());
        }
        assertEquals(0, openCount.get());
    }

    private static MediaCodecHelper.VendorParameterSession session(
            List<String> parameters,
            AtomicInteger releaseCount) {
        return new MediaCodecHelper.VendorParameterSession() {
            @Override
            public List<String> getSupportedVendorParameters() {
                return parameters;
            }

            @Override
            public void release() {
                releaseCount.incrementAndGet();
            }
        };
    }
}
