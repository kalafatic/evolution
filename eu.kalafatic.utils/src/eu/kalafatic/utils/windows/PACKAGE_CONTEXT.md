# PACKAGE CONTEXT

## Directory: git/evolution/eu.kalafatic.utils/src/eu/kalafatic/utils/windows/

## Domain: general

## Components
* `WinFileAssociationUtils.java`: package eu.kalafatic.utils.windows; public class WinFileAssociationUtils { public static String getAssoc(String ext) { Process process = WinCMDUtils.getInstance().processCommand("cmd.exe", "/c", "assoc", ext); return WinCMDUtils.getInstance().getProcessOutput(process); } public static String getFtype(String ftype) { Process process = WinCMDUtils.getInstance().processCommand("cmd.exe", "/c", "ftype", ftype);
* `WinCMDUtils.java`: package eu.kalafatic.utils.windows; import java.io.BufferedReader; import java.io.IOException; import java.io.InputStreamReader; public class WinCMDUtils { private volatile static WinCMDUtils INSTANCE; public static WinCMDUtils getInstance() { if (INSTANCE == null) { synchronized (WinCMDUtils.class) { INSTANCE = new WinCMDUtils(); } } return INSTANCE; }
* `WinRegistryUtils.java`: package eu.kalafatic.utils.windows; import java.io.File; import java.io.IOException; import java.io.InputStream; import java.io.StringWriter; import java.text.MessageFormat; import java.util.HashMap; import java.util.Map; import eu.kalafatic.utils.constants.FCMDConstants; import eu.kalafatic.utils.model.Association; public class WinRegistryUtils { public static Map<String, Association> associations = new HashMap<String, Association>(); public static String getFromRegistry(String cmd, String token) { try { Process process = Runtime.getRuntime().exec(cmd); StreamReader reader = new StreamReader(process.getInputStream()); reader.start(); process.waitFor();
