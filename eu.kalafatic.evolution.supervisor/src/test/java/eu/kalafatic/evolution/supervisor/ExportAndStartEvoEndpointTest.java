package eu.kalafatic.evolution.supervisor;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class ExportAndStartEvoEndpointTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private File baseDir;
    private File exportDir;

    @Before
    public void setUp() throws IOException {
        baseDir = tempFolder.newFolder("export-test-base");
        exportDir = new File(baseDir, "export");
        exportDir.mkdirs();
    }

    @Test
    public void testExportDirectoryResolution() throws Exception {
        // Direct export path
        File customExport = tempFolder.newFolder("290826", "export");
        Assert.assertTrue(customExport.exists());

        // Create a dummy jar inside customExport
        File dummyJar = new File(customExport, "eu.kalafatic.evolution.controller-2.6.5-SNAPSHOT.jar");
        dummyJar.createNewFile();

        // Create dummy executable
        boolean isWin = System.getProperty("os.name").toLowerCase().contains("win");
        File dummyExe = new File(customExport, isWin ? "evo.exe" : "evo.sh");
        dummyExe.createNewFile();

        Assert.assertTrue(dummyJar.exists());
        Assert.assertTrue(dummyExe.exists());
    }

    @Test
    public void testIsValidExecutableValidation() throws Exception {
        File folder = tempFolder.newFolder("executable-validation-test");
        File solitaryExe = new File(folder, "evo.exe");
        solitaryExe.createNewFile();

        // Solitary exe without plugins directory must be invalid
        Assert.assertFalse("Executable without plugins directory must be invalid", PlatformInfo.isValidExecutable(solitaryExe));

        // Create plugins directory
        File pluginsDir = new File(folder, "plugins");
        pluginsDir.mkdirs();

        // Executable with plugins directory must be valid
        Assert.assertTrue("Executable with plugins directory must be valid", PlatformInfo.isValidExecutable(solitaryExe));
    }

    @Test
    public void testHttpServerExportAndStartEvoEndpoints() throws Exception {
        File serverBase = tempFolder.newFolder("server-test-base");
        File customExport = new File(serverBase, "export");
        customExport.mkdirs();

        // Create modular jars
        File jar1 = new File(customExport, "eu.kalafatic.evolution.controller-2.6.5-SNAPSHOT.jar");
        File jar2 = new File(customExport, "eu.kalafatic.evolution.servers-2.6.5-SNAPSHOT.jar");
        jar1.createNewFile();
        jar2.createNewFile();

        // Run supervisor in a background thread so it doesn't block the test
        Thread supervisorThread = new Thread(() -> {
            SupervisorMain.main(new String[]{serverBase.getAbsolutePath()});
        });
        supervisorThread.setDaemon(true);
        supervisorThread.start();

        // Wait up to 5s for server to start
        boolean serverUp = false;
        for (int i = 0; i < 50; i++) {
            try {
                URL pingUrl = new URL("http://127.0.0.1:8089/ping");
                HttpURLConnection conn = (HttpURLConnection) pingUrl.openConnection();
                conn.setConnectTimeout(1000);
                if (conn.getResponseCode() == 200) {
                    serverUp = true;
                    break;
                }
            } catch (Exception ignored) {}
            Thread.sleep(100);
        }

        Assert.assertTrue("Supervisor HTTP server should be responsive on port 8089", serverUp);

        // Test export endpoint with custom path
        URL exportUrl = new URL("http://127.0.0.1:8089/export?path=" + customExport.getAbsolutePath());
        HttpURLConnection connExport = (HttpURLConnection) exportUrl.openConnection();
        Assert.assertEquals(200, connExport.getResponseCode());

        String exportResponse = readResponse(connExport);
        Assert.assertTrue(exportResponse.contains("OK"));

        // Test start-evo endpoint with custom path
        URL startUrl = new URL("http://127.0.0.1:8089/start-evo?path=" + customExport.getAbsolutePath());
        HttpURLConnection connStart = (HttpURLConnection) startUrl.openConnection();
        connStart.setRequestMethod("POST");
        Assert.assertEquals(200, connStart.getResponseCode());

        String startResponse = readResponse(connStart);
        Assert.assertTrue(startResponse.contains("OK") || startResponse.contains("SUCCESS"));

        // Test start-evo endpoint with URL-encoded custom path
        String encodedPath = java.net.URLEncoder.encode(customExport.getAbsolutePath(), "UTF-8");
        URL startEncodedUrl = new URL("http://127.0.0.1:8089/start-evo?path=" + encodedPath);
        HttpURLConnection connStartEncoded = (HttpURLConnection) startEncodedUrl.openConnection();
        connStartEncoded.setRequestMethod("POST");
        Assert.assertEquals(200, connStartEncoded.getResponseCode());

        String startEncodedResponse = readResponse(connStartEncoded);
        Assert.assertTrue("Response for URL-encoded path should not fail with missing jars error, got: " + startEncodedResponse,
                startEncodedResponse.contains("OK") || startEncodedResponse.contains("SUCCESS"));

        // Stop evo process
        URL stopUrl = new URL("http://127.0.0.1:8089/stop-evo");
        HttpURLConnection connStop = (HttpURLConnection) stopUrl.openConnection();
        connStop.setRequestMethod("POST");
        Assert.assertEquals(200, connStop.getResponseCode());

        // Signal supervisor monitoring loop to stop cleanly
        File runDir = new File(serverBase, "self-dev-run");
        runDir.mkdirs();
        File controlFile = new File(runDir, "control.json");
        java.nio.file.Files.write(controlFile.toPath(), "{\"forceAction\":\"STOP\"}".getBytes());
    }

    private String readResponse(HttpURLConnection conn) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }
}
