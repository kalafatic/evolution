/**
 */
package eu.kalafatic.evolution.model.orchestration;

import org.eclipse.emf.ecore.EObject;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Genome Snapshot</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.GenomeSnapshot#getTimestamp <em>Timestamp</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.GenomeSnapshot#getArchitectureArtifact <em>Architecture Artifact</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.GenomeSnapshot#getUseCaseArtifact <em>Use Case Artifact</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.GenomeSnapshot#getMilestoneArtifact <em>Milestone Artifact</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.GenomeSnapshot#getGenomeArtifact <em>Genome Artifact</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.GenomeSnapshot#getDashboardArtifact <em>Dashboard Artifact</em>}</li>
 * </ul>
 *
 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getGenomeSnapshot()
 * @model
 * @generated
 */
public interface GenomeSnapshot extends EObject {
	/**
	 * Returns the value of the '<em><b>Timestamp</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Timestamp</em>' attribute.
	 * @see #setTimestamp(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getGenomeSnapshot_Timestamp()
	 * @model
	 * @generated
	 */
	String getTimestamp();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.GenomeSnapshot#getTimestamp <em>Timestamp</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Timestamp</em>' attribute.
	 * @see #getTimestamp()
	 * @generated
	 */
	void setTimestamp(String value);

	/**
	 * Returns the value of the '<em><b>Architecture Artifact</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Architecture Artifact</em>' attribute.
	 * @see #setArchitectureArtifact(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getGenomeSnapshot_ArchitectureArtifact()
	 * @model
	 * @generated
	 */
	String getArchitectureArtifact();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.GenomeSnapshot#getArchitectureArtifact <em>Architecture Artifact</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Architecture Artifact</em>' attribute.
	 * @see #getArchitectureArtifact()
	 * @generated
	 */
	void setArchitectureArtifact(String value);

	/**
	 * Returns the value of the '<em><b>Use Case Artifact</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Use Case Artifact</em>' attribute.
	 * @see #setUseCaseArtifact(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getGenomeSnapshot_UseCaseArtifact()
	 * @model
	 * @generated
	 */
	String getUseCaseArtifact();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.GenomeSnapshot#getUseCaseArtifact <em>Use Case Artifact</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Use Case Artifact</em>' attribute.
	 * @see #getUseCaseArtifact()
	 * @generated
	 */
	void setUseCaseArtifact(String value);

	/**
	 * Returns the value of the '<em><b>Milestone Artifact</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Milestone Artifact</em>' attribute.
	 * @see #setMilestoneArtifact(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getGenomeSnapshot_MilestoneArtifact()
	 * @model
	 * @generated
	 */
	String getMilestoneArtifact();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.GenomeSnapshot#getMilestoneArtifact <em>Milestone Artifact</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Milestone Artifact</em>' attribute.
	 * @see #getMilestoneArtifact()
	 * @generated
	 */
	void setMilestoneArtifact(String value);

	/**
	 * Returns the value of the '<em><b>Genome Artifact</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Genome Artifact</em>' attribute.
	 * @see #setGenomeArtifact(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getGenomeSnapshot_GenomeArtifact()
	 * @model
	 * @generated
	 */
	String getGenomeArtifact();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.GenomeSnapshot#getGenomeArtifact <em>Genome Artifact</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Genome Artifact</em>' attribute.
	 * @see #getGenomeArtifact()
	 * @generated
	 */
	void setGenomeArtifact(String value);

	/**
	 * Returns the value of the '<em><b>Dashboard Artifact</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Dashboard Artifact</em>' attribute.
	 * @see #setDashboardArtifact(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getGenomeSnapshot_DashboardArtifact()
	 * @model
	 * @generated
	 */
	String getDashboardArtifact();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.GenomeSnapshot#getDashboardArtifact <em>Dashboard Artifact</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Dashboard Artifact</em>' attribute.
	 * @see #getDashboardArtifact()
	 * @generated
	 */
	void setDashboardArtifact(String value);

} // GenomeSnapshot
