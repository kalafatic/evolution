package eu.kalafatic.evolution.model.orchestration;

import org.eclipse.emf.ecore.EObject;

public interface GenomeSnapshot extends EObject {
	String getTimestamp();
	void setTimestamp(String value);

	String getArchitectureArtifact();
	void setArchitectureArtifact(String value);

	String getUseCaseArtifact();
	void setUseCaseArtifact(String value);

	String getMilestoneArtifact();
	void setMilestoneArtifact(String value);

	String getGenomeArtifact();
	void setGenomeArtifact(String value);

	String getDashboardArtifact();
	void setDashboardArtifact(String value);
}
