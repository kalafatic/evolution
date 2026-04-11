package eu.kalafatic.evolution.model.orchestration.impl;

import eu.kalafatic.evolution.model.orchestration.AIProvider;
import eu.kalafatic.evolution.model.orchestration.OrchestrationPackage;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.MinimalEObjectImpl;

public class AIProviderImpl extends MinimalEObjectImpl.Container implements AIProvider {
    /**
     * @generated NOT
     */
    protected String name;
    protected String url;
    protected String apiKey;
    protected String format;
    protected boolean local;
    protected String defaultModel;
    protected boolean apiKeyEncrypted;
    protected boolean useEnvVar;
    protected String envVarName;
    protected String state;
    protected String stateDescription;
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
}
