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
import org.eclipse.emf.ecore.util.InternalEList;

import eu.kalafatic.evolution.model.orchestration.AiMode;
import eu.kalafatic.evolution.model.orchestration.ChatMessage;
import eu.kalafatic.evolution.model.orchestration.ChatSession;
import eu.kalafatic.evolution.model.orchestration.OrchestrationPackage;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Chat Session</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * </p>
 * <ul>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.ChatSessionImpl#getId <em>Id</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.ChatSessionImpl#getMessages <em>Messages</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.ChatSessionImpl#isIterativeMode <em>Iterative Mode</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.ChatSessionImpl#isSelfIterativeMode <em>Self Iterative Mode</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.ChatSessionImpl#isDarwinMode <em>Darwin Mode</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.ChatSessionImpl#isGitAutomation <em>Git Automation</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.ChatSessionImpl#getMaxIterations <em>Max Iterations</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.ChatSessionImpl#isStepMode <em>Step Mode</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.ChatSessionImpl#getTargetPath <em>Target Path</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.ChatSessionImpl#getTargetType <em>Target Type</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.ChatSessionImpl#getOutputPath <em>Output Path</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.ChatSessionImpl#isAutoApprove <em>Auto Approve</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.ChatSessionImpl#getAiMode <em>Ai Mode</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.ChatSessionImpl#getLocalModel <em>Local Model</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.ChatSessionImpl#getRemoteModel <em>Remote Model</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.ChatSessionImpl#getBitState <em>Bit State</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.ChatSessionImpl#getExpansion <em>Expansion</em>}</li>
 * </ul>
 *
 * @generated
 */
public class ChatSessionImpl extends MinimalEObjectImpl.Container implements ChatSession {
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
	 * The cached value of the '{@link #getMessages() <em>Messages</em>}' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getMessages()
	 * @generated
	 * @ordered
	 */
	protected EList<ChatMessage> messages;

	/**
	 * The default value of the '{@link #isIterativeMode() <em>Iterative Mode</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isIterativeMode()
	 * @generated
	 * @ordered
	 */
	protected static final boolean ITERATIVE_MODE_EDEFAULT = true;

	/**
	 * The cached value of the '{@link #isIterativeMode() <em>Iterative Mode</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isIterativeMode()
	 * @generated
	 * @ordered
	 */
	protected boolean iterativeMode = ITERATIVE_MODE_EDEFAULT;

	/**
	 * The default value of the '{@link #isSelfIterativeMode() <em>Self Iterative Mode</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isSelfIterativeMode()
	 * @generated
	 * @ordered
	 */
	protected static final boolean SELF_ITERATIVE_MODE_EDEFAULT = false;

	/**
	 * The cached value of the '{@link #isSelfIterativeMode() <em>Self Iterative Mode</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isSelfIterativeMode()
	 * @generated
	 * @ordered
	 */
	protected boolean selfIterativeMode = SELF_ITERATIVE_MODE_EDEFAULT;

	/**
	 * The default value of the '{@link #isDarwinMode() <em>Darwin Mode</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isDarwinMode()
	 * @generated
	 * @ordered
	 */
	protected static final boolean DARWIN_MODE_EDEFAULT = true;

	/**
	 * The cached value of the '{@link #isDarwinMode() <em>Darwin Mode</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isDarwinMode()
	 * @generated
	 * @ordered
	 */
	protected boolean darwinMode = DARWIN_MODE_EDEFAULT;

	/**
	 * The default value of the '{@link #isGitAutomation() <em>Git Automation</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isGitAutomation()
	 * @generated
	 * @ordered
	 */
	protected static final boolean GIT_AUTOMATION_EDEFAULT = false;

	/**
	 * The cached value of the '{@link #isGitAutomation() <em>Git Automation</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isGitAutomation()
	 * @generated
	 * @ordered
	 */
	protected boolean gitAutomation = GIT_AUTOMATION_EDEFAULT;

	/**
	 * The default value of the '{@link #getMaxIterations() <em>Max Iterations</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getMaxIterations()
	 * @generated
	 * @ordered
	 */
	protected static final int MAX_ITERATIONS_EDEFAULT = 1;

	/**
	 * The cached value of the '{@link #getMaxIterations() <em>Max Iterations</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getMaxIterations()
	 * @generated
	 * @ordered
	 */
	protected int maxIterations = MAX_ITERATIONS_EDEFAULT;

	/**
	 * The default value of the '{@link #isStepMode() <em>Step Mode</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isStepMode()
	 * @generated
	 * @ordered
	 */
	protected static final boolean STEP_MODE_EDEFAULT = false;

	/**
	 * The cached value of the '{@link #isStepMode() <em>Step Mode</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isStepMode()
	 * @generated
	 * @ordered
	 */
	protected boolean stepMode = STEP_MODE_EDEFAULT;

	/**
	 * The default value of the '{@link #getTargetPath() <em>Target Path</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getTargetPath()
	 * @generated
	 * @ordered
	 */
	protected static final String TARGET_PATH_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getTargetPath() <em>Target Path</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getTargetPath()
	 * @generated
	 * @ordered
	 */
	protected String targetPath = TARGET_PATH_EDEFAULT;

	/**
	 * The default value of the '{@link #getTargetType() <em>Target Type</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getTargetType()
	 * @generated
	 * @ordered
	 */
	protected static final String TARGET_TYPE_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getTargetType() <em>Target Type</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getTargetType()
	 * @generated
	 * @ordered
	 */
	protected String targetType = TARGET_TYPE_EDEFAULT;

	/**
	 * The default value of the '{@link #getOutputPath() <em>Output Path</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getOutputPath()
	 * @generated
	 * @ordered
	 */
	protected static final String OUTPUT_PATH_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getOutputPath() <em>Output Path</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getOutputPath()
	 * @generated
	 * @ordered
	 */
	protected String outputPath = OUTPUT_PATH_EDEFAULT;

	/**
	 * The default value of the '{@link #isAutoApprove() <em>Auto Approve</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isAutoApprove()
	 * @generated
	 * @ordered
	 */
	protected static final boolean AUTO_APPROVE_EDEFAULT = false;

	/**
	 * The cached value of the '{@link #isAutoApprove() <em>Auto Approve</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isAutoApprove()
	 * @generated
	 * @ordered
	 */
	protected boolean autoApprove = AUTO_APPROVE_EDEFAULT;

	/**
	 * The default value of the '{@link #getAiMode() <em>Ai Mode</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getAiMode()
	 * @generated
	 * @ordered
	 */
	protected static final AiMode AI_MODE_EDEFAULT = AiMode.LOCAL;

	/**
	 * The cached value of the '{@link #getAiMode() <em>Ai Mode</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getAiMode()
	 * @generated
	 * @ordered
	 */
	protected AiMode aiMode = AI_MODE_EDEFAULT;

	/**
	 * The default value of the '{@link #getLocalModel() <em>Local Model</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getLocalModel()
	 * @generated
	 * @ordered
	 */
	protected static final String LOCAL_MODEL_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getLocalModel() <em>Local Model</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getLocalModel()
	 * @generated
	 * @ordered
	 */
	protected String localModel = LOCAL_MODEL_EDEFAULT;

	/**
	 * The default value of the '{@link #getRemoteModel() <em>Remote Model</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getRemoteModel()
	 * @generated
	 * @ordered
	 */
	protected static final String REMOTE_MODEL_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getRemoteModel() <em>Remote Model</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getRemoteModel()
	 * @generated
	 * @ordered
	 */
	protected String remoteModel = REMOTE_MODEL_EDEFAULT;

	/**
	 * The default value of the '{@link #getBitState() <em>Bit State</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getBitState()
	 * @generated
	 * @ordered
	 */
	protected static final long BIT_STATE_EDEFAULT = 0L;

	/**
	 * The cached value of the '{@link #getBitState() <em>Bit State</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getBitState()
	 * @generated
	 * @ordered
	 */
	protected long bitState = BIT_STATE_EDEFAULT;

	/**
	 * The default value of the '{@link #getExpansion() <em>Expansion</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getExpansion()
	 * @generated
	 * @ordered
	 */
	protected static final int EXPANSION_EDEFAULT = 5;

	/**
	 * The cached value of the '{@link #getExpansion() <em>Expansion</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getExpansion()
	 * @generated
	 * @ordered
	 */
	protected int expansion = EXPANSION_EDEFAULT;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected ChatSessionImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return OrchestrationPackage.Literals.CHAT_SESSION;
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
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.CHAT_SESSION__ID, oldId, id));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EList<ChatMessage> getMessages() {
		if (messages == null) {
			messages = new EObjectContainmentEList<ChatMessage>(ChatMessage.class, this, OrchestrationPackage.CHAT_SESSION__MESSAGES);
		}
		return messages;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public boolean isIterativeMode() {
		return iterativeMode;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setIterativeMode(boolean newIterativeMode) {
		boolean oldIterativeMode = iterativeMode;
		iterativeMode = newIterativeMode;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.CHAT_SESSION__ITERATIVE_MODE, oldIterativeMode, iterativeMode));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public boolean isSelfIterativeMode() {
		return selfIterativeMode;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setSelfIterativeMode(boolean newSelfIterativeMode) {
		boolean oldSelfIterativeMode = selfIterativeMode;
		selfIterativeMode = newSelfIterativeMode;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.CHAT_SESSION__SELF_ITERATIVE_MODE, oldSelfIterativeMode, selfIterativeMode));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public boolean isDarwinMode() {
		return darwinMode;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setDarwinMode(boolean newDarwinMode) {
		boolean oldDarwinMode = darwinMode;
		darwinMode = newDarwinMode;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.CHAT_SESSION__DARWIN_MODE, oldDarwinMode, darwinMode));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public boolean isGitAutomation() {
		return gitAutomation;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setGitAutomation(boolean newGitAutomation) {
		boolean oldGitAutomation = gitAutomation;
		gitAutomation = newGitAutomation;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.CHAT_SESSION__GIT_AUTOMATION, oldGitAutomation, gitAutomation));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public int getMaxIterations() {
		return maxIterations;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setMaxIterations(int newMaxIterations) {
		int oldMaxIterations = maxIterations;
		maxIterations = newMaxIterations;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.CHAT_SESSION__MAX_ITERATIONS, oldMaxIterations, maxIterations));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public boolean isStepMode() {
		return stepMode;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setStepMode(boolean newStepMode) {
		boolean oldStepMode = stepMode;
		stepMode = newStepMode;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.CHAT_SESSION__STEP_MODE, oldStepMode, stepMode));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getTargetPath() {
		return targetPath;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setTargetPath(String newTargetPath) {
		String oldTargetPath = targetPath;
		targetPath = newTargetPath;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.CHAT_SESSION__TARGET_PATH, oldTargetPath, targetPath));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getTargetType() {
		return targetType;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setTargetType(String newTargetType) {
		String oldTargetType = targetType;
		targetType = newTargetType;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.CHAT_SESSION__TARGET_TYPE, oldTargetType, targetType));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getOutputPath() {
		return outputPath;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setOutputPath(String newOutputPath) {
		String oldOutputPath = outputPath;
		outputPath = newOutputPath;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.CHAT_SESSION__OUTPUT_PATH, oldOutputPath, outputPath));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public boolean isAutoApprove() {
		return autoApprove;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setAutoApprove(boolean newAutoApprove) {
		boolean oldAutoApprove = autoApprove;
		autoApprove = newAutoApprove;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.CHAT_SESSION__AUTO_APPROVE, oldAutoApprove, autoApprove));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public AiMode getAiMode() {
		return aiMode;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setAiMode(AiMode newAiMode) {
		AiMode oldAiMode = aiMode;
		aiMode = newAiMode == null ? AI_MODE_EDEFAULT : newAiMode;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.CHAT_SESSION__AI_MODE, oldAiMode, aiMode));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getLocalModel() {
		return localModel;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setLocalModel(String newLocalModel) {
		String oldLocalModel = localModel;
		localModel = newLocalModel;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.CHAT_SESSION__LOCAL_MODEL, oldLocalModel, localModel));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getRemoteModel() {
		return remoteModel;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setRemoteModel(String newRemoteModel) {
		String oldRemoteModel = remoteModel;
		remoteModel = newRemoteModel;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.CHAT_SESSION__REMOTE_MODEL, oldRemoteModel, remoteModel));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public long getBitState() {
		return bitState;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setBitState(long newBitState) {
		long oldBitState = bitState;
		bitState = newBitState;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.CHAT_SESSION__BIT_STATE, oldBitState, bitState));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public int getExpansion() {
		return expansion;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setExpansion(int newExpansion) {
		int oldExpansion = expansion;
		expansion = newExpansion;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.CHAT_SESSION__EXPANSION, oldExpansion, expansion));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
		switch (featureID) {
			case OrchestrationPackage.CHAT_SESSION__MESSAGES:
				return ((InternalEList<?>)getMessages()).basicRemove(otherEnd, msgs);
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
			case OrchestrationPackage.CHAT_SESSION__ID:
				return getId();
			case OrchestrationPackage.CHAT_SESSION__MESSAGES:
				return getMessages();
			case OrchestrationPackage.CHAT_SESSION__ITERATIVE_MODE:
				return isIterativeMode();
			case OrchestrationPackage.CHAT_SESSION__SELF_ITERATIVE_MODE:
				return isSelfIterativeMode();
			case OrchestrationPackage.CHAT_SESSION__DARWIN_MODE:
				return isDarwinMode();
			case OrchestrationPackage.CHAT_SESSION__GIT_AUTOMATION:
				return isGitAutomation();
			case OrchestrationPackage.CHAT_SESSION__MAX_ITERATIONS:
				return getMaxIterations();
			case OrchestrationPackage.CHAT_SESSION__STEP_MODE:
				return isStepMode();
			case OrchestrationPackage.CHAT_SESSION__TARGET_PATH:
				return getTargetPath();
			case OrchestrationPackage.CHAT_SESSION__TARGET_TYPE:
				return getTargetType();
			case OrchestrationPackage.CHAT_SESSION__OUTPUT_PATH:
				return getOutputPath();
			case OrchestrationPackage.CHAT_SESSION__AUTO_APPROVE:
				return isAutoApprove();
			case OrchestrationPackage.CHAT_SESSION__AI_MODE:
				return getAiMode();
			case OrchestrationPackage.CHAT_SESSION__LOCAL_MODEL:
				return getLocalModel();
			case OrchestrationPackage.CHAT_SESSION__REMOTE_MODEL:
				return getRemoteModel();
			case OrchestrationPackage.CHAT_SESSION__BIT_STATE:
				return getBitState();
			case OrchestrationPackage.CHAT_SESSION__EXPANSION:
				return getExpansion();
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
			case OrchestrationPackage.CHAT_SESSION__ID:
				setId((String)newValue);
				return;
			case OrchestrationPackage.CHAT_SESSION__MESSAGES:
				getMessages().clear();
				getMessages().addAll((Collection<? extends ChatMessage>)newValue);
				return;
			case OrchestrationPackage.CHAT_SESSION__ITERATIVE_MODE:
				setIterativeMode((Boolean)newValue);
				return;
			case OrchestrationPackage.CHAT_SESSION__SELF_ITERATIVE_MODE:
				setSelfIterativeMode((Boolean)newValue);
				return;
			case OrchestrationPackage.CHAT_SESSION__DARWIN_MODE:
				setDarwinMode((Boolean)newValue);
				return;
			case OrchestrationPackage.CHAT_SESSION__GIT_AUTOMATION:
				setGitAutomation((Boolean)newValue);
				return;
			case OrchestrationPackage.CHAT_SESSION__MAX_ITERATIONS:
				setMaxIterations((Integer)newValue);
				return;
			case OrchestrationPackage.CHAT_SESSION__STEP_MODE:
				setStepMode((Boolean)newValue);
				return;
			case OrchestrationPackage.CHAT_SESSION__TARGET_PATH:
				setTargetPath((String)newValue);
				return;
			case OrchestrationPackage.CHAT_SESSION__TARGET_TYPE:
				setTargetType((String)newValue);
				return;
			case OrchestrationPackage.CHAT_SESSION__OUTPUT_PATH:
				setOutputPath((String)newValue);
				return;
			case OrchestrationPackage.CHAT_SESSION__AUTO_APPROVE:
				setAutoApprove((Boolean)newValue);
				return;
			case OrchestrationPackage.CHAT_SESSION__AI_MODE:
				setAiMode((AiMode)newValue);
				return;
			case OrchestrationPackage.CHAT_SESSION__LOCAL_MODEL:
				setLocalModel((String)newValue);
				return;
			case OrchestrationPackage.CHAT_SESSION__REMOTE_MODEL:
				setRemoteModel((String)newValue);
				return;
			case OrchestrationPackage.CHAT_SESSION__BIT_STATE:
				setBitState((Long)newValue);
				return;
			case OrchestrationPackage.CHAT_SESSION__EXPANSION:
				setExpansion((Integer)newValue);
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
			case OrchestrationPackage.CHAT_SESSION__ID:
				setId(ID_EDEFAULT);
				return;
			case OrchestrationPackage.CHAT_SESSION__MESSAGES:
				getMessages().clear();
				return;
			case OrchestrationPackage.CHAT_SESSION__ITERATIVE_MODE:
				setIterativeMode(ITERATIVE_MODE_EDEFAULT);
				return;
			case OrchestrationPackage.CHAT_SESSION__SELF_ITERATIVE_MODE:
				setSelfIterativeMode(SELF_ITERATIVE_MODE_EDEFAULT);
				return;
			case OrchestrationPackage.CHAT_SESSION__DARWIN_MODE:
				setDarwinMode(DARWIN_MODE_EDEFAULT);
				return;
			case OrchestrationPackage.CHAT_SESSION__GIT_AUTOMATION:
				setGitAutomation(GIT_AUTOMATION_EDEFAULT);
				return;
			case OrchestrationPackage.CHAT_SESSION__MAX_ITERATIONS:
				setMaxIterations(MAX_ITERATIONS_EDEFAULT);
				return;
			case OrchestrationPackage.CHAT_SESSION__STEP_MODE:
				setStepMode(STEP_MODE_EDEFAULT);
				return;
			case OrchestrationPackage.CHAT_SESSION__TARGET_PATH:
				setTargetPath(TARGET_PATH_EDEFAULT);
				return;
			case OrchestrationPackage.CHAT_SESSION__TARGET_TYPE:
				setTargetType(TARGET_TYPE_EDEFAULT);
				return;
			case OrchestrationPackage.CHAT_SESSION__OUTPUT_PATH:
				setOutputPath(OUTPUT_PATH_EDEFAULT);
				return;
			case OrchestrationPackage.CHAT_SESSION__AUTO_APPROVE:
				setAutoApprove(AUTO_APPROVE_EDEFAULT);
				return;
			case OrchestrationPackage.CHAT_SESSION__AI_MODE:
				setAiMode(AI_MODE_EDEFAULT);
				return;
			case OrchestrationPackage.CHAT_SESSION__LOCAL_MODEL:
				setLocalModel(LOCAL_MODEL_EDEFAULT);
				return;
			case OrchestrationPackage.CHAT_SESSION__REMOTE_MODEL:
				setRemoteModel(REMOTE_MODEL_EDEFAULT);
				return;
			case OrchestrationPackage.CHAT_SESSION__BIT_STATE:
				setBitState(BIT_STATE_EDEFAULT);
				return;
			case OrchestrationPackage.CHAT_SESSION__EXPANSION:
				setExpansion(EXPANSION_EDEFAULT);
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
			case OrchestrationPackage.CHAT_SESSION__ID:
				return ID_EDEFAULT == null ? id != null : !ID_EDEFAULT.equals(id);
			case OrchestrationPackage.CHAT_SESSION__MESSAGES:
				return messages != null && !messages.isEmpty();
			case OrchestrationPackage.CHAT_SESSION__ITERATIVE_MODE:
				return iterativeMode != ITERATIVE_MODE_EDEFAULT;
			case OrchestrationPackage.CHAT_SESSION__SELF_ITERATIVE_MODE:
				return selfIterativeMode != SELF_ITERATIVE_MODE_EDEFAULT;
			case OrchestrationPackage.CHAT_SESSION__DARWIN_MODE:
				return darwinMode != DARWIN_MODE_EDEFAULT;
			case OrchestrationPackage.CHAT_SESSION__GIT_AUTOMATION:
				return gitAutomation != GIT_AUTOMATION_EDEFAULT;
			case OrchestrationPackage.CHAT_SESSION__MAX_ITERATIONS:
				return maxIterations != MAX_ITERATIONS_EDEFAULT;
			case OrchestrationPackage.CHAT_SESSION__STEP_MODE:
				return stepMode != STEP_MODE_EDEFAULT;
			case OrchestrationPackage.CHAT_SESSION__TARGET_PATH:
				return TARGET_PATH_EDEFAULT == null ? targetPath != null : !TARGET_PATH_EDEFAULT.equals(targetPath);
			case OrchestrationPackage.CHAT_SESSION__TARGET_TYPE:
				return TARGET_TYPE_EDEFAULT == null ? targetType != null : !TARGET_TYPE_EDEFAULT.equals(targetType);
			case OrchestrationPackage.CHAT_SESSION__OUTPUT_PATH:
				return OUTPUT_PATH_EDEFAULT == null ? outputPath != null : !OUTPUT_PATH_EDEFAULT.equals(outputPath);
			case OrchestrationPackage.CHAT_SESSION__AUTO_APPROVE:
				return autoApprove != AUTO_APPROVE_EDEFAULT;
			case OrchestrationPackage.CHAT_SESSION__AI_MODE:
				return aiMode != AI_MODE_EDEFAULT;
			case OrchestrationPackage.CHAT_SESSION__LOCAL_MODEL:
				return LOCAL_MODEL_EDEFAULT == null ? localModel != null : !LOCAL_MODEL_EDEFAULT.equals(localModel);
			case OrchestrationPackage.CHAT_SESSION__REMOTE_MODEL:
				return REMOTE_MODEL_EDEFAULT == null ? remoteModel != null : !REMOTE_MODEL_EDEFAULT.equals(remoteModel);
			case OrchestrationPackage.CHAT_SESSION__BIT_STATE:
				return bitState != BIT_STATE_EDEFAULT;
			case OrchestrationPackage.CHAT_SESSION__EXPANSION:
				return expansion != EXPANSION_EDEFAULT;
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
		result.append(", iterativeMode: ");
		result.append(iterativeMode);
		result.append(", selfIterativeMode: ");
		result.append(selfIterativeMode);
		result.append(", darwinMode: ");
		result.append(darwinMode);
		result.append(", gitAutomation: ");
		result.append(gitAutomation);
		result.append(", maxIterations: ");
		result.append(maxIterations);
		result.append(", stepMode: ");
		result.append(stepMode);
		result.append(", targetPath: ");
		result.append(targetPath);
		result.append(", targetType: ");
		result.append(targetType);
		result.append(", outputPath: ");
		result.append(outputPath);
		result.append(", autoApprove: ");
		result.append(autoApprove);
		result.append(", aiMode: ");
		result.append(aiMode);
		result.append(", localModel: ");
		result.append(localModel);
		result.append(", remoteModel: ");
		result.append(remoteModel);
		result.append(", bitState: ");
		result.append(bitState);
		result.append(", expansion: ");
		result.append(expansion);
		result.append(')');
		return result.toString();
	}

} //ChatSessionImpl
