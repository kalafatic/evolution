package eu.kalafatic.evolution.view.wizards;

import java.net.URL;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

public class MavenSettingsPage extends WizardPage {
    private Text goalsText, profilesText;
    private Button skipCheck;
    private ControlDecoration mavenDecorator;
    private Job validationJob;

    public MavenSettingsPage() {
        super("MavenSettingsPage");
        setTitle("Maven Settings");
        setDescription("Configure Maven build goals.");
    }

    @Override
    public void createControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);
        container.setLayout(new GridLayout(2, false));

        new Label(container, SWT.NONE).setText("Goals (comma separated):");
        goalsText = new Text(container, SWT.BORDER);
        goalsText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        goalsText.setText("clean, install");

        new Label(container, SWT.NONE).setText("Profiles (comma separated):");
        profilesText = new Text(container, SWT.BORDER);
        profilesText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        mavenDecorator = new ControlDecoration(goalsText, SWT.TOP | SWT.LEFT);
        mavenDecorator.setImage(FieldDecorationRegistry.getDefault()
                .getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
        mavenDecorator.hide();

        goalsText.addModifyListener(e -> validateMaven());

        skipCheck = new Button(container, SWT.CHECK);
        skipCheck.setText("Skip this step and setup later");
        skipCheck.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, true, false, 2, 1));
        skipCheck.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                validateMaven();
            }
        });

        Link mavenHelpLink = new Link(container, SWT.NONE);
        mavenHelpLink.setText("<a>How to install Maven?</a>");
        mavenHelpLink.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false, 2, 1));
        mavenHelpLink.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                openUrl("https://maven.apache.org/download.cgi");
            }
        });

        setControl(container);
        validateMaven();
    }

    private void validateMaven() {
        if (skipCheck.getSelection()) {
            if (validationJob != null) validationJob.cancel();
            mavenDecorator.hide();
            setPageComplete(true);
            setErrorMessage(null);
            return;
        }
        if (validationJob != null) validationJob.cancel();
        validationJob = new Job("Validate Maven") {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                boolean success = false;
                try {
                    String cmd = System.getProperty("os.name").toLowerCase().contains("win") ? "mvn.cmd" : "mvn";
                    Process process = new ProcessBuilder(cmd, "-v").start();
                    success = (process.waitFor() == 0);
                } catch (Exception e) {}

                final boolean finalSuccess = success;
                Display.getDefault().asyncExec(() -> {
                    if (finalSuccess) {
                        mavenDecorator.hide();
                        setPageComplete(true);
                        setErrorMessage(null);
                    } else {
                        showMavenError();
                    }
                });
                return Status.OK_STATUS;
            }
        };
        validationJob.setSystem(true);
        validationJob.schedule(500);
    }

    private void showMavenError() {
        mavenDecorator.setDescriptionText("Maven is not installed or not in PATH.");
        mavenDecorator.show();
        setPageComplete(false);
        setErrorMessage("Maven is required to build the project.");
    }

    private void openUrl(String url) {
        try {
            PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL(url));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getGoals() { return goalsText.getText(); }
    public String getProfiles() { return profilesText.getText(); }
    public boolean isSkipped() { return skipCheck.getSelection(); }
}
