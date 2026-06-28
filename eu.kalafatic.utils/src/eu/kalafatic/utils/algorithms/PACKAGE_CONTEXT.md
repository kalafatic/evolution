# PACKAGE CONTEXT

## Directory: git/evolution-240526-ok/eu.kalafatic.utils/src/eu/kalafatic/utils/algorithms/

## Domain: general

## Components
* `HashUtils.java`: package eu.kalafatic.utils.algorithms; import java.io.FileInputStream; import java.io.IOException; import java.io.UnsupportedEncodingException; import java.nio.ByteBuffer; import java.nio.channels.FileChannel; import java.security.MessageDigest; import java.security.NoSuchAlgorithmException; import java.util.Arrays; import eu.kalafatic.utils.lib.EEncoding; import eu.kalafatic.utils.lib.EHash; import eu.kalafatic.utils.log.Log; import eu.kalafatic.utils.preferences.ECorePreferences; public class HashUtils { private volatile static HashUtils INSTANCE; public static HashUtils getInstance() { if (INSTANCE == null) { synchronized (HashUtils.class) { INSTANCE = new HashUtils(); }
