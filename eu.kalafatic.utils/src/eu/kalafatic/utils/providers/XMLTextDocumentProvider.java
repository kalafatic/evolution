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
package eu.kalafatic.utils.providers;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.AbstractDocument;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.rules.FastPartitioner;
import org.eclipse.ui.editors.text.TextFileDocumentProvider;

import eu.kalafatic.utils.xml.XMLPartitionScanner;
import eu.kalafatic.utils.xml.XMLStringPartitionScanner;

/**
 * The Class class XMLTextDocumentProvider.
 * @author Petr Kalafatic
 * @project Gemini
 * @version 3.0.0
 */
public class XMLTextDocumentProvider extends TextFileDocumentProvider {

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.editors.text.TextFileDocumentProvider#createFileInfo(java.lang.Object)
	 */
	@Override
	protected FileInfo createFileInfo(Object element) throws CoreException {
		FileInfo info = super.createFileInfo(element);
		if (info == null) {
			info = createEmptyFileInfo();
		}
		IDocument document = info.fTextFileBuffer.getDocument();
		if (document != null) {

			IDocumentPartitioner partitioner = new FastPartitioner(new XMLPartitionScanner(), new String[] { XMLPartitionScanner.XML_TAG, XMLPartitionScanner.XML_COMMENT,
					XMLPartitionScanner.XML_START_TAG, XMLPartitionScanner.XML_PI, XMLPartitionScanner.XML_DOCTYPE, XMLPartitionScanner.XML_END_TAG, XMLPartitionScanner.XML_TEXT,
					XMLPartitionScanner.XML_CDATA, XMLPartitionScanner.XML_COMMENT });

			partitioner.connect(document);
			document.setDocumentPartitioner(partitioner);

			// Adding our string partition scanner to the document
			partitioner = new FastPartitioner(new XMLStringPartitionScanner(), new String[] { XMLStringPartitionScanner.XML_STRING });

			partitioner.connect(document);
			((AbstractDocument) document).setDocumentPartitioner(XMLStringPartitionScanner.XML_STRING, partitioner);
		}
		return info;

	}

}
