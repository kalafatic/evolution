/**
 */
package eu.kalafatic.evolution.model.orchestration.provider;


import eu.kalafatic.evolution.model.orchestration.ChatSession;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.OrchestrationPackage;

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
 * This is the item provider adapter for a {@link eu.kalafatic.evolution.model.orchestration.ChatSession} object.
 * <!-- begin-user-doc -->
 * <!-- end-user-doc -->
 * @generated
 */
public class ChatSessionItemProvider 
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
	public ChatSessionItemProvider(AdapterFactory adapterFactory) {
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
			addIterativeModePropertyDescriptor(object);
			addSelfIterativeModePropertyDescriptor(object);
			addDarwinModePropertyDescriptor(object);
			addGitAutomationPropertyDescriptor(object);
			addMaxIterationsPropertyDescriptor(object);
			addStepModePropertyDescriptor(object);
			addTargetPathPropertyDescriptor(object);
			addTargetTypePropertyDescriptor(object);
			addOutputPathPropertyDescriptor(object);
			addAutoApprovePropertyDescriptor(object);
			addAiModePropertyDescriptor(object);
			addLocalModelPropertyDescriptor(object);
			addRemoteModelPropertyDescriptor(object);
			addBitStatePropertyDescriptor(object);
			addExpansionPropertyDescriptor(object);
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
				 getString("_UI_ChatSession_id_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_ChatSession_id_feature", "_UI_ChatSession_type"),
				 OrchestrationPackage.Literals.CHAT_SESSION__ID,
				 true,
				 false,
				 false,
				 ItemPropertyDescriptor.GENERIC_VALUE_IMAGE,
				 null,
				 null));
	}

	/**
	 * This adds a property descriptor for the Iterative Mode feature.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void addIterativeModePropertyDescriptor(Object object) {
		itemPropertyDescriptors.add
			(createItemPropertyDescriptor
				(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
				 getResourceLocator(),
				 getString("_UI_ChatSession_iterativeMode_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_ChatSession_iterativeMode_feature", "_UI_ChatSession_type"),
				 OrchestrationPackage.Literals.CHAT_SESSION__ITERATIVE_MODE,
				 true,
				 false,
				 false,
				 ItemPropertyDescriptor.BOOLEAN_VALUE_IMAGE,
				 null,
				 null));
	}

	/**
	 * This adds a property descriptor for the Self Iterative Mode feature.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void addSelfIterativeModePropertyDescriptor(Object object) {
		itemPropertyDescriptors.add
			(createItemPropertyDescriptor
				(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
				 getResourceLocator(),
				 getString("_UI_ChatSession_selfIterativeMode_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_ChatSession_selfIterativeMode_feature", "_UI_ChatSession_type"),
				 OrchestrationPackage.Literals.CHAT_SESSION__SELF_ITERATIVE_MODE,
				 true,
				 false,
				 false,
				 ItemPropertyDescriptor.BOOLEAN_VALUE_IMAGE,
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
				 getString("_UI_ChatSession_darwinMode_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_ChatSession_darwinMode_feature", "_UI_ChatSession_type"),
				 OrchestrationPackage.Literals.CHAT_SESSION__DARWIN_MODE,
				 true,
				 false,
				 false,
				 ItemPropertyDescriptor.BOOLEAN_VALUE_IMAGE,
				 null,
				 null));
	}

	/**
	 * This adds a property descriptor for the Git Automation feature.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void addGitAutomationPropertyDescriptor(Object object) {
		itemPropertyDescriptors.add
			(createItemPropertyDescriptor
				(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
				 getResourceLocator(),
				 getString("_UI_ChatSession_gitAutomation_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_ChatSession_gitAutomation_feature", "_UI_ChatSession_type"),
				 OrchestrationPackage.Literals.CHAT_SESSION__GIT_AUTOMATION,
				 true,
				 false,
				 false,
				 ItemPropertyDescriptor.BOOLEAN_VALUE_IMAGE,
				 null,
				 null));
	}

	/**
	 * This adds a property descriptor for the Max Iterations feature.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void addMaxIterationsPropertyDescriptor(Object object) {
		itemPropertyDescriptors.add
			(createItemPropertyDescriptor
				(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
				 getResourceLocator(),
				 getString("_UI_ChatSession_maxIterations_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_ChatSession_maxIterations_feature", "_UI_ChatSession_type"),
				 OrchestrationPackage.Literals.CHAT_SESSION__MAX_ITERATIONS,
				 true,
				 false,
				 false,
				 ItemPropertyDescriptor.INTEGRAL_VALUE_IMAGE,
				 null,
				 null));
	}

	/**
	 * This adds a property descriptor for the Step Mode feature.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void addStepModePropertyDescriptor(Object object) {
		itemPropertyDescriptors.add
			(createItemPropertyDescriptor
				(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
				 getResourceLocator(),
				 getString("_UI_ChatSession_stepMode_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_ChatSession_stepMode_feature", "_UI_ChatSession_type"),
				 OrchestrationPackage.Literals.CHAT_SESSION__STEP_MODE,
				 true,
				 false,
				 false,
				 ItemPropertyDescriptor.BOOLEAN_VALUE_IMAGE,
				 null,
				 null));
	}

	/**
	 * This adds a property descriptor for the Target Path feature.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void addTargetPathPropertyDescriptor(Object object) {
		itemPropertyDescriptors.add
			(createItemPropertyDescriptor
				(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
				 getResourceLocator(),
				 getString("_UI_ChatSession_targetPath_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_ChatSession_targetPath_feature", "_UI_ChatSession_type"),
				 OrchestrationPackage.Literals.CHAT_SESSION__TARGET_PATH,
				 true,
				 false,
				 false,
				 ItemPropertyDescriptor.GENERIC_VALUE_IMAGE,
				 null,
				 null));
	}

	/**
	 * This adds a property descriptor for the Target Type feature.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void addTargetTypePropertyDescriptor(Object object) {
		itemPropertyDescriptors.add
			(createItemPropertyDescriptor
				(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
				 getResourceLocator(),
				 getString("_UI_ChatSession_targetType_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_ChatSession_targetType_feature", "_UI_ChatSession_type"),
				 OrchestrationPackage.Literals.CHAT_SESSION__TARGET_TYPE,
				 true,
				 false,
				 false,
				 ItemPropertyDescriptor.GENERIC_VALUE_IMAGE,
				 null,
				 null));
	}

	/**
	 * This adds a property descriptor for the Output Path feature.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void addOutputPathPropertyDescriptor(Object object) {
		itemPropertyDescriptors.add
			(createItemPropertyDescriptor
				(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
				 getResourceLocator(),
				 getString("_UI_ChatSession_outputPath_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_ChatSession_outputPath_feature", "_UI_ChatSession_type"),
				 OrchestrationPackage.Literals.CHAT_SESSION__OUTPUT_PATH,
				 true,
				 false,
				 false,
				 ItemPropertyDescriptor.GENERIC_VALUE_IMAGE,
				 null,
				 null));
	}

	/**
	 * This adds a property descriptor for the Auto Approve feature.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void addAutoApprovePropertyDescriptor(Object object) {
		itemPropertyDescriptors.add
			(createItemPropertyDescriptor
				(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
				 getResourceLocator(),
				 getString("_UI_ChatSession_autoApprove_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_ChatSession_autoApprove_feature", "_UI_ChatSession_type"),
				 OrchestrationPackage.Literals.CHAT_SESSION__AUTO_APPROVE,
				 true,
				 false,
				 false,
				 ItemPropertyDescriptor.BOOLEAN_VALUE_IMAGE,
				 null,
				 null));
	}

	/**
	 * This adds a property descriptor for the Ai Mode feature.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void addAiModePropertyDescriptor(Object object) {
		itemPropertyDescriptors.add
			(createItemPropertyDescriptor
				(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
				 getResourceLocator(),
				 getString("_UI_ChatSession_aiMode_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_ChatSession_aiMode_feature", "_UI_ChatSession_type"),
				 OrchestrationPackage.Literals.CHAT_SESSION__AI_MODE,
				 true,
				 false,
				 false,
				 ItemPropertyDescriptor.GENERIC_VALUE_IMAGE,
				 null,
				 null));
	}

	/**
	 * This adds a property descriptor for the Local Model feature.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void addLocalModelPropertyDescriptor(Object object) {
		itemPropertyDescriptors.add
			(createItemPropertyDescriptor
				(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
				 getResourceLocator(),
				 getString("_UI_ChatSession_localModel_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_ChatSession_localModel_feature", "_UI_ChatSession_type"),
				 OrchestrationPackage.Literals.CHAT_SESSION__LOCAL_MODEL,
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
				 getString("_UI_ChatSession_remoteModel_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_ChatSession_remoteModel_feature", "_UI_ChatSession_type"),
				 OrchestrationPackage.Literals.CHAT_SESSION__REMOTE_MODEL,
				 true,
				 false,
				 false,
				 ItemPropertyDescriptor.GENERIC_VALUE_IMAGE,
				 null,
				 null));
	}

	/**
	 * This adds a property descriptor for the Bit State feature.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void addBitStatePropertyDescriptor(Object object) {
		itemPropertyDescriptors.add
			(createItemPropertyDescriptor
				(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
				 getResourceLocator(),
				 getString("_UI_ChatSession_bitState_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_ChatSession_bitState_feature", "_UI_ChatSession_type"),
				 OrchestrationPackage.Literals.CHAT_SESSION__BIT_STATE,
				 true,
				 false,
				 false,
				 ItemPropertyDescriptor.INTEGRAL_VALUE_IMAGE,
				 null,
				 null));
	}

	/**
	 * This adds a property descriptor for the Expansion feature.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void addExpansionPropertyDescriptor(Object object) {
		itemPropertyDescriptors.add
			(createItemPropertyDescriptor
				(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
				 getResourceLocator(),
				 getString("_UI_ChatSession_expansion_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_ChatSession_expansion_feature", "_UI_ChatSession_type"),
				 OrchestrationPackage.Literals.CHAT_SESSION__EXPANSION,
				 true,
				 false,
				 false,
				 ItemPropertyDescriptor.INTEGRAL_VALUE_IMAGE,
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
			childrenFeatures.add(OrchestrationPackage.Literals.CHAT_SESSION__MESSAGES);
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
	 * This returns ChatSession.gif.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public Object getImage(Object object) {
		return overlayImage(object, getResourceLocator().getImage("full/obj16/ChatSession"));
	}

	/**
	 * This returns the label text for the adapted class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getText(Object object) {
		String label = ((ChatSession)object).getId();
		return label == null || label.length() == 0 ?
			getString("_UI_ChatSession_type") :
			getString("_UI_ChatSession_type") + " " + label;
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

		switch (notification.getFeatureID(ChatSession.class)) {
			case OrchestrationPackage.CHAT_SESSION__ID:
			case OrchestrationPackage.CHAT_SESSION__ITERATIVE_MODE:
			case OrchestrationPackage.CHAT_SESSION__SELF_ITERATIVE_MODE:
			case OrchestrationPackage.CHAT_SESSION__DARWIN_MODE:
			case OrchestrationPackage.CHAT_SESSION__GIT_AUTOMATION:
			case OrchestrationPackage.CHAT_SESSION__MAX_ITERATIONS:
			case OrchestrationPackage.CHAT_SESSION__STEP_MODE:
			case OrchestrationPackage.CHAT_SESSION__TARGET_PATH:
			case OrchestrationPackage.CHAT_SESSION__TARGET_TYPE:
			case OrchestrationPackage.CHAT_SESSION__OUTPUT_PATH:
			case OrchestrationPackage.CHAT_SESSION__AUTO_APPROVE:
			case OrchestrationPackage.CHAT_SESSION__AI_MODE:
			case OrchestrationPackage.CHAT_SESSION__LOCAL_MODEL:
			case OrchestrationPackage.CHAT_SESSION__REMOTE_MODEL:
			case OrchestrationPackage.CHAT_SESSION__BIT_STATE:
			case OrchestrationPackage.CHAT_SESSION__EXPANSION:
				fireNotifyChanged(new ViewerNotification(notification, notification.getNotifier(), false, true));
				return;
			case OrchestrationPackage.CHAT_SESSION__MESSAGES:
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
				(OrchestrationPackage.Literals.CHAT_SESSION__MESSAGES,
				 OrchestrationFactory.eINSTANCE.createChatMessage()));
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
