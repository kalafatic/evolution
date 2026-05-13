package eu.kalafatic.evolution.controller.orchestration.mediated.analysis;

import static org.junit.Assert.*;
import java.util.List;
import org.junit.Test;
import eu.kalafatic.evolution.controller.orchestration.mediated.model.FileDescriptor;
import eu.kalafatic.evolution.controller.orchestration.mediated.model.TargetDescriptor;

public class ContextCuratorTest {
    @Test
    public void testCurate() {
        TargetDescriptor target = new TargetDescriptor("/root");
        FileDescriptor f1 = new FileDescriptor("Main.java", "java", 100);
        f1.getTags().add("Entry Point");
        target.getFiles().add(f1);

        FileDescriptor f2 = new FileDescriptor("pom.xml", "xml", 200);
        target.getFiles().add(f2);

        ContextCurator curator = new ContextCurator();
        List<String> curated = curator.curate(target);

        assertEquals(2, curated.size());
        assertTrue(curated.contains("Main.java"));
        assertTrue(curated.contains("pom.xml"));
    }
}
