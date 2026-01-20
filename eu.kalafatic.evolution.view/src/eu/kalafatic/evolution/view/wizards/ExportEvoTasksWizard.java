package eu.kalafatic.evolution.view.wizards;

import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;

public class ExportEvoTasksWizard extends Wizard implements IExportWizard {

    public ExportEvoTasksWizard() {
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
