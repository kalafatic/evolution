import eu.kalafatic.evolution.selfdev.genome.util.DashboardTemplate;
import eu.kalafatic.evolution.selfdev.genome.util.SimpleMarkdownConverter;
import java.nio.file.Files;
import java.io.File;

public class SampleDashboardGenerator {
    public static void main(String[] args) throws Exception {
        String archMd = "# Arch\n- Component A: Description A\n- Component B: Description B";
        String ucMd = "# Use Cases\n## UC1\nPurpose: Test purpose";
        String milestoneMd = "# Milestone\n## Stable\n- File 1";
        String genomeJson = "{\"identity\": {\"name\": \"Test\", \"version\": \"v1\"}}";

        String archHtml = SimpleMarkdownConverter.toHtml(archMd);
        String ucHtml = SimpleMarkdownConverter.toHtml(ucMd);
        String milestoneHtml = SimpleMarkdownConverter.toHtml(milestoneMd);

        String dashboardHtml = DashboardTemplate.getHtml("TestProject", "130626", archHtml, ucHtml, milestoneHtml, genomeJson);
        Files.write(new File("sample_dashboard.html").toPath(), dashboardHtml.getBytes());
        System.out.println("Generated sample_dashboard.html");
    }
}
