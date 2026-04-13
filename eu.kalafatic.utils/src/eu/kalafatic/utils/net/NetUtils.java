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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Matcher;

import eu.kalafatic.utils.constants.ERegex;

/**
 * The Class class NetUtils.
 * @author Petr Kalafatic
 * @project Gemini
 * @version 3.0.0
 */
public class NetUtils {

	// ---------------------------------------------------------------
	// ---------------------------------------------------------------

	public static Set<Object> getGateway() {
		Set<Object> set = new HashSet<Object>();
		try {
			Process result = Runtime.getRuntime().exec("netstat -rn");
			// Process pro = Runtime.getRuntime().exec("cmd.exe /c route print");

			BufferedReader output = new BufferedReader(new InputStreamReader(result.getInputStream()));

			String line;
			while ((line = output.readLine()) != null) {
				line = line.trim();

				StringTokenizer st = new StringTokenizer(line, " ", false);

				try {
					if (st.hasMoreTokens()) {
						String targetRouting = st.nextToken();
						Matcher matcher = ERegex.CIDR_ADDRESS.getPattern().matcher(targetRouting);

						if (matcher.matches()) {
							List<String> data = new ArrayList<String>();

							data.add(targetRouting);
							data.add(st.nextToken());
							data.add(st.nextToken());
							data.add(st.nextToken());
							data.add(st.nextToken());

							set.add(data);

						}
					}
				} catch (Exception e) {

				}
			}
		} catch (Exception e) {
			// adapter = new String();
		}
		return set;
	}

	/**
	 * Checks if is i pv4.
	 * @param host the host
	 * @return true, if is i pv4
	 */
	public static boolean isIPv4(String host) {
		boolean result = false;
		try {
			result = InetAddress.getByName(host) instanceof Inet4Address;
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		return result;
	}

	public static String getLocalHost() {
		try {
			return InetAddress.getLocalHost().toString();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "localhost";
	}

	public static String getLocalIP() {
		try {
			return InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "127.0.0.1";
	}

	// ---------------------------------------------------------------

	/**
	 * Gets the local host.
	 * @param size the size
	 * @param iPv4 the i pv4
	 * @return the local host
	 */
	public static String[] getLocalHost(int size, boolean iPv4) {
		List<String> ip = new ArrayList<String>();
		try {
			InetAddress address = InetAddress.getLocalHost();
			InetAddress[] all = InetAddress.getAllByName(address.getHostName());
			size = (size > all.length) ? all.length : size;

			for (int i = 0; i < size; i++) {
				if (iPv4) {
					if (all[i] instanceof Inet4Address) {
						ip.add(all[i].getHostName());
					}
				} else {
					if (all[i] instanceof Inet6Address) {
						ip.add(all[i].getHostName());
					}
				}
			}

		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		return ip.toArray(new String[ip.size()]);
	}

	// ---------------------------------------------------------------

	/**
	 * Gets the local host ip.
	 * @param size the size
	 * @param iPv4 the i pv4
	 * @return the local host ip
	 */
	public String[][] getLocalHostIP(int size, boolean iPv4) {
		List<String[]> ip = new ArrayList<String[]>();
		try {
			InetAddress address = InetAddress.getLocalHost();
			InetAddress[] all = InetAddress.getAllByName(address.getHostName());
			size = (size > all.length) ? all.length : size;

			String[] hostIP = new String[2];

			for (int i = 0; i < size; i++) {
				if (iPv4) {
					if (all[i] instanceof Inet4Address) {
						hostIP[0] = all[i].getHostName();
						hostIP[1] = all[i].getHostAddress();
						ip.add(hostIP);
					}
				} else {
					if (all[i] instanceof Inet6Address) {
						hostIP[0] = all[i].getHostName();
						hostIP[1] = all[i].getHostAddress();
						ip.add(hostIP);
					}
				}
			}
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		return ip.toArray(new String[ip.size()][2]);
	}

	public static boolean validateHostOrAddress(String text) {
		// TODO Auto-generated method stub
		return false;
	}

	public static String getHost(String text) {
		// TODO Auto-generated method stub
		return null;
	}

}
