package eu.kalafatic.utils.providers;

import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.DefaultTextHover;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.ITextDoubleClickStrategy;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.information.IInformationPresenter;
import org.eclipse.jface.text.information.InformationPresenter;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.quickassist.IQuickAssistAssistant;
import org.eclipse.jface.text.quickassist.QuickAssistAssistant;
import org.eclipse.jface.text.reconciler.IReconciler;
import org.eclipse.jface.text.reconciler.Reconciler;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.editors.text.TextSourceViewerConfiguration;
import org.eclipse.ui.texteditor.spelling.SpellingCorrectionProcessor;

import eu.kalafatic.utils.xml.IXMLColorConstants;
import eu.kalafatic.utils.xml.NonRuleBasedDamagerRepairer;
import eu.kalafatic.utils.xml.XMLDoubleClickStrategy;
import eu.kalafatic.utils.xml.XMLPartitionScanner;
import eu.kalafatic.utils.xml.XMLScanner;
import eu.kalafatic.utils.xml.XMLSpellingReconcileStrategy;
import eu.kalafatic.utils.xml.XMLStringPartitionScanner;
import eu.kalafatic.utils.xml.XMLTagScanner;

public class XMLConfiguration extends TextSourceViewerConfiguration {

	private XMLDoubleClickStrategy doubleClickStrategy;
	private XMLTagScanner tagScanner;
	private XMLScanner scanner;
	private ColorManager colorManager;

	public XMLConfiguration(ColorManager colorManager) {
		super();
		this.colorManager = colorManager;
	}

	@Override
	public String[] getConfiguredContentTypes(ISourceViewer sourceViewer) {
		return new String[] { IDocument.DEFAULT_CONTENT_TYPE, XMLPartitionScanner.XML_COMMENT, XMLPartitionScanner.XML_TAG, XMLStringPartitionScanner.XML_STRING };
	}

	@Override
	public ITextDoubleClickStrategy getDoubleClickStrategy(ISourceViewer sourceViewer, String contentType) {
		if (doubleClickStrategy == null) {
			doubleClickStrategy = new XMLDoubleClickStrategy();
		}
		return doubleClickStrategy;
	}

	protected XMLScanner getXMLScanner() {
		if (scanner == null) {
			scanner = new XMLScanner(colorManager);
			scanner.setDefaultReturnToken(new Token(new TextAttribute(colorManager.getColor(IXMLColorConstants.DEFAULT))));
		}
		return scanner;
	}

	protected XMLTagScanner getXMLTagScanner() {
		if (tagScanner == null) {
			tagScanner = new XMLTagScanner(colorManager);
			tagScanner.setDefaultReturnToken(new Token(new TextAttribute(colorManager.getColor(IXMLColorConstants.TAG))));
		}
		return tagScanner;
	}

	@Override
	public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {
		PresentationReconciler reconciler = new PresentationReconciler();

		DefaultDamagerRepairer dr = new DefaultDamagerRepairer(getXMLTagScanner());
		reconciler.setDamager(dr, XMLPartitionScanner.XML_TAG);
		reconciler.setRepairer(dr, XMLPartitionScanner.XML_TAG);

		dr = new DefaultDamagerRepairer(getXMLScanner());
		reconciler.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
		reconciler.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);

		// wire up the xml string content type too
		dr = new DefaultDamagerRepairer(getXMLTagScanner());
		reconciler.setDamager(dr, XMLStringPartitionScanner.XML_STRING);
		reconciler.setRepairer(dr, XMLStringPartitionScanner.XML_STRING);

		NonRuleBasedDamagerRepairer ndr = new NonRuleBasedDamagerRepairer(new TextAttribute(colorManager.getColor(IXMLColorConstants.XML_COMMENT)));
		reconciler.setDamager(ndr, XMLPartitionScanner.XML_COMMENT);
		reconciler.setRepairer(ndr, XMLPartitionScanner.XML_COMMENT);

		return reconciler;
	}

	/**
	 * Providing custom XMLSpellingReconcileStrategy to enable spell checking for only strings
	 */
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.text.source.SourceViewerConfiguration#getReconciler (org.eclipse.jface.text.source.ISourceViewer)
	 */
	@Override
	public IReconciler getReconciler(ISourceViewer sourceViewer) {
		XMLSpellingReconcileStrategy strategy = new XMLSpellingReconcileStrategy(sourceViewer, EditorsUI.getSpellingService());
		Reconciler reconciler = new Reconciler();
		reconciler.setReconcilingStrategy(strategy, XMLStringPartitionScanner.XML_STRING);
		reconciler.setDocumentPartitioning(XMLStringPartitionScanner.XML_STRING);
		return reconciler;

	}

	@Override
	public IQuickAssistAssistant getQuickAssistAssistant(ISourceViewer sourceViewer) {
		IQuickAssistAssistant assist = new QuickAssistAssistant();
		assist.setQuickAssistProcessor(new SpellingCorrectionProcessor());
		assist.install(sourceViewer);
		return assist;
	}

	@Override
	public IContentAssistant getContentAssistant(ISourceViewer sourceViewer) {
		// Create content assistant
		ContentAssistant assistant = new ContentAssistant();

		// required to display additional info
		assistant.setInformationControlCreator(new IInformationControlCreator() {
			@Override
			public IInformationControl createInformationControl(Shell parent) {
				return new DefaultInformationControl(parent);
			}
		});

		// processor.setFile(file);
		// // Set this processor for each supported content type
		// assistant.setContentAssistProcessor(processor,
		// XMLPartitionScanner.XML_TAG);
		// assistant.setContentAssistProcessor(processor,
		// XMLPartitionScanner.XML_DEFAULT);
		// assistant.setContentAssistProcessor(processor,
		// IDocument.DEFAULT_CONTENT_TYPE);

		// Return the content assistant
		return assistant;
	}

	@Override
	public IInformationPresenter getInformationPresenter(ISourceViewer sourceViewer) {

		return new InformationPresenter(getInformationControlCreator(sourceViewer));
	}

	@Override
	public ITextHover getTextHover(ISourceViewer sourceViewer, String contentType) {
		if (XMLStringPartitionScanner.XML_STRING.equalsIgnoreCase(contentType)) {
			System.out.print(contentType);
			return new DefaultTextHover(sourceViewer);
		} else {
			// return new DefaultTextHover(sourceViewer);
			return super.getTextHover(sourceViewer, contentType);
		}
	}

}