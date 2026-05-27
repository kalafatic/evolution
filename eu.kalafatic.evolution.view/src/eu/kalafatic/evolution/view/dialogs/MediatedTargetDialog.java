package eu.kalafatic.evolution.view.dialogs;

import java.io.File;
import java.util.LinkedHashMap;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;

import eu.kalafatic.evolution.controller.agents.MetadataAgent;
import eu.kalafatic.evolution.controller.agents.MetadataResult;
import eu.kalafatic.evolution.controller.manager.ProjectModelManager;
import eu.kalafatic.evolution.model.orchestration.ChatSession;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.utils.dialogs.DynamicField;
import eu.kalafatic.utils.dialogs.DynamicMapDialog;
import eu.kalafatic.utils.factories.GUIFactory;

/**
 * Dialog for selecting a mediated analysis target.
 */
public class MediatedTargetDialog extends DynamicMapDialog {
    private ChatSession session;
    private File projectRoot;
    private MultiPageEditor editor;

    private ProgressBar progressBar;
    private Label progressLabel;

    private static final String TARGET_PATH = "targetPath";
    private static final String TARGET_TYPE = "targetType";
    private static final String OUTPUT_PATH = "outputPath";

    public MediatedTargetDialog(Shell parentShell, ChatSession session, File projectRoot, MultiPageEditor editor) {
        super(parentShell, createFields(session, projectRoot));
        this.session = session;
        this.projectRoot = projectRoot;
        this.editor = editor;
        setTitle("Mediated Target Settings");
        setContainerWidth(700);
    }

    private static LinkedHashMap<String, DynamicField> createFields(ChatSession session, File projectRoot) {
        LinkedHashMap<String, DynamicField> fields = new LinkedHashMap<>();

        boolean selfDev = session != null && session.isSelfIterativeMode();
        String initialPath = session != null ? session.getTargetPath() : "";
        java.util.List<String> comboItems = new java.util.ArrayList<>();

        if (selfDev) {
            if (initialPath == null || initialPath.isEmpty()) {
                initialPath = ProjectModelManager.getInstance().findEvolutionRepository();
            }
            java.util.List<String> allRepos = ProjectModelManager.getInstance().getAvailableLocalRepositories();
            for (String repo : allRepos) {
                File repoDir = new File(repo);
                if (repoDir.getName().toLowerCase().startsWith("evo")) {
                    comboItems.add(repo);
                }
            }
        } else {
            if (initialPath == null || initialPath.isEmpty()) {
                initialPath = System.getProperty("user.home");
            }
            File userHome = new File(System.getProperty("user.home"));
            File[] files = userHome.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.isDirectory() && !f.getName().startsWith(".")) {
                        comboItems.add(f.getAbsolutePath());
                    }
                }
            }
            java.util.Collections.sort(comboItems);
        }

        fields.put(TARGET_PATH, new DynamicField("Target Path:", DynamicField.TYPE_COMBO | DynamicField.DIRECTORY, initialPath, comboItems));

        String initialType = session != null ? session.getTargetType() : "Project";
        fields.put(TARGET_TYPE, new DynamicField("Target Type:", DynamicField.TYPE_COMBO, initialType, "Project", "Folder", "PDF", "HTML", "Markdown"));

        String initialOutput = session != null ? session.getOutputPath() : "";
        if (initialOutput == null || initialOutput.isEmpty()) {
            initialOutput = getDefaultOutputPath();
        }
        fields.put(OUTPUT_PATH, new DynamicField("Output Path:", DynamicField.TYPE_TEXT | DynamicField.DIRECTORY, initialOutput));

        return fields;
    }

    @Override
    protected void createFieldEditor(Composite parent, String key, DynamicField field) {
        super.createFieldEditor(parent, key, field);

        // Add "Generate AI Metadata" button after the last field
        if (OUTPUT_PATH.equals(key)) {
            GUIFactory.INSTANCE.createLabel(parent, "Metadata:");
            Button syncBtn = GUIFactory.INSTANCE.createButton(parent, "Generate AI Metadata");
            GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
            gd.widthHint = GUIFactory.BUTTON_WIDTH * 2;
            syncBtn.setLayoutData(gd);
            syncBtn.setToolTipText("Generate AI Metadata sidecar files for the target project");

            new Label(parent, SWT.NONE); // Filler
            progressLabel = new Label(parent, SWT.NONE);
            progressLabel.setText("Ready");
            GridData labelGd = new GridData(GridData.FILL_HORIZONTAL);
            labelGd.exclude = true;
            progressLabel.setLayoutData(labelGd);
            progressLabel.setVisible(false);

            new Label(parent, SWT.NONE); // Filler
            progressBar = new ProgressBar(parent, SWT.HORIZONTAL | SWT.SMOOTH);
            GridData pbGd = new GridData(GridData.FILL_HORIZONTAL);
            pbGd.exclude = true;
            progressBar.setLayoutData(pbGd);
            progressBar.setVisible(false);

            syncBtn.addListener(SWT.Selection, e -> {
                String path = getString(TARGET_PATH);
                if (path != null && !path.isEmpty()) {
                    File root = new File(path);
                    if (root.exists() && root.isDirectory()) {
                        syncBtn.setEnabled(false);
                        progressLabel.setVisible(true);
                        ((GridData) progressLabel.getLayoutData()).exclude = false;
                        progressBar.setVisible(true);
                        ((GridData) progressBar.getLayoutData()).exclude = false;
                        parent.layout(true);

                        Job job = new Job("Generating AI Metadata") {
                            @Override
                            protected IStatus run(IProgressMonitor monitor) {
                                MetadataAgent generator = new MetadataAgent();
                                final MetadataResult result = generator.generate(root, new IProgressMonitor() {
                                    @Override public void beginTask(String name, int totalWork) {
                                        Display.getDefault().asyncExec(() -> {
                                            if (!progressBar.isDisposed()) {
                                                progressBar.setMaximum(totalWork);
                                                progressBar.setSelection(0);
                                                progressLabel.setText(name);
                                            }
                                        });
                                    }
                                    @Override public void done() {}
                                    @Override public void internalWorked(double work) {}
                                    @Override public boolean isCanceled() { return monitor.isCanceled(); }
                                    @Override public void setCanceled(boolean value) { monitor.setCanceled(value); }
                                    @Override public void setTaskName(String name) {
                                        Display.getDefault().asyncExec(() -> {
                                            if (!progressLabel.isDisposed()) progressLabel.setText(name);
                                        });
                                    }
                                    @Override public void subTask(String name) {
                                        Display.getDefault().asyncExec(() -> {
                                            if (!progressLabel.isDisposed()) progressLabel.setText(name);
                                        });
                                    }
                                    @Override public void worked(int work) {
                                        Display.getDefault().asyncExec(() -> {
                                            if (!progressBar.isDisposed()) progressBar.setSelection(progressBar.getSelection() + work);
                                        });
                                    }
                                });

                                Display.getDefault().asyncExec(() -> {
                                    if (syncBtn.isDisposed()) return;
                                    syncBtn.setEnabled(true);
                                    progressLabel.setVisible(false);
                                    ((GridData) progressLabel.getLayoutData()).exclude = true;
                                    progressBar.setVisible(false);
                                    ((GridData) progressBar.getLayoutData()).exclude = true;
                                    parent.layout(true);

                                    if (result != null && !monitor.isCanceled()) {
                                        MetadataResultDialog resDlg = new MetadataResultDialog(getShell(), result, projectRoot);
                                        resDlg.open();
                                    }
                                });
                                return Status.OK_STATUS;
                            }
                        };
                        job.setUser(true);
                        job.schedule();
                    }
                }
            });
        }
    }

    private static String findGitRoot(File root) {
        File current = root;
        while (current != null) {
            File gitDir = new File(current, ".git");
            if (gitDir.exists() && gitDir.isDirectory()) {
                return current.getAbsolutePath();
            }
            current = current.getParentFile();
        }
        return root.getAbsolutePath();
    }

    private static String getDefaultOutputPath() {
        File projectsDir = new File(System.getProperty("user.home"), "projects");
        File evoDir = new File(projectsDir, "evo");
        File targetDir = new File(evoDir, "target");
        return targetDir.getAbsolutePath() + File.separator;
    }

    @Override
    protected void okPressed() {
        if (!validate()) return;
        saveValues();

        if (session != null) {
            session.setTargetPath(getString(TARGET_PATH));
            session.setTargetType(getString(TARGET_TYPE));
            session.setOutputPath(getString(OUTPUT_PATH));
        }
        if (editor != null) {
            editor.setDirty(true);
        }
        super.okPressed();
    }

    public String getSelectedPath() {
        return session != null ? session.getTargetPath() : null;
    }

    public String getSelectedType() {
        return session != null ? session.getTargetType() : null;
    }
}
