package eu.kalafatic.evolution.view.wizards;

import java.io.File;
import java.net.URL;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jgit.api.Git;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;

import eu.kalafatic.evolution.controller.manager.ProjectModelManager;
import eu.kalafatic.evolution.controller.orchestration.selfdev.EclipseGitRepositoryManager;
import eu.kalafatic.evolution.controller.orchestration.selfdev.GitManager;
import eu.kalafatic.utils.factories.GUIFactory;

public class GitSettingsPage extends AWizardPage {

	private Text repoUrlText, branchText, usernameText, passwordText;
	private Text repoUrlText1, branchText1, usernameText1, passwordText1;
	private Combo localPathText;
	private Combo localPathText1;
	private Button skipCheck;
	private ControlDecoration gitDecorator, infoDecorator;
	private ControlDecoration gitDecorator1, infoDecorator1;
	private Job validationJob;

	public GitSettingsPage() {
		super("GitSettingsPage");
		setTitle("Git Settings");
		setDescription("Configure Git repository settings.");
	}

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);

		if (visible && orchestrator != null && orchestrator.getGit() != null
				&& orchestrator.getSupervisorSettings().getGit() != null) {
			try {
				eu.kalafatic.evolution.model.orchestration.Git git = orchestrator.getGit();

				if (git.getRepositoryUrl() != null)
					repoUrlText.setText(git.getRepositoryUrl());
				if (git.getBranch() != null)
					branchText.setText(git.getBranch());
				if (git.getUsername() != null)
					usernameText.setText(git.getUsername());
				if (git.getPassword() != null)
					passwordText.setText(git.getPassword());
				if (git.getLocalPath() != null) {
					String lp = git.getLocalPath();
					if (localPathText.indexOf(lp) < 0) {
						localPathText.add(lp);
					}
					localPathText.setText(lp);
				}
				eu.kalafatic.evolution.model.orchestration.Git git1 = orchestrator.getSupervisorSettings().getGit();

				if (git.getRepositoryUrl() != null)
					repoUrlText1.setText(git1.getRepositoryUrl());
				if (git.getBranch() != null)
					branchText1.setText(git1.getBranch());
				if (git1.getUsername() != null)
					usernameText1.setText(git1.getUsername());
				if (git1.getPassword() != null)
					passwordText1.setText(git1.getPassword());
				if (git1.getLocalPath() != null) {
					String lp = git1.getLocalPath();
					if (localPathText1.indexOf(lp) < 0) {
						localPathText1.add(lp);
					}
					localPathText.setText(lp);
				}

				GitManager.createOrOpenRepo(repoUrlText.getText());
				GitManager.createOrOpenRepo(repoUrlText1.getText());

				setPageComplete(true);

			} catch (Exception e) {
				setPageComplete(false);
				e.printStackTrace();
			}
		}
	}

	@Override
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(1, false));

		Group groupCodebase = GUIFactory.INSTANCE.createGroup(container, "Codebase", 3);

		GUIFactory.INSTANCE.createLabel(groupCodebase, "Repository URL:");
		repoUrlText = new Text(groupCodebase, SWT.BORDER);
		repoUrlText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		repoUrlText.setText(GitManager.DEFAULT_GIT_URL);
		repoUrlText.setEditable(false);

		gitDecorator = new ControlDecoration(repoUrlText, SWT.TOP | SWT.LEFT);
		gitDecorator.setImage(
				FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
		gitDecorator.hide();

		infoDecorator = new ControlDecoration(repoUrlText, SWT.TOP | SWT.LEFT);
		infoDecorator.setImage(FieldDecorationRegistry.getDefault()
				.getFieldDecoration(FieldDecorationRegistry.DEC_INFORMATION).getImage());
		infoDecorator
				.setDescriptionText("Leave empty to initialize a local Git repository with a standard .gitignore.");
		infoDecorator.setShowOnlyOnFocus(false);

		repoUrlText.addModifyListener(e -> validateGit());
		GUIFactory.INSTANCE.createLabel(groupCodebase);

		GUIFactory.INSTANCE.createLabel(groupCodebase, "Branch:");
		branchText = new Text(groupCodebase, SWT.BORDER);
		branchText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		branchText.setText("master");
		GUIFactory.INSTANCE.createLabel(groupCodebase);

		GUIFactory.INSTANCE.createLabel(groupCodebase, "Username:");
		usernameText = new Text(groupCodebase, SWT.BORDER);
		usernameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		usernameText.setText("admin");
		GUIFactory.INSTANCE.createLabel(groupCodebase);

		GUIFactory.INSTANCE.createLabel(groupCodebase, "Password/Token:");
		passwordText = new Text(groupCodebase, SWT.BORDER | SWT.PASSWORD);
		passwordText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		GUIFactory.INSTANCE.createLabel(groupCodebase);

		GUIFactory.INSTANCE.createLabel(groupCodebase, "Local Path:");
		Composite pathComp = new Composite(groupCodebase, SWT.NONE);
		pathComp.setLayout(new GridLayout(2, false));
		pathComp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		localPathText = new Combo(pathComp, SWT.BORDER | SWT.DROP_DOWN);
		localPathText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		List<String> repos = ProjectModelManager.getInstance().getAvailableLocalRepositories();

		String localRepo = GitManager.getDefaultRepositoryPath() +  "evolution";

		if (!repos.contains(localRepo)) {
			repos.add(localRepo);
		}
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

		Button testBtn = new Button(groupCodebase, SWT.PUSH);
		testBtn.setText("Test Connection");
		testBtn.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false, 2, 1));
		testBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				testConnection();
			}
		});

		Group groupEvo = GUIFactory.INSTANCE.createGroup(container, "Evo", 3);

		GUIFactory.INSTANCE.createLabel(groupEvo, "Repository URL:");
		repoUrlText1 = new Text(groupEvo, SWT.BORDER);
		repoUrlText1.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		repoUrlText1.setText(EclipseGitRepositoryManager.getRuntimeWorkspacePath() + "evo");
		repoUrlText1.setEditable(true);

		gitDecorator1 = new ControlDecoration(repoUrlText1, SWT.TOP | SWT.LEFT);
		gitDecorator1.setImage(
				FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
		gitDecorator1.hide();

		infoDecorator1 = new ControlDecoration(repoUrlText1, SWT.TOP | SWT.LEFT);
		infoDecorator1.setImage(FieldDecorationRegistry.getDefault()
				.getFieldDecoration(FieldDecorationRegistry.DEC_INFORMATION).getImage());
		infoDecorator1
				.setDescriptionText("Leave empty to initialize a local Git repository with a standard .gitignore.");
		infoDecorator1.setShowOnlyOnFocus(false);

		repoUrlText1.addModifyListener(e -> validateGit());
		GUIFactory.INSTANCE.createLabel(groupEvo);

		GUIFactory.INSTANCE.createLabel(groupEvo, "Branch:");
		branchText1 = new Text(groupEvo, SWT.BORDER);
		branchText1.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		branchText1.setText("master");
		GUIFactory.INSTANCE.createLabel(groupEvo);

		GUIFactory.INSTANCE.createLabel(groupEvo, "Username:");
		usernameText1 = new Text(groupEvo, SWT.BORDER);
		usernameText1.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		usernameText1.setText("admin");
		GUIFactory.INSTANCE.createLabel(groupEvo);

		GUIFactory.INSTANCE.createLabel(groupEvo, "Password/Token:");
		passwordText1 = new Text(groupEvo, SWT.BORDER | SWT.PASSWORD);
		passwordText1.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		GUIFactory.INSTANCE.createLabel(groupEvo);

		GUIFactory.INSTANCE.createLabel(groupEvo, "Local Path:");
		pathComp = new Composite(groupEvo, SWT.NONE);
		pathComp.setLayout(new GridLayout(2, false));
		pathComp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		GUIFactory.INSTANCE.createLabel(groupEvo);

		localPathText1 = new Combo(pathComp, SWT.BORDER | SWT.DROP_DOWN);
		localPathText1.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		repos = ProjectModelManager.getInstance().getAvailableLocalRepositories();

		// IProject project = wizard.g;
		localRepo = EclipseGitRepositoryManager.getRuntimeWorkspacePath() + "evo";

		if (!repos.contains(localRepo)) {
			repos.add(localRepo);
		}

		for (String r : repos) {
			localPathText1.add(r);
		}

		// Default selection logic: evolution or evo
		if (repos.isEmpty()) {
			localPathText1.setText("repo");
		} else {
			boolean found = false;
			for (int i = 0; i < localPathText1.getItemCount(); i++) {
				String item = localPathText1.getItem(i).toLowerCase();
				if (item.contains("evolution") || item.contains("/evo")) {
					localPathText1.select(i);
					found = true;
					break;
				}
			}
			if (!found) {
				localPathText1.select(0);
			}
		}

		browseBtn = new Button(pathComp, SWT.PUSH);
		browseBtn.setText("Browse...");
		browseBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				DirectoryDialog dialog = new DirectoryDialog(getShell());
				dialog.setText("Select Local Git Repository Directory");
				dialog.setFilterPath(localPathText1.getText());
				String selected = dialog.open();
				if (selected != null) {
					localPathText1.setText(selected);
				}
			}
		});

		testBtn = new Button(groupEvo, SWT.PUSH);
		testBtn.setText("Test Connection");
		testBtn.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false, 2, 1));
		testBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				testConnection();
			}
		});
		GUIFactory.INSTANCE.createLabel(groupEvo);
		GUIFactory.INSTANCE.createLabel(groupEvo);

		Group guidance = GUIFactory.INSTANCE.createGroup(container, "Guidance", 2);

		skipCheck = new Button(guidance, SWT.CHECK);
		skipCheck.setText("Skip this step and setup later");
		skipCheck.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, true, false, 2, 1));
		skipCheck.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				validateGit();
			}
		});

		Link gitHelpLink = new Link(guidance, SWT.NONE);
		gitHelpLink.setText("<a>How to install Git?</a>");
		gitHelpLink.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false, 2, 1));
		gitHelpLink.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				openUrl("https://git-scm.com/downloads");
			}
		});

		setControl(container);
//		validateGit();
//		validateGit1();

	}

	private void validateGit() {
		if (skipCheck.getSelection()) {
			if (validationJob != null)
				validationJob.cancel();
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

		if (validationJob != null)
			validationJob.cancel();
		validationJob = new Job("Validate Git") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				boolean success = false;
				try {

					Process process = new ProcessBuilder("git", "--version").start();
					success = (process.waitFor() == 0);
				} catch (Exception e) {
				}

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

	private void validateGit1() {
		if (skipCheck.getSelection()) {
			if (validationJob != null)
				validationJob.cancel();
			gitDecorator1.hide();
			infoDecorator1.hide();
			setPageComplete(true);
			setErrorMessage(null);
			return;
		}

		if (repoUrlText1.getText().isEmpty()) {
			infoDecorator1.show();
		} else {
			infoDecorator1.hide();
		}

		if (validationJob != null)
			validationJob.cancel();
		validationJob = new Job("Validate Git") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				boolean success = false;
				try {

					Process process = new ProcessBuilder("git", "--version").start();
					success = (process.waitFor() == 0);
				} catch (Exception e) {
				}

				final boolean finalSuccess = success;
				Display.getDefault().asyncExec(() -> {
					if (finalSuccess) {
						gitDecorator1.hide();
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
			org.eclipse.jface.dialogs.MessageDialog.openWarning(getShell(), "Git Test",
					"Please enter a valid repository URL first.");
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
							remoteUrl = "https://" + java.net.URLEncoder.encode(user, "UTF-8") + ":"
									+ java.net.URLEncoder.encode(pass, "UTF-8") + "@" + url.substring(8);
						}
					}

					ProcessBuilder pb = new ProcessBuilder("git", "ls-remote", remoteUrl, "HEAD");
					Process process = pb.start();
					boolean success = (process.waitFor() == 0);

					Display.getDefault().asyncExec(() -> {
						if (success) {
							org.eclipse.jface.dialogs.MessageDialog.openInformation(getShell(), "Git Test",
									"Connection successful!");
						} else {
							org.eclipse.jface.dialogs.MessageDialog.openError(getShell(), "Git Test",
									"Connection failed. Check URL and credentials.");
						}
					});
				} catch (Exception e) {
					Display.getDefault().asyncExec(() -> {
						org.eclipse.jface.dialogs.MessageDialog.openError(getShell(), "Git Test",
								"Error: " + e.getMessage());
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
			support.createBrowser(IWorkbenchBrowserSupport.AS_EDITOR, "evoGitHelp", "Git Help", "Git Help")
					.openURL(new URL(url));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public String getRepoUrl() {
		return repoUrlText.getText();
	}

	public String getBranch() {
		return branchText.getText();
	}

	public String getUsername() {
		return usernameText.getText();
	}

	public String getPassword() {
		return passwordText.getText();
	}

	public String getLocalPath() {
		return localPathText.getText();
	}

	public String getRepoUrl1() {
		return repoUrlText1.getText();
	}

	public String getBranch1() {
		return branchText.getText();
	}

	public String getUsername1() {
		return usernameText1.getText();
	}

	public String getPassword1() {
		return passwordText1.getText();
	}

	public String getLocalPath1() {
		return localPathText1.getText();
	}

	public boolean isSkipped() {
		return skipCheck.getSelection();
	}
}
