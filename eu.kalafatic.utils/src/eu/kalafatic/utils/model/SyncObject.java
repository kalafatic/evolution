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

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import eu.kalafatic.utils.interfaces.ESyncType;

/**
 * The Class class SyncObject.
 * @author Petr Kalafatic
 * @project Gemini
 * @version 3.0.0
 */
@SuppressWarnings({ "serial", "rawtypes" })
public class SyncObject implements Serializable {

	/** The name. */
	public String name;

	/** The data. */
	public Object data;

	/** The type. */
	public ESyncType type;

	/** The to. */
	public Map<ESyncType, List> to = new HashMap<ESyncType, List>();

	/** The FLAGS. */
	public int FLAGS = 0;

	/** The note. */
	public Set<String> note = new HashSet<String>();

	/** The parameters. */
	public Map<Object, Object> parameters = new HashMap<Object, Object>();

	/**
	 * Instantiates a new sync object.
	 * @param name the name
	 * @param data the data
	 * @param type the type
	 */
	public SyncObject(String name, Object data, ESyncType type) {
		this.name = name;
		this.data = data;
		this.type = type;
	}

	/**
	 * Note as string.
	 * @return the string
	 */
	public String noteAsString() {
		StringBuffer sb = new StringBuffer();
		for (String string : note) {
			sb.append(string + ", ");
		}
		return sb.toString();
	}
}
