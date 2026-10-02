package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import eu.kalafatic.evolution.controller.orchestration.selfdev.net.LinuxNetworkAccessProvider;
import eu.kalafatic.evolution.controller.orchestration.selfdev.net.MacNetworkAccessProvider;
import eu.kalafatic.evolution.controller.orchestration.selfdev.net.NetworkAccessManager;
import eu.kalafatic.evolution.controller.orchestration.selfdev.net.NetworkAccessResult;
import eu.kalafatic.evolution.controller.orchestration.selfdev.net.UnsupportedNetworkAccessProvider;
import eu.kalafatic.evolution.controller.orchestration.selfdev.net.WindowsNetworkAccessProvider;

public class NetworkAccessManagerTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private File mockExe;

    @Before
    public void setUp() throws Exception {
        mockExe = tempFolder.newFile("evo.exe");
    }

    @Test
    public void testNetworkAccessResultBuilder() {
        NetworkAccessResult res = new NetworkAccessResult.Builder()
                .provider("TestProvider")
                .executable(mockExe)
                .ports("8787")
                .scope("LOOPBACK")
                .direction("INBOUND")
                .ruleName("EVO Test Rule")
                .success(true)
                .supported(true)
                .permissionCreated(true)
                .message("Rule created successfully")
                .build();

        assertTrue(res.isSuccess());
        assertTrue(res.isSupported());
        assertTrue(res.isPermissionCreated());
        assertFalse(res.isPermissionAlreadyPresent());
        assertFalse(res.isRequiresElevation());
        assertEquals("TestProvider", res.getProvider());
        assertEquals(mockExe, res.getExecutable());
        assertEquals("8787", res.getPorts());
        assertEquals("LOOPBACK", res.getScope());
        assertEquals("INBOUND", res.getDirection());
        assertEquals("EVO Test Rule", res.getRuleName());
        assertEquals("Rule created successfully", res.getMessage());
    }

    @Test
    public void testLinuxAndMacProviders() {
        LinuxNetworkAccessProvider linuxProvider = new LinuxNetworkAccessProvider();
        assertEquals("LinuxNetworkAccessProvider", linuxProvider.getName());
        NetworkAccessResult linuxRes = linuxProvider.prepareNetworkAccess(mockExe, 48081, null);
        assertTrue(linuxRes.isSuccess());
        assertTrue(linuxRes.isSupported());

        MacNetworkAccessProvider macProvider = new MacNetworkAccessProvider();
        assertEquals("MacNetworkAccessProvider", macProvider.getName());
        NetworkAccessResult macRes = macProvider.prepareNetworkAccess(mockExe, 48081, null);
        assertTrue(macRes.isSuccess());
        assertTrue(macRes.isSupported());

        UnsupportedNetworkAccessProvider unsupportedProvider = new UnsupportedNetworkAccessProvider();
        NetworkAccessResult unsupRes = unsupportedProvider.prepareNetworkAccess(mockExe, 48081, null);
        assertTrue(unsupRes.isSuccess());
        assertFalse(unsupRes.isSupported());
        assertTrue(unsupRes.isSkipped());
    }

    @Test
    public void testWindowsProviderMissingExecutable() {
        WindowsNetworkAccessProvider provider = new WindowsNetworkAccessProvider(command -> new WindowsNetworkAccessProvider.CommandResult(0, "", ""));
        NetworkAccessResult res = provider.prepareNetworkAccess(new File(tempFolder.getRoot(), "non_existent.exe"), 48081, null);
        assertFalse(res.isSuccess());
        assertEquals("Executable file does not exist", res.getMessage());
    }

    @Test
    public void testWindowsProviderRuleAlreadyExists() {
        // Mock command runner returning exitCode=0 and rule output for checkRuleExists
        List<String[]> executedCommands = new ArrayList<>();
        WindowsNetworkAccessProvider.CommandRunner runner = command -> {
            executedCommands.add(command);
            return new WindowsNetworkAccessProvider.CommandResult(0, "EVO RCP Self-Dev (123456)", "");
        };

        WindowsNetworkAccessProvider provider = new WindowsNetworkAccessProvider(runner);
        NetworkAccessResult res = provider.prepareNetworkAccess(mockExe, 48081, null);

        assertTrue(res.isSuccess());
        assertTrue(res.isPermissionAlreadyPresent());
        assertFalse(res.isPermissionCreated());
        assertNotNull(res.getRuleName());
    }

    @Test
    public void testWindowsProviderRuleCreationSuccess() {
        List<String[]> executedCommands = new ArrayList<>();
        WindowsNetworkAccessProvider.CommandRunner runner = command -> {
            executedCommands.add(command);
            String cmdStr = String.join(" ", command);
            if (cmdStr.contains("Get-NetFirewallRule")) {
                // Return found only after creation
                boolean alreadyCreated = executedCommands.size() > 2;
                return new WindowsNetworkAccessProvider.CommandResult(alreadyCreated ? 0 : 1, alreadyCreated ? "EVO Rule" : "", "");
            }
            if (cmdStr.contains("New-NetFirewallRule")) {
                return new WindowsNetworkAccessProvider.CommandResult(0, "New Rule Created", "");
            }
            return new WindowsNetworkAccessProvider.CommandResult(0, "", "");
        };

        WindowsNetworkAccessProvider provider = new WindowsNetworkAccessProvider(runner);
        NetworkAccessResult res = provider.prepareNetworkAccess(mockExe, 48081, null);

        assertTrue(res.isSuccess());
        assertTrue(res.isPermissionCreated());
        assertFalse(res.isPermissionAlreadyPresent());
    }

    @Test
    public void testWindowsProviderRuleCreationElevationError() {
        WindowsNetworkAccessProvider.CommandRunner runner = command -> {
            String cmdStr = String.join(" ", command);
            if (cmdStr.contains("Get-NetFirewallRule") || cmdStr.contains("show rule")) {
                return new WindowsNetworkAccessProvider.CommandResult(1, "", "No rule");
            }
            return new WindowsNetworkAccessProvider.CommandResult(1, "", "Access is denied. Requested operation requires elevation.");
        };

        WindowsNetworkAccessProvider provider = new WindowsNetworkAccessProvider(runner);
        NetworkAccessResult res = provider.prepareNetworkAccess(mockExe, 48081, null);

        assertFalse(res.isSuccess());
        assertTrue(res.isRequiresElevation());
        assertNotNull(res.getError());
    }

    @Test
    public void testNetworkAccessManagerIntegration() {
        NetworkAccessManager manager = NetworkAccessManager.getInstance();
        manager.setProvider(new LinuxNetworkAccessProvider());

        NetworkAccessResult res = manager.prepareNetworkAccess(mockExe, 48081, null);
        assertTrue(res.isSuccess());
        assertEquals("LinuxNetworkAccessProvider", res.getProvider());

        // Restore default provider detection
        manager.setProvider(null);
    }
}
