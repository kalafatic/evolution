package eu.kalafatic.evolution.view.dialogs;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import eu.kalafatic.evolution.controller.manager.EnvironmentSuggestionService;
import eu.kalafatic.evolution.controller.manager.EnvironmentSuggestionService.Suggestion;

public class ProjectSetupWizardDialog extends TitleAreaDialog {

    private List<Suggestion> suggestions;
    private List<Suggestion> selectedSuggestions = new ArrayList<>();
    private Table table;

    public ProjectSetupWizardDialog(Shell parentShell, List<Suggestion> suggestions) {
        super(parentShell);
        this.suggestions = suggestions;
    }

    @Override
    public void create() {
        super.create();
        setTitle("AI Evolution Project Setup Wizard");
        setMessage("We detected some missing or suboptimal settings. Select the ones you want to fix.");
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite area = (Composite) super.createDialogArea(parent);
        Composite container = new Composite(area, SWT.NONE);
        container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        container.setLayout(new GridLayout(1, false));

        table = new Table(container, SWT.CHECK | SWT.BORDER | SWT.FULL_SELECTION);
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        String[] titles = { "Setting", "Current", "Suggested", "Reason" };
        for (String title : titles) {
            TableColumn column = new TableColumn(table, SWT.NONE);
            column.setText(title);
            column.setWidth(150);
        }

        for (Suggestion s : suggestions) {
            TableItem item = new TableItem(table, SWT.NONE);
            item.setText(new String[] { s.field, s.currentValue, s.suggestedValue, s.reason });
            item.setChecked(s.isMissing);
            item.setData(s);
        }

        return area;
    }

    @Override
    protected boolean isResizable() {
        return true;
    }

    @Override
    protected void okPressed() {
        selectedSuggestions.clear();
        for (TableItem item : table.getItems()) {
            if (item.getChecked()) {
                selectedSuggestions.add((Suggestion) item.getData());
            }
        }
        super.okPressed();
    }

    public List<Suggestion> getSelectedSuggestions() {
        return selectedSuggestions;
    }
}
