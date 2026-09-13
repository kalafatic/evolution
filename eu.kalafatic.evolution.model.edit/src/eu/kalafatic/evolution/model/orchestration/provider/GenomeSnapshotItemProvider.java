/**
 */
package eu.kalafatic.evolution.model.orchestration.provider;


import eu.kalafatic.evolution.model.orchestration.GenomeSnapshot;
import eu.kalafatic.evolution.model.orchestration.OrchestrationPackage;

import java.util.Collection;
import java.util.List;

import org.eclipse.emf.common.notify.AdapterFactory;
import org.eclipse.emf.common.notify.Notification;

import org.eclipse.emf.common.util.ResourceLocator;

import org.eclipse.emf.edit.provider.ComposeableAdapterFactory;
import org.eclipse.emf.edit.provider.IEditingDomainItemProvider;
import org.eclipse.emf.edit.provider.IItemLabelProvider;
import org.eclipse.emf.edit.provider.IItemPropertyDescriptor;
import org.eclipse.emf.edit.provider.IItemPropertySource;
import org.eclipse.emf.edit.provider.IStructuredItemContentProvider;
import org.eclipse.emf.edit.provider.ITreeItemContentProvider;
import org.eclipse.emf.edit.provider.ItemPropertyDescriptor;
import org.eclipse.emf.edit.provider.ItemProviderAdapter;
import org.eclipse.emf.edit.provider.ViewerNotification;

/**
 * This is the item provider adapter for a {@link eu.kalafatic.evolution.model.orchestration.GenomeSnapshot} object.
 * <!-- begin-user-doc -->
 * <!-- end-user-doc -->
 * @generated
 */
public class GenomeSnapshotItemProvider 
	extends ItemProviderAdapter
	implements
		IEditingDomainItemProvider,
		IStructuredItemContentProvider,
		ITreeItemContentProvider,
		IItemLabelProvider,
		IItemPropertySource {
	/**
	 * This constructs an instance from a factory and a notifier.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public GenomeSnapshotItemProvider(AdapterFactory adapterFactory) {
		super(adapterFactory);
	}

	/**
	 * This returns the property descriptors for the adapted class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public List<IItemPropertyDescriptor> getPropertyDescriptors(Object object) {
		if (itemPropertyDescriptors == null) {
			super.getPropertyDescriptors(object);

			addTimestampPropertyDescriptor(object);
			addArchitectureArtifactPropertyDescriptor(object);
			addUseCaseArtifactPropertyDescriptor(object);
			addMilestoneArtifactPropertyDescriptor(object);
			addGenomeArtifactPropertyDescriptor(object);
			addDashboardArtifactPropertyDescriptor(object);
		}
		return itemPropertyDescriptors;
	}

	/**
	 * This adds a property descriptor for the Timestamp feature.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void addTimestampPropertyDescriptor(Object object) {
		itemPropertyDescriptors.add
			(createItemPropertyDescriptor
				(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
				 getResourceLocator(),
				 getString("_UI_GenomeSnapshot_timestamp_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_GenomeSnapshot_timestamp_feature", "_UI_GenomeSnapshot_type"),
				 OrchestrationPackage.Literals.GENOME_SNAPSHOT__TIMESTAMP,
				 true,
				 false,
				 false,
				 ItemPropertyDescriptor.GENERIC_VALUE_IMAGE,
				 null,
				 null));
	}

	/**
	 * This adds a property descriptor for the Architecture Artifact feature.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void addArchitectureArtifactPropertyDescriptor(Object object) {
		itemPropertyDescriptors.add
			(createItemPropertyDescriptor
				(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
				 getResourceLocator(),
				 getString("_UI_GenomeSnapshot_architectureArtifact_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_GenomeSnapshot_architectureArtifact_feature", "_UI_GenomeSnapshot_type"),
				 OrchestrationPackage.Literals.GENOME_SNAPSHOT__ARCHITECTURE_ARTIFACT,
				 true,
				 false,
				 false,
				 ItemPropertyDescriptor.GENERIC_VALUE_IMAGE,
				 null,
				 null));
	}

	/**
	 * This adds a property descriptor for the Use Case Artifact feature.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void addUseCaseArtifactPropertyDescriptor(Object object) {
		itemPropertyDescriptors.add
			(createItemPropertyDescriptor
				(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
				 getResourceLocator(),
				 getString("_UI_GenomeSnapshot_useCaseArtifact_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_GenomeSnapshot_useCaseArtifact_feature", "_UI_GenomeSnapshot_type"),
				 OrchestrationPackage.Literals.GENOME_SNAPSHOT__USE_CASE_ARTIFACT,
				 true,
				 false,
				 false,
				 ItemPropertyDescriptor.GENERIC_VALUE_IMAGE,
				 null,
				 null));
	}

	/**
	 * This adds a property descriptor for the Milestone Artifact feature.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void addMilestoneArtifactPropertyDescriptor(Object object) {
		itemPropertyDescriptors.add
			(createItemPropertyDescriptor
				(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
				 getResourceLocator(),
				 getString("_UI_GenomeSnapshot_milestoneArtifact_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_GenomeSnapshot_milestoneArtifact_feature", "_UI_GenomeSnapshot_type"),
				 OrchestrationPackage.Literals.GENOME_SNAPSHOT__MILESTONE_ARTIFACT,
				 true,
				 false,
				 false,
				 ItemPropertyDescriptor.GENERIC_VALUE_IMAGE,
				 null,
				 null));
	}

	/**
	 * This adds a property descriptor for the Genome Artifact feature.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void addGenomeArtifactPropertyDescriptor(Object object) {
		itemPropertyDescriptors.add
			(createItemPropertyDescriptor
				(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
				 getResourceLocator(),
				 getString("_UI_GenomeSnapshot_genomeArtifact_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_GenomeSnapshot_genomeArtifact_feature", "_UI_GenomeSnapshot_type"),
				 OrchestrationPackage.Literals.GENOME_SNAPSHOT__GENOME_ARTIFACT,
				 true,
				 false,
				 false,
				 ItemPropertyDescriptor.GENERIC_VALUE_IMAGE,
				 null,
				 null));
	}

	/**
	 * This adds a property descriptor for the Dashboard Artifact feature.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void addDashboardArtifactPropertyDescriptor(Object object) {
		itemPropertyDescriptors.add
			(createItemPropertyDescriptor
				(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
				 getResourceLocator(),
				 getString("_UI_GenomeSnapshot_dashboardArtifact_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_GenomeSnapshot_dashboardArtifact_feature", "_UI_GenomeSnapshot_type"),
				 OrchestrationPackage.Literals.GENOME_SNAPSHOT__DASHBOARD_ARTIFACT,
				 true,
				 false,
				 false,
				 ItemPropertyDescriptor.GENERIC_VALUE_IMAGE,
				 null,
				 null));
	}

	/**
	 * This returns GenomeSnapshot.gif.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public Object getImage(Object object) {
		return overlayImage(object, getResourceLocator().getImage("full/obj16/GenomeSnapshot"));
	}

	/**
	 * This returns the label text for the adapted class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getText(Object object) {
		String label = ((GenomeSnapshot)object).getTimestamp();
		return label == null || label.length() == 0 ?
			getString("_UI_GenomeSnapshot_type") :
			getString("_UI_GenomeSnapshot_type") + " " + label;
	}


	/**
	 * This handles model notifications by calling {@link #updateChildren} to update any cached
	 * children and by creating a viewer notification, which it passes to {@link #fireNotifyChanged}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void notifyChanged(Notification notification) {
		updateChildren(notification);

		switch (notification.getFeatureID(GenomeSnapshot.class)) {
			case OrchestrationPackage.GENOME_SNAPSHOT__TIMESTAMP:
			case OrchestrationPackage.GENOME_SNAPSHOT__ARCHITECTURE_ARTIFACT:
			case OrchestrationPackage.GENOME_SNAPSHOT__USE_CASE_ARTIFACT:
			case OrchestrationPackage.GENOME_SNAPSHOT__MILESTONE_ARTIFACT:
			case OrchestrationPackage.GENOME_SNAPSHOT__GENOME_ARTIFACT:
			case OrchestrationPackage.GENOME_SNAPSHOT__DASHBOARD_ARTIFACT:
				fireNotifyChanged(new ViewerNotification(notification, notification.getNotifier(), false, true));
				return;
		}
		super.notifyChanged(notification);
	}

	/**
	 * This adds {@link org.eclipse.emf.edit.command.CommandParameter}s describing the children
	 * that can be created under this object.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected void collectNewChildDescriptors(Collection<Object> newChildDescriptors, Object object) {
		super.collectNewChildDescriptors(newChildDescriptors, object);
	}

	/**
	 * Return the resource locator for this item provider's resources.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public ResourceLocator getResourceLocator() {
		return EvolutionEditPlugin.INSTANCE;
	}

}
