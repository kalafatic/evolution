package eu.kalafatic.evolution.view.views;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.part.ViewPart;

public class InternalBrowserView extends ViewPart {

    public static final String ID = "eu.kalafatic.evolution.view.views.InternalBrowserView";

    private Browser browser;
    private Text urlText;

    @Override
    public void createPartControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);
        container.setLayout(new GridLayout(2, false));

        urlText = new Text(container, SWT.BORDER);
        urlText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        urlText.setText("https://ollama.com");

        Button goButton = new Button(container, SWT.PUSH);
        goButton.setText("Go");
        goButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                browser.setUrl(urlText.getText());
            }
        });

        browser = new Browser(container, SWT.NONE);
        browser.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
        browser.setUrl(urlText.getText());
    }

    @Override
    public void setFocus() {
        urlText.setFocus();
    }
}
