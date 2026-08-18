package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import eu.kalafatic.evolution.forge.data.api.collector.CollectionContext;
import eu.kalafatic.evolution.forge.data.api.collector.CollectionResult;
import eu.kalafatic.evolution.forge.data.api.collector.CollectorType;
import eu.kalafatic.evolution.forge.data.api.collector.SourceType;
import eu.kalafatic.evolution.forge.data.api.collector.TrainingDataCollector;
import eu.kalafatic.evolution.forge.data.api.collector.TrainingDataItem;
import eu.kalafatic.evolution.forge.data.impl.collector.DataPreparationPipeline;
import eu.kalafatic.evolution.forge.data.impl.collector.DiskDataCollector;
import eu.kalafatic.evolution.forge.data.impl.collector.InternetDataCollector;
import eu.kalafatic.evolution.forge.data.impl.collector.MCPDataCollector;
import eu.kalafatic.evolution.forge.data.impl.collector.PdfDataCollector;
import eu.kalafatic.evolution.forge.data.impl.collector.ProjectDataCollector;
import eu.kalafatic.evolution.forge.data.impl.collector.TrainingDataCollectorManager;

public class TrainingDataCollectorsTest {

    private Path tempProjectDir;
    private Path tempDiskDir;
    private Path javaFile;
    private Path mdFile;
    private Path externalTxtFile;

    @Before
    public void setUp() throws Exception {
        tempProjectDir = Files.createTempDirectory("collector-test-project-");
        tempDiskDir = Files.createTempDirectory("collector-test-disk-");

        // Create sample project files
        javaFile = tempProjectDir.resolve("src/main/java/eu/kalafatic/Bar.java");
        Files.createDirectories(javaFile.getParent());
        Files.writeString(javaFile,
            "package eu.kalafatic;\n" +
            "import java.util.List;\n" +
            "public class Bar {\n" +
            "    public void execute() {}\n" +
            "}");

        mdFile = tempProjectDir.resolve("README.md");
        Files.writeString(mdFile,
            "# System Overview\n\n" +
            "This project provides modular LLM training data collectors.");

        // Create external disk files
        externalTxtFile = tempDiskDir.resolve("guide.txt");
        Files.writeString(externalTxtFile,
            "External Guide Document\n" +
            "Contains technical background info for disk collection.");
    }

    @Test
    public void testProjectDataCollector() {
        ProjectDataCollector collector = new ProjectDataCollector();
        assertEquals(CollectorType.PROJECT, collector.getType());
        assertEquals("Project Data Collector", collector.getName());

        CollectionContext context = new CollectionContext();
        context.setProjectDirectory(tempProjectDir.toFile());

        CollectionResult result = collector.collect(context);
        assertEquals(CollectionResult.Status.SUCCESS, result.getStatus());
        assertEquals(2, result.getItems().size());

        TrainingDataItem itemJava = null;
        TrainingDataItem itemMd = null;
        for (TrainingDataItem item : result.getItems()) {
            assertEquals(SourceType.PROJECT, item.getSourceType());
            if (item.getTitle().equals("Bar.java")) itemJava = item;
            if (item.getTitle().equals("README.md")) itemMd = item;
        }

        assertNotNull(itemJava);
        assertNotNull(itemMd);
        assertEquals("java", itemJava.getMetadata().get("language"));
        assertEquals("eu.kalafatic", itemJava.getMetadata().get("package"));
        assertEquals("Bar", itemJava.getMetadata().get("symbol"));
    }

    @Test
    public void testPdfDataCollector() throws Exception {
        PdfDataCollector collector = new PdfDataCollector();
        assertEquals(CollectorType.PDF, collector.getType());
        assertEquals("PDF Data Collector", collector.getName());

        Path pdfFile = tempProjectDir.resolve("doc.pdf");
        String dummyPdfContent =
            "%PDF-1.4\n" +
            "1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n" +
            "2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n" +
            "3 0 obj\n<< /Type /Page /Parent 2 0 R /Contents 4 0 R >>\nendobj\n" +
            "4 0 obj\n<< /Length 55 >>\nstream\n" +
            "BT\n" +
            "(EVO Architecture PDF Documentation) Tj\n" +
            "ET\n" +
            "endstream\nendobj\n" +
            "trailer\n<< /Root 1 0 R >>\n%%EOF";
        Files.writeString(pdfFile, dummyPdfContent);

        CollectionContext context = new CollectionContext();
        context.setProjectDirectory(tempProjectDir.toFile());

        CollectionResult result = collector.collect(context);
        assertEquals(CollectionResult.Status.SUCCESS, result.getStatus());
        assertEquals(1, result.getItems().size());

        TrainingDataItem item = result.getItems().get(0);
        assertEquals(SourceType.PDF, item.getSourceType());
        assertEquals("doc.pdf", item.getTitle());
        assertTrue(item.getContent().contains("EVO Architecture PDF Documentation"));
    }

    @Test
    public void testDiskDataCollector() {
        DiskDataCollector collector = new DiskDataCollector();
        assertEquals(CollectorType.DISK, collector.getType());

        CollectionContext context = new CollectionContext();
        context.addDiskPath(tempDiskDir.toFile());

        CollectionResult result = collector.collect(context);
        assertEquals(CollectionResult.Status.SUCCESS, result.getStatus());
        assertEquals(1, result.getItems().size());

        TrainingDataItem item = result.getItems().get(0);
        assertEquals(SourceType.DISK, item.getSourceType());
        assertEquals("guide.txt", item.getTitle());
        assertTrue(item.getContent().contains("External Guide Document"));
    }

    @Test
    public void testMCPDataCollector() {
        // Test unavailable MCP gracefully
        MCPDataCollector unavailableCollector = new MCPDataCollector(null);
        CollectionContext context = new CollectionContext();

        CollectionResult unavailResult = unavailableCollector.collect(context);
        assertEquals(CollectionResult.Status.FAILED, unavailResult.getStatus());
        assertTrue(unavailResult.getErrorMessage().contains("unavailable"));

        // Test available mocked MCP
        MCPDataCollector.McpProvider mockProvider = new MCPDataCollector.McpProvider() {
            @Override
            public boolean isAvailable() {
                return true;
            }

            @Override
            public List<TrainingDataItem> fetchMcpData(List<String> toolsOrResources) {
                List<TrainingDataItem> list = new ArrayList<>();
                TrainingDataItem item = new TrainingDataItem(SourceType.MCP, "mcp://docs/arch", "MCP Arch Doc", "Architecture knowledge from MCP");
                item.addMetadata("server", "docs-server");
                item.addMetadata("tool", "doc-search");
                list.add(item);
                return list;
            }
        };

        MCPDataCollector mockCollector = new MCPDataCollector(mockProvider);
        CollectionResult mockResult = mockCollector.collect(context);
        assertEquals(CollectionResult.Status.SUCCESS, mockResult.getStatus());
        assertEquals(1, mockResult.getItems().size());

        TrainingDataItem mcpItem = mockResult.getItems().get(0);
        assertEquals(SourceType.MCP, mcpItem.getSourceType());
        assertEquals("docs-server", mcpItem.getMetadata().get("server"));
    }

    @Test
    public void testInternetDataCollector() {
        // Test fetcher offline / unavailable
        InternetDataCollector offlineCollector = new InternetDataCollector(null);
        CollectionContext context = new CollectionContext();

        CollectionResult offlineResult = offlineCollector.collect(context);
        assertEquals(CollectionResult.Status.SUCCESS, offlineResult.getStatus()); // No topics requested -> empty success

        context.addSearchTopic("transformer architectures");
        CollectionResult unavailResult = offlineCollector.collect(context);
        assertEquals(CollectionResult.Status.FAILED, unavailResult.getStatus());

        // Test active search fetcher
        InternetDataCollector.WebSearchFetcher mockFetcher = query -> {
            List<TrainingDataItem> list = new ArrayList<>();
            TrainingDataItem item = new TrainingDataItem(SourceType.INTERNET, "https://docs.example.com/" + query, query, "Official web content for " + query);
            list.add(item);
            return list;
        };

        InternetDataCollector onlineCollector = new InternetDataCollector(mockFetcher);
        CollectionResult onlineResult = onlineCollector.collect(context);
        assertEquals(CollectionResult.Status.SUCCESS, onlineResult.getStatus());
        assertEquals(1, onlineResult.getItems().size());

        TrainingDataItem webItem = onlineResult.getItems().get(0);
        assertEquals(SourceType.INTERNET, webItem.getSourceType());
        assertEquals("transformer architectures", webItem.getMetadata().get("query"));
    }

    @Test
    public void testTrainingDataCollectorManager() {
        TrainingDataCollectorManager manager = new TrainingDataCollectorManager();

        CollectionContext context = new CollectionContext();
        context.setProjectDirectory(tempProjectDir.toFile());
        context.addDiskPath(tempDiskDir.toFile());

        // Select only Project and Disk sources
        Set<CollectorType> selected = new HashSet<>();
        selected.add(CollectorType.PROJECT);
        selected.add(CollectorType.DISK);

        CollectionResult result = manager.collect(context, selected);
        assertEquals(CollectionResult.Status.SUCCESS, result.getStatus());

        // 2 project items + 1 disk item + 1 collection summary item = 4 total items
        assertEquals(4, result.getItems().size());

        // Verify failure isolation when MCP is selected but unavailable
        selected.add(CollectorType.MCP);
        CollectionResult partialResult = manager.collect(context, selected);
        assertEquals("Failure in MCP must produce PARTIAL status without crashing entire process",
                CollectionResult.Status.PARTIAL, partialResult.getStatus());
        assertFalse(partialResult.getItems().isEmpty());
    }

    @Test
    public void testDataPreparationPipelineAndDeduplication() {
        DataPreparationPipeline pipeline = new DataPreparationPipeline();

        CollectionContext context = new CollectionContext();
        context.setProjectDirectory(tempProjectDir.toFile());
        context.addDiskPath(tempDiskDir.toFile());

        Set<CollectorType> selected = new HashSet<>();
        selected.add(CollectorType.PROJECT);
        selected.add(CollectorType.DISK);

        List<DataPreparationPipeline.PreparedRecord> records = pipeline.runPipeline(context, selected);
        assertNotNull(records);
        assertFalse("Prepared records should be generated", records.isEmpty());

        String jsonl = pipeline.exportToJsonL(records);
        assertTrue("Exported JSONL must contain prompt and response keys", jsonl.contains("\"prompt\":"));
        assertTrue(jsonl.contains("\"response\":"));
    }
}
