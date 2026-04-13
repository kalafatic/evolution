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
package eu.kalafatic.utils.application;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.eclipse.swt.widgets.DateTime;

import eu.kalafatic.utils.constants.EDateFormat;

/**
 * The Class class DateUtils.
 * @author Petr Kalafatic
 * @project Gemini
 * @version 3.0.0
 */
public class DateUtils {

	// ---------------------------------------------------------------
	// ---------------------------------------------------------------

	/**
	 * Date time to time.
	 * @param dateTime the date time
	 * @return the long
	 */
	public static Long dateTimeToTime(DateTime dateTime) {
		Calendar calendar = Calendar.getInstance();
		calendar.clear();
		calendar.set(dateTime.getYear(), dateTime.getMonth(), dateTime.getDay());
		return calendar.getTime().getTime();
	}

	// ---------------------------------------------------------------

	/**
	 * Time to date time.
	 * @param dateTime the date time
	 * @param date the date
	 */
	public static void timeToDateTime(DateTime dateTime, Date date) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		dateTime.setYear(calendar.get(Calendar.YEAR));
		dateTime.setMonth(calendar.get(Calendar.MONTH));
		dateTime.setDay(calendar.get(Calendar.DAY_OF_MONTH));
	}

	// ---------------------------------------------------------------

	/**
	 * Decode time.
	 * @param time the time
	 * @param eDateFormat the e date format
	 * @return the string
	 */
	public static String decodeTime(Long time, EDateFormat eDateFormat) {
		SimpleDateFormat sdf = new SimpleDateFormat(eDateFormat.getLiteral());
		return sdf.format(new Date(time));
	}

}
