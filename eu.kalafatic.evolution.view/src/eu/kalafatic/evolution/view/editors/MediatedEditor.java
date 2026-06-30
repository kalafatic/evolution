package eu.kalafatic.evolution.view.editors;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;

import eu.kalafatic.evolution.view.util.ZipUtil;

/**
 * Multi-page editor for opening and editing ZIP files in mediated mode.
 */
public class MediatedEditor extends MultiPageEditorPart {

    public static final String ID = "eu.kalafatic.evolution.view.editors.MediatedEditor";

    private File zipFile;
    private File tempDir;
    private List<TextEditor> editors = new ArrayList<>();
    private Composite overviewComposite;
    private FormToolkit toolkit;

    @Override
    public void init(IEditorSite site, IEditorInput input) throws PartInitException {
        super.init(site, input);
        if (input instanceof IFileEditorInput) {
            this.zipFile = ((IFileEditorInput) input).getFile().getLocation().toFile();
        } else if (input instanceof FileStoreEditorInput) {
            this.zipFile = new File(((FileStoreEditorInput) input).getURI());
        } else {
            // Try to adapt to IFile
            IFile file = input.getAdapter(IFile.class);
            if (file != null) {
                this.zipFile = file.getLocation().toFile();
            } else {
                throw new PartInitException("Invalid Input Type: " + input.getClass().getName());
            }
        }
        setPartName("Mediated: " + zipFile.getName());

        try {
            tempDir = Files.createTempDirectory("evo-mediated-").toFile();
            ZipUtil.unpack(zipFile, tempDir);
        } catch (IOException e) {
            throw new PartInitException("Failed to unpack ZIP file", e);
        }
    }

    @Override
    protected void createPages() {
        toolkit = new FormToolkit(getContainer().getDisplay());
        createOverviewPage();
        createFileTabs(tempDir, "");
    }

    private void createOverviewPage() {
        overviewComposite = toolkit.createComposite(getContainer());
        overviewComposite.setLayout(new GridLayout(1, false));

        Label label = toolkit.createLabel(overviewComposite, "Mediated Editor: " + zipFile.getAbsolutePath(), SWT.BOLD);
        label.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

        Composite buttonBar = toolkit.createComposite(overviewComposite);
        buttonBar.setLayout(new GridLayout(4, false));

        Button addFileBtn = toolkit.createButton(buttonBar, "Add File", SWT.PUSH);
        addFileBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                handleAddFile();
            }
        });

        Button removeFileBtn = toolkit.createButton(buttonBar, "Remove Current File", SWT.PUSH);
        removeFileBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                handleRemoveFile();
            }
        });

        Button saveBtn = toolkit.createButton(buttonBar, "Save", SWT.PUSH);
        saveBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                doSave(null);
            }
        });

        Button exportBtn = toolkit.createButton(buttonBar, "Export As...", SWT.PUSH);
        exportBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                doSaveAs();
            }
        });

        int index = addPage(overviewComposite);
        setPageText(index, "Overview");
    }

    private void createFileTabs(File dir, String pathPrefix) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (file.isDirectory()) {
                createFileTabs(file, pathPrefix + file.getName() + "/");
            } else {
                addFileTab(file, pathPrefix + file.getName());
            }
        }
    }

    private void addFileTab(File file, String label) {
        try {
            TextEditor editor = new TextEditor();
            IFileStore fileStore = EFS.getLocalFileSystem().getStore(new Path(file.getAbsolutePath()));
            int index = addPage(editor, new FileStoreEditorInput(fileStore));
            setPageText(index, label);
            editors.add(editor);
        } catch (PartInitException e) {
            // Log error
        }
    }

    private void handleAddFile() {
        InputDialog dlg = new InputDialog(getContainer().getShell(), "Add File", "Enter relative file path:", "", null);
        if (dlg.open() == Window.OK) {
            String path = dlg.getValue();
            if (path != null && !path.isEmpty()) {
                try {
                    File newFile = new File(tempDir, path).getCanonicalFile();
                    if (!newFile.getPath().startsWith(tempDir.getCanonicalPath())) {
                        MessageDialog.openError(getContainer().getShell(), "Error", "Invalid path: File must be inside the ZIP.");
                        return;
                    }
                    newFile.getParentFile().mkdirs();
                    if (newFile.createNewFile()) {
                        refreshTabs();
                    }
                } catch (IOException e) {
                    MessageDialog.openError(getContainer().getShell(), "Error", "Failed to create file: " + e.getMessage());
                }
            }
        }
    }

    private void handleRemoveFile() {
        int index = getActivePage();
        if (index > 0) { // Index 0 is Overview
            String label = getPageText(index);
            try {
                File fileToRemove = new File(tempDir, label).getCanonicalFile();
                if (fileToRemove.exists() && fileToRemove.getPath().startsWith(tempDir.getCanonicalPath())) {
                    if (MessageDialog.openConfirm(getContainer().getShell(), "Confirm", "Delete " + label + "?")) {
                        fileToRemove.delete();
                        removePage(index);
                        editors.remove(index - 1);
                    }
                }
            } catch (IOException e) {
                // Ignore
            }
        }
    }

    private void refreshTabs() {
        for (int i = getPageCount() - 1; i > 0; i--) {
            removePage(i);
        }
        editors.clear();
        createFileTabs(tempDir, "");
    }

    @Override
    public void doSave(IProgressMonitor monitor) {
        for (TextEditor editor : editors) {
            editor.doSave(monitor);
        }
        try {
            ZipUtil.pack(tempDir, zipFile);
            IFile[] files = ResourcesPlugin.getWorkspace().getRoot().findFilesForLocationURI(zipFile.toURI());
            for (IFile f : files) {
                f.refreshLocal(IResource.DEPTH_ZERO, monitor);
            }
        } catch (Exception e) {
            // Log error
        }
    }

    @Override
    public void doSaveAs() {
        org.eclipse.swt.widgets.FileDialog dialog = new org.eclipse.swt.widgets.FileDialog(getSite().getShell(), SWT.SAVE);
        dialog.setFilterExtensions(new String[]{"*.zip"});
        dialog.setFileName(zipFile.getName());
        String path = dialog.open();
        if (path != null) {
            File newZip = new File(path);
            try {
                for (TextEditor editor : editors) {
                    editor.doSave(null);
                }
                ZipUtil.pack(tempDir, newZip);
                this.zipFile = newZip;
                setPartName("Mediated: " + zipFile.getName());
                firePropertyChange(IEditorPart.PROP_DIRTY);
            } catch (IOException e) {
                MessageDialog.openError(getSite().getShell(), "Error", "Failed to export ZIP: " + e.getMessage());
            }
        }
    }

    @Override
    public boolean isSaveAsAllowed() {
        return true;
    }

    @Override
    public void dispose() {
        super.dispose();
        if (toolkit != null) toolkit.dispose();
        if (tempDir != null && tempDir.exists()) {
            deleteDirectory(tempDir);
        }
    }

    private void deleteDirectory(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) deleteDirectory(file);
                else file.delete();
            }
        }
        dir.delete();
    }
}
