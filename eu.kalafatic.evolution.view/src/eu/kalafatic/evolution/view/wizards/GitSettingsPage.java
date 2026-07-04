package eu.kalafatic.evolution.view.wizards;

import java.net.URL;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
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
import eu.kalafatic.evolution.controller.tools.EclipseGitEvoTool;
import eu.kalafatic.evolution.controller.tools.EclipseGitEvoTool.GitOpResult;
import eu.kalafatic.utils.factories.GUIFactory;

public class GitSettingsPage extends AWizardPage {

	private Combo repoSelector;
	private Text repoUrlText, branchText, usernameText, passwordText;
	private Combo localPathCombo;
	private ControlDecoration urlDecorator, pathDecorator;
	private Button skipCheck;
	private Job validationJob;

	private String currentRepoId = EclipseGitEvoTool.REPO_EVOLUTION;

	public GitSettingsPage() {
		super("GitSettingsPage");
		setTitle("Git Settings");
		setDescription("Configure Git repository settings for the EVO platform.");
	}

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {
			loadRepoSettings(currentRepoId);
			validateForm();
		}
	}

	private void loadRepoSettings(String id) {
		repoUrlText.setText(nonNull(EclipseGitEvoTool.getRepositoryRemote(id)));
		branchText.setText(nonNull(EclipseGitEvoTool.getRepositoryBranch(id)));
		usernameText.setText(nonNull(EclipseGitEvoTool.getRepositoryUsername(id)));
		passwordText.setText(nonNull(EclipseGitEvoTool.getRepositoryPassword(id)));

		String path = EclipseGitEvoTool.getRepositoryPath(id);
		if (path != null) {
			if (localPathCombo.indexOf(path) < 0) localPathCombo.add(path);
			localPathCombo.setText(path);
		}
	}

	private void saveCurrentSettings() {
		EclipseGitEvoTool.changeRemoteUrl(currentRepoId, repoUrlText.getText());
		EclipseGitEvoTool.changeRepositoryLocation(currentRepoId, localPathCombo.getText());
		EclipseGitEvoTool.changeBranch(currentRepoId, branchText.getText());
		EclipseGitEvoTool.changeCredentials(currentRepoId, usernameText.getText(), passwordText.getText());
	}

	@Override
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(1, false));

		Group selectorGroup = GUIFactory.INSTANCE.createGroup(container, "Repository Selection", 2);
		GUIFactory.INSTANCE.createLabel(selectorGroup, "Select Repository:");
		repoSelector = new Combo(selectorGroup, SWT.READ_ONLY | SWT.DROP_DOWN);
		repoSelector.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		for (String id : EclipseGitEvoTool.getRegisteredRepositoryIds()) {
			repoSelector.add(id);
		}
		repoSelector.select(0);
		repoSelector.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				saveCurrentSettings();
				currentRepoId = repoSelector.getText();
				loadRepoSettings(currentRepoId);
				validateForm();
			}
		});

		Group settingsGroup = GUIFactory.INSTANCE.createGroup(container, "Git Configuration", 3);

		GUIFactory.INSTANCE.createLabel(settingsGroup, "Repository URL:");
		repoUrlText = new Text(settingsGroup, SWT.BORDER);
		repoUrlText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		urlDecorator = createErrorDecorator(repoUrlText, "URL cannot be empty");
		repoUrlText.addModifyListener(e -> validateForm());
		GUIFactory.INSTANCE.createLabel(settingsGroup, "");

		GUIFactory.INSTANCE.createLabel(settingsGroup, "Branch:");
		branchText = new Text(settingsGroup, SWT.BORDER);
		branchText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		GUIFactory.INSTANCE.createLabel(settingsGroup, "");

		GUIFactory.INSTANCE.createLabel(settingsGroup, "Username:");
		usernameText = new Text(settingsGroup, SWT.BORDER);
		usernameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		GUIFactory.INSTANCE.createLabel(settingsGroup, "");

		GUIFactory.INSTANCE.createLabel(settingsGroup, "Password/Token:");
		passwordText = new Text(settingsGroup, SWT.BORDER | SWT.PASSWORD);
		passwordText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		GUIFactory.INSTANCE.createLabel(settingsGroup, "");

		GUIFactory.INSTANCE.createLabel(settingsGroup, "Local Path:");
		localPathCombo = new Combo(settingsGroup, SWT.BORDER | SWT.DROP_DOWN);
		localPathCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		pathDecorator = createErrorDecorator(localPathCombo, "Path cannot be empty");
		localPathCombo.addModifyListener(e -> validateForm());

		for (String r : ProjectModelManager.getInstance().getAvailableLocalRepositories()) {
			localPathCombo.add(r);
		}

		Button browseBtn = new Button(settingsGroup, SWT.PUSH);
		browseBtn.setText("Browse...");
		browseBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				DirectoryDialog dialog = new DirectoryDialog(getShell());
				dialog.setText("Select Local Git Repository Directory");
				String selected = dialog.open();
				if (selected != null) localPathCombo.setText(selected);
			}
		});

		Composite btnComp = new Composite(container, SWT.NONE);
		btnComp.setLayout(new GridLayout(2, false));

		Button testBtn = new Button(btnComp, SWT.PUSH);
		testBtn.setText("Test Connection");
		testBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) { testConnection(); }
		});

		skipCheck = new Button(container, SWT.CHECK);
		skipCheck.setText("Skip validation and setup later");
		skipCheck.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) { validateForm(); }
		});

		Link gitHelpLink = new Link(container, SWT.NONE);
		gitHelpLink.setText("<a>How to install Git?</a>");
		gitHelpLink.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) { openUrl("https://git-scm.com/downloads"); }
		});

		setControl(container);
	}

	private ControlDecoration createErrorDecorator(org.eclipse.swt.widgets.Control control, String description) {
		ControlDecoration dec = new ControlDecoration(control, SWT.TOP | SWT.LEFT);
		dec.setImage(FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
		dec.setDescriptionText(description);
		dec.hide();
		return dec;
	}

	private void validateForm() {
		boolean ok = true;
		if (!skipCheck.getSelection()) {
			if (repoUrlText.getText().isEmpty()) { urlDecorator.show(); ok = false; } else urlDecorator.hide();
			if (localPathCombo.getText().isEmpty()) { pathDecorator.show(); ok = false; } else pathDecorator.hide();
		} else {
			urlDecorator.hide();
			pathDecorator.hide();
		}

		setPageComplete(ok);
		if (ok) saveCurrentSettings();
	}

	private void testConnection() {
		String url = repoUrlText.getText();
		if (url.isEmpty()) return;

		Job job = new Job("Testing Git Connection") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					ProcessBuilder pb = new ProcessBuilder("git", "ls-remote", url, "HEAD");
					boolean success = (pb.start().waitFor() == 0);
					Display.getDefault().asyncExec(() -> {
						if (success) org.eclipse.jface.dialogs.MessageDialog.openInformation(getShell(), "Git Test", "Connection successful!");
						else org.eclipse.jface.dialogs.MessageDialog.openError(getShell(), "Git Test", "Connection failed.");
					});
				} catch (Exception e) {}
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
		} catch (Exception e) {}
	}

	public String getRepoUrl() {
		return EclipseGitEvoTool.getRepositoryRemote(EclipseGitEvoTool.REPO_EVOLUTION);
	}

	public String getBranch() {
		return branchText.getText(); // Temporary: wizard handles branch separately
	}

	public String getUsername() {
		return usernameText.getText();
	}

	public String getPassword() {
		return passwordText.getText();
	}

	public String getLocalPath() {
		return EclipseGitEvoTool.getRepositoryPath(EclipseGitEvoTool.REPO_EVOLUTION);
	}

	public String getRepoUrl1() {
		return EclipseGitEvoTool.getRepositoryRemote(EclipseGitEvoTool.REPO_WORKSPACE);
	}

	public String getBranch1() {
		return branchText.getText();
	}

	public String getUsername1() {
		return usernameText.getText();
	}

	public String getPassword1() {
		return passwordText.getText();
	}

	public String getLocalPath1() {
		return EclipseGitEvoTool.getRepositoryPath(EclipseGitEvoTool.REPO_WORKSPACE);
	}

	public boolean isSkipped() {
		return skipCheck.getSelection();
	}

	private String nonNull(String s) { return s == null ? "" : s; }
}
