package eu.kalafatic.utils.constants;

import org.eclipse.swt.graphics.Image;

public enum EProject {

	KEY("Key", FCoreImageConstants.PROJECT_IMG, 1 << 0),

	DATA("Data", FCoreImageConstants.PROJECT_IMG, 1 << 0),

	VALUE("Value", FCoreImageConstants.PROJECT_IMG, 1 << 0),

	NAME("Name", FCoreImageConstants.PROJECT_IMG, 1 << 0),

	UI("UI", FCoreImageConstants.PROJECT_IMG, 1 << 0),

	TYPE("Type", FCoreImageConstants.PROJECT_IMG, 1 << 0),

	ATTRIBUTES("Attributes", FCoreImageConstants.LIST_IMG, 1 << 1),

	CHILDREN("Children", FCoreImageConstants.LIST_IMG, 1 << 2),

	ELEMENT("Element", FCoreImageConstants.LIST_IMG, 1 << 2),

	ELEMENTS("Elements", FCoreImageConstants.LIST_IMG, 1 << 2),

	DOCUMENT("ES", FCoreImageConstants.FILE_IMG, 1 << 3),

	INPUT("ZH", FCoreImageConstants.FILE_IMG, 1 << 4),

	OUTPUT("RU", FCoreImageConstants.FILE_IMG, 1 << 5),

	PROJECT("Project", FCoreImageConstants.FILE_IMG, 1 << 6),

	SUBPROJECTS("Sub-Projects", FCoreImageConstants.LIST_IMG, 1 << 7),

	MANIFEST("Manifest", FCoreImageConstants.FILE_IMG, 1 << 8),

	METADATA("RU", FCoreImageConstants.FILE_IMG, 1 << 9),

	MACHINE("RU", FCoreImageConstants.DRIVE_IMG_1, 1 << 10),

	SERVICE("RU", FCoreImageConstants.FILE_IMG, 1 << 11),

	FILE("RU", FCoreImageConstants.FILE_IMG, 1 << 11),

	;

	/** The literal. */
	public String literal;

	/** The image. */
	public Image image;

	public int bits;

	/**
	 * Instantiates a new e lang.
	 * @param index the index
	 * @param literal the literal
	 * @param image the image
	 */
	EProject(String literal, Image image, int bits) {
		this.literal = literal;
		this.image = image;
		this.bits = bits;
	}

	// ---------------------------------------------------------------
	// ---------------------------------------------------------------

	public static boolean contains(EProject eProject, int bits) {
		return (eProject.bits & bits) != 0;
	}

	// ---------------------------------------------------------------

	// public static EExt getExtension(String ext) {
	// for (EExt eExt : EExt.values()) {
	// if (ext.equalsIgnoreCase(eExt.ext)) {
	// return eExt;
	// }
	// }
	// return null;
	// }

}
