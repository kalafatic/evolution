/**
 */
package eu.kalafatic.evolution.model.orchestration.impl;

import eu.kalafatic.evolution.model.orchestration.FeedbackLevel;
import eu.kalafatic.evolution.model.orchestration.LogLevel;
import eu.kalafatic.evolution.model.orchestration.OrchestrationPackage;
import eu.kalafatic.evolution.model.orchestration.Task;
import eu.kalafatic.evolution.model.orchestration.TaskStatus;

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
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.TaskImpl#getRationale <em>Rationale</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.TaskImpl#getScheduledTime <em>Scheduled Time</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.TaskImpl#isSelected <em>Selected</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.TaskImpl#getGoal <em>Goal</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.TaskImpl#getPlan <em>Plan</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.TaskImpl#getArtifacts <em>Artifacts</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.TaskImpl#getPrompt <em>Prompt</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.TaskImpl#getAttachments <em>Attachments</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.TaskImpl#getLogLevel <em>Log Level</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.TaskImpl#getFeedbackLevel <em>Feedback Level</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.TaskImpl#isAutoEscalate <em>Auto Escalate</em>}</li>
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
	 * The default value of the '{@link #getScheduledTime() <em>Scheduled Time</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getScheduledTime()
	 * @generated
	 * @ordered
	 */
	protected static final String SCHEDULED_TIME_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getScheduledTime() <em>Scheduled Time</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getScheduledTime()
	 * @generated
	 * @ordered
	 */
	protected String scheduledTime = SCHEDULED_TIME_EDEFAULT;

	/**
	 * The default value of the '{@link #isSelected() <em>Selected</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isSelected()
	 * @generated
	 * @ordered
	 */
	protected static final boolean SELECTED_EDEFAULT = false;

	/**
	 * The cached value of the '{@link #isSelected() <em>Selected</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isSelected()
	 * @generated
	 * @ordered
	 */
	protected boolean selected = SELECTED_EDEFAULT;

	/**
	 * The default value of the '{@link #getGoal() <em>Goal</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getGoal()
	 * @generated
	 * @ordered
	 */
	protected static final String GOAL_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getGoal() <em>Goal</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getGoal()
	 * @generated
	 * @ordered
	 */
	protected String goal = GOAL_EDEFAULT;

	/**
	 * The default value of the '{@link #getPlan() <em>Plan</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getPlan()
	 * @generated
	 * @ordered
	 */
	protected static final String PLAN_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getPlan() <em>Plan</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getPlan()
	 * @generated
	 * @ordered
	 */
	protected String plan = PLAN_EDEFAULT;

	/**
	 * The default value of the '{@link #getArtifacts() <em>Artifacts</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getArtifacts()
	 * @generated
	 * @ordered
	 */
	protected static final String ARTIFACTS_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getArtifacts() <em>Artifacts</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getArtifacts()
	 * @generated
	 * @ordered
	 */
	protected String artifacts = ARTIFACTS_EDEFAULT;

	/**
	 * The default value of the '{@link #getPrompt() <em>Prompt</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getPrompt()
	 * @generated
	 * @ordered
	 */
	protected static final String PROMPT_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getPrompt() <em>Prompt</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getPrompt()
	 * @generated
	 * @ordered
	 */
	protected String prompt = PROMPT_EDEFAULT;

	/**
	 * The cached value of the '{@link #getAttachments() <em>Attachments</em>}' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getAttachments()
	 * @generated
	 * @ordered
	 */
	protected EList<String> attachments;

	/**
	 * The default value of the '{@link #getLogLevel() <em>Log Level</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getLogLevel()
	 * @generated
	 * @ordered
	 */
	protected static final LogLevel LOG_LEVEL_EDEFAULT = LogLevel.INFO;

	/**
	 * The cached value of the '{@link #getLogLevel() <em>Log Level</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getLogLevel()
	 * @generated
	 * @ordered
	 */
	protected LogLevel logLevel = LOG_LEVEL_EDEFAULT;

	/**
	 * The default value of the '{@link #getFeedbackLevel() <em>Feedback Level</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getFeedbackLevel()
	 * @generated
	 * @ordered
	 */
	protected static final FeedbackLevel FEEDBACK_LEVEL_EDEFAULT = FeedbackLevel.SIMPLE;

	/**
	 * The cached value of the '{@link #getFeedbackLevel() <em>Feedback Level</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getFeedbackLevel()
	 * @generated
	 * @ordered
	 */
	protected FeedbackLevel feedbackLevel = FEEDBACK_LEVEL_EDEFAULT;

	/**
	 * The default value of the '{@link #isAutoEscalate() <em>Auto Escalate</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isAutoEscalate()
	 * @generated
	 * @ordered
	 */
	protected static final boolean AUTO_ESCALATE_EDEFAULT = true;

	/**
	 * The cached value of the '{@link #isAutoEscalate() <em>Auto Escalate</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isAutoEscalate()
	 * @generated
	 * @ordered
	 */
	protected boolean autoEscalate = AUTO_ESCALATE_EDEFAULT;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public LogLevel getLogLevel() {
		return logLevel;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public FeedbackLevel getFeedbackLevel() {
		return feedbackLevel;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setFeedbackLevel(FeedbackLevel newFeedbackLevel) {
		FeedbackLevel oldFeedbackLevel = feedbackLevel;
		feedbackLevel = newFeedbackLevel == null ? FEEDBACK_LEVEL_EDEFAULT : newFeedbackLevel;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.TASK__FEEDBACK_LEVEL, oldFeedbackLevel, feedbackLevel));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public boolean isAutoEscalate() {
		return autoEscalate;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setAutoEscalate(boolean newAutoEscalate) {
		boolean oldAutoEscalate = autoEscalate;
		autoEscalate = newAutoEscalate;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.TASK__AUTO_ESCALATE, oldAutoEscalate, autoEscalate));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setLogLevel(LogLevel newLogLevel) {
		LogLevel oldLogLevel = logLevel;
		logLevel = newLogLevel == null ? LOG_LEVEL_EDEFAULT : newLogLevel;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.TASK__LOG_LEVEL, oldLogLevel, logLevel));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getPrompt() {
		return prompt;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setPrompt(String newPrompt) {
		String oldPrompt = prompt;
		prompt = newPrompt;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.TASK__PROMPT, oldPrompt, prompt));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EList<String> getAttachments() {
		if (attachments == null) {
			attachments = new org.eclipse.emf.ecore.util.EDataTypeUniqueEList<String>(String.class, this, OrchestrationPackage.TASK__ATTACHMENTS);
		}
		return attachments;
	}

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
	public String getArtifacts() {
		return artifacts;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setArtifacts(String newArtifacts) {
		String oldArtifacts = artifacts;
		artifacts = newArtifacts;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.TASK__ARTIFACTS, oldArtifacts, artifacts));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getGoal() {
		return goal;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setGoal(String newGoal) {
		String oldGoal = goal;
		goal = newGoal;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.TASK__GOAL, oldGoal, goal));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getPlan() {
		return plan;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setPlan(String newPlan) {
		String oldPlan = plan;
		plan = newPlan;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.TASK__PLAN, oldPlan, plan));
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
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.TASK__RATIONALE, oldRationale, rationale));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getScheduledTime() {
		return scheduledTime;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setScheduledTime(String newScheduledTime) {
		String oldScheduledTime = scheduledTime;
		scheduledTime = newScheduledTime;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.TASK__SCHEDULED_TIME, oldScheduledTime, scheduledTime));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public boolean isSelected() {
		return selected;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setSelected(boolean newSelected) {
		boolean oldSelected = selected;
		selected = newSelected;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.TASK__SELECTED, oldSelected, selected));
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
			case OrchestrationPackage.TASK__RATIONALE:
				return getRationale();
			case OrchestrationPackage.TASK__SCHEDULED_TIME:
				return getScheduledTime();
			case OrchestrationPackage.TASK__SELECTED:
				return isSelected();
			case OrchestrationPackage.TASK__GOAL:
				return getGoal();
			case OrchestrationPackage.TASK__PLAN:
				return getPlan();
			case OrchestrationPackage.TASK__ARTIFACTS:
				return getArtifacts();
			case OrchestrationPackage.TASK__PROMPT:
				return getPrompt();
			case OrchestrationPackage.TASK__ATTACHMENTS:
				return getAttachments();
			case OrchestrationPackage.TASK__LOG_LEVEL:
				return getLogLevel();
			case OrchestrationPackage.TASK__FEEDBACK_LEVEL:
				return getFeedbackLevel();
			case OrchestrationPackage.TASK__AUTO_ESCALATE:
				return isAutoEscalate();
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
			case OrchestrationPackage.TASK__RATIONALE:
				setRationale((String)newValue);
				return;
			case OrchestrationPackage.TASK__SCHEDULED_TIME:
				setScheduledTime((String)newValue);
				return;
			case OrchestrationPackage.TASK__SELECTED:
				setSelected((Boolean)newValue);
				return;
			case OrchestrationPackage.TASK__GOAL:
				setGoal((String)newValue);
				return;
			case OrchestrationPackage.TASK__PLAN:
				setPlan((String)newValue);
				return;
			case OrchestrationPackage.TASK__ARTIFACTS:
				setArtifacts((String)newValue);
				return;
			case OrchestrationPackage.TASK__PROMPT:
				setPrompt((String)newValue);
				return;
			case OrchestrationPackage.TASK__ATTACHMENTS:
				getAttachments().clear();
				getAttachments().addAll((Collection<? extends String>)newValue);
				return;
			case OrchestrationPackage.TASK__LOG_LEVEL:
				setLogLevel((LogLevel)newValue);
				return;
			case OrchestrationPackage.TASK__FEEDBACK_LEVEL:
				setFeedbackLevel((FeedbackLevel)newValue);
				return;
			case OrchestrationPackage.TASK__AUTO_ESCALATE:
				setAutoEscalate((Boolean)newValue);
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
			case OrchestrationPackage.TASK__RATIONALE:
				setRationale(RATIONALE_EDEFAULT);
				return;
			case OrchestrationPackage.TASK__SCHEDULED_TIME:
				setScheduledTime(SCHEDULED_TIME_EDEFAULT);
				return;
			case OrchestrationPackage.TASK__SELECTED:
				setSelected(SELECTED_EDEFAULT);
				return;
			case OrchestrationPackage.TASK__GOAL:
				setGoal(GOAL_EDEFAULT);
				return;
			case OrchestrationPackage.TASK__PLAN:
				setPlan(PLAN_EDEFAULT);
				return;
			case OrchestrationPackage.TASK__ARTIFACTS:
				setArtifacts(ARTIFACTS_EDEFAULT);
				return;
			case OrchestrationPackage.TASK__PROMPT:
				setPrompt(PROMPT_EDEFAULT);
				return;
			case OrchestrationPackage.TASK__ATTACHMENTS:
				getAttachments().clear();
				return;
			case OrchestrationPackage.TASK__LOG_LEVEL:
				setLogLevel(LOG_LEVEL_EDEFAULT);
				return;
			case OrchestrationPackage.TASK__FEEDBACK_LEVEL:
				setFeedbackLevel(FEEDBACK_LEVEL_EDEFAULT);
				return;
			case OrchestrationPackage.TASK__AUTO_ESCALATE:
				setAutoEscalate(AUTO_ESCALATE_EDEFAULT);
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
			case OrchestrationPackage.TASK__RATIONALE:
				return RATIONALE_EDEFAULT == null ? rationale != null : !RATIONALE_EDEFAULT.equals(rationale);
			case OrchestrationPackage.TASK__SCHEDULED_TIME:
				return SCHEDULED_TIME_EDEFAULT == null ? scheduledTime != null : !SCHEDULED_TIME_EDEFAULT.equals(scheduledTime);
			case OrchestrationPackage.TASK__SELECTED:
				return selected != SELECTED_EDEFAULT;
			case OrchestrationPackage.TASK__GOAL:
				return GOAL_EDEFAULT == null ? goal != null : !GOAL_EDEFAULT.equals(goal);
			case OrchestrationPackage.TASK__PLAN:
				return PLAN_EDEFAULT == null ? plan != null : !PLAN_EDEFAULT.equals(plan);
			case OrchestrationPackage.TASK__ARTIFACTS:
				return ARTIFACTS_EDEFAULT == null ? artifacts != null : !ARTIFACTS_EDEFAULT.equals(artifacts);
			case OrchestrationPackage.TASK__PROMPT:
				return PROMPT_EDEFAULT == null ? prompt != null : !PROMPT_EDEFAULT.equals(prompt);
			case OrchestrationPackage.TASK__ATTACHMENTS:
				return attachments != null && !attachments.isEmpty();
			case OrchestrationPackage.TASK__LOG_LEVEL:
				return logLevel != LOG_LEVEL_EDEFAULT;
			case OrchestrationPackage.TASK__FEEDBACK_LEVEL:
				return feedbackLevel != FEEDBACK_LEVEL_EDEFAULT;
			case OrchestrationPackage.TASK__AUTO_ESCALATE:
				return autoEscalate != AUTO_ESCALATE_EDEFAULT;
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
		result.append(", rationale: ");
		result.append(rationale);
		result.append(", scheduledTime: ");
		result.append(scheduledTime);
		result.append(", selected: ");
		result.append(selected);
		result.append(", goal: ");
		result.append(goal);
		result.append(", plan: ");
		result.append(plan);
		result.append(", artifacts: ");
		result.append(artifacts);
		result.append(", prompt: ");
		result.append(prompt);
		result.append(", attachments: ");
		result.append(attachments);
		result.append(", logLevel: ");
		result.append(logLevel);
		result.append(", feedbackLevel: ");
		result.append(feedbackLevel);
		result.append(", autoEscalate: ");
		result.append(autoEscalate);
		result.append(')');
		return result.toString();
	}

} //TaskImpl
