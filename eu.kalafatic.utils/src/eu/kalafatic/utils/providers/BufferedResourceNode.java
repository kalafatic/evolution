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
package eu.kalafatic.utils.providers;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.compare.IEditableContent;
import org.eclipse.compare.IStreamContentAccessor;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.ResourceNode;
import org.eclipse.compare.structuremergeviewer.IStructureComparator;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;


/**
 * The Class class BufferedResourceNode.
 * 
 * @author Petr Kalafatic
 * @project Gemini
 * @version 3.0.0
 */
public class BufferedResourceNode extends ResourceNode {

	/** The dirty. */
	private boolean fDirty = false;
	
	/** The delete file. */
	private IFile fDeleteFile;

	/**
	 * Instantiates a new buffered resource node.
	 * 
	 * @param resource
	 *            the resource
	 */
	public BufferedResourceNode(IResource resource) {
		super(resource);
	}

	/*
	 * Returns <code>true</code> if buffer contains uncommitted changes.
	 */
	/**
	 * Checks if is dirty.
	 * 
	 * @return true, if is dirty
	 */
	public boolean isDirty() {
		return fDirty;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.compare.ResourceNode#createChild(org.eclipse.core.resources.IResource)
	 */
	@Override
	protected IStructureComparator createChild(IResource child) {
		return new BufferedResourceNode(child);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.compare.BufferedContent#setContent(byte[])
	 */
	@Override
	public void setContent(byte[] contents) {
		fDirty = true;
		super.setContent(contents);
	}

	/*
	 * Commits buffered contents to resource.
	 */
	/**
	 * Commit.
	 * 
	 * @param pm
	 *            the pm
	 * @throws CoreException
	 *             the core exception
	 */
	public void commit(IProgressMonitor pm) throws CoreException {
		if (fDirty) {

			if (fDeleteFile != null) {
				fDeleteFile.delete(true, true, pm);
				return;
			}

			IResource resource = getResource();
			if (resource instanceof IFile) {

				byte[] bytes = getContent();
				ByteArrayInputStream is = new ByteArrayInputStream(bytes);
				try {
					IFile file = (IFile) resource;
					if (file.exists()) {
						file.setContents(is, false, true, pm);
					} else {
						file.create(is, false, pm);
					}
					fDirty = false;
				} finally {
					if (is != null) {
						try {
							is.close();
						} catch (IOException ex) {
							// Silently ignored
						}
					}
				}
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.compare.ResourceNode#replace(org.eclipse.compare.ITypedElement, org.eclipse.compare.ITypedElement)
	 */
	@Override
	public ITypedElement replace(ITypedElement child, ITypedElement other) {

		if (child == null) { // add resource
			// create a node without a resource behind it!
			IResource resource = getResource();
			if (resource instanceof IFolder) {
				IFolder folder = (IFolder) resource;
				IFile file = folder.getFile(other.getName());
				child = new BufferedResourceNode(file);
			}
		}

		if (other == null) { // delete resource
			IResource resource = getResource();
			if (resource instanceof IFolder) {
				IFolder folder = (IFolder) resource;
				IFile file = folder.getFile(child.getName());
				if (file != null && file.exists()) {
					fDeleteFile = file;
					fDirty = true;
				}
			}
			return null;
		}

		if (other instanceof IStreamContentAccessor
				&& child instanceof IEditableContent) {
			IEditableContent dst = (IEditableContent) child;

			try {
				InputStream is = ((IStreamContentAccessor) other).getContents();
				byte[] bytes = Utilities.readBytes(is);
				if (bytes != null) {
					dst.setContent(bytes);
				}
			} catch (CoreException ex) {
				// NeedWork
			}
		}
		return child;
	}
}
