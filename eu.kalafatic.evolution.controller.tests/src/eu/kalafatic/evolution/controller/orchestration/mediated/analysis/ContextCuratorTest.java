package eu.kalafatic.evolution.controller.mediation.analysis;

import static org.junit.Assert.*;
import java.util.List;
import org.junit.Test;
import eu.kalafatic.evolution.controller.mediation.model.FileDescriptor;
import eu.kalafatic.evolution.controller.mediation.model.TargetDescriptor;

public class ContextCuratorTest {
    @Test
    public void testCurate() {
        TargetDescriptor target = new TargetDescriptor("/root");
        FileDescriptor f1 = new FileDescriptor("Main.java", "java", 100);
        // The new curate() filter looks for "Executory" or "Annotated"
        f1.getTags().add("Executory");
        target.getFiles().add(f1);

        FileDescriptor f2 = new FileDescriptor("pom.xml", "xml", 200);
        target.getFiles().add(f2);

        ContextCurator curator = new ContextCurator();
        List<String> curated = curator.curate(target);

        // Only tagged architectural/executory files are curated
        assertEquals(1, curated.size());
        assertTrue(curated.contains("Main.java"));
    }
}
