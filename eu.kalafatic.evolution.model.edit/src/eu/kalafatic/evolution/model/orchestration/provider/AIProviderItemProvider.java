/**
 */
package eu.kalafatic.evolution.model.orchestration.provider;


import eu.kalafatic.evolution.model.orchestration.AIProvider;
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
 * This is the item provider adapter for a {@link eu.kalafatic.evolution.model.orchestration.AIProvider} object.
 * <!-- begin-user-doc -->
 * <!-- end-user-doc -->
 * @generated
 */
public class AIProviderItemProvider 
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
	public AIProviderItemProvider(AdapterFactory adapterFactory) {
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

			addNamePropertyDescriptor(object);
			addUrlPropertyDescriptor(object);
			addApiKeyPropertyDescriptor(object);
			addFormatPropertyDescriptor(object);
			addLocalPropertyDescriptor(object);
			addDefaultModelPropertyDescriptor(object);
			addApiKeyEncryptedPropertyDescriptor(object);
			addUseEnvVarPropertyDescriptor(object);
			addEnvVarNamePropertyDescriptor(object);
			addStatePropertyDescriptor(object);
			addStateDescriptionPropertyDescriptor(object);
			addRatingPropertyDescriptor(object);
			addRatingAnalyzePropertyDescriptor(object);
			addRatingChatPropertyDescriptor(object);
			addRatingProgrammingPropertyDescriptor(object);
		}
		return itemPropertyDescriptors;
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
				 getString("_UI_AIProvider_name_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_AIProvider_name_feature", "_UI_AIProvider_type"),
				 OrchestrationPackage.Literals.AI_PROVIDER__NAME,
				 true,
				 false,
				 false,
				 ItemPropertyDescriptor.GENERIC_VALUE_IMAGE,
				 null,
				 null));
	}

	/**
	 * This adds a property descriptor for the Url feature.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void addUrlPropertyDescriptor(Object object) {
		itemPropertyDescriptors.add
			(createItemPropertyDescriptor
				(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
				 getResourceLocator(),
				 getString("_UI_AIProvider_url_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_AIProvider_url_feature", "_UI_AIProvider_type"),
				 OrchestrationPackage.Literals.AI_PROVIDER__URL,
				 true,
				 false,
				 false,
				 ItemPropertyDescriptor.GENERIC_VALUE_IMAGE,
				 null,
				 null));
	}

	/**
	 * This adds a property descriptor for the Api Key feature.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void addApiKeyPropertyDescriptor(Object object) {
		itemPropertyDescriptors.add
			(createItemPropertyDescriptor
				(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
				 getResourceLocator(),
				 getString("_UI_AIProvider_apiKey_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_AIProvider_apiKey_feature", "_UI_AIProvider_type"),
				 OrchestrationPackage.Literals.AI_PROVIDER__API_KEY,
				 true,
				 false,
				 false,
				 ItemPropertyDescriptor.GENERIC_VALUE_IMAGE,
				 null,
				 null));
	}

	/**
	 * This adds a property descriptor for the Format feature.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void addFormatPropertyDescriptor(Object object) {
		itemPropertyDescriptors.add
			(createItemPropertyDescriptor
				(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
				 getResourceLocator(),
				 getString("_UI_AIProvider_format_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_AIProvider_format_feature", "_UI_AIProvider_type"),
				 OrchestrationPackage.Literals.AI_PROVIDER__FORMAT,
				 true,
				 false,
				 false,
				 ItemPropertyDescriptor.GENERIC_VALUE_IMAGE,
				 null,
				 null));
	}

	/**
	 * This adds a property descriptor for the Local feature.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void addLocalPropertyDescriptor(Object object) {
		itemPropertyDescriptors.add
			(createItemPropertyDescriptor
				(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
				 getResourceLocator(),
				 getString("_UI_AIProvider_local_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_AIProvider_local_feature", "_UI_AIProvider_type"),
				 OrchestrationPackage.Literals.AI_PROVIDER__LOCAL,
				 true,
				 false,
				 false,
				 ItemPropertyDescriptor.BOOLEAN_VALUE_IMAGE,
				 null,
				 null));
	}

	/**
	 * This adds a property descriptor for the Default Model feature.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void addDefaultModelPropertyDescriptor(Object object) {
		itemPropertyDescriptors.add
			(createItemPropertyDescriptor
				(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
				 getResourceLocator(),
				 getString("_UI_AIProvider_defaultModel_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_AIProvider_defaultModel_feature", "_UI_AIProvider_type"),
				 OrchestrationPackage.Literals.AI_PROVIDER__DEFAULT_MODEL,
				 true,
				 false,
				 false,
				 ItemPropertyDescriptor.GENERIC_VALUE_IMAGE,
				 null,
				 null));
	}

	/**
	 * This adds a property descriptor for the Api Key Encrypted feature.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void addApiKeyEncryptedPropertyDescriptor(Object object) {
		itemPropertyDescriptors.add
			(createItemPropertyDescriptor
				(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
				 getResourceLocator(),
				 getString("_UI_AIProvider_apiKeyEncrypted_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_AIProvider_apiKeyEncrypted_feature", "_UI_AIProvider_type"),
				 OrchestrationPackage.Literals.AI_PROVIDER__API_KEY_ENCRYPTED,
				 true,
				 false,
				 false,
				 ItemPropertyDescriptor.BOOLEAN_VALUE_IMAGE,
				 null,
				 null));
	}

	/**
	 * This adds a property descriptor for the Use Env Var feature.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void addUseEnvVarPropertyDescriptor(Object object) {
		itemPropertyDescriptors.add
			(createItemPropertyDescriptor
				(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
				 getResourceLocator(),
				 getString("_UI_AIProvider_useEnvVar_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_AIProvider_useEnvVar_feature", "_UI_AIProvider_type"),
				 OrchestrationPackage.Literals.AI_PROVIDER__USE_ENV_VAR,
				 true,
				 false,
				 false,
				 ItemPropertyDescriptor.BOOLEAN_VALUE_IMAGE,
				 null,
				 null));
	}

	/**
	 * This adds a property descriptor for the Env Var Name feature.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void addEnvVarNamePropertyDescriptor(Object object) {
		itemPropertyDescriptors.add
			(createItemPropertyDescriptor
				(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
				 getResourceLocator(),
				 getString("_UI_AIProvider_envVarName_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_AIProvider_envVarName_feature", "_UI_AIProvider_type"),
				 OrchestrationPackage.Literals.AI_PROVIDER__ENV_VAR_NAME,
				 true,
				 false,
				 false,
				 ItemPropertyDescriptor.GENERIC_VALUE_IMAGE,
				 null,
				 null));
	}

	/**
	 * This adds a property descriptor for the State feature.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void addStatePropertyDescriptor(Object object) {
		itemPropertyDescriptors.add
			(createItemPropertyDescriptor
				(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
				 getResourceLocator(),
				 getString("_UI_AIProvider_state_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_AIProvider_state_feature", "_UI_AIProvider_type"),
				 OrchestrationPackage.Literals.AI_PROVIDER__STATE,
				 true,
				 false,
				 false,
				 ItemPropertyDescriptor.GENERIC_VALUE_IMAGE,
				 null,
				 null));
	}

	/**
	 * This adds a property descriptor for the State Description feature.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void addStateDescriptionPropertyDescriptor(Object object) {
		itemPropertyDescriptors.add
			(createItemPropertyDescriptor
				(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
				 getResourceLocator(),
				 getString("_UI_AIProvider_stateDescription_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_AIProvider_stateDescription_feature", "_UI_AIProvider_type"),
				 OrchestrationPackage.Literals.AI_PROVIDER__STATE_DESCRIPTION,
				 true,
				 false,
				 false,
				 ItemPropertyDescriptor.GENERIC_VALUE_IMAGE,
				 null,
				 null));
	}

	/**
	 * This adds a property descriptor for the Rating feature.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void addRatingPropertyDescriptor(Object object) {
		itemPropertyDescriptors.add
			(createItemPropertyDescriptor
				(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
				 getResourceLocator(),
				 getString("_UI_AIProvider_rating_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_AIProvider_rating_feature", "_UI_AIProvider_type"),
				 OrchestrationPackage.Literals.AI_PROVIDER__RATING,
				 true,
				 false,
				 false,
				 ItemPropertyDescriptor.INTEGRAL_VALUE_IMAGE,
				 null,
				 null));
	}

	/**
	 * This adds a property descriptor for the Rating Analyze feature.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void addRatingAnalyzePropertyDescriptor(Object object) {
		itemPropertyDescriptors.add
			(createItemPropertyDescriptor
				(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
				 getResourceLocator(),
				 getString("_UI_AIProvider_ratingAnalyze_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_AIProvider_ratingAnalyze_feature", "_UI_AIProvider_type"),
				 OrchestrationPackage.Literals.AI_PROVIDER__RATING_ANALYZE,
				 true,
				 false,
				 false,
				 ItemPropertyDescriptor.INTEGRAL_VALUE_IMAGE,
				 null,
				 null));
	}

	/**
	 * This adds a property descriptor for the Rating Chat feature.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void addRatingChatPropertyDescriptor(Object object) {
		itemPropertyDescriptors.add
			(createItemPropertyDescriptor
				(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
				 getResourceLocator(),
				 getString("_UI_AIProvider_ratingChat_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_AIProvider_ratingChat_feature", "_UI_AIProvider_type"),
				 OrchestrationPackage.Literals.AI_PROVIDER__RATING_CHAT,
				 true,
				 false,
				 false,
				 ItemPropertyDescriptor.INTEGRAL_VALUE_IMAGE,
				 null,
				 null));
	}

	/**
	 * This adds a property descriptor for the Rating Programming feature.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void addRatingProgrammingPropertyDescriptor(Object object) {
		itemPropertyDescriptors.add
			(createItemPropertyDescriptor
				(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
				 getResourceLocator(),
				 getString("_UI_AIProvider_ratingProgramming_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_AIProvider_ratingProgramming_feature", "_UI_AIProvider_type"),
				 OrchestrationPackage.Literals.AI_PROVIDER__RATING_PROGRAMMING,
				 true,
				 false,
				 false,
				 ItemPropertyDescriptor.INTEGRAL_VALUE_IMAGE,
				 null,
				 null));
	}

	/**
	 * This returns AIProvider.gif.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public Object getImage(Object object) {
		return overlayImage(object, getResourceLocator().getImage("full/obj16/AIProvider"));
	}

	/**
	 * This returns the label text for the adapted class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getText(Object object) {
		String label = ((AIProvider)object).getName();
		return label == null || label.length() == 0 ?
			getString("_UI_AIProvider_type") :
			getString("_UI_AIProvider_type") + " " + label;
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

		switch (notification.getFeatureID(AIProvider.class)) {
			case OrchestrationPackage.AI_PROVIDER__NAME:
			case OrchestrationPackage.AI_PROVIDER__URL:
			case OrchestrationPackage.AI_PROVIDER__API_KEY:
			case OrchestrationPackage.AI_PROVIDER__FORMAT:
			case OrchestrationPackage.AI_PROVIDER__LOCAL:
			case OrchestrationPackage.AI_PROVIDER__DEFAULT_MODEL:
			case OrchestrationPackage.AI_PROVIDER__API_KEY_ENCRYPTED:
			case OrchestrationPackage.AI_PROVIDER__USE_ENV_VAR:
			case OrchestrationPackage.AI_PROVIDER__ENV_VAR_NAME:
			case OrchestrationPackage.AI_PROVIDER__STATE:
			case OrchestrationPackage.AI_PROVIDER__STATE_DESCRIPTION:
			case OrchestrationPackage.AI_PROVIDER__RATING:
			case OrchestrationPackage.AI_PROVIDER__RATING_ANALYZE:
			case OrchestrationPackage.AI_PROVIDER__RATING_CHAT:
			case OrchestrationPackage.AI_PROVIDER__RATING_PROGRAMMING:
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
