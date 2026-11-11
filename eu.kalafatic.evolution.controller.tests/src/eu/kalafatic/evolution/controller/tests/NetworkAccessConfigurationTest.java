package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import eu.kalafatic.evolution.controller.resource.NetworkInterfaceHelper;
import eu.kalafatic.evolution.controller.resource.NetworkMode;
import eu.kalafatic.evolution.controller.resource.PortRegistry;

public class NetworkAccessConfigurationTest {

    private String originalMode;
    private String originalBind;
    private String originalBaseUrl;
    private String originalTrusted;

    @Before
    public void setUp() {
        originalMode = System.getProperty(NetworkInterfaceHelper.PROP_NETWORK_MODE);
        originalBind = System.getProperty(NetworkInterfaceHelper.PROP_BIND_ADDRESS);
        originalBaseUrl = System.getProperty(NetworkInterfaceHelper.PROP_EXTERNAL_BASE_URL);
        originalTrusted = System.getProperty(NetworkInterfaceHelper.PROP_TRUSTED_PROXIES);

        System.clearProperty(NetworkInterfaceHelper.PROP_NETWORK_MODE);
        System.clearProperty(NetworkInterfaceHelper.PROP_BIND_ADDRESS);
        System.clearProperty(NetworkInterfaceHelper.PROP_EXTERNAL_BASE_URL);
        System.clearProperty(NetworkInterfaceHelper.PROP_TRUSTED_PROXIES);
    }

    @After
    public void tearDown() {
        restoreProperty(NetworkInterfaceHelper.PROP_NETWORK_MODE, originalMode);
        restoreProperty(NetworkInterfaceHelper.PROP_BIND_ADDRESS, originalBind);
        restoreProperty(NetworkInterfaceHelper.PROP_EXTERNAL_BASE_URL, originalBaseUrl);
        restoreProperty(NetworkInterfaceHelper.PROP_TRUSTED_PROXIES, originalTrusted);
    }

    private void restoreProperty(String key, String val) {
        if (val != null) {
            System.setProperty(key, val);
        } else {
            System.clearProperty(key);
        }
    }

    @Test
    public void testNetworkModeDefaultsToLoopback() {
        NetworkMode mode = NetworkInterfaceHelper.resolveNetworkMode();
        assertEquals(NetworkMode.LOOPBACK, mode);
        assertEquals("127.0.0.1", mode.getDefaultBindAddress());
    }

    @Test
    public void testNetworkModeFromString() {
        assertEquals(NetworkMode.LOOPBACK, NetworkMode.fromString("loopback"));
        assertEquals(NetworkMode.LOOPBACK, NetworkMode.fromString("localhost"));
        assertEquals(NetworkMode.LAN, NetworkMode.fromString("lan"));
        assertEquals(NetworkMode.ALL_INTERFACES, NetworkMode.fromString("all_interfaces"));
        assertEquals(NetworkMode.ALL_INTERFACES, NetworkMode.fromString("all"));
        assertEquals(NetworkMode.ALL_INTERFACES, NetworkMode.fromString("0.0.0.0"));
        assertEquals(NetworkMode.LOOPBACK, NetworkMode.fromString("unknown_mode"));
        assertEquals(NetworkMode.LOOPBACK, NetworkMode.fromString(null));
    }

    @Test
    public void testResolveBindAddressInLoopbackMode() {
        System.setProperty(NetworkInterfaceHelper.PROP_NETWORK_MODE, "LOOPBACK");
        String bind = NetworkInterfaceHelper.resolveBindAddress("HTTP_PRIMARY", "127.0.0.1");
        assertEquals("127.0.0.1", bind);
    }

    @Test
    public void testResolveBindAddressInAllInterfacesMode() {
        System.setProperty(NetworkInterfaceHelper.PROP_NETWORK_MODE, "ALL_INTERFACES");
        String bind = NetworkInterfaceHelper.resolveBindAddress("HTTP_PRIMARY", "127.0.0.1");
        assertEquals("0.0.0.0", bind);
    }

    @Test
    public void testResolveBindAddressInLanMode() {
        System.setProperty(NetworkInterfaceHelper.PROP_NETWORK_MODE, "LAN");
        String bind = NetworkInterfaceHelper.resolveBindAddress("HTTP_PRIMARY", "127.0.0.1");
        assertNotNull(bind);
        assertFalse("Bind address should not be empty", bind.isEmpty());
        assertFalse("LAN bind address must not be 0.0.0.0", "0.0.0.0".equals(bind));
    }

    @Test
    public void testSupervisorControlIsAlwaysLoopback() {
        System.setProperty(NetworkInterfaceHelper.PROP_NETWORK_MODE, "ALL_INTERFACES");
        String bindControl = NetworkInterfaceHelper.resolveBindAddress("SUPERVISOR_CONTROL", "127.0.0.1");
        assertEquals("127.0.0.1", bindControl);

        System.setProperty(NetworkInterfaceHelper.PROP_NETWORK_MODE, "LAN");
        String bindControlLan = NetworkInterfaceHelper.resolveBindAddress("SUPERVISOR_CONTROL", "127.0.0.1");
        assertEquals("127.0.0.1", bindControlLan);
    }

    @Test
    public void testReverseProxyHeaderResolutionFromTrustedProxy() {
        Map<String, String> headers = new HashMap<>();
        headers.put("x-forwarded-host", "evo.kalafatic.eu");
        headers.put("x-forwarded-proto", "https");
        headers.put("x-forwarded-prefix", "/app");

        // Request from loopback (trusted proxy by default)
        String resolved = NetworkInterfaceHelper.resolveExternalUrl("127.0.0.1", headers, 28080, "/dashboard.html");
        assertEquals("https://evo.kalafatic.eu/app/dashboard.html", resolved);
    }

    @Test
    public void testReverseProxyHeaderIgnoredFromUntrustedClient() {
        Map<String, String> headers = new HashMap<>();
        headers.put("x-forwarded-host", "attacker.com");
        headers.put("x-forwarded-proto", "https");

        // Request from arbitrary remote untrusted IP
        String resolved = NetworkInterfaceHelper.resolveExternalUrl("203.0.113.195", headers, 28080, "/dashboard.html");
        assertFalse("Must not trust forwarded headers from untrusted client", resolved.contains("attacker.com"));
        assertTrue("Must use advertised base URL for untrusted client", resolved.contains(":28080/dashboard.html"));
    }

    @Test
    public void testConfiguredExternalBaseUrlTakesPrecedence() {
        System.setProperty(NetworkInterfaceHelper.PROP_EXTERNAL_BASE_URL, "https://evo.kalafatic.eu");
        Map<String, String> headers = new HashMap<>();
        headers.put("host", "untrusted.host");

        String resolved = NetworkInterfaceHelper.resolveExternalUrl("192.168.0.50", headers, 28080, "/dashboard.html");
        assertEquals("https://evo.kalafatic.eu/dashboard.html", resolved);
    }

    @Test
    public void testCanonicalPortsPreserved() {
        assertEquals(28080, PortRegistry.EVO_HTTP_PRIMARY.getDefaultPort());
        assertEquals(28180, PortRegistry.EVO_HTTP_SECONDARY.getDefaultPort());
        assertEquals(28280, PortRegistry.EVO_MCP.getDefaultPort());
        assertEquals(28380, PortRegistry.EVO_DEVELOP.getDefaultPort());
        assertEquals(28480, PortRegistry.EVO_SUPERVISOR_API.getDefaultPort());
        assertEquals(28580, PortRegistry.EVO_SUPERVISOR_CONTROL.getDefaultPort());
        assertEquals(28680, PortRegistry.EVO_AUXILIARY.getDefaultPort());
    }
}
