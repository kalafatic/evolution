/**
 */
package eu.kalafatic.evolution.model.orchestration.provider;


import eu.kalafatic.evolution.model.orchestration.ForgeSession;
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
 * This is the item provider adapter for a {@link eu.kalafatic.evolution.model.orchestration.ForgeSession} object.
 * <!-- begin-user-doc -->
 * <!-- end-user-doc -->
 * @generated
 */
public class ForgeSessionItemProvider 
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
	public ForgeSessionItemProvider(AdapterFactory adapterFactory) {
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

			addSessionIdPropertyDescriptor(object);
			addNamePropertyDescriptor(object);
			addCreatedAtPropertyDescriptor(object);
			addLastModifiedPropertyDescriptor(object);
			addStatusPropertyDescriptor(object);
			addActiveModelIdPropertyDescriptor(object);
			addSelectedModelTypePropertyDescriptor(object);
		}
		return itemPropertyDescriptors;
	}

	/**
	 * This adds a property descriptor for the Session Id feature.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void addSessionIdPropertyDescriptor(Object object) {
		itemPropertyDescriptors.add
			(createItemPropertyDescriptor
				(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
				 getResourceLocator(),
				 getString("_UI_ForgeSession_sessionId_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_ForgeSession_sessionId_feature", "_UI_ForgeSession_type"),
				 OrchestrationPackage.Literals.FORGE_SESSION__SESSION_ID,
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
				 getString("_UI_ForgeSession_name_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_ForgeSession_name_feature", "_UI_ForgeSession_type"),
				 OrchestrationPackage.Literals.FORGE_SESSION__NAME,
				 true,
				 false,
				 false,
				 ItemPropertyDescriptor.GENERIC_VALUE_IMAGE,
				 null,
				 null));
	}

	/**
	 * This adds a property descriptor for the Created At feature.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void addCreatedAtPropertyDescriptor(Object object) {
		itemPropertyDescriptors.add
			(createItemPropertyDescriptor
				(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
				 getResourceLocator(),
				 getString("_UI_ForgeSession_createdAt_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_ForgeSession_createdAt_feature", "_UI_ForgeSession_type"),
				 OrchestrationPackage.Literals.FORGE_SESSION__CREATED_AT,
				 true,
				 false,
				 false,
				 ItemPropertyDescriptor.INTEGRAL_VALUE_IMAGE,
				 null,
				 null));
	}

	/**
	 * This adds a property descriptor for the Last Modified feature.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void addLastModifiedPropertyDescriptor(Object object) {
		itemPropertyDescriptors.add
			(createItemPropertyDescriptor
				(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
				 getResourceLocator(),
				 getString("_UI_ForgeSession_lastModified_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_ForgeSession_lastModified_feature", "_UI_ForgeSession_type"),
				 OrchestrationPackage.Literals.FORGE_SESSION__LAST_MODIFIED,
				 true,
				 false,
				 false,
				 ItemPropertyDescriptor.INTEGRAL_VALUE_IMAGE,
				 null,
				 null));
	}

	/**
	 * This adds a property descriptor for the Status feature.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void addStatusPropertyDescriptor(Object object) {
		itemPropertyDescriptors.add
			(createItemPropertyDescriptor
				(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
				 getResourceLocator(),
				 getString("_UI_ForgeSession_status_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_ForgeSession_status_feature", "_UI_ForgeSession_type"),
				 OrchestrationPackage.Literals.FORGE_SESSION__STATUS,
				 true,
				 false,
				 false,
				 ItemPropertyDescriptor.GENERIC_VALUE_IMAGE,
				 null,
				 null));
	}

	/**
	 * This adds a property descriptor for the Active Model Id feature.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void addActiveModelIdPropertyDescriptor(Object object) {
		itemPropertyDescriptors.add
			(createItemPropertyDescriptor
				(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
				 getResourceLocator(),
				 getString("_UI_ForgeSession_activeModelId_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_ForgeSession_activeModelId_feature", "_UI_ForgeSession_type"),
				 OrchestrationPackage.Literals.FORGE_SESSION__ACTIVE_MODEL_ID,
				 true,
				 false,
				 false,
				 ItemPropertyDescriptor.GENERIC_VALUE_IMAGE,
				 null,
				 null));
	}

	/**
	 * This adds a property descriptor for the Selected Model Type feature.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void addSelectedModelTypePropertyDescriptor(Object object) {
		itemPropertyDescriptors.add
			(createItemPropertyDescriptor
				(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
				 getResourceLocator(),
				 getString("_UI_ForgeSession_selectedModelType_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_ForgeSession_selectedModelType_feature", "_UI_ForgeSession_type"),
				 OrchestrationPackage.Literals.FORGE_SESSION__SELECTED_MODEL_TYPE,
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
			childrenFeatures.add(OrchestrationPackage.Literals.FORGE_SESSION__GIT);
			childrenFeatures.add(OrchestrationPackage.Literals.FORGE_SESSION__MODEL_STATE);
			childrenFeatures.add(OrchestrationPackage.Literals.FORGE_SESSION__EXPERIMENTS);
			childrenFeatures.add(OrchestrationPackage.Literals.FORGE_SESSION__SNAPSHOTS);
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
	 * This returns ForgeSession.gif.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public Object getImage(Object object) {
		return overlayImage(object, getResourceLocator().getImage("full/obj16/ForgeSession"));
	}

	/**
	 * This returns the label text for the adapted class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getText(Object object) {
		String label = ((ForgeSession)object).getName();
		return label == null || label.length() == 0 ?
			getString("_UI_ForgeSession_type") :
			getString("_UI_ForgeSession_type") + " " + label;
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

		switch (notification.getFeatureID(ForgeSession.class)) {
			case OrchestrationPackage.FORGE_SESSION__SESSION_ID:
			case OrchestrationPackage.FORGE_SESSION__NAME:
			case OrchestrationPackage.FORGE_SESSION__CREATED_AT:
			case OrchestrationPackage.FORGE_SESSION__LAST_MODIFIED:
			case OrchestrationPackage.FORGE_SESSION__STATUS:
			case OrchestrationPackage.FORGE_SESSION__ACTIVE_MODEL_ID:
			case OrchestrationPackage.FORGE_SESSION__SELECTED_MODEL_TYPE:
				fireNotifyChanged(new ViewerNotification(notification, notification.getNotifier(), false, true));
				return;
			case OrchestrationPackage.FORGE_SESSION__GIT:
			case OrchestrationPackage.FORGE_SESSION__MODEL_STATE:
			case OrchestrationPackage.FORGE_SESSION__EXPERIMENTS:
			case OrchestrationPackage.FORGE_SESSION__SNAPSHOTS:
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
				(OrchestrationPackage.Literals.FORGE_SESSION__GIT,
				 OrchestrationFactory.eINSTANCE.createGit()));

		newChildDescriptors.add
			(createChildParameter
				(OrchestrationPackage.Literals.FORGE_SESSION__MODEL_STATE,
				 OrchestrationFactory.eINSTANCE.createSessionModelState()));

		newChildDescriptors.add
			(createChildParameter
				(OrchestrationPackage.Literals.FORGE_SESSION__EXPERIMENTS,
				 OrchestrationFactory.eINSTANCE.createSessionExperiment()));

		newChildDescriptors.add
			(createChildParameter
				(OrchestrationPackage.Literals.FORGE_SESSION__SNAPSHOTS,
				 OrchestrationFactory.eINSTANCE.createSessionSnapshot()));
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
