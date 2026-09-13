/**
 */
package eu.kalafatic.evolution.model.orchestration.impl;

import eu.kalafatic.evolution.model.orchestration.GenomeSnapshot;
import eu.kalafatic.evolution.model.orchestration.OrchestrationPackage;

import org.eclipse.emf.common.notify.Notification;

import org.eclipse.emf.ecore.EClass;

import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.MinimalEObjectImpl;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Genome Snapshot</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * </p>
 * <ul>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.GenomeSnapshotImpl#getTimestamp <em>Timestamp</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.GenomeSnapshotImpl#getArchitectureArtifact <em>Architecture Artifact</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.GenomeSnapshotImpl#getUseCaseArtifact <em>Use Case Artifact</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.GenomeSnapshotImpl#getMilestoneArtifact <em>Milestone Artifact</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.GenomeSnapshotImpl#getGenomeArtifact <em>Genome Artifact</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.GenomeSnapshotImpl#getDashboardArtifact <em>Dashboard Artifact</em>}</li>
 * </ul>
 *
 * @generated
 */
public class GenomeSnapshotImpl extends MinimalEObjectImpl.Container implements GenomeSnapshot {
	/**
	 * The default value of the '{@link #getTimestamp() <em>Timestamp</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getTimestamp()
	 * @generated
	 * @ordered
	 */
	protected static final String TIMESTAMP_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getTimestamp() <em>Timestamp</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getTimestamp()
	 * @generated
	 * @ordered
	 */
	protected String timestamp = TIMESTAMP_EDEFAULT;

	/**
	 * The default value of the '{@link #getArchitectureArtifact() <em>Architecture Artifact</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getArchitectureArtifact()
	 * @generated
	 * @ordered
	 */
	protected static final String ARCHITECTURE_ARTIFACT_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getArchitectureArtifact() <em>Architecture Artifact</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getArchitectureArtifact()
	 * @generated
	 * @ordered
	 */
	protected String architectureArtifact = ARCHITECTURE_ARTIFACT_EDEFAULT;

	/**
	 * The default value of the '{@link #getUseCaseArtifact() <em>Use Case Artifact</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getUseCaseArtifact()
	 * @generated
	 * @ordered
	 */
	protected static final String USE_CASE_ARTIFACT_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getUseCaseArtifact() <em>Use Case Artifact</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getUseCaseArtifact()
	 * @generated
	 * @ordered
	 */
	protected String useCaseArtifact = USE_CASE_ARTIFACT_EDEFAULT;

	/**
	 * The default value of the '{@link #getMilestoneArtifact() <em>Milestone Artifact</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getMilestoneArtifact()
	 * @generated
	 * @ordered
	 */
	protected static final String MILESTONE_ARTIFACT_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getMilestoneArtifact() <em>Milestone Artifact</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getMilestoneArtifact()
	 * @generated
	 * @ordered
	 */
	protected String milestoneArtifact = MILESTONE_ARTIFACT_EDEFAULT;

	/**
	 * The default value of the '{@link #getGenomeArtifact() <em>Genome Artifact</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getGenomeArtifact()
	 * @generated
	 * @ordered
	 */
	protected static final String GENOME_ARTIFACT_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getGenomeArtifact() <em>Genome Artifact</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getGenomeArtifact()
	 * @generated
	 * @ordered
	 */
	protected String genomeArtifact = GENOME_ARTIFACT_EDEFAULT;

	/**
	 * The default value of the '{@link #getDashboardArtifact() <em>Dashboard Artifact</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getDashboardArtifact()
	 * @generated
	 * @ordered
	 */
	protected static final String DASHBOARD_ARTIFACT_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getDashboardArtifact() <em>Dashboard Artifact</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getDashboardArtifact()
	 * @generated
	 * @ordered
	 */
	protected String dashboardArtifact = DASHBOARD_ARTIFACT_EDEFAULT;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected GenomeSnapshotImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return OrchestrationPackage.Literals.GENOME_SNAPSHOT;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getTimestamp() {
		return timestamp;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setTimestamp(String newTimestamp) {
		String oldTimestamp = timestamp;
		timestamp = newTimestamp;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.GENOME_SNAPSHOT__TIMESTAMP, oldTimestamp, timestamp));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getArchitectureArtifact() {
		return architectureArtifact;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setArchitectureArtifact(String newArchitectureArtifact) {
		String oldArchitectureArtifact = architectureArtifact;
		architectureArtifact = newArchitectureArtifact;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.GENOME_SNAPSHOT__ARCHITECTURE_ARTIFACT, oldArchitectureArtifact, architectureArtifact));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getUseCaseArtifact() {
		return useCaseArtifact;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setUseCaseArtifact(String newUseCaseArtifact) {
		String oldUseCaseArtifact = useCaseArtifact;
		useCaseArtifact = newUseCaseArtifact;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.GENOME_SNAPSHOT__USE_CASE_ARTIFACT, oldUseCaseArtifact, useCaseArtifact));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getMilestoneArtifact() {
		return milestoneArtifact;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setMilestoneArtifact(String newMilestoneArtifact) {
		String oldMilestoneArtifact = milestoneArtifact;
		milestoneArtifact = newMilestoneArtifact;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.GENOME_SNAPSHOT__MILESTONE_ARTIFACT, oldMilestoneArtifact, milestoneArtifact));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getGenomeArtifact() {
		return genomeArtifact;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setGenomeArtifact(String newGenomeArtifact) {
		String oldGenomeArtifact = genomeArtifact;
		genomeArtifact = newGenomeArtifact;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.GENOME_SNAPSHOT__GENOME_ARTIFACT, oldGenomeArtifact, genomeArtifact));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getDashboardArtifact() {
		return dashboardArtifact;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setDashboardArtifact(String newDashboardArtifact) {
		String oldDashboardArtifact = dashboardArtifact;
		dashboardArtifact = newDashboardArtifact;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.GENOME_SNAPSHOT__DASHBOARD_ARTIFACT, oldDashboardArtifact, dashboardArtifact));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public Object eGet(int featureID, boolean resolve, boolean coreType) {
		switch (featureID) {
			case OrchestrationPackage.GENOME_SNAPSHOT__TIMESTAMP:
				return getTimestamp();
			case OrchestrationPackage.GENOME_SNAPSHOT__ARCHITECTURE_ARTIFACT:
				return getArchitectureArtifact();
			case OrchestrationPackage.GENOME_SNAPSHOT__USE_CASE_ARTIFACT:
				return getUseCaseArtifact();
			case OrchestrationPackage.GENOME_SNAPSHOT__MILESTONE_ARTIFACT:
				return getMilestoneArtifact();
			case OrchestrationPackage.GENOME_SNAPSHOT__GENOME_ARTIFACT:
				return getGenomeArtifact();
			case OrchestrationPackage.GENOME_SNAPSHOT__DASHBOARD_ARTIFACT:
				return getDashboardArtifact();
		}
		return super.eGet(featureID, resolve, coreType);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void eSet(int featureID, Object newValue) {
		switch (featureID) {
			case OrchestrationPackage.GENOME_SNAPSHOT__TIMESTAMP:
				setTimestamp((String)newValue);
				return;
			case OrchestrationPackage.GENOME_SNAPSHOT__ARCHITECTURE_ARTIFACT:
				setArchitectureArtifact((String)newValue);
				return;
			case OrchestrationPackage.GENOME_SNAPSHOT__USE_CASE_ARTIFACT:
				setUseCaseArtifact((String)newValue);
				return;
			case OrchestrationPackage.GENOME_SNAPSHOT__MILESTONE_ARTIFACT:
				setMilestoneArtifact((String)newValue);
				return;
			case OrchestrationPackage.GENOME_SNAPSHOT__GENOME_ARTIFACT:
				setGenomeArtifact((String)newValue);
				return;
			case OrchestrationPackage.GENOME_SNAPSHOT__DASHBOARD_ARTIFACT:
				setDashboardArtifact((String)newValue);
				return;
		}
		super.eSet(featureID, newValue);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void eUnset(int featureID) {
		switch (featureID) {
			case OrchestrationPackage.GENOME_SNAPSHOT__TIMESTAMP:
				setTimestamp(TIMESTAMP_EDEFAULT);
				return;
			case OrchestrationPackage.GENOME_SNAPSHOT__ARCHITECTURE_ARTIFACT:
				setArchitectureArtifact(ARCHITECTURE_ARTIFACT_EDEFAULT);
				return;
			case OrchestrationPackage.GENOME_SNAPSHOT__USE_CASE_ARTIFACT:
				setUseCaseArtifact(USE_CASE_ARTIFACT_EDEFAULT);
				return;
			case OrchestrationPackage.GENOME_SNAPSHOT__MILESTONE_ARTIFACT:
				setMilestoneArtifact(MILESTONE_ARTIFACT_EDEFAULT);
				return;
			case OrchestrationPackage.GENOME_SNAPSHOT__GENOME_ARTIFACT:
				setGenomeArtifact(GENOME_ARTIFACT_EDEFAULT);
				return;
			case OrchestrationPackage.GENOME_SNAPSHOT__DASHBOARD_ARTIFACT:
				setDashboardArtifact(DASHBOARD_ARTIFACT_EDEFAULT);
				return;
		}
		super.eUnset(featureID);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public boolean eIsSet(int featureID) {
		switch (featureID) {
			case OrchestrationPackage.GENOME_SNAPSHOT__TIMESTAMP:
				return TIMESTAMP_EDEFAULT == null ? timestamp != null : !TIMESTAMP_EDEFAULT.equals(timestamp);
			case OrchestrationPackage.GENOME_SNAPSHOT__ARCHITECTURE_ARTIFACT:
				return ARCHITECTURE_ARTIFACT_EDEFAULT == null ? architectureArtifact != null : !ARCHITECTURE_ARTIFACT_EDEFAULT.equals(architectureArtifact);
			case OrchestrationPackage.GENOME_SNAPSHOT__USE_CASE_ARTIFACT:
				return USE_CASE_ARTIFACT_EDEFAULT == null ? useCaseArtifact != null : !USE_CASE_ARTIFACT_EDEFAULT.equals(useCaseArtifact);
			case OrchestrationPackage.GENOME_SNAPSHOT__MILESTONE_ARTIFACT:
				return MILESTONE_ARTIFACT_EDEFAULT == null ? milestoneArtifact != null : !MILESTONE_ARTIFACT_EDEFAULT.equals(milestoneArtifact);
			case OrchestrationPackage.GENOME_SNAPSHOT__GENOME_ARTIFACT:
				return GENOME_ARTIFACT_EDEFAULT == null ? genomeArtifact != null : !GENOME_ARTIFACT_EDEFAULT.equals(genomeArtifact);
			case OrchestrationPackage.GENOME_SNAPSHOT__DASHBOARD_ARTIFACT:
				return DASHBOARD_ARTIFACT_EDEFAULT == null ? dashboardArtifact != null : !DASHBOARD_ARTIFACT_EDEFAULT.equals(dashboardArtifact);
		}
		return super.eIsSet(featureID);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String toString() {
		if (eIsProxy()) return super.toString();

		StringBuilder result = new StringBuilder(super.toString());
		result.append(" (timestamp: ");
		result.append(timestamp);
		result.append(", architectureArtifact: ");
		result.append(architectureArtifact);
		result.append(", useCaseArtifact: ");
		result.append(useCaseArtifact);
		result.append(", milestoneArtifact: ");
		result.append(milestoneArtifact);
		result.append(", genomeArtifact: ");
		result.append(genomeArtifact);
		result.append(", dashboardArtifact: ");
		result.append(dashboardArtifact);
		result.append(')');
		return result.toString();
	}

} //GenomeSnapshotImpl
