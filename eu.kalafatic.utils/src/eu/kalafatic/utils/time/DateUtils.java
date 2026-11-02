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
package eu.kalafatic.utils.time;

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

	/**
	 * Formats a duration in milliseconds into a concise, human-readable string.
	 * Examples:
	 *   0 -> "0 ms"
	 *   450 -> "450 ms"
	 *   12500 -> "12.50 s (12500 ms)"
	 *   598453 -> "9 min 58 sec (598453 ms)"
	 *   11494322 -> "3 hr 11 min 34 sec (11494322 ms)"
	 *   90061000 -> "1 d 1 hr 1 min 1 sec (90061000 ms)"
	 *
	 * @param durationMs the duration in milliseconds
	 * @return formatted duration string
	 */
	public static String formatDuration(long durationMs) {
		if (durationMs < 0) {
			return "0 ms";
		}
		if (durationMs < 1000) {
			return durationMs + " ms";
		}
		long totalSec = durationMs / 1000;
		if (totalSec < 60) {
			return String.format("%.2f s (%d ms)", durationMs / 1000.0, durationMs);
		}
		long sec = totalSec % 60;
		long totalMin = totalSec / 60;
		if (totalMin < 60) {
			return String.format("%d min %d sec (%d ms)", totalMin, sec, durationMs);
		}
		long min = totalMin % 60;
		long totalHours = totalMin / 60;
		if (totalHours < 24) {
			return String.format("%d hr %d min %d sec (%d ms)", totalHours, min, sec, durationMs);
		}
		long hours = totalHours % 24;
		long days = totalHours / 24;
		return String.format("%d d %d hr %d min %d sec (%d ms)", days, hours, min, sec, durationMs);
	}

}
