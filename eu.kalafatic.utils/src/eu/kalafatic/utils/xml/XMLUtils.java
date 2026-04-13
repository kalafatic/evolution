package eu.kalafatic.utils.xml;

import java.util.Map;

import eu.kalafatic.utils.constants.EProject;

public class XMLUtils {

	// ---------------------------------------------------------------

	public static String getName(Map map, EProject eProject) {
		String result = "";

		Map<String, Object> attributes = (Map<String, Object>) map.get(EProject.ATTRIBUTES.name());

		if (attributes != null) {
			result += (String) attributes.get(EProject.NAME.name());

		}
		if (eProject != null) {
			String type = (String) map.get(eProject.name());
			if (type == null) {
				type = (String) attributes.get(eProject.name());
				result += " (" + type + ")";
			}
		}
		return result;

	}

}
