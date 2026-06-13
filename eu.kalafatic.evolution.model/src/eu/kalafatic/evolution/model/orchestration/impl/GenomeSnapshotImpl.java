package eu.kalafatic.evolution.model.orchestration.impl;

import eu.kalafatic.evolution.model.orchestration.GenomeSnapshot;
import eu.kalafatic.evolution.model.orchestration.OrchestrationPackage;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.MinimalEObjectImpl;

public class GenomeSnapshotImpl extends MinimalEObjectImpl.Container implements GenomeSnapshot {
	protected String timestamp = null;
	protected String architectureArtifact = null;
	protected String useCaseArtifact = null;
	protected String milestoneArtifact = null;
	protected String genomeArtifact = null;
	protected String dashboardArtifact = null;

	protected GenomeSnapshotImpl() {
		super();
	}

	@Override
	protected EClass eStaticClass() {
		return OrchestrationPackage.Literals.GENOME_SNAPSHOT;
	}

	public String getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(String newTimestamp) {
		String oldTimestamp = timestamp;
		timestamp = newTimestamp;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.GENOME_SNAPSHOT__TIMESTAMP, oldTimestamp, timestamp));
	}

	public String getArchitectureArtifact() {
		return architectureArtifact;
	}

	public void setArchitectureArtifact(String newArchitectureArtifact) {
		String oldArchitectureArtifact = architectureArtifact;
		architectureArtifact = newArchitectureArtifact;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.GENOME_SNAPSHOT__ARCHITECTURE_ARTIFACT, oldArchitectureArtifact, architectureArtifact));
	}

	public String getUseCaseArtifact() {
		return useCaseArtifact;
	}

	public void setUseCaseArtifact(String newUseCaseArtifact) {
		String oldUseCaseArtifact = useCaseArtifact;
		useCaseArtifact = newUseCaseArtifact;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.GENOME_SNAPSHOT__USE_CASE_ARTIFACT, oldUseCaseArtifact, useCaseArtifact));
	}

	public String getMilestoneArtifact() {
		return milestoneArtifact;
	}

	public void setMilestoneArtifact(String newMilestoneArtifact) {
		String oldMilestoneArtifact = milestoneArtifact;
		milestoneArtifact = newMilestoneArtifact;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.GENOME_SNAPSHOT__MILESTONE_ARTIFACT, oldMilestoneArtifact, milestoneArtifact));
	}

	public String getGenomeArtifact() {
		return genomeArtifact;
	}

	public void setGenomeArtifact(String newGenomeArtifact) {
		String oldGenomeArtifact = genomeArtifact;
		genomeArtifact = newGenomeArtifact;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.GENOME_SNAPSHOT__GENOME_ARTIFACT, oldGenomeArtifact, genomeArtifact));
	}

	public String getDashboardArtifact() {
		return dashboardArtifact;
	}

	public void setDashboardArtifact(String newDashboardArtifact) {
		String oldDashboardArtifact = dashboardArtifact;
		dashboardArtifact = newDashboardArtifact;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.GENOME_SNAPSHOT__DASHBOARD_ARTIFACT, oldDashboardArtifact, dashboardArtifact));
	}

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

	@Override
	public void eUnset(int featureID) {
		switch (featureID) {
			case OrchestrationPackage.GENOME_SNAPSHOT__TIMESTAMP:
				setTimestamp((String)null);
				return;
			case OrchestrationPackage.GENOME_SNAPSHOT__ARCHITECTURE_ARTIFACT:
				setArchitectureArtifact((String)null);
				return;
			case OrchestrationPackage.GENOME_SNAPSHOT__USE_CASE_ARTIFACT:
				setUseCaseArtifact((String)null);
				return;
			case OrchestrationPackage.GENOME_SNAPSHOT__MILESTONE_ARTIFACT:
				setMilestoneArtifact((String)null);
				return;
			case OrchestrationPackage.GENOME_SNAPSHOT__GENOME_ARTIFACT:
				setGenomeArtifact((String)null);
				return;
			case OrchestrationPackage.GENOME_SNAPSHOT__DASHBOARD_ARTIFACT:
				setDashboardArtifact((String)null);
				return;
		}
		super.eUnset(featureID);
	}

	@Override
	public boolean eIsSet(int featureID) {
		switch (featureID) {
			case OrchestrationPackage.GENOME_SNAPSHOT__TIMESTAMP:
				return timestamp != null;
			case OrchestrationPackage.GENOME_SNAPSHOT__ARCHITECTURE_ARTIFACT:
				return architectureArtifact != null;
			case OrchestrationPackage.GENOME_SNAPSHOT__USE_CASE_ARTIFACT:
				return useCaseArtifact != null;
			case OrchestrationPackage.GENOME_SNAPSHOT__MILESTONE_ARTIFACT:
				return milestoneArtifact != null;
			case OrchestrationPackage.GENOME_SNAPSHOT__GENOME_ARTIFACT:
				return genomeArtifact != null;
			case OrchestrationPackage.GENOME_SNAPSHOT__DASHBOARD_ARTIFACT:
				return dashboardArtifact != null;
		}
		return super.eIsSet(featureID);
	}

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
}
