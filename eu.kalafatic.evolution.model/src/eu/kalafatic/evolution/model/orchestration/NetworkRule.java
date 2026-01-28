package eu.kalafatic.evolution.model.orchestration;

import org.eclipse.emf.common.util.EList;

/**
 * @model
 * @generated NOT
 */
public interface NetworkRule extends Rule {
	/**
	 * @model
	 * @generated NOT
	 */
	EList<String> getAllowedDomains();

	/**
	 * @model
	 * @generated NOT
	 */
	boolean isAllowAll();

	/**
	 * @generated NOT
	 */
	void setAllowAll(boolean value);

} // NetworkRule
