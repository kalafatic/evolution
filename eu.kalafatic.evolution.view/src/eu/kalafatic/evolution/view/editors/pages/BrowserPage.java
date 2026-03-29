package eu.kalafatic.evolution.view.editors.pages;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;

public class BrowserPage extends Composite {
    private Text urlText;
    private Browser browser;
    private Orchestrator orchestrator;

    public BrowserPage(Composite parent, MultiPageEditor editor, Orchestrator orchestrator) {
        super(parent, SWT.NONE);
        this.orchestrator = orchestrator;
        createControl();
    }

    private void createControl() {
        this.setLayout(new GridLayout(2, false));
        urlText = new Text(this, SWT.BORDER);
        urlText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        urlText.setText("https://ollama.com");
        Button goButton = new Button(this, SWT.PUSH);
        goButton.setText("Go");
        goButton.addSelectionListener(new SelectionAdapter() {
            @Override public void widgetSelected(SelectionEvent e) { if (browser != null && !browser.isDisposed()) browser.setUrl(urlText.getText()); }
        });
        browser = new Browser(this, SWT.NONE);
        browser.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
        browser.setUrl(urlText.getText());
    }

    public void setOrchestrator(Orchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }
}
