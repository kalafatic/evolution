/**
 */
package eu.kalafatic.evolution.model.orchestration.impl;

import eu.kalafatic.evolution.model.orchestration.EvaluationResult;
import eu.kalafatic.evolution.model.orchestration.Iteration;
import eu.kalafatic.evolution.model.orchestration.IterationStatus;
import eu.kalafatic.evolution.model.orchestration.OrchestrationPackage;
import eu.kalafatic.evolution.model.orchestration.Task;

import java.util.Collection;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.NotificationChain;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.InternalEObject;

import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.MinimalEObjectImpl;

import org.eclipse.emf.ecore.util.EObjectContainmentEList;
import org.eclipse.emf.ecore.util.InternalEList;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Iteration</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * </p>
 * <ul>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.IterationImpl#getId <em>Id</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.IterationImpl#getBranchName <em>Branch Name</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.IterationImpl#getTasks <em>Tasks</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.IterationImpl#getEvaluationResult <em>Evaluation Result</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.IterationImpl#getStatus <em>Status</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.IterationImpl#getPhase <em>Phase</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.IterationImpl#getComments <em>Comments</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.IterationImpl#getRating <em>Rating</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.IterationImpl#getRationale <em>Rationale</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.IterationImpl#getJustification <em>Justification</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.IterationImpl#getSemanticPressure <em>Semantic Pressure</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.IterationImpl#getSurvivalArgument <em>Survival Argument</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.IterationImpl#getTradeoffs <em>Tradeoffs</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.IterationImpl#getFailureRisks <em>Failure Risks</em>}</li>
 * </ul>
 *
 * @generated
 */
public class IterationImpl extends MinimalEObjectImpl.Container implements Iteration {
	/**
	 * The default value of the '{@link #getId() <em>Id</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getId()
	 * @generated
	 * @ordered
	 */
	protected static final String ID_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getId() <em>Id</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getId()
	 * @generated
	 * @ordered
	 */
	protected String id = ID_EDEFAULT;

	/**
	 * The default value of the '{@link #getBranchName() <em>Branch Name</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getBranchName()
	 * @generated
	 * @ordered
	 */
	protected static final String BRANCH_NAME_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getBranchName() <em>Branch Name</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getBranchName()
	 * @generated
	 * @ordered
	 */
	protected String branchName = BRANCH_NAME_EDEFAULT;

	/**
	 * The cached value of the '{@link #getTasks() <em>Tasks</em>}' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getTasks()
	 * @generated
	 * @ordered
	 */
	protected EList<Task> tasks;

	/**
	 * The cached value of the '{@link #getEvaluationResult() <em>Evaluation Result</em>}' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getEvaluationResult()
	 * @generated
	 * @ordered
	 */
	protected EvaluationResult evaluationResult;

	/**
	 * The default value of the '{@link #getStatus() <em>Status</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getStatus()
	 * @generated
	 * @ordered
	 */
	protected static final IterationStatus STATUS_EDEFAULT = IterationStatus.PENDING;

	/**
	 * The cached value of the '{@link #getStatus() <em>Status</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getStatus()
	 * @generated
	 * @ordered
	 */
	protected IterationStatus status = STATUS_EDEFAULT;

	/**
	 * The default value of the '{@link #getPhase() <em>Phase</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getPhase()
	 * @generated
	 * @ordered
	 */
	protected static final String PHASE_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getPhase() <em>Phase</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getPhase()
	 * @generated
	 * @ordered
	 */
	protected String phase = PHASE_EDEFAULT;

	/**
	 * The default value of the '{@link #getComments() <em>Comments</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getComments()
	 * @generated
	 * @ordered
	 */
	protected static final String COMMENTS_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getComments() <em>Comments</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getComments()
	 * @generated
	 * @ordered
	 */
	protected String comments = COMMENTS_EDEFAULT;

	/**
	 * The default value of the '{@link #getRating() <em>Rating</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getRating()
	 * @generated
	 * @ordered
	 */
	protected static final int RATING_EDEFAULT = 0;

	/**
	 * The cached value of the '{@link #getRating() <em>Rating</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getRating()
	 * @generated
	 * @ordered
	 */
	protected int rating = RATING_EDEFAULT;

	/**
	 * The default value of the '{@link #getRationale() <em>Rationale</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getRationale()
	 * @generated
	 * @ordered
	 */
	protected static final String RATIONALE_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getRationale() <em>Rationale</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getRationale()
	 * @generated
	 * @ordered
	 */
	protected String rationale = RATIONALE_EDEFAULT;

	/**
	 * The default value of the '{@link #getJustification() <em>Justification</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getJustification()
	 * @generated
	 * @ordered
	 */
	protected static final String JUSTIFICATION_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getJustification() <em>Justification</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getJustification()
	 * @generated
	 * @ordered
	 */
	protected String justification = JUSTIFICATION_EDEFAULT;

	/**
	 * The default value of the '{@link #getSemanticPressure() <em>Semantic Pressure</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getSemanticPressure()
	 * @generated
	 * @ordered
	 */
	protected static final String SEMANTIC_PRESSURE_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getSemanticPressure() <em>Semantic Pressure</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getSemanticPressure()
	 * @generated
	 * @ordered
	 */
	protected String semanticPressure = SEMANTIC_PRESSURE_EDEFAULT;

	/**
	 * The default value of the '{@link #getSurvivalArgument() <em>Survival Argument</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getSurvivalArgument()
	 * @generated
	 * @ordered
	 */
	protected static final String SURVIVAL_ARGUMENT_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getSurvivalArgument() <em>Survival Argument</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getSurvivalArgument()
	 * @generated
	 * @ordered
	 */
	protected String survivalArgument = SURVIVAL_ARGUMENT_EDEFAULT;

	/**
	 * The default value of the '{@link #getTradeoffs() <em>Tradeoffs</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getTradeoffs()
	 * @generated
	 * @ordered
	 */
	protected static final String TRADEOFFS_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getTradeoffs() <em>Tradeoffs</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getTradeoffs()
	 * @generated
	 * @ordered
	 */
	protected String tradeoffs = TRADEOFFS_EDEFAULT;

	/**
	 * The default value of the '{@link #getFailureRisks() <em>Failure Risks</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getFailureRisks()
	 * @generated
	 * @ordered
	 */
	protected static final String FAILURE_RISKS_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getFailureRisks() <em>Failure Risks</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getFailureRisks()
	 * @generated
	 * @ordered
	 */
	protected String failureRisks = FAILURE_RISKS_EDEFAULT;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected IterationImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return OrchestrationPackage.Literals.ITERATION;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getId() {
		return id;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setId(String newId) {
		String oldId = id;
		id = newId;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.ITERATION__ID, oldId, id));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getBranchName() {
		return branchName;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setBranchName(String newBranchName) {
		String oldBranchName = branchName;
		branchName = newBranchName;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.ITERATION__BRANCH_NAME, oldBranchName, branchName));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EList<Task> getTasks() {
		if (tasks == null) {
			tasks = new EObjectContainmentEList<Task>(Task.class, this, OrchestrationPackage.ITERATION__TASKS);
		}
		return tasks;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EvaluationResult getEvaluationResult() {
		return evaluationResult;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public NotificationChain basicSetEvaluationResult(EvaluationResult newEvaluationResult, NotificationChain msgs) {
		EvaluationResult oldEvaluationResult = evaluationResult;
		evaluationResult = newEvaluationResult;
		if (eNotificationRequired()) {
			ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, OrchestrationPackage.ITERATION__EVALUATION_RESULT, oldEvaluationResult, newEvaluationResult);
			if (msgs == null) msgs = notification; else msgs.add(notification);
		}
		return msgs;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setEvaluationResult(EvaluationResult newEvaluationResult) {
		if (newEvaluationResult != evaluationResult) {
			NotificationChain msgs = null;
			if (evaluationResult != null)
				msgs = ((InternalEObject)evaluationResult).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - OrchestrationPackage.ITERATION__EVALUATION_RESULT, null, msgs);
			if (newEvaluationResult != null)
				msgs = ((InternalEObject)newEvaluationResult).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - OrchestrationPackage.ITERATION__EVALUATION_RESULT, null, msgs);
			msgs = basicSetEvaluationResult(newEvaluationResult, msgs);
			if (msgs != null) msgs.dispatch();
		}
		else if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.ITERATION__EVALUATION_RESULT, newEvaluationResult, newEvaluationResult));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public IterationStatus getStatus() {
		return status;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setStatus(IterationStatus newStatus) {
		IterationStatus oldStatus = status;
		status = newStatus == null ? STATUS_EDEFAULT : newStatus;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.ITERATION__STATUS, oldStatus, status));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getPhase() {
		return phase;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setPhase(String newPhase) {
		String oldPhase = phase;
		phase = newPhase;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.ITERATION__PHASE, oldPhase, phase));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getComments() {
		return comments;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setComments(String newComments) {
		String oldComments = comments;
		comments = newComments;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.ITERATION__COMMENTS, oldComments, comments));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public int getRating() {
		return rating;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setRating(int newRating) {
		int oldRating = rating;
		rating = newRating;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.ITERATION__RATING, oldRating, rating));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getRationale() {
		return rationale;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setRationale(String newRationale) {
		String oldRationale = rationale;
		rationale = newRationale;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.ITERATION__RATIONALE, oldRationale, rationale));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getJustification() {
		return justification;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setJustification(String newJustification) {
		String oldJustification = justification;
		justification = newJustification;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.ITERATION__JUSTIFICATION, oldJustification, justification));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getSemanticPressure() {
		return semanticPressure;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setSemanticPressure(String newSemanticPressure) {
		String oldSemanticPressure = semanticPressure;
		semanticPressure = newSemanticPressure;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.ITERATION__SEMANTIC_PRESSURE, oldSemanticPressure, semanticPressure));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getSurvivalArgument() {
		return survivalArgument;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setSurvivalArgument(String newSurvivalArgument) {
		String oldSurvivalArgument = survivalArgument;
		survivalArgument = newSurvivalArgument;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.ITERATION__SURVIVAL_ARGUMENT, oldSurvivalArgument, survivalArgument));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getTradeoffs() {
		return tradeoffs;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setTradeoffs(String newTradeoffs) {
		String oldTradeoffs = tradeoffs;
		tradeoffs = newTradeoffs;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.ITERATION__TRADEOFFS, oldTradeoffs, tradeoffs));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getFailureRisks() {
		return failureRisks;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setFailureRisks(String newFailureRisks) {
		String oldFailureRisks = failureRisks;
		failureRisks = newFailureRisks;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.ITERATION__FAILURE_RISKS, oldFailureRisks, failureRisks));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
		switch (featureID) {
			case OrchestrationPackage.ITERATION__TASKS:
				return ((InternalEList<?>)getTasks()).basicRemove(otherEnd, msgs);
			case OrchestrationPackage.ITERATION__EVALUATION_RESULT:
				return basicSetEvaluationResult(null, msgs);
		}
		return super.eInverseRemove(otherEnd, featureID, msgs);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public Object eGet(int featureID, boolean resolve, boolean coreType) {
		switch (featureID) {
			case OrchestrationPackage.ITERATION__ID:
				return getId();
			case OrchestrationPackage.ITERATION__BRANCH_NAME:
				return getBranchName();
			case OrchestrationPackage.ITERATION__TASKS:
				return getTasks();
			case OrchestrationPackage.ITERATION__EVALUATION_RESULT:
				return getEvaluationResult();
			case OrchestrationPackage.ITERATION__STATUS:
				return getStatus();
			case OrchestrationPackage.ITERATION__PHASE:
				return getPhase();
			case OrchestrationPackage.ITERATION__COMMENTS:
				return getComments();
			case OrchestrationPackage.ITERATION__RATING:
				return getRating();
			case OrchestrationPackage.ITERATION__RATIONALE:
				return getRationale();
			case OrchestrationPackage.ITERATION__JUSTIFICATION:
				return getJustification();
			case OrchestrationPackage.ITERATION__SEMANTIC_PRESSURE:
				return getSemanticPressure();
			case OrchestrationPackage.ITERATION__SURVIVAL_ARGUMENT:
				return getSurvivalArgument();
			case OrchestrationPackage.ITERATION__TRADEOFFS:
				return getTradeoffs();
			case OrchestrationPackage.ITERATION__FAILURE_RISKS:
				return getFailureRisks();
		}
		return super.eGet(featureID, resolve, coreType);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void eSet(int featureID, Object newValue) {
		switch (featureID) {
			case OrchestrationPackage.ITERATION__ID:
				setId((String)newValue);
				return;
			case OrchestrationPackage.ITERATION__BRANCH_NAME:
				setBranchName((String)newValue);
				return;
			case OrchestrationPackage.ITERATION__TASKS:
				getTasks().clear();
				getTasks().addAll((Collection<? extends Task>)newValue);
				return;
			case OrchestrationPackage.ITERATION__EVALUATION_RESULT:
				setEvaluationResult((EvaluationResult)newValue);
				return;
			case OrchestrationPackage.ITERATION__STATUS:
				setStatus((IterationStatus)newValue);
				return;
			case OrchestrationPackage.ITERATION__PHASE:
				setPhase((String)newValue);
				return;
			case OrchestrationPackage.ITERATION__COMMENTS:
				setComments((String)newValue);
				return;
			case OrchestrationPackage.ITERATION__RATING:
				setRating((Integer)newValue);
				return;
			case OrchestrationPackage.ITERATION__RATIONALE:
				setRationale((String)newValue);
				return;
			case OrchestrationPackage.ITERATION__JUSTIFICATION:
				setJustification((String)newValue);
				return;
			case OrchestrationPackage.ITERATION__SEMANTIC_PRESSURE:
				setSemanticPressure((String)newValue);
				return;
			case OrchestrationPackage.ITERATION__SURVIVAL_ARGUMENT:
				setSurvivalArgument((String)newValue);
				return;
			case OrchestrationPackage.ITERATION__TRADEOFFS:
				setTradeoffs((String)newValue);
				return;
			case OrchestrationPackage.ITERATION__FAILURE_RISKS:
				setFailureRisks((String)newValue);
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
			case OrchestrationPackage.ITERATION__ID:
				setId(ID_EDEFAULT);
				return;
			case OrchestrationPackage.ITERATION__BRANCH_NAME:
				setBranchName(BRANCH_NAME_EDEFAULT);
				return;
			case OrchestrationPackage.ITERATION__TASKS:
				getTasks().clear();
				return;
			case OrchestrationPackage.ITERATION__EVALUATION_RESULT:
				setEvaluationResult((EvaluationResult)null);
				return;
			case OrchestrationPackage.ITERATION__STATUS:
				setStatus(STATUS_EDEFAULT);
				return;
			case OrchestrationPackage.ITERATION__PHASE:
				setPhase(PHASE_EDEFAULT);
				return;
			case OrchestrationPackage.ITERATION__COMMENTS:
				setComments(COMMENTS_EDEFAULT);
				return;
			case OrchestrationPackage.ITERATION__RATING:
				setRating(RATING_EDEFAULT);
				return;
			case OrchestrationPackage.ITERATION__RATIONALE:
				setRationale(RATIONALE_EDEFAULT);
				return;
			case OrchestrationPackage.ITERATION__JUSTIFICATION:
				setJustification(JUSTIFICATION_EDEFAULT);
				return;
			case OrchestrationPackage.ITERATION__SEMANTIC_PRESSURE:
				setSemanticPressure(SEMANTIC_PRESSURE_EDEFAULT);
				return;
			case OrchestrationPackage.ITERATION__SURVIVAL_ARGUMENT:
				setSurvivalArgument(SURVIVAL_ARGUMENT_EDEFAULT);
				return;
			case OrchestrationPackage.ITERATION__TRADEOFFS:
				setTradeoffs(TRADEOFFS_EDEFAULT);
				return;
			case OrchestrationPackage.ITERATION__FAILURE_RISKS:
				setFailureRisks(FAILURE_RISKS_EDEFAULT);
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
			case OrchestrationPackage.ITERATION__ID:
				return ID_EDEFAULT == null ? id != null : !ID_EDEFAULT.equals(id);
			case OrchestrationPackage.ITERATION__BRANCH_NAME:
				return BRANCH_NAME_EDEFAULT == null ? branchName != null : !BRANCH_NAME_EDEFAULT.equals(branchName);
			case OrchestrationPackage.ITERATION__TASKS:
				return tasks != null && !tasks.isEmpty();
			case OrchestrationPackage.ITERATION__EVALUATION_RESULT:
				return evaluationResult != null;
			case OrchestrationPackage.ITERATION__STATUS:
				return status != STATUS_EDEFAULT;
			case OrchestrationPackage.ITERATION__PHASE:
				return PHASE_EDEFAULT == null ? phase != null : !PHASE_EDEFAULT.equals(phase);
			case OrchestrationPackage.ITERATION__COMMENTS:
				return COMMENTS_EDEFAULT == null ? comments != null : !COMMENTS_EDEFAULT.equals(comments);
			case OrchestrationPackage.ITERATION__RATING:
				return rating != RATING_EDEFAULT;
			case OrchestrationPackage.ITERATION__RATIONALE:
				return RATIONALE_EDEFAULT == null ? rationale != null : !RATIONALE_EDEFAULT.equals(rationale);
			case OrchestrationPackage.ITERATION__JUSTIFICATION:
				return JUSTIFICATION_EDEFAULT == null ? justification != null : !JUSTIFICATION_EDEFAULT.equals(justification);
			case OrchestrationPackage.ITERATION__SEMANTIC_PRESSURE:
				return SEMANTIC_PRESSURE_EDEFAULT == null ? semanticPressure != null : !SEMANTIC_PRESSURE_EDEFAULT.equals(semanticPressure);
			case OrchestrationPackage.ITERATION__SURVIVAL_ARGUMENT:
				return SURVIVAL_ARGUMENT_EDEFAULT == null ? survivalArgument != null : !SURVIVAL_ARGUMENT_EDEFAULT.equals(survivalArgument);
			case OrchestrationPackage.ITERATION__TRADEOFFS:
				return TRADEOFFS_EDEFAULT == null ? tradeoffs != null : !TRADEOFFS_EDEFAULT.equals(tradeoffs);
			case OrchestrationPackage.ITERATION__FAILURE_RISKS:
				return FAILURE_RISKS_EDEFAULT == null ? failureRisks != null : !FAILURE_RISKS_EDEFAULT.equals(failureRisks);
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
		result.append(" (id: ");
		result.append(id);
		result.append(", branchName: ");
		result.append(branchName);
		result.append(", status: ");
		result.append(status);
		result.append(", phase: ");
		result.append(phase);
		result.append(", comments: ");
		result.append(comments);
		result.append(", rating: ");
		result.append(rating);
		result.append(", rationale: ");
		result.append(rationale);
		result.append(", justification: ");
		result.append(justification);
		result.append(", semanticPressure: ");
		result.append(semanticPressure);
		result.append(", survivalArgument: ");
		result.append(survivalArgument);
		result.append(", tradeoffs: ");
		result.append(tradeoffs);
		result.append(", failureRisks: ");
		result.append(failureRisks);
		result.append(')');
		return result.toString();
	}

} //IterationImpl
