package eu.kalafatic.evolution.view.wizards;

import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;

public class ImportEvoTasksWizard extends Wizard implements IImportWizard {

    public ImportEvoTasksWizard() {
        super();
    }

    @Override
    public boolean performFinish() {
        // TODO Auto-generated method stub
        return true;
    }

    @Override
    public void init(IWorkbench workbench, org.eclipse.jface.viewers.IStructuredSelection selection) {
        // TODO Auto-generated method stub
    }
}
