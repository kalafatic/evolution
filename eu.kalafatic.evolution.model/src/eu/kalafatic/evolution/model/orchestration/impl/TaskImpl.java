/**
 */
package eu.kalafatic.evolution.model.orchestration.impl;

import java.util.Collection;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.NotificationChain;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.MinimalEObjectImpl;
import org.eclipse.emf.ecore.util.EObjectContainmentEList;
import org.eclipse.emf.ecore.util.EObjectResolvingEList;
import org.eclipse.emf.ecore.util.InternalEList;

import eu.kalafatic.evolution.model.orchestration.OrchestrationPackage;
import eu.kalafatic.evolution.model.orchestration.Task;
import eu.kalafatic.evolution.model.orchestration.TaskStatus;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Task</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * </p>
 * <ul>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.TaskImpl#getId <em>Id</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.TaskImpl#getName <em>Name</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.TaskImpl#getType <em>Type</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.TaskImpl#getStatus <em>Status</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.TaskImpl#getNext <em>Next</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.TaskImpl#getSubTasks <em>Sub Tasks</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.TaskImpl#getResponse <em>Response</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.TaskImpl#getFeedback <em>Feedback</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.TaskImpl#isApprovalRequired <em>Approval Required</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.TaskImpl#getLoopToTaskId <em>Loop To Task Id</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.TaskImpl#getPriority <em>Priority</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.TaskImpl#getResultSummary <em>Result Summary</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.TaskImpl#getDescription <em>Description</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.TaskImpl#getRating <em>Rating</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.TaskImpl#isLikes <em>Likes</em>}</li>
 * </ul>
 *
 * @generated
 */
public class TaskImpl extends MinimalEObjectImpl.Container implements Task {
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
	 * The default value of the '{@link #getName() <em>Name</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getName()
	 * @generated
	 * @ordered
	 */
	protected static final String NAME_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getName() <em>Name</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getName()
	 * @generated
	 * @ordered
	 */
	protected String name = NAME_EDEFAULT;

	/**
	 * The default value of the '{@link #getType() <em>Type</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getType()
	 * @generated
	 * @ordered
	 */
	protected static final String TYPE_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getType() <em>Type</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getType()
	 * @generated
	 * @ordered
	 */
	protected String type = TYPE_EDEFAULT;

	/**
	 * The default value of the '{@link #getStatus() <em>Status</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getStatus()
	 * @generated
	 * @ordered
	 */
	protected static final TaskStatus STATUS_EDEFAULT = TaskStatus.PENDING;

	/**
	 * The cached value of the '{@link #getStatus() <em>Status</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getStatus()
	 * @generated
	 * @ordered
	 */
	protected TaskStatus status = STATUS_EDEFAULT;

	/**
	 * The cached value of the '{@link #getNext() <em>Next</em>}' reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getNext()
	 * @generated
	 * @ordered
	 */
	protected EList<Task> next;

	/**
	 * The cached value of the '{@link #getSubTasks() <em>Sub Tasks</em>}' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getSubTasks()
	 * @generated
	 * @ordered
	 */
	protected EList<Task> subTasks;

	/**
	 * The default value of the '{@link #getResponse() <em>Response</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getResponse()
	 * @generated
	 * @ordered
	 */
	protected static final String RESPONSE_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getResponse() <em>Response</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getResponse()
	 * @generated
	 * @ordered
	 */
	protected String response = RESPONSE_EDEFAULT;

	/**
	 * The default value of the '{@link #getFeedback() <em>Feedback</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getFeedback()
	 * @generated
	 * @ordered
	 */
	protected static final String FEEDBACK_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getFeedback() <em>Feedback</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getFeedback()
	 * @generated
	 * @ordered
	 */
	protected String feedback = FEEDBACK_EDEFAULT;

	/**
	 * The default value of the '{@link #isApprovalRequired() <em>Approval Required</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isApprovalRequired()
	 * @generated
	 * @ordered
	 */
	protected static final boolean APPROVAL_REQUIRED_EDEFAULT = true;

	/**
	 * The cached value of the '{@link #isApprovalRequired() <em>Approval Required</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isApprovalRequired()
	 * @generated
	 * @ordered
	 */
	protected boolean approvalRequired = APPROVAL_REQUIRED_EDEFAULT;

	/**
	 * The default value of the '{@link #getLoopToTaskId() <em>Loop To Task Id</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getLoopToTaskId()
	 * @generated
	 * @ordered
	 */
	protected static final String LOOP_TO_TASK_ID_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getLoopToTaskId() <em>Loop To Task Id</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getLoopToTaskId()
	 * @generated
	 * @ordered
	 */
	protected String loopToTaskId = LOOP_TO_TASK_ID_EDEFAULT;

	/**
	 * The default value of the '{@link #getPriority() <em>Priority</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getPriority()
	 * @generated
	 * @ordered
	 */
	protected static final int PRIORITY_EDEFAULT = 0;

	/**
	 * The cached value of the '{@link #getPriority() <em>Priority</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getPriority()
	 * @generated
	 * @ordered
	 */
	protected int priority = PRIORITY_EDEFAULT;

	/**
	 * The default value of the '{@link #getResultSummary() <em>Result Summary</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getResultSummary()
	 * @generated
	 * @ordered
	 */
	protected static final String RESULT_SUMMARY_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getResultSummary() <em>Result Summary</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getResultSummary()
	 * @generated
	 * @ordered
	 */
	protected String resultSummary = RESULT_SUMMARY_EDEFAULT;

	/**
	 * The default value of the '{@link #getDescription() <em>Description</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getDescription()
	 * @generated
	 * @ordered
	 */
	protected static final String DESCRIPTION_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getDescription() <em>Description</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getDescription()
	 * @generated
	 * @ordered
	 */
	protected String description = DESCRIPTION_EDEFAULT;

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
	 * The default value of the '{@link #isLikes() <em>Likes</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isLikes()
	 * @generated
	 * @ordered
	 */
	protected static final boolean LIKES_EDEFAULT = false;

	/**
	 * The cached value of the '{@link #isLikes() <em>Likes</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isLikes()
	 * @generated
	 * @ordered
	 */
	protected boolean likes = LIKES_EDEFAULT;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected TaskImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return OrchestrationPackage.Literals.TASK;
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
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.TASK__ID, oldId, id));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getName() {
		return name;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setName(String newName) {
		String oldName = name;
		name = newName;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.TASK__NAME, oldName, name));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getType() {
		return type;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setType(String newType) {
		String oldType = type;
		type = newType;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.TASK__TYPE, oldType, type));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public TaskStatus getStatus() {
		return status;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setStatus(TaskStatus newStatus) {
		TaskStatus oldStatus = status;
		status = newStatus == null ? STATUS_EDEFAULT : newStatus;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.TASK__STATUS, oldStatus, status));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EList<Task> getNext() {
		if (next == null) {
			next = new EObjectResolvingEList<Task>(Task.class, this, OrchestrationPackage.TASK__NEXT);
		}
		return next;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EList<Task> getSubTasks() {
		if (subTasks == null) {
			subTasks = new EObjectContainmentEList<Task>(Task.class, this, OrchestrationPackage.TASK__SUB_TASKS);
		}
		return subTasks;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getResponse() {
		return response;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setResponse(String newResponse) {
		String oldResponse = response;
		response = newResponse;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.TASK__RESPONSE, oldResponse, response));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getFeedback() {
		return feedback;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setFeedback(String newFeedback) {
		String oldFeedback = feedback;
		feedback = newFeedback;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.TASK__FEEDBACK, oldFeedback, feedback));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public boolean isApprovalRequired() {
		return approvalRequired;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setApprovalRequired(boolean newApprovalRequired) {
		boolean oldApprovalRequired = approvalRequired;
		approvalRequired = newApprovalRequired;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.TASK__APPROVAL_REQUIRED, oldApprovalRequired, approvalRequired));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getLoopToTaskId() {
		return loopToTaskId;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setLoopToTaskId(String newLoopToTaskId) {
		String oldLoopToTaskId = loopToTaskId;
		loopToTaskId = newLoopToTaskId;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.TASK__LOOP_TO_TASK_ID, oldLoopToTaskId, loopToTaskId));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public int getPriority() {
		return priority;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setPriority(int newPriority) {
		int oldPriority = priority;
		priority = newPriority;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.TASK__PRIORITY, oldPriority, priority));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getResultSummary() {
		return resultSummary;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setResultSummary(String newResultSummary) {
		String oldResultSummary = resultSummary;
		resultSummary = newResultSummary;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.TASK__RESULT_SUMMARY, oldResultSummary, resultSummary));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getDescription() {
		return description;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setDescription(String newDescription) {
		String oldDescription = description;
		description = newDescription;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.TASK__DESCRIPTION, oldDescription, description));
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
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.TASK__RATING, oldRating, rating));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public boolean isLikes() {
		return likes;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setLikes(boolean newLikes) {
		boolean oldLikes = likes;
		likes = newLikes;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.TASK__LIKES, oldLikes, likes));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
		switch (featureID) {
			case OrchestrationPackage.TASK__SUB_TASKS:
				return ((InternalEList<?>)getSubTasks()).basicRemove(otherEnd, msgs);
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
			case OrchestrationPackage.TASK__ID:
				return getId();
			case OrchestrationPackage.TASK__NAME:
				return getName();
			case OrchestrationPackage.TASK__TYPE:
				return getType();
			case OrchestrationPackage.TASK__STATUS:
				return getStatus();
			case OrchestrationPackage.TASK__NEXT:
				return getNext();
			case OrchestrationPackage.TASK__SUB_TASKS:
				return getSubTasks();
			case OrchestrationPackage.TASK__RESPONSE:
				return getResponse();
			case OrchestrationPackage.TASK__FEEDBACK:
				return getFeedback();
			case OrchestrationPackage.TASK__APPROVAL_REQUIRED:
				return isApprovalRequired();
			case OrchestrationPackage.TASK__LOOP_TO_TASK_ID:
				return getLoopToTaskId();
			case OrchestrationPackage.TASK__PRIORITY:
				return getPriority();
			case OrchestrationPackage.TASK__RESULT_SUMMARY:
				return getResultSummary();
			case OrchestrationPackage.TASK__DESCRIPTION:
				return getDescription();
			case OrchestrationPackage.TASK__RATING:
				return getRating();
			case OrchestrationPackage.TASK__LIKES:
				return isLikes();
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
			case OrchestrationPackage.TASK__ID:
				setId((String)newValue);
				return;
			case OrchestrationPackage.TASK__NAME:
				setName((String)newValue);
				return;
			case OrchestrationPackage.TASK__TYPE:
				setType((String)newValue);
				return;
			case OrchestrationPackage.TASK__STATUS:
				setStatus((TaskStatus)newValue);
				return;
			case OrchestrationPackage.TASK__NEXT:
				getNext().clear();
				getNext().addAll((Collection<? extends Task>)newValue);
				return;
			case OrchestrationPackage.TASK__SUB_TASKS:
				getSubTasks().clear();
				getSubTasks().addAll((Collection<? extends Task>)newValue);
				return;
			case OrchestrationPackage.TASK__RESPONSE:
				setResponse((String)newValue);
				return;
			case OrchestrationPackage.TASK__FEEDBACK:
				setFeedback((String)newValue);
				return;
			case OrchestrationPackage.TASK__APPROVAL_REQUIRED:
				setApprovalRequired((Boolean)newValue);
				return;
			case OrchestrationPackage.TASK__LOOP_TO_TASK_ID:
				setLoopToTaskId((String)newValue);
				return;
			case OrchestrationPackage.TASK__PRIORITY:
				setPriority((Integer)newValue);
				return;
			case OrchestrationPackage.TASK__RESULT_SUMMARY:
				setResultSummary((String)newValue);
				return;
			case OrchestrationPackage.TASK__DESCRIPTION:
				setDescription((String)newValue);
				return;
			case OrchestrationPackage.TASK__RATING:
				setRating((Integer)newValue);
				return;
			case OrchestrationPackage.TASK__LIKES:
				setLikes((Boolean)newValue);
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
			case OrchestrationPackage.TASK__ID:
				setId(ID_EDEFAULT);
				return;
			case OrchestrationPackage.TASK__NAME:
				setName(NAME_EDEFAULT);
				return;
			case OrchestrationPackage.TASK__TYPE:
				setType(TYPE_EDEFAULT);
				return;
			case OrchestrationPackage.TASK__STATUS:
				setStatus(STATUS_EDEFAULT);
				return;
			case OrchestrationPackage.TASK__NEXT:
				getNext().clear();
				return;
			case OrchestrationPackage.TASK__SUB_TASKS:
				getSubTasks().clear();
				return;
			case OrchestrationPackage.TASK__RESPONSE:
				setResponse(RESPONSE_EDEFAULT);
				return;
			case OrchestrationPackage.TASK__FEEDBACK:
				setFeedback(FEEDBACK_EDEFAULT);
				return;
			case OrchestrationPackage.TASK__APPROVAL_REQUIRED:
				setApprovalRequired(APPROVAL_REQUIRED_EDEFAULT);
				return;
			case OrchestrationPackage.TASK__LOOP_TO_TASK_ID:
				setLoopToTaskId(LOOP_TO_TASK_ID_EDEFAULT);
				return;
			case OrchestrationPackage.TASK__PRIORITY:
				setPriority(PRIORITY_EDEFAULT);
				return;
			case OrchestrationPackage.TASK__RESULT_SUMMARY:
				setResultSummary(RESULT_SUMMARY_EDEFAULT);
				return;
			case OrchestrationPackage.TASK__DESCRIPTION:
				setDescription(DESCRIPTION_EDEFAULT);
				return;
			case OrchestrationPackage.TASK__RATING:
				setRating(RATING_EDEFAULT);
				return;
			case OrchestrationPackage.TASK__LIKES:
				setLikes(LIKES_EDEFAULT);
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
			case OrchestrationPackage.TASK__ID:
				return ID_EDEFAULT == null ? id != null : !ID_EDEFAULT.equals(id);
			case OrchestrationPackage.TASK__NAME:
				return NAME_EDEFAULT == null ? name != null : !NAME_EDEFAULT.equals(name);
			case OrchestrationPackage.TASK__TYPE:
				return TYPE_EDEFAULT == null ? type != null : !TYPE_EDEFAULT.equals(type);
			case OrchestrationPackage.TASK__STATUS:
				return status != STATUS_EDEFAULT;
			case OrchestrationPackage.TASK__NEXT:
				return next != null && !next.isEmpty();
			case OrchestrationPackage.TASK__SUB_TASKS:
				return subTasks != null && !subTasks.isEmpty();
			case OrchestrationPackage.TASK__RESPONSE:
				return RESPONSE_EDEFAULT == null ? response != null : !RESPONSE_EDEFAULT.equals(response);
			case OrchestrationPackage.TASK__FEEDBACK:
				return FEEDBACK_EDEFAULT == null ? feedback != null : !FEEDBACK_EDEFAULT.equals(feedback);
			case OrchestrationPackage.TASK__APPROVAL_REQUIRED:
				return approvalRequired != APPROVAL_REQUIRED_EDEFAULT;
			case OrchestrationPackage.TASK__LOOP_TO_TASK_ID:
				return LOOP_TO_TASK_ID_EDEFAULT == null ? loopToTaskId != null : !LOOP_TO_TASK_ID_EDEFAULT.equals(loopToTaskId);
			case OrchestrationPackage.TASK__PRIORITY:
				return priority != PRIORITY_EDEFAULT;
			case OrchestrationPackage.TASK__RESULT_SUMMARY:
				return RESULT_SUMMARY_EDEFAULT == null ? resultSummary != null : !RESULT_SUMMARY_EDEFAULT.equals(resultSummary);
			case OrchestrationPackage.TASK__DESCRIPTION:
				return DESCRIPTION_EDEFAULT == null ? description != null : !DESCRIPTION_EDEFAULT.equals(description);
			case OrchestrationPackage.TASK__RATING:
				return rating != RATING_EDEFAULT;
			case OrchestrationPackage.TASK__LIKES:
				return likes != LIKES_EDEFAULT;
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
		result.append(", name: ");
		result.append(name);
		result.append(", type: ");
		result.append(type);
		result.append(", status: ");
		result.append(status);
		result.append(", response: ");
		result.append(response);
		result.append(", feedback: ");
		result.append(feedback);
		result.append(", approvalRequired: ");
		result.append(approvalRequired);
		result.append(", loopToTaskId: ");
		result.append(loopToTaskId);
		result.append(", priority: ");
		result.append(priority);
		result.append(", resultSummary: ");
		result.append(resultSummary);
		result.append(", description: ");
		result.append(description);
		result.append(", rating: ");
		result.append(rating);
		result.append(", likes: ");
		result.append(likes);
		result.append(')');
		return result.toString();
	}

} //TaskImpl
