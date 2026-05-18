package eu.kalafatic.evolution.view.dialogs;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import eu.kalafatic.evolution.controller.agents.MetadataAgent;
import eu.kalafatic.evolution.controller.agents.MetadataResult;
import eu.kalafatic.utils.factories.GUIFactory;

/**
 * Dialog for displaying metadata generation results and providing actions.
 */
public class MetadataResultDialog extends TitleAreaDialog {

    private final MetadataResult result;
    private final File projectRoot;
    private Table table;
    private Text summaryText;
    private Text prefixText;

    public MetadataResultDialog(Shell parentShell, MetadataResult result, File projectRoot) {
        super(parentShell);
        this.result = result;
        this.projectRoot = projectRoot;
        setShellStyle(getShellStyle() | SWT.RESIZE);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite area = (Composite) super.createDialogArea(parent);
        Composite container = new Composite(area, SWT.NONE);
        container.setLayout(new GridLayout(1, false));
        container.setLayoutData(new GridData(GridData.FILL_BOTH));

        setTitle("Metadata Generation Results");
        setMessage("Review the generated AI metadata and select actions.");

        // Summary Text
        GUIFactory.INSTANCE.createLabel(container, "Analysis Summary:");
        summaryText = GUIFactory.INSTANCE.createText(container, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.READ_ONLY,
                result.getSummary(), "Summary of metadata generation", 0, 80, true, false);

        // Files Table
        GUIFactory.INSTANCE.createLabel(container, "Generated/Updated Files:");
        table = new Table(container, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL);
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        GridData gdTable = new GridData(GridData.FILL_BOTH);
        gdTable.heightHint = 200;
        table.setLayoutData(gdTable);

        TableColumn col1 = new TableColumn(table, SWT.NONE);
        col1.setText("File Name");
        col1.setWidth(200);

        TableColumn col2 = new TableColumn(table, SWT.NONE);
        col2.setText("Relative Path");
        col2.setWidth(400);

        for (File file : result.getGeneratedFiles()) {
            TableItem item = new TableItem(table, SWT.NONE);
            item.setText(0, file.getName());
            String relPath = result.getRoot().toURI().relativize(file.toURI()).getPath();
            item.setText(1, relPath);
        }

        // Renaming Area
        Composite renameComp = new Composite(container, SWT.NONE);
        renameComp.setLayout(new GridLayout(2, false));
        renameComp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        GUIFactory.INSTANCE.createLabel(renameComp, "Prefix for summaries:");
        prefixText = GUIFactory.INSTANCE.createText(renameComp, SWT.BORDER, "", "Prefix for ARCHITECTURE_CONTEXT.md etc.", 0, 0, true, true);
        prefixText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        // Actions Area
        Composite actionsComp = new Composite(container, SWT.NONE);
        actionsComp.setLayout(new GridLayout(3, true));
        actionsComp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Button importBtn = GUIFactory.INSTANCE.createButton(actionsComp, "Import to Project");
        importBtn.setToolTipText("Copy all generated files to the current project root");
        importBtn.addListener(SWT.Selection, e -> handleImport());

        Button zipBtn = GUIFactory.INSTANCE.createButton(actionsComp, "Zip Results");
        zipBtn.setToolTipText("Package all results into a ZIP archive");
        zipBtn.addListener(SWT.Selection, e -> handleZip());

        Button renameBtn = GUIFactory.INSTANCE.createButton(actionsComp, "Rename Summaries");
        renameBtn.setToolTipText("Add the prefix to the main context files");
        renameBtn.addListener(SWT.Selection, e -> handleRename());

        return area;
    }

    private void handleImport() {
        if (projectRoot == null || !projectRoot.exists()) {
            setErrorMessage("No active project root to import to.");
            return;
        }

        try {
            for (File file : result.getGeneratedFiles()) {
                String relPath = result.getRoot().toURI().relativize(file.toURI()).getPath();
                File dest = new File(projectRoot, relPath);
                if (!dest.getParentFile().exists()) {
                    dest.getParentFile().mkdirs();
                }
                java.nio.file.Files.copy(file.toPath(), dest.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            setMessage("Successfully imported all metadata files to project root: " + projectRoot.getAbsolutePath(),
                    org.eclipse.jface.dialogs.IMessageProvider.INFORMATION);
        } catch (IOException e) {
            setErrorMessage("Import failed: " + e.getMessage());
        }
    }

    private void handleZip() {
        FileDialog fd = new FileDialog(getShell(), SWT.SAVE);
        fd.setFileName("metadata_results.zip");
        fd.setFilterExtensions(new String[] {"*.zip"});
        String path = fd.open();
        if (path == null) return;

        File zipFile = new File(path);
        try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(new java.io.FileOutputStream(zipFile))) {
            for (File file : result.getGeneratedFiles()) {
                String relPath = result.getRoot().toURI().relativize(file.toURI()).getPath();
                java.util.zip.ZipEntry entry = new java.util.zip.ZipEntry(relPath);
                zos.putNextEntry(entry);
                java.nio.file.Files.copy(file.toPath(), zos);
                zos.closeEntry();
            }
            setMessage("Results zipped to: " + path, org.eclipse.jface.dialogs.IMessageProvider.INFORMATION);
        } catch (IOException e) {
            setErrorMessage("Zipping failed: " + e.getMessage());
        }
    }

    private void handleRename() {
        String prefix = prefixText.getText().trim();
        if (prefix.isEmpty()) {
            setErrorMessage("Please enter a prefix.");
            return;
        }

        java.util.List<File> toRename = new java.util.ArrayList<>();
        for (File file : result.getGeneratedFiles()) {
            String name = file.getName();
            if (name.equals(MetadataAgent.ARCHITECTURE_CONTEXT) ||
                name.equals(MetadataAgent.SEMANTIC_OVERVIEW) ||
                name.equals(MetadataAgent.TRAJECTORY_MAP)) {
                toRename.add(file);
            }
        }

        try {
            for (File file : toRename) {
                File newFile = new File(file.getParentFile(), prefix + "_" + file.getName());
                java.nio.file.Files.move(file.toPath(), newFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);

                // Update result list
                int index = result.getGeneratedFiles().indexOf(file);
                if (index != -1) {
                    result.getGeneratedFiles().set(index, newFile);
                }
            }
            refreshTable();
            setMessage("Main files renamed with prefix: " + prefix, org.eclipse.jface.dialogs.IMessageProvider.INFORMATION);
        } catch (IOException e) {
            setErrorMessage("Rename failed: " + e.getMessage());
        }
    }

    private void refreshTable() {
        table.removeAll();
        for (File file : result.getGeneratedFiles()) {
            TableItem item = new TableItem(table, SWT.NONE);
            item.setText(0, file.getName());
            String relPath = result.getRoot().toURI().relativize(file.toURI()).getPath();
            item.setText(1, relPath);
        }
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
    }
}
