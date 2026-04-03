/**
 */
package eu.kalafatic.evolution.model.orchestration.provider;


import eu.kalafatic.evolution.model.orchestration.EvaluationResult;
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
 * This is the item provider adapter for a {@link eu.kalafatic.evolution.model.orchestration.EvaluationResult} object.
 * @generated
 */
public class EvaluationResultItemProvider
	extends ItemProviderAdapter
	implements
		IEditingDomainItemProvider,
		IStructuredItemContentProvider,
		ITreeItemContentProvider,
		IItemLabelProvider,
		IItemPropertySource {
	/**
	 * @generated
	 */
	public EvaluationResultItemProvider(AdapterFactory adapterFactory) {
		super(adapterFactory);
	}

	/**
	 * @generated
	 */
	@Override
	public List<IItemPropertyDescriptor> getPropertyDescriptors(Object object) {
		if (itemPropertyDescriptors == null) {
			super.getPropertyDescriptors(object);

			addSuccessPropertyDescriptor(object);
			addTestPassRatePropertyDescriptor(object);
			addCoverageChangePropertyDescriptor(object);
			addErrorsPropertyDescriptor(object);
			addDecisionPropertyDescriptor(object);
			addUserSatisfactionPropertyDescriptor(object);
		}
		return itemPropertyDescriptors;
	}

	protected void addSuccessPropertyDescriptor(Object object) {
		itemPropertyDescriptors.add(createItemPropertyDescriptor(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(), getResourceLocator(), "Success", "Success", OrchestrationPackage.Literals.EVALUATION_RESULT__SUCCESS, true, false, false, ItemPropertyDescriptor.BOOLEAN_VALUE_IMAGE, null, null));
	}
	protected void addTestPassRatePropertyDescriptor(Object object) {
		itemPropertyDescriptors.add(createItemPropertyDescriptor(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(), getResourceLocator(), "Test Pass Rate", "Test Pass Rate", OrchestrationPackage.Literals.EVALUATION_RESULT__TEST_PASS_RATE, true, false, false, ItemPropertyDescriptor.REAL_VALUE_IMAGE, null, null));
	}
	protected void addCoverageChangePropertyDescriptor(Object object) {
		itemPropertyDescriptors.add(createItemPropertyDescriptor(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(), getResourceLocator(), "Coverage Change", "Coverage Change", OrchestrationPackage.Literals.EVALUATION_RESULT__COVERAGE_CHANGE, true, false, false, ItemPropertyDescriptor.REAL_VALUE_IMAGE, null, null));
	}
	protected void addErrorsPropertyDescriptor(Object object) {
		itemPropertyDescriptors.add(createItemPropertyDescriptor(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(), getResourceLocator(), "Errors", "Errors", OrchestrationPackage.Literals.EVALUATION_RESULT__ERRORS, true, false, false, ItemPropertyDescriptor.GENERIC_VALUE_IMAGE, null, null));
	}
	protected void addDecisionPropertyDescriptor(Object object) {
		itemPropertyDescriptors.add(createItemPropertyDescriptor(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(), getResourceLocator(), "Decision", "Decision", OrchestrationPackage.Literals.EVALUATION_RESULT__DECISION, true, false, false, ItemPropertyDescriptor.GENERIC_VALUE_IMAGE, null, null));
	}
	protected void addUserSatisfactionPropertyDescriptor(Object object) {
		itemPropertyDescriptors.add(createItemPropertyDescriptor(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(), getResourceLocator(), "User Satisfaction", "User Satisfaction", OrchestrationPackage.Literals.EVALUATION_RESULT__USER_SATISFACTION, true, false, false, ItemPropertyDescriptor.REAL_VALUE_IMAGE, null, null));
	}

	@Override
	public Object getImage(Object object) {
		return overlayImage(object, getResourceLocator().getImage("full/obj16/EvaluationResult"));
	}

	@Override
	public String getText(Object object) {
		return "Evaluation Result";
	}

	@Override
	public void notifyChanged(Notification notification) {
		updateChildren(notification);
		switch (notification.getFeatureID(EvaluationResult.class)) {
			case OrchestrationPackage.EVALUATION_RESULT__SUCCESS:
			case OrchestrationPackage.EVALUATION_RESULT__TEST_PASS_RATE:
			case OrchestrationPackage.EVALUATION_RESULT__COVERAGE_CHANGE:
			case OrchestrationPackage.EVALUATION_RESULT__ERRORS:
			case OrchestrationPackage.EVALUATION_RESULT__DECISION:
			case OrchestrationPackage.EVALUATION_RESULT__USER_SATISFACTION:
				fireNotifyChanged(new ViewerNotification(notification, notification.getNotifier(), false, true));
				return;
		}
		super.notifyChanged(notification);
	}

	@Override
	public ResourceLocator getResourceLocator() {
		return EvolutionEditPlugin.INSTANCE;
	}
}
