package eu.kalafatic.evolution.gpu.diag;

import org.junit.Test;
import static org.junit.Assert.*;

public class NvidiaGpuDetectorTest {

    @Test
    public void testDetectGpuNonDestructive() {
        NvidiaGpuDetector detector = new NvidiaGpuDetector();
        NvidiaGpuDetector.GpuDeviceInfo info = detector.detectGpu();
        assertNotNull(info);
        assertNotNull(info.getDeviceName());
        assertNotNull(info.getConfigErrors());
        assertNotNull(info.getRawDiagnostics());
    }
}
