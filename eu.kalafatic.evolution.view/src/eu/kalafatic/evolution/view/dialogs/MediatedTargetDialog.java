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
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;

import eu.kalafatic.evolution.controller.agents.MetadataAgent;
import eu.kalafatic.evolution.controller.agents.MetadataResult;
import eu.kalafatic.evolution.controller.discovery.SourceDiscoveryResult;
import eu.kalafatic.evolution.controller.manager.ProjectModelManager;
import eu.kalafatic.evolution.forge.model.target.ForgeTarget;
import eu.kalafatic.evolution.forge.model.target.ForgeTargetDetector;
import eu.kalafatic.evolution.forge.model.target.ForgeTargetType;
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
    private Label typeLabel;
    private Label statusLabel;

    private static final String TARGET_PATH = "targetPath";
    private static final String TARGET_TYPE = "targetType";
    private static final String OUTPUT_PATH = "outputPath";
    private static final String SKIP_METADATA = "skipMetadata";
    private static final String CLEAN_METADATA = "cleanMetadata";
    private static final String TIMESTAMP_METADATA = "timestampMetadata";

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

        String initialPath = session != null ? session.getTargetPath() : "";

        // Use Discovery to obtain all target paths
        SourceDiscoveryResult discovery = ProjectModelManager.getInstance().getOrDiscoverWorkspace();
        java.util.List<String> comboItems = new java.util.ArrayList<>();

        if (discovery.getPrimaryRepository() != null) {
            comboItems.add(discovery.getPrimaryRepository().getAbsolutePath());
        }

        for (File repo : discovery.getGitRepositories()) {
            String path = repo.getAbsolutePath();
            if (!comboItems.contains(path)) comboItems.add(path);
        }

        for (File pRoot : discovery.getProjectRoots()) {
            String path = pRoot.getAbsolutePath();
            if (!comboItems.contains(path)) comboItems.add(path);
        }

        if (initialPath == null || initialPath.isEmpty()) {
            initialPath = discovery.getPrimaryRepository() != null ?
                         discovery.getPrimaryRepository().getAbsolutePath() :
                         System.getProperty("user.home");
        }

        fields.put(TARGET_PATH, new DynamicField("Target Path:", DynamicField.TYPE_COMBO | DynamicField.DIRECTORY, initialPath, comboItems));

        String initialType = session != null ? session.getTargetType() : "Project";
        fields.put(TARGET_TYPE, new DynamicField("Target Type:", DynamicField.TYPE_COMBO, initialType, "Project", "Folder", "PDF", "HTML", "Markdown", "EVO Model", "EVO Workspace"));

        String initialOutput = session != null ? session.getOutputPath() : "";
        if (initialOutput == null || initialOutput.isEmpty()) {
            initialOutput = getDefaultOutputPath();
        }
        fields.put(OUTPUT_PATH, new DynamicField("Output Path:", DynamicField.TYPE_TEXT | DynamicField.DIRECTORY, initialOutput));

        fields.put(SKIP_METADATA, new DynamicField("Skip Existing:", DynamicField.TYPE_CHECKBOX, false));
        fields.put(CLEAN_METADATA, new DynamicField("Clean Target:", DynamicField.TYPE_CHECKBOX, false));
        fields.put(TIMESTAMP_METADATA, new DynamicField("Add Timestamp:", DynamicField.TYPE_CHECKBOX, false));

        return fields;
    }

    @Override
    protected void createFieldEditor(Composite parent, String key, DynamicField field) {
        super.createFieldEditor(parent, key, field);

        if (TARGET_PATH.equals(key)) {
            // Add File Browse button alongside directory browse button
            Control ctrl = controls.get(TARGET_PATH);
            if (ctrl instanceof Composite) {
                Composite comp = (Composite) ctrl;
                Button browseFileBtn = new Button(comp, SWT.PUSH);
                browseFileBtn.setText("Browse File...");
                browseFileBtn.addListener(SWT.Selection, e -> {
                    org.eclipse.swt.widgets.FileDialog dlg = new org.eclipse.swt.widgets.FileDialog(getShell(), SWT.OPEN);
                    dlg.setFilterExtensions(new String[] { "*.evo;*.txt;*.pdf;*.html;*.json;*.md;*.csv", "*.*" });
                    dlg.setFilterNames(new String[] { "EVO Model & Data Files (*.evo, *.txt, *.pdf, etc.)", "All Files (*.*)" });
                    String sel = dlg.open();
                    if (sel != null && !sel.isEmpty()) {
                        for (Control child : comp.getChildren()) {
                            if (child instanceof Combo) {
                                ((Combo) child).setText(sel);
                            } else if (child instanceof org.eclipse.swt.widgets.Text) {
                                ((org.eclipse.swt.widgets.Text) child).setText(sel);
                            }
                        }
                    }
                });
                comp.layout(true);
            }

            // Target Inspection status panel
            new Label(parent, SWT.NONE); // Filler
            Composite statusComp = new Composite(parent, SWT.NONE);
            statusComp.setLayout(new org.eclipse.swt.layout.GridLayout(2, false));
            statusComp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            Label typeHeader = new Label(statusComp, SWT.NONE);
            typeHeader.setText("Detected Target Type:");
            typeLabel = new Label(statusComp, SWT.NONE);
            typeLabel.setText("Training Data");
            typeLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            Label statusHeader = new Label(statusComp, SWT.NONE);
            statusHeader.setText("Target Status:");
            statusLabel = new Label(statusComp, SWT.NONE);
            statusLabel.setText("Ready");
            statusLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            if (ctrl instanceof Composite) {
                for (Control child : ((Composite) ctrl).getChildren()) {
                    if (child instanceof Combo) {
                        Combo combo = (Combo) child;
                        combo.addModifyListener(e -> updateTargetStatus(combo.getText()));
                    } else if (child instanceof org.eclipse.swt.widgets.Text) {
                        org.eclipse.swt.widgets.Text text = (org.eclipse.swt.widgets.Text) child;
                        text.addModifyListener(e -> updateTargetStatus(text.getText()));
                    }
                }
            }
            updateTargetStatus(getString(TARGET_PATH));
        }

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
                                MetadataAgent.Options options = new MetadataAgent.Options();
                                options.skipExisting = getBoolean(SKIP_METADATA);
                                options.cleanExisting = getBoolean(CLEAN_METADATA);
                                options.useTimestamp = getBoolean(TIMESTAMP_METADATA);

                                final MetadataResult result = generator.generate(root, options, new IProgressMonitor() {
                                    private long lastUpdate = 0;
                                    private int pendingWork = 0;

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
                                        updateSubTask(name);
                                    }
                                    @Override public void subTask(String name) {
                                        updateSubTask(name);
                                    }
                                    @Override public void worked(int work) {
                                        pendingWork += work;
                                        long now = System.currentTimeMillis();
                                        if (now - lastUpdate > 100) { // Throttle UI updates to 10Hz
                                            int toReport = pendingWork;
                                            pendingWork = 0;
                                            lastUpdate = now;
                                            Display.getDefault().asyncExec(() -> {
                                                if (!progressBar.isDisposed()) progressBar.setSelection(progressBar.getSelection() + toReport);
                                            });
                                        }
                                    }
                                    private void updateSubTask(String name) {
                                        long now = System.currentTimeMillis();
                                        if (now - lastUpdate > 100) {
                                            Display.getDefault().asyncExec(() -> {
                                                if (!progressLabel.isDisposed()) progressLabel.setText(name);
                                            });
                                        }
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

    private void updateTargetStatus(String pathStr) {
        if (typeLabel == null || statusLabel == null || typeLabel.isDisposed() || statusLabel.isDisposed()) return;
        ForgeTarget target = ForgeTargetDetector.detect(pathStr);
        String typeName = target.getType().name().replace("_", " ");
        typeLabel.setText(typeName);
        statusLabel.setText(target.getStatusMessage());

        Control typeControl = controls.get(TARGET_TYPE);
        if (typeControl instanceof Combo) {
            Combo combo = (Combo) typeControl;
            if (target.getType() == ForgeTargetType.EVO_MODEL) {
                combo.setText("EVO Model");
            } else if (target.getType() == ForgeTargetType.EVO_WORKSPACE) {
                combo.setText("EVO Workspace");
            }
        }
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
