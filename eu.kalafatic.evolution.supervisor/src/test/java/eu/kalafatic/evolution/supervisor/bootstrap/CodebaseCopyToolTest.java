package eu.kalafatic.evolution.supervisor.bootstrap;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.Assert.*;

public class CodebaseCopyToolTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void testCopy() throws IOException {
        File source = folder.newFolder("source");
        File sub = new File(source, "sub");
        sub.mkdirs();
        Files.write(new File(source, "file1.txt").toPath(), "content1".getBytes());
        Files.write(new File(sub, "file2.txt").toPath(), "content2".getBytes());

        File target = new File(folder.getRoot(), "target");

        CodebaseCopyTool tool = new CodebaseCopyTool();
        CopyConfiguration config = new CopyConfiguration(source, target);
        config.addExclusion("target");

        CopyResult result = tool.copy(config);

        assertTrue(result.isSuccess());
        assertEquals(2, result.getFilesCopied());
        assertTrue(new File(target, "file1.txt").exists());
        assertTrue(new File(target, "sub/file2.txt").exists());
    }

    @Test
    public void testExclusion() throws IOException {
        File source = folder.newFolder("source2");
        File targetDir = new File(source, "target");
        targetDir.mkdirs();
        Files.write(new File(source, "file1.txt").toPath(), "content1".getBytes());
        Files.write(new File(targetDir, "excluded.txt").toPath(), "excluded".getBytes());

        File target = new File(folder.getRoot(), "target2");

        CodebaseCopyTool tool = new CodebaseCopyTool();
        CopyConfiguration config = new CopyConfiguration(source, target);
        config.addExclusion("target");

        CopyResult result = tool.copy(config);

        assertTrue(result.isSuccess());
        assertEquals(1, result.getFilesCopied());
        assertTrue(new File(target, "file1.txt").exists());
        assertFalse(new File(target, "target").exists());
    }
}
