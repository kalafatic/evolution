package eu.kalafatic.utils.providers;

import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

import eu.kalafatic.utils.constants.EProject;
import eu.kalafatic.utils.constants.FCoreImageConstants;
import eu.kalafatic.utils.xml.XMLUtils;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class ProjectLabelProvider extends LabelProvider implements ILabelProvider {

	// ---------------------------------------------------------------
	// ---------------------------------------------------------------

	@Override
	public Image getImage(Object element) {
		try {
			if (element instanceof Entry) {
				Entry<String, Object> entry = (Entry<String, Object>) element;

				if (entry.getKey().equals(EProject.CHILDREN.name())) {
					return FCoreImageConstants.TREE_IMG;
				} else if (entry.getKey().equals(EProject.ATTRIBUTES.name())) {
					return FCoreImageConstants.LIST_IMG;
				} else if (entry.getKey().equals(EProject.ELEMENTS.name())) {
					// return FCoreImageConstants.ELEMENTS_IMG;
				} else if (entry.getKey().equals(EProject.DATA.name())) {
					return null;
				} else if (entry.getKey().equals(EProject.VALUE.name())) {
					// return FCoreImageConstants.FRAME_IMG;
				} else {
					return FCoreImageConstants.FILE_IMG;
				}
			} else if (element instanceof Map) {
				Map<String, Object> map = (Map<String, Object>) element;

				if (map.get(EProject.KEY.name()) == null) {
					return FCoreImageConstants.LIST_IMG;
				} else if (map.get(EProject.KEY.name()).equals(EProject.MANIFEST.name())) {
					// return FCoreImageConstants.MAINTAIN_IMG;
				} else {
					return FCoreImageConstants.PROJECT_IMG;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return FCoreImageConstants.FILE_IMG;
	}

	// ---------------------------------------------------------------

	@Override
	public String getText(Object element) {
		try {
			if (element instanceof IResource) {
				return ((IResource) element).getName();

			} else if (element instanceof Map) {
				Map<String, Object> map = (Map<String, Object>) element;
				if (map.get(EProject.KEY.name()) == null) {
					return EProject.ATTRIBUTES.literal;
				}
				return XMLUtils.getName(map, EProject.KEY);

			} else if (element instanceof Entry) {
				Entry<String, Object> entry = (Entry<String, Object>) element;

				if (entry.getKey().equals(EProject.ATTRIBUTES.name())) {
					return EProject.ATTRIBUTES.literal;

				} else if (entry.getKey().equals(EProject.CHILDREN.name())) {
					return EProject.SUBPROJECTS.literal;

				} else if (entry.getKey().equals(EProject.ELEMENTS.name())) {
					return EProject.ELEMENTS.literal;
				} else if (entry.getKey().equals(EProject.DATA.name())) {
					return EProject.DATA.literal + ": " + entry.getValue().getClass();

				} else if (entry.getKey().equals(EProject.VALUE.name())) {
					String name = entry.getValue().getClass().getName();
					if (name != null) {
						return entry.getValue() + " (" + name.substring(name.lastIndexOf('.') + 1) + ")";
					}
					return (String) entry.getValue();
				}
				// simple elements
				return entry.getKey().toLowerCase() + ": " + entry.getValue();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "pppppp";
	}

	// ---------------------------------------------------------------

	@Override
	public boolean isLabelProperty(Object arg0, String arg1) {
		return false;
	}

	// ---------------------------------------------------------------

	@Override
	public void dispose() {}

	// ---------------------------------------------------------------

	@Override
	public void addListener(ILabelProviderListener listener) {

	}

	// ---------------------------------------------------------------

	@Override
	public void removeListener(ILabelProviderListener listener) {

	}
}
