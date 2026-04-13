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

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The Class class SubnetUtils.
 * @author Petr Kalafatic
 * @project Gemini
 * @version 3.0.0
 */
public class SubnetUtils {

	/** The Constant IP_ADDRESS. */
	private static final String IP_ADDRESS = "(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})";
	
	/** The Constant SLASH_FORMAT. */
	private static final String SLASH_FORMAT = IP_ADDRESS + "/(\\d{1,3})";
	
	/** The Constant addressPattern. */
	private static final Pattern addressPattern = Pattern.compile(IP_ADDRESS);
	
	/** The Constant cidrPattern. */
	private static final Pattern cidrPattern = Pattern.compile(SLASH_FORMAT);
	
	/** The Constant NBITS. */
	private static final int NBITS = 32;

	/** The netmask. */
	private int netmask = 0;
	
	/** The address. */
	private int address = 0;
	
	/** The network. */
	private int network = 0;
	
	/** The broadcast. */
	private int broadcast = 0;

	/** The inclusive host count. */
	private boolean inclusiveHostCount = false;

	/**
	 * Instantiates a new subnet utils.
	 * @param cidrNotation the cidr notation
	 */
	public SubnetUtils(String cidrNotation) {
		calculate(cidrNotation);
	}

	/**
	 * Instantiates a new subnet utils.
	 * @param address the address
	 * @param mask the mask
	 */
	public SubnetUtils(String address, String mask) {
		calculate(toCidrNotation(address, mask));
	}

	/**
	 * Checks if is inclusive host count.
	 * @return true, if is inclusive host count
	 */
	public boolean isInclusiveHostCount() {
		return inclusiveHostCount;
	}

	/**
	 * Sets the inclusive host count.
	 * @param inclusiveHostCount the new inclusive host count
	 */
	public void setInclusiveHostCount(boolean inclusiveHostCount) {
		this.inclusiveHostCount = inclusiveHostCount;
	}

	/**
	 * The Class class SubnetInfo.
	 * @author Petr Kalafatic
	 * @project Gemini
	 * @version 3.0.0
	 */
	public final class SubnetInfo {
		
		/**
		 * Instantiates a new subnet info.
		 */
		private SubnetInfo() {}

		/**
		 * Netmask.
		 * @return the int
		 */
		private int netmask() {
			return netmask;
		}

		/**
		 * Network.
		 * @return the int
		 */
		private int network() {
			return network;
		}

		/**
		 * Address.
		 * @return the int
		 */
		private int address() {
			return address;
		}

		/**
		 * Broadcast.
		 * @return the int
		 */
		private int broadcast() {
			return broadcast;
		}

		/**
		 * Low.
		 * @return the int
		 */
		private int low() {
			return (isInclusiveHostCount() ? network() : broadcast() - network() > 1 ? network() + 1 : 0);
		}

		/**
		 * High.
		 * @return the int
		 */
		private int high() {
			return (isInclusiveHostCount() ? broadcast() : broadcast() - network() > 1 ? broadcast() - 1 : 0);
		}

		/**
		 * Checks if is in range.
		 * @param address the address
		 * @return true, if is in range
		 */
		public boolean isInRange(String address) {
			return isInRange(toInteger(address));
		}

		/**
		 * Checks if is in range.
		 * @param address the address
		 * @return true, if is in range
		 */
		private boolean isInRange(int address) {
			int diff = address - low();
			return (diff >= 0 && (diff <= (high() - low())));
		}

		/**
		 * Gets the broadcast address.
		 * @return the broadcast address
		 */
		public String getBroadcastAddress() {
			return format(toArray(broadcast()));
		}

		/**
		 * Gets the network address.
		 * @return the network address
		 */
		public String getNetworkAddress() {
			return format(toArray(network()));
		}

		/**
		 * Gets the netmask.
		 * @return the netmask
		 */
		public String getNetmask() {
			return format(toArray(netmask()));
		}

		/**
		 * Gets the address.
		 * @return the address
		 */
		public String getAddress() {
			return format(toArray(address()));
		}

		/**
		 * Gets the low address.
		 * @return the low address
		 */
		public String getLowAddress() {
			return format(toArray(low()));
		}

		/**
		 * Gets the high address.
		 * @return the high address
		 */
		public String getHighAddress() {
			return format(toArray(high()));
		}

		/**
		 * Gets the address count.
		 * @return the address count
		 */
		public int getAddressCount() {
			int count = broadcast() - network() + (isInclusiveHostCount() ? 1 : -1);
			return count < 0 ? 0 : count;
		}

		/**
		 * As integer.
		 * @param address the address
		 * @return the int
		 */
		public int asInteger(String address) {
			return toInteger(address);
		}

		/**
		 * Gets the cidr signature.
		 * @return the cidr signature
		 */
		public String getCidrSignature() {
			return toCidrNotation(format(toArray(address())), format(toArray(netmask())));
		}

		/**
		 * Gets the all addresses.
		 * @return the all addresses
		 */
		public String[] getAllAddresses() {
			int ct = getAddressCount();
			String[] addresses = new String[ct];
			if (ct == 0) {
				return addresses;
			}
			for (int add = low(), j = 0; add <= high(); ++add, ++j) {
				addresses[j] = format(toArray(add));
			}
			return addresses;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			final StringBuilder buf = new StringBuilder();
			buf.append("CIDR Signature:\t[").append(getCidrSignature()).append("]").append(" Netmask: [").append(getNetmask()).append("]\n").append("Network:\t[").append(getNetworkAddress())
					.append("]\n").append("Broadcast:\t[").append(getBroadcastAddress()).append("]\n").append("First Address:\t[").append(getLowAddress()).append("]\n").append("Last Address:\t[")
					.append(getHighAddress()).append("]\n").append("# Addresses:\t[").append(getAddressCount()).append("]\n");
			return buf.toString();
		}
	}

	/**
	 * Gets the info.
	 * @return the info
	 */
	public final SubnetInfo getInfo() {
		return new SubnetInfo();
	}

	/*
	 * Initialize the internal fields from the supplied CIDR mask
	 */
	/**
	 * Calculate.
	 * @param mask the mask
	 */
	private void calculate(String mask) {
		Matcher matcher = cidrPattern.matcher(mask);

		if (matcher.matches()) {
			address = matchAddress(matcher);

			/* Create a binary netmask from the number of bits specification /x */
			int cidrPart = rangeCheck(Integer.parseInt(matcher.group(5)), 0, NBITS);
			for (int j = 0; j < cidrPart; ++j) {
				netmask |= (1 << 31 - j);
			}

			/* Calculate base network address */
			network = (address & netmask);

			/* Calculate broadcast address */
			broadcast = network | ~(netmask);
		} else {
			throw new IllegalArgumentException("Could not parse [" + mask + "]");
		}
	}

	/*
	 * Convert a dotted decimal format address to a packed integer format
	 */
	/**
	 * To integer.
	 * @param address the address
	 * @return the int
	 */
	private int toInteger(String address) {
		Matcher matcher = addressPattern.matcher(address);
		if (matcher.matches()) {
			return matchAddress(matcher);
		} else {
			throw new IllegalArgumentException("Could not parse [" + address + "]");
		}
	}

	/*
	 * Convenience method to extract the components of a dotted decimal address and pack into an integer using a regex match
	 */
	/**
	 * Match address.
	 * @param matcher the matcher
	 * @return the int
	 */
	private int matchAddress(Matcher matcher) {
		int addr = 0;
		for (int i = 1; i <= 4; ++i) {
			int n = (rangeCheck(Integer.parseInt(matcher.group(i)), -1, 255));
			addr |= ((n & 0xff) << 8 * (4 - i));
		}
		return addr;
	}

	/*
	 * Convert a packed integer address into a 4-element array
	 */
	/**
	 * To array.
	 * @param val the val
	 * @return the int[]
	 */
	private int[] toArray(int val) {
		int ret[] = new int[4];
		for (int j = 3; j >= 0; --j) {
			ret[j] |= ((val >>> 8 * (3 - j)) & (0xff));
		}
		return ret;
	}

	/*
	 * Convert a 4-element array into dotted decimal format
	 */
	/**
	 * Format.
	 * @param octets the octets
	 * @return the string
	 */
	private String format(int[] octets) {
		StringBuilder str = new StringBuilder();
		for (int i = 0; i < octets.length; ++i) {
			str.append(octets[i]);
			if (i != octets.length - 1) {
				str.append(".");
			}
		}
		return str.toString();
	}

	/*
	 * Convenience function to check integer boundaries. Checks if a value x is in the range (begin,end]. Returns x if it is in range, throws an
	 * exception otherwise.
	 */
	/**
	 * Range check.
	 * @param value the value
	 * @param begin the begin
	 * @param end the end
	 * @return the int
	 */
	private int rangeCheck(int value, int begin, int end) {
		if (value > begin && value <= end) { // (begin,end]
			return value;
		}

		throw new IllegalArgumentException("Value [" + value + "] not in range (" + begin + "," + end + "]");
	}

	/*
	 * Count the number of 1-bits in a 32-bit integer using a divide-and-conquer strategy see Hacker's Delight section 5.1
	 */
	/**
	 * Pop.
	 * @param x the x
	 * @return the int
	 */
	int pop(int x) {
		x = x - ((x >>> 1) & 0x55555555);
		x = (x & 0x33333333) + ((x >>> 2) & 0x33333333);
		x = (x + (x >>> 4)) & 0x0F0F0F0F;
		x = x + (x >>> 8);
		x = x + (x >>> 16);
		return x & 0x0000003F;
	}

	/*
	 * Convert two dotted decimal addresses to a single xxx.xxx.xxx.xxx/yy format by counting the 1-bit population in the mask address. (It may be
	 * better to count NBITS-#trailing zeroes for this case)
	 */
	/**
	 * To cidr notation.
	 * @param addr the addr
	 * @param mask the mask
	 * @return the string
	 */
	private String toCidrNotation(String addr, String mask) {
		return addr + "/" + pop(toInteger(mask));
	}
}
