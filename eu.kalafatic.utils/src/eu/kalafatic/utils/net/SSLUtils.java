/*******************************************************************************
 * Copyright (c) 2010, Petr Kalafatic (gemini@kalafatic.eu).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU GPL Version 3 
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.txt  
 * 
 * Contributors:
 *     Petr Kalafatic - initial API and implementation
 ******************************************************************************/
package eu.kalafatic.utils.net;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.util.Collections;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;

import eu.kalafatic.utils.constants.FConstants;
import eu.kalafatic.utils.preferences.ECorePreferences;

/**
 * The Class class SSLUtils.
 * @author Petr Kalafatic
 * @project Gemini
 * @version 3.0.0
 */
public class SSLUtils {

	/** The INSTANCE. */
	private volatile static SSLUtils INSTANCE;

	/**
	 * Gets the single instance of SSLUtils.
	 * @return single instance of SSLUtils
	 */
	public static SSLUtils getInstance() {
		if (INSTANCE == null) {
			synchronized (SSLUtils.class) {
				INSTANCE = new SSLUtils();
			}
		}
		return INSTANCE;
	}

	// ---------------------------------------------------------------
	// ---------------------------------------------------------------

	/**
	 * Load keystore.
	 * @return the key store
	 * @throws Exception the exception
	 */
	public KeyStore loadKeystore() throws Exception {

		KeyStore keyStore = null, javaKeyStore = null, jreKeyStore = null;
		char[] geminiKSKey = "traged".toCharArray();
		char[] javaKSKey = "changeit".toCharArray();

		String workspaceCertPath = FConstants.PREFERENCES.get(ECorePreferences.CERT_LOC.getName(), (String) ECorePreferences.CERT_LOC.getDef());

		String appCertPath = FConstants.CERT_LOC.concat(File.separator).concat("gemini.jks");

		String keyStorePath = workspaceCertPath.concat(File.separator).concat("gemini.jks");

		try {
			keyStore = KeyStore.getInstance("JKS");
			keyStore.load(new FileInputStream(appCertPath), geminiKSKey);

			if (FConstants.PREFERENCES.getBoolean(ECorePreferences.CERT_ENABLED.getName(), (Boolean) ECorePreferences.CERT_ENABLED.getDef())) {
				return keyStore;
			}

			File keyStorefile = new File(keyStorePath);

			String fileName = System.getProperty("java.home") + "/lib/security/cacerts";

			File javaKSFile = new File(fileName);
			File jreKSFile = new File("c:/Program Files/Java/jre6/lib/security/cacerts");

			FileInputStream stream = new FileInputStream(javaKSFile);
			javaKeyStore = KeyStore.getInstance("JKS");
			javaKeyStore.load(stream, javaKSKey);

			stream = new FileInputStream(jreKSFile);
			jreKeyStore = KeyStore.getInstance("JKS");
			jreKeyStore.load(stream, javaKSKey);

			Certificate certificate = keyStore.getCertificate("gemini");

			javaKeyStore.setCertificateEntry("gemini", certificate);
			jreKeyStore.setCertificateEntry("gemini", certificate);

			keyStore.store(new FileOutputStream(keyStorefile), geminiKSKey);
			javaKeyStore.store(new FileOutputStream(javaKSFile), javaKSKey);
			jreKeyStore.store(new FileOutputStream(javaKSFile), javaKSKey);

			System.setProperty("javax.net.ssl.keyStore", keyStorePath);
			System.setProperty("javax.net.ssl.keyStoreType", "JKS");
			System.setProperty("javax.net.ssl.keyStorePassword", "traged");

			System.setProperty("javax.net.ssl.trustStore", keyStorePath);
			System.setProperty("javax.net.ssl.trustStorePassword", "traged");

			// setTrustedKS();

			FConstants.PREFERENCES.putBoolean(ECorePreferences.CERT_ENABLED.getName(), true);
			FConstants.PREFERENCES.flush();

			System.setProperty("javax.net.debug", "all");

		} catch (Exception e) {
			e.printStackTrace();
		}
		return keyStore;
	}

	// ---------------------------------------------------------------

	/**
	 * Sets the trusted ks.
	 */
	@SuppressWarnings("unused")
	private void setTrustedKS() {
		Bundle bundle = Platform.getBundle("eu.kalafatic.gemini.core");
		Path path = new Path("certificates/gemini.jks");
		URL url = FileLocator.find(bundle, path, Collections.EMPTY_MAP);

		try {
			URL fileUrl = FileLocator.toFileURL(url);
			String trustStoreFile = fileUrl.getPath();

			System.setProperty("javax.net.ssl.trustStore", trustStoreFile);
			System.setProperty("javax.net.ssl.keyStorePassword", "traged");
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}
