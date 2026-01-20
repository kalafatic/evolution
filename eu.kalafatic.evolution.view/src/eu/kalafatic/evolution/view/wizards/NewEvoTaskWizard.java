package eu.kalafatic.evolution.view.wizards;

import org.eclipse.jface.wizard.Wizard;

public class NewEvoTaskWizard extends Wizard {

    public NewEvoTaskWizard() {
        super();
        setNeedsProgressMonitor(true);
    }

    @Override
    public boolean performFinish() {
        // TODO Auto-generated method stub
        return true;
    }
}
