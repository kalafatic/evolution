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
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;

public class GitSettingsPage extends WizardPage {
    private Text repoUrlText, branchText, usernameText, localPathText;
    private Button skipCheck;
    private ControlDecoration gitDecorator;
    private Job validationJob;

    public GitSettingsPage() {
        super("GitSettingsPage");
        setTitle("Git Settings");
        setDescription("Configure Git repository settings.");
    }

    @Override
    public void createControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);
        container.setLayout(new GridLayout(2, false));

        new Label(container, SWT.NONE).setText("Repository URL:");
        repoUrlText = new Text(container, SWT.BORDER);
        repoUrlText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        repoUrlText.setText("https://github.com/kalafatic/evo.git");

        gitDecorator = new ControlDecoration(repoUrlText, SWT.TOP | SWT.LEFT);
        gitDecorator.setImage(FieldDecorationRegistry.getDefault()
                .getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
        gitDecorator.hide();

        repoUrlText.addModifyListener(e -> validateGit());

        Link gitHelpLink = new Link(container, SWT.NONE);
        gitHelpLink.setText("<a>How to install Git?</a>");
        gitHelpLink.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false, 2, 1));
        gitHelpLink.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                openUrl("https://git-scm.com/downloads");
            }
        });

        new Label(container, SWT.NONE).setText("Branch:");
        branchText = new Text(container, SWT.BORDER);
        branchText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        branchText.setText("master");

        new Label(container, SWT.NONE).setText("Username:");
        usernameText = new Text(container, SWT.BORDER);
        usernameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        usernameText.setText("admin");

        new Label(container, SWT.NONE).setText("Local Path:");
        localPathText = new Text(container, SWT.BORDER);
        localPathText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        localPathText.setText("repo");

        skipCheck = new Button(container, SWT.CHECK);
        skipCheck.setText("Skip this step and setup later");
        skipCheck.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, true, false, 2, 1));
        skipCheck.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                validateGit();
            }
        });

        setControl(container);
        validateGit();
    }

    private void validateGit() {
        if (skipCheck.getSelection()) {
            if (validationJob != null) validationJob.cancel();
            gitDecorator.hide();
            setPageComplete(true);
            setErrorMessage(null);
            return;
        }
        if (validationJob != null) validationJob.cancel();
        validationJob = new Job("Validate Git") {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                boolean success = false;
                try {
                    Process process = new ProcessBuilder("git", "--version").start();
                    success = (process.waitFor() == 0);
                } catch (Exception e) {}

                final boolean finalSuccess = success;
                Display.getDefault().asyncExec(() -> {
                    if (finalSuccess) {
                        gitDecorator.hide();
                        setPageComplete(true);
                        setErrorMessage(null);
                    } else {
                        showGitError();
                    }
                });
                return Status.OK_STATUS;
            }
        };
        validationJob.setSystem(true);
        validationJob.schedule(500);
    }

    private void showGitError() {
        gitDecorator.setDescriptionText("Git is not installed or not in PATH.");
        gitDecorator.show();
        setPageComplete(false);
        setErrorMessage("Git is required to clone the repository.");
    }

    private void openUrl(String url) {
        try {
            IWorkbenchBrowserSupport support = PlatformUI.getWorkbench().getBrowserSupport();
            support.createBrowser(IWorkbenchBrowserSupport.AS_EDITOR, "evoGitHelp", "Git Help", "Git Help").openURL(new URL(url));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getRepoUrl() { return repoUrlText.getText(); }
    public String getBranch() { return branchText.getText(); }
    public String getUsername() { return usernameText.getText(); }
    public String getLocalPath() { return localPathText.getText(); }
    public boolean isSkipped() { return skipCheck.getSelection(); }
}
