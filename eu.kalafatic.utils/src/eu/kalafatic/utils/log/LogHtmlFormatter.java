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
package eu.kalafatic.utils.log;

import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * The Class class LogHtmlFormatter.
 * 
 * @author Petr Kalafatic
 * @project Gemini
 * @version 3.0.0
 */
public class LogHtmlFormatter extends Formatter {
	
	// This method is called for every log records
    /* (non-Javadoc)
	 * @see java.util.logging.Formatter#format(java.util.logging.LogRecord)
	 */
	public String format(LogRecord rec) {
        StringBuffer buf = new StringBuffer(1000);
        // Bold any levels >= WARNING
        if (rec.getLevel().intValue() >= Level.WARNING.intValue()) {
            buf.append("<b>");
            buf.append(rec.getLevel());
            buf.append("</b>");
        } else {
            buf.append(rec.getLevel());
        }
        buf.append(" ");
        buf.append(rec.getMillis());
        buf.append("  >>>  ");
        buf.append("From Class : ");
        buf.append(rec.getSourceClassName());
        buf.append("  >>>  ");
        buf.append("From Method : ");
        buf.append(rec.getSourceMethodName());
        buf.append("  >>>  ");
       
        buf.append("<font color=\"#0000FF\">");
        buf.append(formatMessage(rec));        
        buf.append("</font>");
       
        buf.append('\n');
        return buf.toString();
    }

    // This method is called just after the handler using this
    // formatter is created
    /* (non-Javadoc)
     * @see java.util.logging.Formatter#getHead(java.util.logging.Handler)
     */
    public String getHead(Handler h) {
        return "<HTML><HEAD>"+(new Date())+"</HEAD><BODY><PRE>\n";
    }

    // This method is called just after the handler using this
    // formatter is closed
    /* (non-Javadoc)
     * @see java.util.logging.Formatter#getTail(java.util.logging.Handler)
     */
    public String getTail(Handler h) {
        return "</PRE></BODY></HTML>\n";
    }


}
