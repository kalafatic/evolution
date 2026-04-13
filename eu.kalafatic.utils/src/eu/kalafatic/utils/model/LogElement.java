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

/**
 * The Class class LogElement.
 * 
 * @author Petr Kalafatic
 * @project Gemini
 * @version 3.0.0
 */
public class LogElement {

	/** The trace. */
	private String type, date, name, msg, trace;

	/** The exception. */
	private Exception exception;

	/**
	 * Instantiates a new log element.
	 */
	public LogElement() {
	}

	/**
	 * Instantiates a new log element.
	 * 
	 * @param type
	 *            the type
	 * @param date
	 *            the date
	 * @param name
	 *            the name
	 * @param msg
	 *            the msg
	 * @param trace
	 *            the trace
	 */
	public LogElement(String type, String date, String name, String msg,
			String trace) {
		this.type = type;
		this.date = date;
		this.name = name;
		this.msg = msg;
		this.trace = trace;
	}

	// ---------------------------------------------------------------
	// ---------------------------------------------------------------

	/**
	 * Gets the line msg.
	 * 
	 * @return the line msg
	 */
	public String getLineMsg() {
		return type + " >> " + date + " >> " + name + " : " + msg + " ... [ "
				+ trace + " ]";
	}

	// ---------------------------------------------------------------

	/**
	 * Clear.
	 */
	public void clear() {
		this.type = "";
		this.date = "";
		this.name = "";
		this.msg = "";
		this.trace = "";
	}

	// ---------------------------------------------------------------

	/**
	 * Gets the type.
	 * 
	 * @return the type
	 */
	public String getType() {
		return type;
	}

	// ---------------------------------------------------------------

	/**
	 * Sets the type.
	 * 
	 * @param type
	 *            the new type
	 */
	public void setType(String type) {
		this.type = type;
	}

	// ---------------------------------------------------------------

	/**
	 * Gets the date.
	 * 
	 * @return the date
	 */
	public String getDate() {
		return date;
	}

	// ---------------------------------------------------------------

	/**
	 * Sets the date.
	 * 
	 * @param date
	 *            the new date
	 */
	public void setDate(String date) {
		this.date = date;
	}

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
	 * Gets the msg.
	 * 
	 * @return the msg
	 */
	public String getMsg() {
		return msg;
	}

	// ---------------------------------------------------------------

	/**
	 * Sets the msg.
	 * 
	 * @param msg
	 *            the new msg
	 */
	public void setMsg(String msg) {
		this.msg = msg;
	}

	// ---------------------------------------------------------------

	/**
	 * Gets the trace.
	 * 
	 * @return the trace
	 */
	public String getTrace() {
		return trace;
	}

	// ---------------------------------------------------------------

	/**
	 * Sets the trace.
	 * 
	 * @param trace
	 *            the new trace
	 */
	public void setTrace(String trace) {
		this.trace = trace;
	}

	// ---------------------------------------------------------------

	/**
	 * Gets the exception.
	 * 
	 * @return the exception
	 */
	public Exception getException() {
		return exception;
	}

	// ---------------------------------------------------------------

	/**
	 * Sets the exception.
	 * 
	 * @param exception
	 *            the new exception
	 */
	public void setException(Exception exception) {
		this.exception = exception;
	}

}
