package eu.kalafatic.evolution.view.dialogs;

import java.io.File;
import java.util.LinkedHashMap;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

import eu.kalafatic.evolution.controller.agents.MetadataAgent;
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

        String initialPath = session != null ? session.getTargetPath() : "";
        if ((initialPath == null || initialPath.isEmpty()) && projectRoot != null) {
            initialPath = findGitRoot(projectRoot);
        }
        fields.put(TARGET_PATH, new DynamicField("Target Path:", DynamicField.TYPE_TEXT | DynamicField.DIRECTORY, initialPath));

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
            syncBtn.setToolTipText("Generate AI Metadata sidecar files for the target project");
            syncBtn.addListener(SWT.Selection, e -> {
                String path = getString(TARGET_PATH);
                if (path != null && !path.isEmpty()) {
                    File root = new File(path);
                    if (root.exists() && root.isDirectory()) {
                        MetadataAgent generator = new MetadataAgent();
                        generator.generate(root);
                        MessageBox mb = new MessageBox(getShell(), SWT.ICON_INFORMATION | SWT.OK);
                        mb.setText("Metadata Generation");
                        mb.setMessage("AI Metadata generation completed for: " + path);
                        mb.open();
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
