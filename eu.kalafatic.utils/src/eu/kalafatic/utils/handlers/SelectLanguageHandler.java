package eu.kalafatic.utils.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.PlatformUI;

import eu.kalafatic.utils.application.LanguageUtils;

public class SelectLanguageHandler extends AbstractHandler {

	private final String PARAMETER_ID = "eu.kalafatic.utils.handlers.SelectLanguageHandler.Parameter";

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.core.commands.AbstractHandler#execute(org.eclipse.core.commands .ExecutionEvent)
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		String locale = event.getParameter(PARAMETER_ID);
		LanguageUtils.INSTANCE.switchLanguage(locale);
		PlatformUI.getWorkbench().restart();
		return null;
	}
}
