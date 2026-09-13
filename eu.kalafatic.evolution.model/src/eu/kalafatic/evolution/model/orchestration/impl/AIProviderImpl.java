/**
 */
package eu.kalafatic.evolution.model.orchestration.impl;

import eu.kalafatic.evolution.model.orchestration.AIProvider;
import eu.kalafatic.evolution.model.orchestration.OrchestrationPackage;

import org.eclipse.emf.common.notify.Notification;

import org.eclipse.emf.ecore.EClass;

import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.MinimalEObjectImpl;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>AI Provider</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * </p>
 * <ul>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.AIProviderImpl#getName <em>Name</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.AIProviderImpl#getUrl <em>Url</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.AIProviderImpl#getApiKey <em>Api Key</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.AIProviderImpl#getFormat <em>Format</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.AIProviderImpl#isLocal <em>Local</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.AIProviderImpl#getDefaultModel <em>Default Model</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.AIProviderImpl#isApiKeyEncrypted <em>Api Key Encrypted</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.AIProviderImpl#isUseEnvVar <em>Use Env Var</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.AIProviderImpl#getEnvVarName <em>Env Var Name</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.AIProviderImpl#getState <em>State</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.AIProviderImpl#getStateDescription <em>State Description</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.AIProviderImpl#getRating <em>Rating</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.AIProviderImpl#getRatingAnalyze <em>Rating Analyze</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.AIProviderImpl#getRatingChat <em>Rating Chat</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.AIProviderImpl#getRatingProgramming <em>Rating Programming</em>}</li>
 * </ul>
 *
 * @generated
 */
public class AIProviderImpl extends MinimalEObjectImpl.Container implements AIProvider {
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
	 * The default value of the '{@link #getUrl() <em>Url</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getUrl()
	 * @generated
	 * @ordered
	 */
	protected static final String URL_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getUrl() <em>Url</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getUrl()
	 * @generated
	 * @ordered
	 */
	protected String url = URL_EDEFAULT;

	/**
	 * The default value of the '{@link #getApiKey() <em>Api Key</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getApiKey()
	 * @generated
	 * @ordered
	 */
	protected static final String API_KEY_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getApiKey() <em>Api Key</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getApiKey()
	 * @generated
	 * @ordered
	 */
	protected String apiKey = API_KEY_EDEFAULT;

	/**
	 * The default value of the '{@link #getFormat() <em>Format</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getFormat()
	 * @generated
	 * @ordered
	 */
	protected static final String FORMAT_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getFormat() <em>Format</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getFormat()
	 * @generated
	 * @ordered
	 */
	protected String format = FORMAT_EDEFAULT;

	/**
	 * The default value of the '{@link #isLocal() <em>Local</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isLocal()
	 * @generated
	 * @ordered
	 */
	protected static final boolean LOCAL_EDEFAULT = false;

	/**
	 * The cached value of the '{@link #isLocal() <em>Local</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isLocal()
	 * @generated
	 * @ordered
	 */
	protected boolean local = LOCAL_EDEFAULT;

	/**
	 * The default value of the '{@link #getDefaultModel() <em>Default Model</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getDefaultModel()
	 * @generated
	 * @ordered
	 */
	protected static final String DEFAULT_MODEL_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getDefaultModel() <em>Default Model</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getDefaultModel()
	 * @generated
	 * @ordered
	 */
	protected String defaultModel = DEFAULT_MODEL_EDEFAULT;

	/**
	 * The default value of the '{@link #isApiKeyEncrypted() <em>Api Key Encrypted</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isApiKeyEncrypted()
	 * @generated
	 * @ordered
	 */
	protected static final boolean API_KEY_ENCRYPTED_EDEFAULT = false;

	/**
	 * The cached value of the '{@link #isApiKeyEncrypted() <em>Api Key Encrypted</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isApiKeyEncrypted()
	 * @generated
	 * @ordered
	 */
	protected boolean apiKeyEncrypted = API_KEY_ENCRYPTED_EDEFAULT;

	/**
	 * The default value of the '{@link #isUseEnvVar() <em>Use Env Var</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isUseEnvVar()
	 * @generated
	 * @ordered
	 */
	protected static final boolean USE_ENV_VAR_EDEFAULT = false;

	/**
	 * The cached value of the '{@link #isUseEnvVar() <em>Use Env Var</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isUseEnvVar()
	 * @generated
	 * @ordered
	 */
	protected boolean useEnvVar = USE_ENV_VAR_EDEFAULT;

	/**
	 * The default value of the '{@link #getEnvVarName() <em>Env Var Name</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getEnvVarName()
	 * @generated
	 * @ordered
	 */
	protected static final String ENV_VAR_NAME_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getEnvVarName() <em>Env Var Name</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getEnvVarName()
	 * @generated
	 * @ordered
	 */
	protected String envVarName = ENV_VAR_NAME_EDEFAULT;

	/**
	 * The default value of the '{@link #getState() <em>State</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getState()
	 * @generated
	 * @ordered
	 */
	protected static final String STATE_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getState() <em>State</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getState()
	 * @generated
	 * @ordered
	 */
	protected String state = STATE_EDEFAULT;

	/**
	 * The default value of the '{@link #getStateDescription() <em>State Description</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getStateDescription()
	 * @generated
	 * @ordered
	 */
	protected static final String STATE_DESCRIPTION_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getStateDescription() <em>State Description</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getStateDescription()
	 * @generated
	 * @ordered
	 */
	protected String stateDescription = STATE_DESCRIPTION_EDEFAULT;

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
	 * The default value of the '{@link #getRatingAnalyze() <em>Rating Analyze</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getRatingAnalyze()
	 * @generated
	 * @ordered
	 */
	protected static final int RATING_ANALYZE_EDEFAULT = 0;

	/**
	 * The cached value of the '{@link #getRatingAnalyze() <em>Rating Analyze</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getRatingAnalyze()
	 * @generated
	 * @ordered
	 */
	protected int ratingAnalyze = RATING_ANALYZE_EDEFAULT;

	/**
	 * The default value of the '{@link #getRatingChat() <em>Rating Chat</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getRatingChat()
	 * @generated
	 * @ordered
	 */
	protected static final int RATING_CHAT_EDEFAULT = 0;

	/**
	 * The cached value of the '{@link #getRatingChat() <em>Rating Chat</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getRatingChat()
	 * @generated
	 * @ordered
	 */
	protected int ratingChat = RATING_CHAT_EDEFAULT;

	/**
	 * The default value of the '{@link #getRatingProgramming() <em>Rating Programming</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getRatingProgramming()
	 * @generated
	 * @ordered
	 */
	protected static final int RATING_PROGRAMMING_EDEFAULT = 0;

	/**
	 * The cached value of the '{@link #getRatingProgramming() <em>Rating Programming</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getRatingProgramming()
	 * @generated
	 * @ordered
	 */
	protected int ratingProgramming = RATING_PROGRAMMING_EDEFAULT;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected AIProviderImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return OrchestrationPackage.Literals.AI_PROVIDER;
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
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.AI_PROVIDER__NAME, oldName, name));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getUrl() {
		return url;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setUrl(String newUrl) {
		String oldUrl = url;
		url = newUrl;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.AI_PROVIDER__URL, oldUrl, url));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getApiKey() {
		return apiKey;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setApiKey(String newApiKey) {
		String oldApiKey = apiKey;
		apiKey = newApiKey;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.AI_PROVIDER__API_KEY, oldApiKey, apiKey));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getFormat() {
		return format;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setFormat(String newFormat) {
		String oldFormat = format;
		format = newFormat;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.AI_PROVIDER__FORMAT, oldFormat, format));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public boolean isLocal() {
		return local;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setLocal(boolean newLocal) {
		boolean oldLocal = local;
		local = newLocal;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.AI_PROVIDER__LOCAL, oldLocal, local));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getDefaultModel() {
		return defaultModel;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setDefaultModel(String newDefaultModel) {
		String oldDefaultModel = defaultModel;
		defaultModel = newDefaultModel;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.AI_PROVIDER__DEFAULT_MODEL, oldDefaultModel, defaultModel));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public boolean isApiKeyEncrypted() {
		return apiKeyEncrypted;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setApiKeyEncrypted(boolean newApiKeyEncrypted) {
		boolean oldApiKeyEncrypted = apiKeyEncrypted;
		apiKeyEncrypted = newApiKeyEncrypted;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.AI_PROVIDER__API_KEY_ENCRYPTED, oldApiKeyEncrypted, apiKeyEncrypted));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public boolean isUseEnvVar() {
		return useEnvVar;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setUseEnvVar(boolean newUseEnvVar) {
		boolean oldUseEnvVar = useEnvVar;
		useEnvVar = newUseEnvVar;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.AI_PROVIDER__USE_ENV_VAR, oldUseEnvVar, useEnvVar));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getEnvVarName() {
		return envVarName;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setEnvVarName(String newEnvVarName) {
		String oldEnvVarName = envVarName;
		envVarName = newEnvVarName;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.AI_PROVIDER__ENV_VAR_NAME, oldEnvVarName, envVarName));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getState() {
		return state;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setState(String newState) {
		String oldState = state;
		state = newState;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.AI_PROVIDER__STATE, oldState, state));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getStateDescription() {
		return stateDescription;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setStateDescription(String newStateDescription) {
		String oldStateDescription = stateDescription;
		stateDescription = newStateDescription;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.AI_PROVIDER__STATE_DESCRIPTION, oldStateDescription, stateDescription));
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
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.AI_PROVIDER__RATING, oldRating, rating));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public int getRatingAnalyze() {
		return ratingAnalyze;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setRatingAnalyze(int newRatingAnalyze) {
		int oldRatingAnalyze = ratingAnalyze;
		ratingAnalyze = newRatingAnalyze;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.AI_PROVIDER__RATING_ANALYZE, oldRatingAnalyze, ratingAnalyze));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public int getRatingChat() {
		return ratingChat;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setRatingChat(int newRatingChat) {
		int oldRatingChat = ratingChat;
		ratingChat = newRatingChat;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.AI_PROVIDER__RATING_CHAT, oldRatingChat, ratingChat));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public int getRatingProgramming() {
		return ratingProgramming;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setRatingProgramming(int newRatingProgramming) {
		int oldRatingProgramming = ratingProgramming;
		ratingProgramming = newRatingProgramming;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.AI_PROVIDER__RATING_PROGRAMMING, oldRatingProgramming, ratingProgramming));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public Object eGet(int featureID, boolean resolve, boolean coreType) {
		switch (featureID) {
			case OrchestrationPackage.AI_PROVIDER__NAME:
				return getName();
			case OrchestrationPackage.AI_PROVIDER__URL:
				return getUrl();
			case OrchestrationPackage.AI_PROVIDER__API_KEY:
				return getApiKey();
			case OrchestrationPackage.AI_PROVIDER__FORMAT:
				return getFormat();
			case OrchestrationPackage.AI_PROVIDER__LOCAL:
				return isLocal();
			case OrchestrationPackage.AI_PROVIDER__DEFAULT_MODEL:
				return getDefaultModel();
			case OrchestrationPackage.AI_PROVIDER__API_KEY_ENCRYPTED:
				return isApiKeyEncrypted();
			case OrchestrationPackage.AI_PROVIDER__USE_ENV_VAR:
				return isUseEnvVar();
			case OrchestrationPackage.AI_PROVIDER__ENV_VAR_NAME:
				return getEnvVarName();
			case OrchestrationPackage.AI_PROVIDER__STATE:
				return getState();
			case OrchestrationPackage.AI_PROVIDER__STATE_DESCRIPTION:
				return getStateDescription();
			case OrchestrationPackage.AI_PROVIDER__RATING:
				return getRating();
			case OrchestrationPackage.AI_PROVIDER__RATING_ANALYZE:
				return getRatingAnalyze();
			case OrchestrationPackage.AI_PROVIDER__RATING_CHAT:
				return getRatingChat();
			case OrchestrationPackage.AI_PROVIDER__RATING_PROGRAMMING:
				return getRatingProgramming();
		}
		return super.eGet(featureID, resolve, coreType);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void eSet(int featureID, Object newValue) {
		switch (featureID) {
			case OrchestrationPackage.AI_PROVIDER__NAME:
				setName((String)newValue);
				return;
			case OrchestrationPackage.AI_PROVIDER__URL:
				setUrl((String)newValue);
				return;
			case OrchestrationPackage.AI_PROVIDER__API_KEY:
				setApiKey((String)newValue);
				return;
			case OrchestrationPackage.AI_PROVIDER__FORMAT:
				setFormat((String)newValue);
				return;
			case OrchestrationPackage.AI_PROVIDER__LOCAL:
				setLocal((Boolean)newValue);
				return;
			case OrchestrationPackage.AI_PROVIDER__DEFAULT_MODEL:
				setDefaultModel((String)newValue);
				return;
			case OrchestrationPackage.AI_PROVIDER__API_KEY_ENCRYPTED:
				setApiKeyEncrypted((Boolean)newValue);
				return;
			case OrchestrationPackage.AI_PROVIDER__USE_ENV_VAR:
				setUseEnvVar((Boolean)newValue);
				return;
			case OrchestrationPackage.AI_PROVIDER__ENV_VAR_NAME:
				setEnvVarName((String)newValue);
				return;
			case OrchestrationPackage.AI_PROVIDER__STATE:
				setState((String)newValue);
				return;
			case OrchestrationPackage.AI_PROVIDER__STATE_DESCRIPTION:
				setStateDescription((String)newValue);
				return;
			case OrchestrationPackage.AI_PROVIDER__RATING:
				setRating((Integer)newValue);
				return;
			case OrchestrationPackage.AI_PROVIDER__RATING_ANALYZE:
				setRatingAnalyze((Integer)newValue);
				return;
			case OrchestrationPackage.AI_PROVIDER__RATING_CHAT:
				setRatingChat((Integer)newValue);
				return;
			case OrchestrationPackage.AI_PROVIDER__RATING_PROGRAMMING:
				setRatingProgramming((Integer)newValue);
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
			case OrchestrationPackage.AI_PROVIDER__NAME:
				setName(NAME_EDEFAULT);
				return;
			case OrchestrationPackage.AI_PROVIDER__URL:
				setUrl(URL_EDEFAULT);
				return;
			case OrchestrationPackage.AI_PROVIDER__API_KEY:
				setApiKey(API_KEY_EDEFAULT);
				return;
			case OrchestrationPackage.AI_PROVIDER__FORMAT:
				setFormat(FORMAT_EDEFAULT);
				return;
			case OrchestrationPackage.AI_PROVIDER__LOCAL:
				setLocal(LOCAL_EDEFAULT);
				return;
			case OrchestrationPackage.AI_PROVIDER__DEFAULT_MODEL:
				setDefaultModel(DEFAULT_MODEL_EDEFAULT);
				return;
			case OrchestrationPackage.AI_PROVIDER__API_KEY_ENCRYPTED:
				setApiKeyEncrypted(API_KEY_ENCRYPTED_EDEFAULT);
				return;
			case OrchestrationPackage.AI_PROVIDER__USE_ENV_VAR:
				setUseEnvVar(USE_ENV_VAR_EDEFAULT);
				return;
			case OrchestrationPackage.AI_PROVIDER__ENV_VAR_NAME:
				setEnvVarName(ENV_VAR_NAME_EDEFAULT);
				return;
			case OrchestrationPackage.AI_PROVIDER__STATE:
				setState(STATE_EDEFAULT);
				return;
			case OrchestrationPackage.AI_PROVIDER__STATE_DESCRIPTION:
				setStateDescription(STATE_DESCRIPTION_EDEFAULT);
				return;
			case OrchestrationPackage.AI_PROVIDER__RATING:
				setRating(RATING_EDEFAULT);
				return;
			case OrchestrationPackage.AI_PROVIDER__RATING_ANALYZE:
				setRatingAnalyze(RATING_ANALYZE_EDEFAULT);
				return;
			case OrchestrationPackage.AI_PROVIDER__RATING_CHAT:
				setRatingChat(RATING_CHAT_EDEFAULT);
				return;
			case OrchestrationPackage.AI_PROVIDER__RATING_PROGRAMMING:
				setRatingProgramming(RATING_PROGRAMMING_EDEFAULT);
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
			case OrchestrationPackage.AI_PROVIDER__NAME:
				return NAME_EDEFAULT == null ? name != null : !NAME_EDEFAULT.equals(name);
			case OrchestrationPackage.AI_PROVIDER__URL:
				return URL_EDEFAULT == null ? url != null : !URL_EDEFAULT.equals(url);
			case OrchestrationPackage.AI_PROVIDER__API_KEY:
				return API_KEY_EDEFAULT == null ? apiKey != null : !API_KEY_EDEFAULT.equals(apiKey);
			case OrchestrationPackage.AI_PROVIDER__FORMAT:
				return FORMAT_EDEFAULT == null ? format != null : !FORMAT_EDEFAULT.equals(format);
			case OrchestrationPackage.AI_PROVIDER__LOCAL:
				return local != LOCAL_EDEFAULT;
			case OrchestrationPackage.AI_PROVIDER__DEFAULT_MODEL:
				return DEFAULT_MODEL_EDEFAULT == null ? defaultModel != null : !DEFAULT_MODEL_EDEFAULT.equals(defaultModel);
			case OrchestrationPackage.AI_PROVIDER__API_KEY_ENCRYPTED:
				return apiKeyEncrypted != API_KEY_ENCRYPTED_EDEFAULT;
			case OrchestrationPackage.AI_PROVIDER__USE_ENV_VAR:
				return useEnvVar != USE_ENV_VAR_EDEFAULT;
			case OrchestrationPackage.AI_PROVIDER__ENV_VAR_NAME:
				return ENV_VAR_NAME_EDEFAULT == null ? envVarName != null : !ENV_VAR_NAME_EDEFAULT.equals(envVarName);
			case OrchestrationPackage.AI_PROVIDER__STATE:
				return STATE_EDEFAULT == null ? state != null : !STATE_EDEFAULT.equals(state);
			case OrchestrationPackage.AI_PROVIDER__STATE_DESCRIPTION:
				return STATE_DESCRIPTION_EDEFAULT == null ? stateDescription != null : !STATE_DESCRIPTION_EDEFAULT.equals(stateDescription);
			case OrchestrationPackage.AI_PROVIDER__RATING:
				return rating != RATING_EDEFAULT;
			case OrchestrationPackage.AI_PROVIDER__RATING_ANALYZE:
				return ratingAnalyze != RATING_ANALYZE_EDEFAULT;
			case OrchestrationPackage.AI_PROVIDER__RATING_CHAT:
				return ratingChat != RATING_CHAT_EDEFAULT;
			case OrchestrationPackage.AI_PROVIDER__RATING_PROGRAMMING:
				return ratingProgramming != RATING_PROGRAMMING_EDEFAULT;
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
		result.append(" (name: ");
		result.append(name);
		result.append(", url: ");
		result.append(url);
		result.append(", apiKey: ");
		result.append(apiKey);
		result.append(", format: ");
		result.append(format);
		result.append(", local: ");
		result.append(local);
		result.append(", defaultModel: ");
		result.append(defaultModel);
		result.append(", apiKeyEncrypted: ");
		result.append(apiKeyEncrypted);
		result.append(", useEnvVar: ");
		result.append(useEnvVar);
		result.append(", envVarName: ");
		result.append(envVarName);
		result.append(", state: ");
		result.append(state);
		result.append(", stateDescription: ");
		result.append(stateDescription);
		result.append(", rating: ");
		result.append(rating);
		result.append(", ratingAnalyze: ");
		result.append(ratingAnalyze);
		result.append(", ratingChat: ");
		result.append(ratingChat);
		result.append(", ratingProgramming: ");
		result.append(ratingProgramming);
		result.append(')');
		return result.toString();
	}

} //AIProviderImpl
