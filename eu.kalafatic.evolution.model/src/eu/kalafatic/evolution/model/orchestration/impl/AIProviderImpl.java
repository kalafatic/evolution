package eu.kalafatic.evolution.model.orchestration.impl;

import eu.kalafatic.evolution.model.orchestration.AIProvider;
import eu.kalafatic.evolution.model.orchestration.OrchestrationPackage;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.MinimalEObjectImpl;

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
     * @generated NOT
     */
    protected String name;
    /**
	 * The default value of the '{@link #getUrl() <em>Url</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getUrl()
	 * @generated
	 * @ordered
	 */
	protected static final String URL_EDEFAULT = null;
				protected String url;
    /**
	 * The default value of the '{@link #getApiKey() <em>Api Key</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getApiKey()
	 * @generated
	 * @ordered
	 */
	protected static final String API_KEY_EDEFAULT = null;
				protected String apiKey;
    /**
	 * The default value of the '{@link #getFormat() <em>Format</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getFormat()
	 * @generated
	 * @ordered
	 */
	protected static final String FORMAT_EDEFAULT = null;
				protected String format;
    /**
	 * The default value of the '{@link #isLocal() <em>Local</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isLocal()
	 * @generated
	 * @ordered
	 */
	protected static final boolean LOCAL_EDEFAULT = false;
				protected boolean local;
    /**
	 * The default value of the '{@link #getDefaultModel() <em>Default Model</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getDefaultModel()
	 * @generated
	 * @ordered
	 */
	protected static final String DEFAULT_MODEL_EDEFAULT = null;
				protected String defaultModel;
    /**
	 * The default value of the '{@link #isApiKeyEncrypted() <em>Api Key Encrypted</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isApiKeyEncrypted()
	 * @generated
	 * @ordered
	 */
	protected static final boolean API_KEY_ENCRYPTED_EDEFAULT = false;
				protected boolean apiKeyEncrypted;
    /**
	 * The default value of the '{@link #isUseEnvVar() <em>Use Env Var</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isUseEnvVar()
	 * @generated
	 * @ordered
	 */
	protected static final boolean USE_ENV_VAR_EDEFAULT = false;
				protected boolean useEnvVar;
    /**
	 * The default value of the '{@link #getEnvVarName() <em>Env Var Name</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getEnvVarName()
	 * @generated
	 * @ordered
	 */
	protected static final String ENV_VAR_NAME_EDEFAULT = null;
				protected String envVarName;
    /**
	 * The default value of the '{@link #getState() <em>State</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getState()
	 * @generated
	 * @ordered
	 */
	protected static final String STATE_EDEFAULT = null;
				protected String state;
    /**
	 * The default value of the '{@link #getStateDescription() <em>State Description</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getStateDescription()
	 * @generated
	 * @ordered
	 */
	protected static final String STATE_DESCRIPTION_EDEFAULT = null;
				protected String stateDescription;
    /**
	 * The default value of the '{@link #getRating() <em>Rating</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getRating()
	 * @generated
	 * @ordered
	 */
	protected static final int RATING_EDEFAULT = 0;
				protected int rating;

    protected AIProviderImpl() { super(); }

    @Override protected EClass eStaticClass() { return OrchestrationPackage.Literals.AI_PROVIDER; }

    /**
     * @generated NOT
     */
    @Override public String getName() { return name; }
    /**
     * @generated NOT
     */
    @Override public void setName(String newName) {
        String oldName = name; name = newName;
        if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.AI_PROVIDER__NAME, oldName, name));
    }

    /**
     * @generated NOT
     */
    @Override public String getUrl() { return url; }
    /**
     * @generated NOT
     */
    @Override public void setUrl(String newUrl) {
        String oldUrl = url; url = newUrl;
        if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.AI_PROVIDER__URL, oldUrl, url));
    }

    /**
     * @generated NOT
     */
    @Override public String getApiKey() { return apiKey; }
    /**
     * @generated NOT
     */
    @Override public void setApiKey(String newApiKey) {
        String oldApiKey = apiKey; apiKey = newApiKey;
        if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.AI_PROVIDER__API_KEY, oldApiKey, apiKey));
    }

    /**
     * @generated NOT
     */
    @Override public String getFormat() { return format; }
    /**
     * @generated NOT
     */
    @Override public void setFormat(String newFormat) {
        String oldFormat = format; format = newFormat;
        if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.AI_PROVIDER__FORMAT, oldFormat, format));
    }

    /**
     * @generated NOT
     */
    @Override public boolean isLocal() { return local; }
    /**
     * @generated NOT
     */
    @Override public void setLocal(boolean newLocal) {
        boolean oldLocal = local; local = newLocal;
        if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.AI_PROVIDER__LOCAL, oldLocal, local));
    }

    /**
     * @generated NOT
     */
    @Override public String getDefaultModel() { return defaultModel; }
    /**
     * @generated NOT
     */
    @Override public void setDefaultModel(String newDefaultModel) {
        String oldDefaultModel = defaultModel; defaultModel = newDefaultModel;
        if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.AI_PROVIDER__DEFAULT_MODEL, oldDefaultModel, defaultModel));
    }

    /**
     * @generated NOT
     */
    @Override public boolean isApiKeyEncrypted() { return apiKeyEncrypted; }
    /**
     * @generated NOT
     */
    @Override public void setApiKeyEncrypted(boolean newApiKeyEncrypted) {
        boolean oldApiKeyEncrypted = apiKeyEncrypted; apiKeyEncrypted = newApiKeyEncrypted;
        if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.AI_PROVIDER__API_KEY_ENCRYPTED, oldApiKeyEncrypted, apiKeyEncrypted));
    }

    /**
     * @generated NOT
     */
    @Override public boolean isUseEnvVar() { return useEnvVar; }
    /**
     * @generated NOT
     */
    @Override public void setUseEnvVar(boolean newUseEnvVar) {
        boolean oldUseEnvVar = useEnvVar; useEnvVar = newUseEnvVar;
        if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.AI_PROVIDER__USE_ENV_VAR, oldUseEnvVar, useEnvVar));
    }

    /**
     * @generated NOT
     */
    @Override public String getEnvVarName() { return envVarName; }
    /**
     * @generated NOT
     */
    @Override public void setEnvVarName(String newEnvVarName) {
        String oldEnvVarName = envVarName; envVarName = newEnvVarName;
        if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.AI_PROVIDER__ENV_VAR_NAME, oldEnvVarName, envVarName));
    }

    /**
     * @generated NOT
     */
    @Override public String getState() { return state; }
    /**
     * @generated NOT
     */
    @Override public void setState(String newState) {
        String oldState = state; state = newState;
        if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.AI_PROVIDER__STATE, oldState, state));
    }

    /**
     * @generated NOT
     */
    @Override public String getStateDescription() { return stateDescription; }
    /**
     * @generated NOT
     */
    @Override public void setStateDescription(String newStateDescription) {
        String oldStateDescription = stateDescription; stateDescription = newStateDescription;
        if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.AI_PROVIDER__STATE_DESCRIPTION, oldStateDescription, stateDescription));
    }

    /**
     * @generated NOT
     */
    @Override public int getRating() { return rating; }
    /**
     * @generated NOT
     */
    @Override public void setRating(int newRating) {
        int oldRating = rating; rating = newRating;
        if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.AI_PROVIDER__RATING, oldRating, rating));
    }

    /**
     * @generated NOT
     */
    @Override
    public Object eGet(int featureID, boolean resolve, boolean coreType) {
        switch (featureID) {
            case OrchestrationPackage.AI_PROVIDER__NAME: return getName();
            case OrchestrationPackage.AI_PROVIDER__URL: return getUrl();
            case OrchestrationPackage.AI_PROVIDER__API_KEY: return getApiKey();
            case OrchestrationPackage.AI_PROVIDER__FORMAT: return getFormat();
            case OrchestrationPackage.AI_PROVIDER__LOCAL: return isLocal();
            case OrchestrationPackage.AI_PROVIDER__DEFAULT_MODEL: return getDefaultModel();
            case OrchestrationPackage.AI_PROVIDER__API_KEY_ENCRYPTED: return isApiKeyEncrypted();
            case OrchestrationPackage.AI_PROVIDER__USE_ENV_VAR: return isUseEnvVar();
            case OrchestrationPackage.AI_PROVIDER__ENV_VAR_NAME: return getEnvVarName();
            case OrchestrationPackage.AI_PROVIDER__STATE: return getState();
            case OrchestrationPackage.AI_PROVIDER__STATE_DESCRIPTION: return getStateDescription();
            case OrchestrationPackage.AI_PROVIDER__RATING: return getRating();
        }
        return super.eGet(featureID, resolve, coreType);
    }

    /**
     * @generated NOT
     */
    @Override
    public void eSet(int featureID, Object newValue) {
        switch (featureID) {
            case OrchestrationPackage.AI_PROVIDER__NAME: setName((String)newValue); return;
            case OrchestrationPackage.AI_PROVIDER__URL: setUrl((String)newValue); return;
            case OrchestrationPackage.AI_PROVIDER__API_KEY: setApiKey((String)newValue); return;
            case OrchestrationPackage.AI_PROVIDER__FORMAT: setFormat((String)newValue); return;
            case OrchestrationPackage.AI_PROVIDER__LOCAL: setLocal((Boolean)newValue); return;
            case OrchestrationPackage.AI_PROVIDER__DEFAULT_MODEL: setDefaultModel((String)newValue); return;
            case OrchestrationPackage.AI_PROVIDER__API_KEY_ENCRYPTED: setApiKeyEncrypted((Boolean)newValue); return;
            case OrchestrationPackage.AI_PROVIDER__USE_ENV_VAR: setUseEnvVar((Boolean)newValue); return;
            case OrchestrationPackage.AI_PROVIDER__ENV_VAR_NAME: setEnvVarName((String)newValue); return;
            case OrchestrationPackage.AI_PROVIDER__STATE: setState((String)newValue); return;
            case OrchestrationPackage.AI_PROVIDER__STATE_DESCRIPTION: setStateDescription((String)newValue); return;
            case OrchestrationPackage.AI_PROVIDER__RATING: setRating((Integer)newValue); return;
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
		result.append(')');
		return result.toString();
	}
}
