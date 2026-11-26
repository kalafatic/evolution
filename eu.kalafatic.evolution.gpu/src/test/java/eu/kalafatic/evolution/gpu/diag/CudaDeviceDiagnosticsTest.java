package eu.kalafatic.evolution.gpu.diag;

import eu.kalafatic.evolution.gpu.cuda.CudaDevice;
import eu.kalafatic.evolution.gpu.cuda.CudaDevice.Kernel;
import org.junit.Assume;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Exercises the exact diagnostic stage sequence used by the EVO RCP
 * {@code CudaDiagnosticService}: device detection → context → device memory
 * allocation → host-to-device transfer → real kernel execution (NVRTC-compiled
 * saxpy) → device-to-host transfer → numerical verification against an
 * independent CPU reference → cleanup. Also verifies the execution
 * instrumentation counters that the Forge GPU verification uses as CUDA
 * execution evidence.
 *
 * <p>Auto-skipped (Assume) on machines without a usable CUDA device.</p>
 */
public class CudaDeviceDiagnosticsTest {

    private static final int N = 4096;
    private static final String SAXPY = """
            extern "C" __global__ void saxpy(float* out, const float* x, const float* y, int n, float a) {
                int i = blockIdx.x * blockDim.x + threadIdx.x;
                if (i < n) out[i] = a * x[i] + y[i];
            }
            """;

    @Test
    public void testDiagnosticStageSequenceAndNumericalVerification() {
        Assume.assumeTrue("CUDA device not available", CudaDevice.isAvailable());

        CudaDevice dev = CudaDevice.tryCreate();
        assertNotNull(dev);

        long kernelsBefore = dev.getKernelLaunchCount();
        long memcpysBefore = dev.getMemcpyCount();
        long allocsBefore = dev.getMemAllocCount();
        long freesBefore = dev.getMemFreeCount();
        long[] memBefore = dev.memGetInfo();

        // Independent CPU reference (expected values).
        float a = 2.5f;
        float[] x = new float[N];
        float[] y = new float[N];
        float[] expected = new float[N];
        java.util.Random r = new java.util.Random(1234L);
        for (int i = 0; i < N; i++) {
            x[i] = r.nextFloat() * 10.0f - 5.0f;
            y[i] = r.nextFloat() * 10.0f - 5.0f;
            expected[i] = a * x[i] + y[i];
        }

        long dX = dev.alloc((long) N * 4);
        long dY = dev.alloc((long) N * 4);
        long dOut = dev.alloc((long) N * 4);
        dev.memcpyHtoD(dX, x);
        dev.memcpyHtoD(dY, y);

        CudaDevice.Module mod = dev.compile("evo_diag_saxpy", SAXPY);
        Kernel kernel = mod.function("saxpy");
        kernel.launch(N, List.of(dOut, dX, dY, N, a));
        dev.synchronize();

        float[] out = new float[N];
        dev.memcpyDtoH(out, dOut);

        // Numerical verification against the CPU reference.
        float maxAbsDiff = 0.0f;
        for (int i = 0; i < N; i++) {
            maxAbsDiff = Math.max(maxAbsDiff, Math.abs(out[i] - expected[i]));
        }
        assertTrue("GPU result deviates from CPU reference (maxAbsDiff=" + maxAbsDiff + ")",
                maxAbsDiff < 1e-4f);

        // Cleanup: all three allocations released, counters balanced.
        dev.free(dOut);
        dev.free(dY);
        dev.free(dX);

        assertEquals("kernel launches must be counted", 1,
                dev.getKernelLaunchCount() - kernelsBefore);
        assertEquals("two H2D + one D2H copies", 3,
                dev.getMemcpyCount() - memcpysBefore);
        assertEquals("three allocations", 3, dev.getMemAllocCount() - allocsBefore);
        assertEquals("three frees", 3, dev.getMemFreeCount() - freesBefore);

        // The device reports the freed memory back (tolerate driver cache noise).
        long[] memAfter = dev.memGetInfo();
        long leaked = memBefore[0] - memAfter[0];
        assertTrue("device memory not fully released (leaked ~" + leaked + " bytes)",
                leaked < 64L * 1024 * 1024);
    }
}
