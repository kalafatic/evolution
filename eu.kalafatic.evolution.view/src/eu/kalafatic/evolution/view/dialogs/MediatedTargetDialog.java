package eu.kalafatic.evolution.view.dialogs;

import java.io.File;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import eu.kalafatic.evolution.controller.mediation.analysis.MetadataGenerator;
import eu.kalafatic.evolution.model.orchestration.ChatSession;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.utils.factories.GUIFactory;

/**
 * Dialog for selecting a mediated analysis target.
 */
public class MediatedTargetDialog extends Dialog {
    private ChatSession session;
    private File projectRoot;
    private MultiPageEditor editor;

    private Text pathText;
    private Combo typeCombo;
    private Text outputText;

    public MediatedTargetDialog(Shell parentShell) {
        super(parentShell);
    }

    public MediatedTargetDialog(Shell parentShell, ChatSession session, File projectRoot, MultiPageEditor editor) {
        super(parentShell);
        this.session = session;
        this.projectRoot = projectRoot;
        this.editor = editor;
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText("Mediated Target Settings");
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite container = (Composite) super.createDialogArea(parent);
        container.setLayout(new GridLayout(3, false));
        GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        gd.widthHint = 700;
        container.setLayoutData(gd);

        GUIFactory.INSTANCE.createLabel(container, "Target Path:");
        String initialPath = session != null ? session.getTargetPath() : null;
        pathText = GUIFactory.INSTANCE.createText(container, initialPath != null ? initialPath : "", true);
        if (pathText.getText().isEmpty() && projectRoot != null) {
            pathText.setText(findGitRoot(projectRoot));
        }

        Button browseTargetBtn = GUIFactory.INSTANCE.createButton(container, "Browse...");
        browseTargetBtn.addListener(SWT.Selection, e -> {
            DirectoryDialog dlg = new DirectoryDialog(getShell());
            dlg.setFilterPath(pathText.getText());
            String path = dlg.open();
            if (path != null) pathText.setText(path);
        });

        GUIFactory.INSTANCE.createLabel(container, "Target Type:");
        typeCombo = GUIFactory.INSTANCE.createCombo(container, (Object[]) new String[] {"Project", "Folder", "PDF", "HTML", "Markdown"});
        String initialType = session != null ? session.getTargetType() : null;
        if (initialType != null && !initialType.isEmpty()) {
            typeCombo.setText(initialType);
        } else {
            typeCombo.select(0);
        }
        // Fill the 3rd column
        new Label(container, SWT.NONE);

        GUIFactory.INSTANCE.createLabel(container, "Output Path:");
        String initialOutput = session != null ? session.getOutputPath() : null;
        outputText = GUIFactory.INSTANCE.createText(container, initialOutput != null ? initialOutput : "", true);
        if (outputText.getText().isEmpty()) {
            outputText.setText(getDefaultOutputPath());
        }

        Button browseOutputBtn = GUIFactory.INSTANCE.createButton(container, "Browse...");
        browseOutputBtn.addListener(SWT.Selection, e -> {
            DirectoryDialog dlg = new DirectoryDialog(getShell());
            dlg.setFilterPath(outputText.getText());
            String path = dlg.open();
            if (path != null) outputText.setText(path);
        });

        // Metadata generation button
        new Label(container, SWT.NONE);
        Button generateMetaBtn = GUIFactory.INSTANCE.createButton(container, "Generate AI Metadata", SWT.PUSH);
        generateMetaBtn.setToolTipText("Generate .ai.json sidecar files for the target project");
        generateMetaBtn.addListener(SWT.Selection, e -> {
            String path = pathText.getText();
            if (path != null && !path.isEmpty()) {
                File root = new File(path);
                if (root.exists() && root.isDirectory()) {
                    MetadataGenerator generator = new MetadataGenerator();
                    generator.generate(root);
                    MessageBox box = new MessageBox(getShell(), SWT.ICON_INFORMATION | SWT.OK);
                    box.setText("Metadata Generation");
                    box.setMessage("AI Metadata generation completed for: " + path);
                    box.open();
                }
            }
        });
        new Label(container, SWT.NONE);

        return container;
    }

    private String findGitRoot(File root) {
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

    private String getDefaultOutputPath() {
        File projectsDir = new File(System.getProperty("user.home"), "projects");
        File evoDir = new File(projectsDir, "evo");
        File targetDir = new File(evoDir, "target");
        return targetDir.getAbsolutePath() + File.separator;
    }

    @Override
    protected void okPressed() {
        if (session != null) {
            session.setTargetPath(pathText.getText());
            session.setTargetType(typeCombo.getText());
            session.setOutputPath(outputText.getText());
        }
        if (editor != null) {
            editor.setDirty(true);
        }
        super.okPressed();
    }

    @Override
    protected boolean isResizable() {
        return true;
    }

    public String getSelectedPath() {
        return session != null ? session.getTargetPath() : null;
    }

    public String getSelectedType() {
        return session != null ? session.getTargetType() : null;
    }
}
