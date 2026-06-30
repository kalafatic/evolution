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
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;

import org.eclipse.swt.widgets.DirectoryDialog;

import eu.kalafatic.evolution.controller.manager.ProjectModelManager;
import java.util.List;

public class GitSettingsPage extends AWizardPage {
    private Text repoUrlText, branchText, usernameText, passwordText;
    private Combo localPathText;
    private Button skipCheck;
    private ControlDecoration gitDecorator, infoDecorator;
    private Job validationJob;

    public GitSettingsPage() {
        super("GitSettingsPage");
        setTitle("Git Settings");
        setDescription("Configure Git repository settings.");
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible && orchestrator != null && orchestrator.getGit() != null) {
            eu.kalafatic.evolution.model.orchestration.Git git = orchestrator.getGit();
            if (git.getRepositoryUrl() != null) repoUrlText.setText(git.getRepositoryUrl());
            if (git.getBranch() != null) branchText.setText(git.getBranch());
            if (git.getUsername() != null) usernameText.setText(git.getUsername());
            if (git.getPassword() != null) passwordText.setText(git.getPassword());
            if (git.getLocalPath() != null) {
                String lp = git.getLocalPath();
                if (localPathText.indexOf(lp) < 0) {
                    localPathText.add(lp);
                }
                localPathText.setText(lp);
            }
        }
    }

    @Override
    public void createControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);
        container.setLayout(new GridLayout(2, false));

        new Label(container, SWT.NONE).setText("Repository URL:");
        repoUrlText = new Text(container, SWT.BORDER);
        repoUrlText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        repoUrlText.setText("https://github.com/kalafatic/evolution/");


        gitDecorator = new ControlDecoration(repoUrlText, SWT.TOP | SWT.LEFT);
        gitDecorator.setImage(FieldDecorationRegistry.getDefault()
                .getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
        gitDecorator.hide();

        infoDecorator = new ControlDecoration(repoUrlText, SWT.TOP | SWT.LEFT);
        infoDecorator.setImage(FieldDecorationRegistry.getDefault()
                .getFieldDecoration(FieldDecorationRegistry.DEC_INFORMATION).getImage());
        infoDecorator.setDescriptionText("Leave empty to initialize a local Git repository with a standard .gitignore.");
        infoDecorator.setShowOnlyOnFocus(false);

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

        new Label(container, SWT.NONE).setText("Password/Token:");
        passwordText = new Text(container, SWT.BORDER | SWT.PASSWORD);
        passwordText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        new Label(container, SWT.NONE).setText("Local Path:");
        Composite pathComp = new Composite(container, SWT.NONE);
        pathComp.setLayout(new GridLayout(2, false));
        pathComp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        localPathText = new Combo(pathComp, SWT.BORDER | SWT.DROP_DOWN);
        localPathText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        List<String> repos = ProjectModelManager.getInstance().getAvailableLocalRepositories();
        for (String r : repos) {
            localPathText.add(r);
        }

        // Default selection logic: evolution or evo
        if (repos.isEmpty()) {
            localPathText.setText("repo");
        } else {
            boolean found = false;
            for (int i = 0; i < localPathText.getItemCount(); i++) {
                String item = localPathText.getItem(i).toLowerCase();
                if (item.contains("evolution") || item.contains("/evo")) {
                    localPathText.select(i);
                    found = true;
                    break;
                }
            }
            if (!found) {
                localPathText.select(0);
            }
        }

        Button browseBtn = new Button(pathComp, SWT.PUSH);
        browseBtn.setText("Browse...");
        browseBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                DirectoryDialog dialog = new DirectoryDialog(getShell());
                dialog.setText("Select Local Git Repository Directory");
                dialog.setFilterPath(localPathText.getText());
                String selected = dialog.open();
                if (selected != null) {
                    localPathText.setText(selected);
                }
            }
        });

        Button testBtn = new Button(container, SWT.PUSH);
        testBtn.setText("Test Connection");
        testBtn.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false, 2, 1));
        testBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                testConnection();
            }
        });

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
            infoDecorator.hide();
            setPageComplete(true);
            setErrorMessage(null);
            return;
        }

        if (repoUrlText.getText().isEmpty()) {
            infoDecorator.show();
        } else {
            infoDecorator.hide();
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

    private void testConnection() {
        String url = repoUrlText.getText();
        if (url == null || url.isEmpty() || url.equals("https://github.com/kalafatic/evolution/")) {
            org.eclipse.jface.dialogs.MessageDialog.openWarning(getShell(), "Git Test", "Please enter a valid repository URL first.");
            return;
        }

        Job job = new Job("Testing Git Connection") {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                try {
                    String user = usernameText.getText();
                    String pass = passwordText.getText();

                    // Construct URL with credentials if provided
                    String remoteUrl = url;
                    if (user != null && !user.isEmpty() && pass != null && !pass.isEmpty()) {
                         if (url.startsWith("https://")) {
                             remoteUrl = "https://" + java.net.URLEncoder.encode(user, "UTF-8") + ":" +
                                         java.net.URLEncoder.encode(pass, "UTF-8") + "@" + url.substring(8);
                         }
                    }

                    ProcessBuilder pb = new ProcessBuilder("git", "ls-remote", remoteUrl, "HEAD");
                    Process process = pb.start();
                    boolean success = (process.waitFor() == 0);

                    Display.getDefault().asyncExec(() -> {
                        if (success) {
                            org.eclipse.jface.dialogs.MessageDialog.openInformation(getShell(), "Git Test", "Connection successful!");
                        } else {
                            org.eclipse.jface.dialogs.MessageDialog.openError(getShell(), "Git Test", "Connection failed. Check URL and credentials.");
                        }
                    });
                } catch (Exception e) {
                    Display.getDefault().asyncExec(() -> {
                        org.eclipse.jface.dialogs.MessageDialog.openError(getShell(), "Git Test", "Error: " + e.getMessage());
                    });
                }
                return Status.OK_STATUS;
            }
        };
        job.setUser(true);
        job.schedule();
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
    public String getPassword() { return passwordText.getText(); }
    public String getLocalPath() { return localPathText.getText(); }
    public boolean isSkipped() { return skipCheck.getSelection(); }
}
