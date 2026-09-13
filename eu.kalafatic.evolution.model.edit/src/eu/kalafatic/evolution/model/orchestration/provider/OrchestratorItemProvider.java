/**
 */
package eu.kalafatic.evolution.model.orchestration.provider;


import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.OrchestrationPackage;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

import java.util.Collection;
import java.util.List;

import org.eclipse.emf.common.notify.AdapterFactory;
import org.eclipse.emf.common.notify.Notification;

import org.eclipse.emf.common.util.ResourceLocator;

import org.eclipse.emf.ecore.EStructuralFeature;

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
 * This is the item provider adapter for a {@link eu.kalafatic.evolution.model.orchestration.Orchestrator} object.
 * <!-- begin-user-doc -->
 * <!-- end-user-doc -->
 * @generated
 */
public class OrchestratorItemProvider 
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
	public OrchestratorItemProvider(AdapterFactory adapterFactory) {
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

			addIdPropertyDescriptor(object);
			addNamePropertyDescriptor(object);
			addRemoteModelPropertyDescriptor(object);
			addAiModePropertyDescriptor(object);
			addMcpServerUrlPropertyDescriptor(object);
			addOpenAiTokenPropertyDescriptor(object);
			addOpenAiModelPropertyDescriptor(object);
			addLocalModelPropertyDescriptor(object);
			addHybridModelPropertyDescriptor(object);
			addOfflineModePropertyDescriptor(object);
			addSharedMemoryPropertyDescriptor(object);
			addDarwinModePropertyDescriptor(object);
			addDefaultTargetPropertyDescriptor(object);
		}
		return itemPropertyDescriptors;
	}

	/**
	 * This adds a property descriptor for the Id feature.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void addIdPropertyDescriptor(Object object) {
		itemPropertyDescriptors.add
			(createItemPropertyDescriptor
				(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
				 getResourceLocator(),
				 getString("_UI_Orchestrator_id_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_Orchestrator_id_feature", "_UI_Orchestrator_type"),
				 OrchestrationPackage.Literals.ORCHESTRATOR__ID,
				 true,
				 false,
				 false,
				 ItemPropertyDescriptor.GENERIC_VALUE_IMAGE,
				 null,
				 null));
	}

	/**
	 * This adds a property descriptor for the Remote Model feature.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void addRemoteModelPropertyDescriptor(Object object) {
		itemPropertyDescriptors.add
			(createItemPropertyDescriptor
				(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
				 getResourceLocator(),
				 getString("_UI_Orchestrator_remoteModel_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_Orchestrator_remoteModel_feature", "_UI_Orchestrator_type"),
				 OrchestrationPackage.Literals.ORCHESTRATOR__REMOTE_MODEL,
				 true,
				 false,
				 false,
				 ItemPropertyDescriptor.GENERIC_VALUE_IMAGE,
				 null,
				 null));
	}
	protected void addAiModePropertyDescriptor(Object object) {
		itemPropertyDescriptors.add(createItemPropertyDescriptor(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(), getResourceLocator(), "AI Mode", "AI Mode", OrchestrationPackage.Literals.ORCHESTRATOR__AI_MODE, true, false, false, ItemPropertyDescriptor.GENERIC_VALUE_IMAGE, null, null));
	}
	protected void addMcpServerUrlPropertyDescriptor(Object object) {
		itemPropertyDescriptors.add(createItemPropertyDescriptor(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(), getResourceLocator(), "MCP Server URL", "MCP Server URL", OrchestrationPackage.Literals.ORCHESTRATOR__MCP_SERVER_URL, true, false, false, ItemPropertyDescriptor.GENERIC_VALUE_IMAGE, null, null));
	}
	/**
	 * This adds a property descriptor for the Open Ai Token feature.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void addOpenAiTokenPropertyDescriptor(Object object) {
		itemPropertyDescriptors.add
			(createItemPropertyDescriptor
				(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
				 getResourceLocator(),
				 getString("_UI_Orchestrator_openAiToken_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_Orchestrator_openAiToken_feature", "_UI_Orchestrator_type"),
				 OrchestrationPackage.Literals.ORCHESTRATOR__OPEN_AI_TOKEN,
				 true,
				 false,
				 false,
				 ItemPropertyDescriptor.GENERIC_VALUE_IMAGE,
				 null,
				 null));
	}

	protected void addOpenAiModelPropertyDescriptor(Object object) {
		itemPropertyDescriptors.add(createItemPropertyDescriptor(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(), getResourceLocator(), "OpenAI Model", "OpenAI Model", OrchestrationPackage.Literals.ORCHESTRATOR__OPEN_AI_MODEL, true, false, false, ItemPropertyDescriptor.GENERIC_VALUE_IMAGE, null, null));
	}
	protected void addLocalModelPropertyDescriptor(Object object) {
		itemPropertyDescriptors.add(createItemPropertyDescriptor(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(), getResourceLocator(), "Local Model", "Local Model", OrchestrationPackage.Literals.ORCHESTRATOR__LOCAL_MODEL, true, false, false, ItemPropertyDescriptor.GENERIC_VALUE_IMAGE, null, null));
	}
	protected void addHybridModelPropertyDescriptor(Object object) {
		itemPropertyDescriptors.add(createItemPropertyDescriptor(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(), getResourceLocator(), "Hybrid Model", "Hybrid Model", OrchestrationPackage.Literals.ORCHESTRATOR__HYBRID_MODEL, true, false, false, ItemPropertyDescriptor.GENERIC_VALUE_IMAGE, null, null));
	}
	protected void addOfflineModePropertyDescriptor(Object object) {
		itemPropertyDescriptors.add(createItemPropertyDescriptor(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(), getResourceLocator(), "Offline Mode", "Offline Mode", OrchestrationPackage.Literals.ORCHESTRATOR__OFFLINE_MODE, true, false, false, ItemPropertyDescriptor.BOOLEAN_VALUE_IMAGE, null, null));
	}
	protected void addSharedMemoryPropertyDescriptor(Object object) {
		itemPropertyDescriptors.add
			(createItemPropertyDescriptor
				(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
				 getResourceLocator(),
				 getString("_UI_Orchestrator_sharedMemory_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_Orchestrator_sharedMemory_feature", "_UI_Orchestrator_type"),
				 OrchestrationPackage.Literals.ORCHESTRATOR__SHARED_MEMORY,
				 true,
				 false,
				 false,
				 ItemPropertyDescriptor.GENERIC_VALUE_IMAGE,
				 null,
				 null));
	}

	/**
	 * This adds a property descriptor for the Darwin Mode feature.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void addDarwinModePropertyDescriptor(Object object) {
		itemPropertyDescriptors.add
			(createItemPropertyDescriptor
				(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
				 getResourceLocator(),
				 getString("_UI_Orchestrator_darwinMode_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_Orchestrator_darwinMode_feature", "_UI_Orchestrator_type"),
				 OrchestrationPackage.Literals.ORCHESTRATOR__DARWIN_MODE,
				 true,
				 false,
				 false,
				 ItemPropertyDescriptor.BOOLEAN_VALUE_IMAGE,
				 null,
				 null));
	}

	/**
	 * This adds a property descriptor for the Default Target feature.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void addDefaultTargetPropertyDescriptor(Object object) {
		itemPropertyDescriptors.add
			(createItemPropertyDescriptor
				(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
				 getResourceLocator(),
				 getString("_UI_Orchestrator_defaultTarget_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_Orchestrator_defaultTarget_feature", "_UI_Orchestrator_type"),
				 OrchestrationPackage.Literals.ORCHESTRATOR__DEFAULT_TARGET,
				 true,
				 false,
				 false,
				 ItemPropertyDescriptor.GENERIC_VALUE_IMAGE,
				 null,
				 null));
	}

	/**
	 * This adds a property descriptor for the Name feature.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void addNamePropertyDescriptor(Object object) {
		itemPropertyDescriptors.add
			(createItemPropertyDescriptor
				(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
				 getResourceLocator(),
				 getString("_UI_Orchestrator_name_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_Orchestrator_name_feature", "_UI_Orchestrator_type"),
				 OrchestrationPackage.Literals.ORCHESTRATOR__NAME,
				 true,
				 false,
				 false,
				 ItemPropertyDescriptor.GENERIC_VALUE_IMAGE,
				 null,
				 null));
	}

	/**
	 * This specifies how to implement {@link #getChildren} and is used to deduce an appropriate feature for an
	 * {@link org.eclipse.emf.edit.command.AddCommand}, {@link org.eclipse.emf.edit.command.RemoveCommand} or
	 * {@link org.eclipse.emf.edit.command.MoveCommand} in {@link #createCommand}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public Collection<? extends EStructuralFeature> getChildrenFeatures(Object object) {
		if (childrenFeatures == null) {
			super.getChildrenFeatures(object);
			childrenFeatures.add(OrchestrationPackage.Literals.ORCHESTRATOR__AGENTS);
			childrenFeatures.add(OrchestrationPackage.Literals.ORCHESTRATOR__TASKS);
			childrenFeatures.add(OrchestrationPackage.Literals.ORCHESTRATOR__TESTS);
			childrenFeatures.add(OrchestrationPackage.Literals.ORCHESTRATOR__GIT);
			childrenFeatures.add(OrchestrationPackage.Literals.ORCHESTRATOR__MAVEN);
			childrenFeatures.add(OrchestrationPackage.Literals.ORCHESTRATOR__LLM);
			childrenFeatures.add(OrchestrationPackage.Literals.ORCHESTRATOR__COMPILER);
			childrenFeatures.add(OrchestrationPackage.Literals.ORCHESTRATOR__OLLAMA);
			childrenFeatures.add(OrchestrationPackage.Literals.ORCHESTRATOR__AI_CHAT);
			childrenFeatures.add(OrchestrationPackage.Literals.ORCHESTRATOR__NEURON_AI);
			childrenFeatures.add(OrchestrationPackage.Literals.ORCHESTRATOR__SELF_DEV_SESSION);
			childrenFeatures.add(OrchestrationPackage.Literals.ORCHESTRATOR__DATABASE);
			childrenFeatures.add(OrchestrationPackage.Literals.ORCHESTRATOR__FILE_CONFIG);
			childrenFeatures.add(OrchestrationPackage.Literals.ORCHESTRATOR__ECLIPSE);
			childrenFeatures.add(OrchestrationPackage.Literals.ORCHESTRATOR__AI_PROVIDERS);
			childrenFeatures.add(OrchestrationPackage.Literals.ORCHESTRATOR__SERVER_SETTINGS);
			childrenFeatures.add(OrchestrationPackage.Literals.ORCHESTRATOR__SERVER_SESSIONS);
			childrenFeatures.add(OrchestrationPackage.Literals.ORCHESTRATOR__MONITORING_HISTORY);
			childrenFeatures.add(OrchestrationPackage.Literals.ORCHESTRATOR__SUPERVISOR_SETTINGS);
			childrenFeatures.add(OrchestrationPackage.Literals.ORCHESTRATOR__NETWORK_ENTRIES);
			childrenFeatures.add(OrchestrationPackage.Literals.ORCHESTRATOR__GENOME_SNAPSHOTS);
			childrenFeatures.add(OrchestrationPackage.Literals.ORCHESTRATOR__FORGE_SESSIONS);
		}
		return childrenFeatures;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EStructuralFeature getChildFeature(Object object, Object child) {
		// Check the type of the specified child object and return the proper feature to use for
		// adding (see {@link AddCommand}) it as a child.

		return super.getChildFeature(object, child);
	}

	/**
	 * This returns Orchestrator.gif.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public Object getImage(Object object) {
		return overlayImage(object, getResourceLocator().getImage("full/obj16/Orchestrator"));
	}

	/**
	 * This returns the label text for the adapted class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getText(Object object) {
		String label = ((Orchestrator)object).getName();
		return label == null || label.length() == 0 ?
			getString("_UI_Orchestrator_type") :
			getString("_UI_Orchestrator_type") + " " + label;
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

		switch (notification.getFeatureID(Orchestrator.class)) {
			case OrchestrationPackage.ORCHESTRATOR__ID:
			case OrchestrationPackage.ORCHESTRATOR__NAME:
			case OrchestrationPackage.ORCHESTRATOR__REMOTE_MODEL:
			case OrchestrationPackage.ORCHESTRATOR__AI_MODE:
			case OrchestrationPackage.ORCHESTRATOR__MCP_SERVER_URL:
			case OrchestrationPackage.ORCHESTRATOR__OPEN_AI_TOKEN:
			case OrchestrationPackage.ORCHESTRATOR__OPEN_AI_MODEL:
			case OrchestrationPackage.ORCHESTRATOR__LOCAL_MODEL:
			case OrchestrationPackage.ORCHESTRATOR__HYBRID_MODEL:
			case OrchestrationPackage.ORCHESTRATOR__OFFLINE_MODE:
			case OrchestrationPackage.ORCHESTRATOR__SHARED_MEMORY:
			case OrchestrationPackage.ORCHESTRATOR__DARWIN_MODE:
			case OrchestrationPackage.ORCHESTRATOR__DEFAULT_TARGET:
				fireNotifyChanged(new ViewerNotification(notification, notification.getNotifier(), false, true));
				return;
			case OrchestrationPackage.ORCHESTRATOR__AGENTS:
			case OrchestrationPackage.ORCHESTRATOR__TASKS:
			case OrchestrationPackage.ORCHESTRATOR__TESTS:
			case OrchestrationPackage.ORCHESTRATOR__GIT:
			case OrchestrationPackage.ORCHESTRATOR__MAVEN:
			case OrchestrationPackage.ORCHESTRATOR__LLM:
			case OrchestrationPackage.ORCHESTRATOR__COMPILER:
			case OrchestrationPackage.ORCHESTRATOR__OLLAMA:
			case OrchestrationPackage.ORCHESTRATOR__AI_CHAT:
			case OrchestrationPackage.ORCHESTRATOR__NEURON_AI:
			case OrchestrationPackage.ORCHESTRATOR__SELF_DEV_SESSION:
			case OrchestrationPackage.ORCHESTRATOR__DATABASE:
			case OrchestrationPackage.ORCHESTRATOR__FILE_CONFIG:
			case OrchestrationPackage.ORCHESTRATOR__ECLIPSE:
			case OrchestrationPackage.ORCHESTRATOR__AI_PROVIDERS:
			case OrchestrationPackage.ORCHESTRATOR__SERVER_SETTINGS:
			case OrchestrationPackage.ORCHESTRATOR__SERVER_SESSIONS:
			case OrchestrationPackage.ORCHESTRATOR__MONITORING_HISTORY:
			case OrchestrationPackage.ORCHESTRATOR__SUPERVISOR_SETTINGS:
			case OrchestrationPackage.ORCHESTRATOR__NETWORK_ENTRIES:
			case OrchestrationPackage.ORCHESTRATOR__GENOME_SNAPSHOTS:
			case OrchestrationPackage.ORCHESTRATOR__FORGE_SESSIONS:
				fireNotifyChanged(new ViewerNotification(notification, notification.getNotifier(), true, false));
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

		newChildDescriptors.add
			(createChildParameter
				(OrchestrationPackage.Literals.ORCHESTRATOR__AGENTS,
				 OrchestrationFactory.eINSTANCE.createAgent()));

		newChildDescriptors.add
			(createChildParameter
				(OrchestrationPackage.Literals.ORCHESTRATOR__TASKS,
				 OrchestrationFactory.eINSTANCE.createTask()));

		newChildDescriptors.add
			(createChildParameter
				(OrchestrationPackage.Literals.ORCHESTRATOR__TESTS,
				 OrchestrationFactory.eINSTANCE.createTest()));

		newChildDescriptors.add
			(createChildParameter
				(OrchestrationPackage.Literals.ORCHESTRATOR__GIT,
				 OrchestrationFactory.eINSTANCE.createGit()));

		newChildDescriptors.add
			(createChildParameter
				(OrchestrationPackage.Literals.ORCHESTRATOR__MAVEN,
				 OrchestrationFactory.eINSTANCE.createMaven()));

		newChildDescriptors.add
			(createChildParameter
				(OrchestrationPackage.Literals.ORCHESTRATOR__LLM,
				 OrchestrationFactory.eINSTANCE.createLLM()));

		newChildDescriptors.add
			(createChildParameter
				(OrchestrationPackage.Literals.ORCHESTRATOR__COMPILER,
				 OrchestrationFactory.eINSTANCE.createCompiler()));

		newChildDescriptors.add
			(createChildParameter
				(OrchestrationPackage.Literals.ORCHESTRATOR__OLLAMA,
				 OrchestrationFactory.eINSTANCE.createOllama()));

		newChildDescriptors.add
			(createChildParameter
				(OrchestrationPackage.Literals.ORCHESTRATOR__AI_CHAT,
				 OrchestrationFactory.eINSTANCE.createAiChat()));

		newChildDescriptors.add
			(createChildParameter
				(OrchestrationPackage.Literals.ORCHESTRATOR__NEURON_AI,
				 OrchestrationFactory.eINSTANCE.createNeuronAI()));

		newChildDescriptors.add
			(createChildParameter
				(OrchestrationPackage.Literals.ORCHESTRATOR__SELF_DEV_SESSION,
				 OrchestrationFactory.eINSTANCE.createSelfDevSession()));

		newChildDescriptors.add
			(createChildParameter
				(OrchestrationPackage.Literals.ORCHESTRATOR__DATABASE,
				 OrchestrationFactory.eINSTANCE.createDatabase()));

		newChildDescriptors.add
			(createChildParameter
				(OrchestrationPackage.Literals.ORCHESTRATOR__FILE_CONFIG,
				 OrchestrationFactory.eINSTANCE.createFileConfig()));

		newChildDescriptors.add
			(createChildParameter
				(OrchestrationPackage.Literals.ORCHESTRATOR__ECLIPSE,
				 OrchestrationFactory.eINSTANCE.createEclipse()));

		newChildDescriptors.add
			(createChildParameter
				(OrchestrationPackage.Literals.ORCHESTRATOR__AI_PROVIDERS,
				 OrchestrationFactory.eINSTANCE.createAIProvider()));

		newChildDescriptors.add
			(createChildParameter
				(OrchestrationPackage.Literals.ORCHESTRATOR__SERVER_SETTINGS,
				 OrchestrationFactory.eINSTANCE.createServerSettings()));

		newChildDescriptors.add
			(createChildParameter
				(OrchestrationPackage.Literals.ORCHESTRATOR__SERVER_SESSIONS,
				 OrchestrationFactory.eINSTANCE.createServerSession()));

		newChildDescriptors.add
			(createChildParameter
				(OrchestrationPackage.Literals.ORCHESTRATOR__MONITORING_HISTORY,
				 OrchestrationFactory.eINSTANCE.createMonitoringData()));

		newChildDescriptors.add
			(createChildParameter
				(OrchestrationPackage.Literals.ORCHESTRATOR__SUPERVISOR_SETTINGS,
				 OrchestrationFactory.eINSTANCE.createSupervisorSettings()));

		newChildDescriptors.add
			(createChildParameter
				(OrchestrationPackage.Literals.ORCHESTRATOR__NETWORK_ENTRIES,
				 OrchestrationFactory.eINSTANCE.createNetworkEntry()));

		newChildDescriptors.add
			(createChildParameter
				(OrchestrationPackage.Literals.ORCHESTRATOR__GENOME_SNAPSHOTS,
				 OrchestrationFactory.eINSTANCE.createGenomeSnapshot()));

		newChildDescriptors.add
			(createChildParameter
				(OrchestrationPackage.Literals.ORCHESTRATOR__FORGE_SESSIONS,
				 OrchestrationFactory.eINSTANCE.createForgeSession()));
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
