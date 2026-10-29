package eu.kalafatic.evolution.gpu.diag;

import org.junit.Test;
import static org.junit.Assert.*;

public class CudaComputeVerifierTest {

    @Test
    public void testVerifyCudaComputeNonDestructive() {
        CudaComputeVerifier verifier = new CudaComputeVerifier();
        CudaComputeVerifier.CudaComputeInfo info = verifier.verifyCudaCompute();
        assertNotNull(info);
        assertNotNull(info.getDriverLibraryPath());
        assertNotNull(info.getRuntimeLibraryPath());
        assertNotNull(info.getComputeDeviceType());
    }
}
