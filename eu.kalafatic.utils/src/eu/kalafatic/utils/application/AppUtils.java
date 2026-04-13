/*******************************************************************************
 * Copyright (c) 2010, Petr Kalafatic (gemini@kalafatic.eu).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU GPL Version 3
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.txt
 *
 * Contributors:
 *     Petr Kalafatic - initial API and implementation
 ******************************************************************************/
package eu.kalafatic.utils.application;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

import eu.kalafatic.utils.dialogs.DialogUtils;
import eu.kalafatic.utils.model.FormEditorInput;

/**
 * The Class class AppUtils.
 * @author Petr Kalafatic
 * @project Gemini
 * @version 3.0.0
 */
public class AppUtils {

	/** The Constant DEFAULT_TEXT_EDITOR_ID. */
	public static final String DEFAULT_TEXT_EDITOR_ID = "org.eclipse.ui.DefaultTextEditor";

	/** The i editor reference. */
	public static IEditorReference iEditorReference;

	/** The INSTANCE. */
	public volatile static AppUtils INSTANCE = new AppUtils();

	// ---------------------------------------------------------------
	// ---------------------------------------------------------------

	/**
	 * Creates the project.
	 * @param projectName the project name
	 * @return the i project
	 */
	public IProject createProject(final String projectName) {
		Assert.isNotNull(projectName);

		// UIJob uiJob = new UIJob("Opens project...") {
		// @Override
		// public IStatus runInUIThread(IProgressMonitor monitor) {
		try {
			IWorkspace workspace = ResourcesPlugin.getWorkspace();
			IWorkspaceRoot root = workspace.getRoot();
			IProject project = root.getProject(projectName);
			if (!project.exists()) {
				project.create(null);
			}
			if (!project.isOpen()) {
				project.open(IResource.FORCE, new NullProgressMonitor());
			}
			IFolder folder = project.getFolder("Core");

			if (!folder.exists()) {
				folder.create(IResource.NONE, true, new NullProgressMonitor());
			}
			IFile file = folder.getFile(projectName + ".xml");
			if (!file.exists()) {
				byte[] bytes = ("<!-- " + projectName + " ® -->").getBytes();
				InputStream source = new ByteArrayInputStream(bytes);
				file.create(source, IResource.FORCE, new NullProgressMonitor());
			}
			ResourcesPlugin.getWorkspace().getRoot().refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
			return project;
		} catch (CoreException e) {
			e.printStackTrace();
		}
		// IViewPart viewPart =
		// PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findView("org.eclipse.ui.navigator.ProjectExplorer");
		// return Status.OK_STATUS;
		// }
		// };
		// uiJob.setPriority(Job.INTERACTIVE);
		// uiJob.setSystem(true);
		// uiJob.schedule();
		return null;
	}

	// ---------------------------------------------------------------

	/**
	 * Creates the marker.
	 * @param file the file
	 * @param type the type
	 * @return the i marker
	 */
	public IMarker createMarker(IFile file, String type) {
		try {
			Assert.isNotNull(file);
			return file.createMarker(type);
		} catch (CoreException e) {
			// Logger.getGlobal().(e.getLocalizedMessage());
		}
		return null;
	}

	// ---------------------------------------------------------------

	/**
	 * Schedule jobs.
	 * @param jobs the jobs
	 */
	public void scheduleJobs(List<Job> jobs) {
		for (final Job job : jobs) {
			Display.getDefault().asyncExec(new Runnable() {
				@Override
				public void run() {
					job.schedule();
				}
			});
		}
	}

	// ---------------------------------------------------------------

	/**
	 * Gets the current perspective.
	 * @return the current perspective
	 */
	public static IPerspectiveDescriptor getCurrentPerspective() {
		IPerspectiveDescriptor perspective = null;
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if ((window != null) && (window.getActivePage() != null)) {
			perspective = window.getActivePage().getPerspective();
		}
		return perspective /*
							 * != null ? perspective :
							 * PlatformUI.getWorkbench().getPerspectiveRegistry().findPerspectiveWithId(ExplorerCorePerspective.ID)
							 */;
	}

	// ---------------------------------------------------------------

	/**
	 * Open editor.
	 * @param editorID the editor id
	 */
	public void openEditor(String editorID) {
		final IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();

		if (isEditorOpen(page, editorID)) {
			int result = DialogUtils.INSTANCE.question("Open new editor ?");

			if (result == SWT.YES) {
				openEditor(page, editorID, "");
			} else {
				page.showEditor(iEditorReference);
			}
		} else {
			openEditor(page, editorID, "");
		}
		page.setEditorAreaVisible(true);

	}

	// ---------------------------------------------------------------

	/**
	 * Open editor.
	 * @param page the page
	 * @param id the id
	 * @param path the path
	 */
	public static void openEditor(IWorkbenchPage page, String id, String path) {
		File input;
		try {
			if (path == null || path.trim().isEmpty()) {
				input = File.createTempFile("temp", ".temp");
			} else {
				input = new File(path);
			}
			IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
			IDE.openEditor(window.getActivePage(), input.toURI(), id, true);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (PartInitException e) {
			e.printStackTrace();
		}
	}

	// ---------------------------------------------------------------

	public void openEditor(String id, File file, Map<?, ?> project) {
		try {
			IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();

			if ((page != null) && (!isEditorOpen(page, id))) {

				FormEditorInput input = new FormEditorInput(file, project);
				page.openEditor(input, id, true);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// ---------------------------------------------------------------

	/**
	 * Open editor.
	 * @param file the file
	 */
	public static void openEditor(File file) {
		try {
			IEditorDescriptor editorDescriptor = getEditorDescriptor(file.getName());
			if (editorDescriptor == null) {
				editorDescriptor = getDefaultEditorDesc();
			}
			openEditor(editorDescriptor, file);

		} catch (PartInitException e) {
			e.printStackTrace();
		}
	}

	// ---------------------------------------------------------------

	/**
	 * Open editor.
	 * @param id the id
	 * @param file the file
	 */
	public void openEditor(String id, File file) {
		try {
			IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();

			IDE.openEditor(page, file.toURI(), id, true);

			// IFileStore iFileStore = EFS.getStore(file.toURI());
			// // IDE.openEditor(page, iFileStore.,id, true);
			//
			// // IEditorPart editor = IDE.openEditor(page,
			// // getEditorInput(iFileStore), id, true);
			//
			// IEditorPart openEditorOnFileStore =
			// IDE.openEditorOnFileStore(page,
			// iFileStore);
			// getEditorId(iFileStore);
			// System.err.println();

		} catch (PartInitException e) {
			e.printStackTrace();
		}
	}

	// ---------------------------------------------------------------

	/**
	 * Open editor.
	 * @param id the id
	 * @param file the file
	 */
	public void openEditor(String id, IFile iFile) {
		try {
			IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
			if (!isEditorOpen(page, id)) {
				IDE.openEditor(page, iFile, id);
			}
		} catch (PartInitException e) {
			e.printStackTrace();
		}
	}

	// ---------------------------------------------------------------

	/**
	 * Gets the editor id.
	 * @param fileStore the file store
	 * @return the editor id
	 * @throws PartInitException the part init exception
	 */
	private String getEditorId(IFileStore fileStore) throws PartInitException {
		String name = fileStore.fetchInfo().getName();
		if (name == null) {
			throw new IllegalArgumentException();
		}

		IContentType contentType = null;
		try {
			InputStream is = null;
			try {
				is = fileStore.openInputStream(EFS.NONE, null);
				contentType = Platform.getContentTypeManager().findContentTypeFor(is, name);
			} finally {
				if (is != null) {
					is.close();
				}
			}
		} catch (CoreException ex) {
			// continue without content type
		} catch (IOException ex) {
			// continue without content type
		}

		IEditorRegistry editorReg = PlatformUI.getWorkbench().getEditorRegistry();
		IEditorDescriptor[] editors = editorReg.getEditors(name, contentType);

		return null;

		// return getEditorDescriptor(name, editorReg,
		// editorReg.getDefaultEditor(name, contentType)).getId();
	}

	// ---------------------------------------------------------------

	/**
	 * Open editor.
	 * @param editorDescriptor the editor descriptor
	 * @param file the file
	 */
	public static void openEditor(IEditorDescriptor editorDescriptor, File file) {
		try {
			IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
			String id = editorDescriptor.getId();
			IDE.openEditor(page, file.toURI(), id, true);
		} catch (PartInitException e) {
			e.printStackTrace();
		}
	}

	// ---------------------------------------------------------------

	/**
	 * Checks if is editor open.
	 * @param page the page
	 * @param id the id
	 * @return true, if is editor open
	 */
	public boolean isEditorOpen(IWorkbenchPage page, String id) {
		IEditorReference[] editorReferences = page.getEditorReferences();
		for (int i = 0; i < editorReferences.length; i++) {
			if (editorReferences[i].getId().equals(id)) {
				iEditorReference = editorReferences[i];
				return true;
			}
		}
		return false;
	}

	// ---------------------------------------------------------------

	/**
	 * Gets the editor reference.
	 * @param page the page
	 * @param id the id
	 * @return the editor reference
	 */
	public IEditorReference getEditorReference(IWorkbenchPage page, String id) {
		IEditorReference[] editorReferences = page.getEditorReferences();
		for (int i = 0; i < editorReferences.length; i++) {
			if (editorReferences[i].getId().equals(id)) {
				return editorReferences[i];
			}
		}
		return null;
	}

	// ---------------------------------------------------------------

	/**
	 * Gets the default editor desc.
	 * @return the default editor desc
	 */
	public static IEditorDescriptor getDefaultEditorDesc() {
		IEditorRegistry editorReg = PlatformUI.getWorkbench().getEditorRegistry();
		IEditorDescriptor iEditorDescriptor = editorReg.findEditor("eu.kalafatic.explorer.view.editors.ExplorerEditor");
		return iEditorDescriptor;
	}

	// ---------------------------------------------------------------

	/**
	 * Gets the editor desc.
	 * @param id the id
	 * @return the editor desc
	 */
	public static IEditorDescriptor getEditorDesc(String id) {
		IEditorRegistry editorReg = PlatformUI.getWorkbench().getEditorRegistry();
		return editorReg.findEditor(id);
	}

	// ---------------------------------------------------------------

	/**
	 * Gets the editor descriptor.
	 * @param fileName the file name
	 * @return the editor descriptor
	 * @throws PartInitException the part init exception
	 */
	public static IEditorDescriptor getEditorDescriptor(String fileName) throws PartInitException {

		IEditorRegistry editorReg = PlatformUI.getWorkbench().getEditorRegistry();

		IEditorDescriptor editorDesc = null;

		// next check the OS for in-place editor (OLE on Win32)
		if (editorReg.isSystemInPlaceEditorAvailable(fileName)) {
			editorDesc = editorReg.findEditor(IEditorRegistry.SYSTEM_INPLACE_EDITOR_ID);
		}

		// next check with the OS for an external editor
		if (editorDesc == null && editorReg.isSystemExternalEditorAvailable(fileName)) {
			editorDesc = editorReg.findEditor(IEditorRegistry.SYSTEM_EXTERNAL_EDITOR_ID);
		}

		// next lookup the default text editor
		if (editorDesc == null) {
			editorDesc = editorReg.findEditor(DEFAULT_TEXT_EDITOR_ID);
		}

		// if no valid editor found, bail out
		if (editorDesc == null) {
			throw new PartInitException("No editor found to edit the file resource.");
		}
		return editorDesc;
	}

	// ---------------------------------------------------------------

	/**
	 * Gets the selected adaptable.
	 * @param selection the selection
	 * @return the selected adaptable
	 * @throws PartInitException the part init exception
	 */
	public static IAdaptable getSelectedAdaptable(ISelection selection) throws PartInitException {
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection iStructuredSelection = (IStructuredSelection) selection;
			if (iStructuredSelection.size() != 1) {
				return null;
			}
			return getSelectedAdaptable(iStructuredSelection.getFirstElement());
		}
		return null;
	}

	// ---------------------------------------------------------------

	/**
	 * Gets the selected adaptable.
	 * @param object the object
	 * @return the selected adaptable
	 */
	private static IAdaptable getSelectedAdaptable(Object object) {
		if (object instanceof IAdaptable) {
			return (IAdaptable) object;
		}
		if (object instanceof File) {
			File file = (File) object;
			IWorkspace workspace = ResourcesPlugin.getWorkspace();
			IPath location = Path.fromOSString(file.getAbsolutePath());

			IFileStore fileStore = EFS.getLocalFileSystem().getStore(new Path(file.getAbsolutePath()));

			// IDE.openEditorOnFileStore(getSite().getPage(), fileStore);

			// IPath iPath = new Path(file.getAbsolutePath());
			// IFile iFile = ResourcesPlugin.getWorkspace().getRoot()
			// .getFile(iPath);

			return fileStore;
		}
		return null;
	}

}
