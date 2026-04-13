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
package eu.kalafatic.utils.xml;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.jface.text.AbstractDocument;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.ui.texteditor.spelling.SpellingAnnotation;
import org.eclipse.ui.texteditor.spelling.SpellingReconcileStrategy;
import org.eclipse.ui.texteditor.spelling.SpellingService;

/**
 * The Class class XMLSpellingReconcileStrategy.
 * 
 * @author Petr Kalafatic
 * @project Gemini
 * @version 3.0.0
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class XMLSpellingReconcileStrategy extends SpellingReconcileStrategy {

	/**
	 * Instantiates a new xML spelling reconcile strategy.
	 * 
	 * @param sourceViewer
	 *            the source viewer
	 * @param spellingService
	 *            the spelling service
	 */
	public XMLSpellingReconcileStrategy(ISourceViewer sourceViewer,
			SpellingService spellingService) {
		super(sourceViewer, spellingService);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.texteditor.spelling.SpellingReconcileStrategy#reconcile(org.eclipse.jface.text.IRegion)
	 */
	@Override
	public void reconcile(IRegion region) {

		AbstractDocument document = (AbstractDocument) getDocument();
		IDocumentPartitioner docPartitioner = document
				.getDocumentPartitioner(XMLStringPartitionScanner.XML_STRING);

		IAnnotationModel model = getAnnotationModel();
		if (region.getOffset() == 0
				&& region.getLength() == document.getLength()) {
			// reconciling whole document
			super.reconcile(region);
			deleteUnwantedAnnotations();
		} else {
			// partial reconciliation
			// preserve spelling annotations first
			Iterator iter = model.getAnnotationIterator();
			Map spellingErrors = new HashMap(1);
			while (iter.hasNext()) {
				Annotation annotation = (Annotation) iter.next();
				if (annotation instanceof SpellingAnnotation) {
					SpellingAnnotation spellingAnnotation = (SpellingAnnotation) annotation;
					Position position = model.getPosition(spellingAnnotation);
					String contentType = docPartitioner.getContentType(position
							.getOffset());

					if (XMLStringPartitionScanner.XML_STRING
							.equalsIgnoreCase(contentType)) {
						spellingErrors.put(spellingAnnotation,
								model.getPosition(annotation));
					}
				}
			}

			// reconcile
			super.reconcile(region);

			// restore annotations
			model = getAnnotationModel();
			iter = spellingErrors.keySet().iterator();
			while (iter.hasNext()) {
				Annotation annotation = (Annotation) iter.next();
				model.addAnnotation(annotation,
						(Position) spellingErrors.get(annotation));
			}
			deleteUnwantedAnnotations();
		}

	}

	/**
	 * Delete unwanted annotations.
	 */
	private void deleteUnwantedAnnotations() {
		AbstractDocument document = (AbstractDocument) getDocument();
		IDocumentPartitioner docPartitioner = document
				.getDocumentPartitioner(XMLStringPartitionScanner.XML_STRING);
		IAnnotationModel model = getAnnotationModel();
		Iterator iter = model.getAnnotationIterator();

		while (iter.hasNext()) {
			Annotation annotation = (Annotation) iter.next();
			if (annotation instanceof SpellingAnnotation) {
				SpellingAnnotation spellingAnnotation = (SpellingAnnotation) annotation;
				Position position = model.getPosition(spellingAnnotation);
				String contentType = docPartitioner.getContentType(position
						.getOffset());
				if (!XMLStringPartitionScanner.XML_STRING
						.equalsIgnoreCase(contentType)) {
					model.removeAnnotation(spellingAnnotation);
				}
			}
		}
	}
}
