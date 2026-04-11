package eu.kalafatic.evolution.view.wizards;

import org.eclipse.jface.wizard.WizardPage;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

public abstract class AWizardPage extends WizardPage {
	protected Orchestrator orchestrator;

	protected AWizardPage(String pageName) {
		super(pageName);
	}

	public void setOrchestrator(Orchestrator orchestrator) {
		this.orchestrator = orchestrator;
	}
}
