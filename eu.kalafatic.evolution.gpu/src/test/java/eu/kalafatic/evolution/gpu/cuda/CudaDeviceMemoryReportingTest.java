package eu.kalafatic.evolution.gpu.cuda;

import org.junit.Assume;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Regression tests for the 32-bit memory-reporting defect.
 *
 * <p>The legacy CUDA driver API {@code cuMemGetInfo} / {@code cuDeviceTotalMem} returns a
 * 32-bit-truncated total ({@code 0xFFFFFFFF} = 4 GiB) on GPUs with &gt;4 GiB of VRAM (observed
 * on an RTX 5070 Ti, 16 GiB, driver 595.99.02). {@link CudaDevice} must therefore report the
 * correct 64-bit total via the {@code _v2} entry points, and must reject nonsensical
 * allocation sizes instead of issuing a bare {@code cuMemAlloc} OOM.</p>
 */
public class CudaDeviceMemoryReportingTest {

    @Test
    public void totalMemoryIsReportedCorrectly() {
        Assume.assumeTrue("CUDA device not available", CudaDevice.isAvailable());

        CudaDevice dev = CudaDevice.tryCreate();
        assertNotNull(dev);

        long total = dev.getTotalMemBytes();
        long[] info = dev.memGetInfo();

        // The physical GPU has 16 GiB. A 32-bit-truncated value would be ~4 GiB (0xFFFFFFFF).
        assertTrue("totalMemBytes must reflect >4 GiB VRAM (got " + total + " bytes)",
                total > 8L * 1024 * 1024 * 1024);
        assertTrue("memGetInfo total must reflect >4 GiB VRAM (got " + info[1] + " bytes)",
                info[1] > 8L * 1024 * 1024 * 1024);
        // The reported total must not be the legacy truncation sentinel.
        assertTrue("total must not be the 32-bit truncation sentinel 0xFFFFFFFF",
                total != 0xFFFFFFFFL);
    }

    @Test
    public void invalidAllocationSizesAreRejected() {
        Assume.assumeTrue("CUDA device not available", CudaDevice.isAvailable());

        CudaDevice dev = CudaDevice.tryCreate();
        assertNotNull(dev);

        try {
            dev.alloc(0);
            fail("alloc(0) must be rejected");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("must be > 0"));
        }

        try {
            dev.alloc(-1024);
            fail("alloc(-1024) must be rejected");
        } catch (IllegalArgumentException expected) {
            // expected
        }

        // A size larger than the device total is guaranteed to fail; the device must fail
        // fast with an actionable diagnostic rather than a bare cuMemAlloc OOM.
        try {
            dev.alloc(dev.getTotalMemBytes() * 2L);
            fail("alloc(2x device total) must be rejected before issuing cuMemAlloc");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("exceeds device total"));
        }
    }

    @Test
    public void repeatedAllocateFreeCyclesDoNotLeak() {
        Assume.assumeTrue("CUDA device not available", CudaDevice.isAvailable());

        CudaDevice dev = CudaDevice.tryCreate();
        long[] before = dev.memGetInfo();
        long allocsBefore = dev.getMemAllocCount();
        long freesBefore = dev.getMemFreeCount();

        final int n = 256;
        for (int i = 0; i < 50; i++) {
            long p = dev.alloc((long) n * 4);
            dev.free(p);
        }

        assertEquals(50, dev.getMemAllocCount() - allocsBefore);
        assertEquals(50, dev.getMemFreeCount() - freesBefore);

        long[] after = dev.memGetInfo();
        long leaked = before[0] - after[0];
        assertTrue("repeated alloc/free leaked device memory (~" + leaked + " bytes)",
                leaked < 64L * 1024 * 1024);
    }
}
