package eu.kalafatic.evolution.gpu.trainer;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.file.Path;

import static org.junit.Assert.*;

public class EvoGpuModelSerializerTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testModelSerializationRoundtrip() throws Exception {
        File modelFile = tempFolder.newFile("test_model.evo");
        Path modelPath = modelFile.toPath();

        EvoGpuModelSerializer serializer = new EvoGpuModelSerializer();
        EvoGpuModelSerializer.ModelContainer container = new EvoGpuModelSerializer.ModelContainer();

        container.setModelName("test_model");
        container.setArchitectureJson("{\"embed_dim\":32}");
        container.setTokenizerJson("{\"vocab_size\":100}");
        container.getTokenToId().put("hello", 1);
        container.getMetadata().put("author", "unit_test");

        float[] data = new float[]{1.0f, 2.5f, -3.14f, 0.0f};
        container.addTensor("test_tensor", new long[]{2, 2}, data);

        serializer.writeModel(modelPath, container);
        assertTrue(modelFile.exists());
        assertTrue(modelFile.length() > 0);

        EvoGpuModelSerializer.ModelContainer readContainer = serializer.readModel(modelPath);
        assertEquals("test_model", readContainer.getModelName());
        assertEquals("{\"embed_dim\":32}", readContainer.getArchitectureJson());
        assertEquals(1, readContainer.getTokenToId().get("hello").intValue());
        assertEquals("unit_test", readContainer.getMetadata().get("author"));
        assertEquals(1, readContainer.getTensorNames().size());
        assertEquals("test_tensor", readContainer.getTensorNames().get(0));

        float[] readData = readContainer.getTensorData().get(0);
        assertArrayEquals(data, readData, 0.0001f);
    }
}
