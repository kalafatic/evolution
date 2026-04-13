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
package eu.kalafatic.utils.model;

import java.util.ArrayList;
import java.util.List;

/**
 * The Class class NetInterface.
 * 
 * @author Petr Kalafatic
 * @project Gemini
 * @version 3.0.0
 */
public class NetInterface {

	/** The mac. */
	private String name, displayName, mac;

	/** The address. */
	private List<String> address;
	
	/** The multicast. */
	private boolean up, virtual, multicast;
	
	/**
	 * Instantiates a new net interface.
	 */
	public NetInterface() {
		address=new ArrayList<String>();
	}
	
	// ---------------------------------------------------------------
	// ---------------------------------------------------------------


	/**
	 * Gets the name.
	 * 
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	
	// ---------------------------------------------------------------

	/**
	 * Sets the name.
	 * 
	 * @param name
	 *            the new name
	 */
	public void setName(String name) {
		this.name = name;
	}
	
	// ---------------------------------------------------------------

	/**
	 * Gets the display name.
	 * 
	 * @return the display name
	 */
	public String getDisplayName() {
		return displayName;
	}
	
	// ---------------------------------------------------------------

	/**
	 * Sets the display name.
	 * 
	 * @param displayName
	 *            the new display name
	 */
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	// ---------------------------------------------------------------
	
	/**
	 * Gets the mac.
	 * 
	 * @return the mac
	 */
	public String getMac() {
		return mac;
	}
	
	// ---------------------------------------------------------------

	/**
	 * Sets the mac.
	 * 
	 * @param mac
	 *            the new mac
	 */
	public void setMac(String mac) {
		this.mac = mac;
	}

	// ---------------------------------------------------------------

	/**
	 * Checks if is up.
	 * 
	 * @return true, if is up
	 */
	public boolean isUp() {
		return up;
	}
	
	// ---------------------------------------------------------------

	/**
	 * Sets the up.
	 * 
	 * @param up
	 *            the new up
	 */
	public void setUp(boolean up) {
		this.up = up;
	}

	// ---------------------------------------------------------------	
	
	/**
	 * Checks if is virtual.
	 * 
	 * @return true, if is virtual
	 */
	public boolean isVirtual() {
		return virtual;
	}
	
	// ---------------------------------------------------------------

	/**
	 * Sets the virtual.
	 * 
	 * @param virtual
	 *            the new virtual
	 */
	public void setVirtual(boolean virtual) {
		this.virtual = virtual;
	}

	// ---------------------------------------------------------------
	
	/**
	 * Checks if is multicast.
	 * 
	 * @return true, if is multicast
	 */
	public boolean isMulticast() {
		return multicast;
	}
	
	// ---------------------------------------------------------------

	/**
	 * Sets the multicast.
	 * 
	 * @param multicast
	 *            the new multicast
	 */
	public void setMulticast(boolean multicast) {
		this.multicast = multicast;
	}

	// ---------------------------------------------------------------

	/**
	 * Gets the address.
	 * 
	 * @return the address
	 */
	public List<String> getAddress() {
		return address;
	}

	// ---------------------------------------------------------------

	/**
	 * Sets the address.
	 * 
	 * @param address
	 *            the new address
	 */
	public void setAddress(List<String> address) {
		this.address = address;
	}
}
