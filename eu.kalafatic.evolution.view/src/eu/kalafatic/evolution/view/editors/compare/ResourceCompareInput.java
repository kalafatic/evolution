package eu.kalafatic.evolution.view.editors.compare;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.ResourceNode;
import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.graphics.Image;

public class ResourceCompareInput extends CompareEditorInput {

	private Object left;
	private Object right;

	public ResourceCompareInput(CompareConfiguration config, Object left, Object right) {
		super(config);
		this.left = left;
		this.right = right;
	}

	@Override
	protected Object prepareInput(IProgressMonitor monitor) {
		ITypedElement leftElement = getElement(left);
		ITypedElement rightElement = getElement(right);
		return new DiffNode(leftElement, rightElement);
	}

	private ITypedElement getElement(Object obj) {
		if (obj instanceof IFile) {
			return new ResourceNode((IFile) obj);
		} else if (obj instanceof StringElement) {
			return (StringElement) obj;
		}
		return null;
	}

	public static class StringElement implements ITypedElement, org.eclipse.compare.IStreamContentAccessor {
		private String content;
		private String name;
		private String type;

		public StringElement(String content, String name, String type) {
			this.content = content;
			this.name = name;
			this.type = type;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public Image getImage() {
			return null;
		}

		@Override
		public String getType() {
			return type;
		}

		@Override
		public InputStream getContents() {
			return new ByteArrayInputStream(content.getBytes());
		}
	}
}
