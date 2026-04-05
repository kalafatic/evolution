package eu.kalafatic.evolution.model.orchestration;

import org.eclipse.emf.ecore.EObject;

/**
 * @model
 */
public interface Test extends EObject {
	/**
	 * @model
	 */
	String getId();
	void setId(String value);

	/**
	 * @model
	 */
	String getName();
	void setName(String value);

	/**
	 * @model
	 */
	String getType();
	void setType(String value);

	/**
	 * @model
	 */
	String getPath();
	void setPath(String value);

	/**
	 * @model
	 */
	TestStatus getStatus();
	void setStatus(TestStatus value);

	/**
	 * @model
	 */
	boolean isSelected();
	void setSelected(boolean value);
}
