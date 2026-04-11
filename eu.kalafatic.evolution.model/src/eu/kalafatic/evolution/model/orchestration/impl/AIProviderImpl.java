package eu.kalafatic.evolution.model.orchestration.impl;

import eu.kalafatic.evolution.model.orchestration.AIProvider;
import eu.kalafatic.evolution.model.orchestration.OrchestrationPackage;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.MinimalEObjectImpl;

public class AIProviderImpl extends MinimalEObjectImpl.Container implements AIProvider {
    protected String name;
    protected String url;
    protected String apiKey;
    protected String format;
    protected boolean local;
    protected String defaultModel;

    protected AIProviderImpl() { super(); }

    @Override protected EClass eStaticClass() { return OrchestrationPackage.Literals.AI_PROVIDER; }

    @Override public String getName() { return name; }
    @Override public void setName(String newName) {
        String oldName = name; name = newName;
        if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.AI_PROVIDER__NAME, oldName, name));
    }

    @Override public String getUrl() { return url; }
    @Override public void setUrl(String newUrl) {
        String oldUrl = url; url = newUrl;
        if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.AI_PROVIDER__URL, oldUrl, url));
    }

    @Override public String getApiKey() { return apiKey; }
    @Override public void setApiKey(String newApiKey) {
        String oldApiKey = apiKey; apiKey = newApiKey;
        if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.AI_PROVIDER__API_KEY, oldApiKey, apiKey));
    }

    @Override public String getFormat() { return format; }
    @Override public void setFormat(String newFormat) {
        String oldFormat = format; format = newFormat;
        if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.AI_PROVIDER__FORMAT, oldFormat, format));
    }

    @Override public boolean isLocal() { return local; }
    @Override public void setLocal(boolean newLocal) {
        boolean oldLocal = local; local = newLocal;
        if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.AI_PROVIDER__LOCAL, oldLocal, local));
    }

    @Override public String getDefaultModel() { return defaultModel; }
    @Override public void setDefaultModel(String newDefaultModel) {
        String oldDefaultModel = defaultModel; defaultModel = newDefaultModel;
        if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.AI_PROVIDER__DEFAULT_MODEL, oldDefaultModel, defaultModel));
    }

    @Override
    public Object eGet(int featureID, boolean resolve, boolean coreType) {
        switch (featureID) {
            case OrchestrationPackage.AI_PROVIDER__NAME: return getName();
            case OrchestrationPackage.AI_PROVIDER__URL: return getUrl();
            case OrchestrationPackage.AI_PROVIDER__API_KEY: return getApiKey();
            case OrchestrationPackage.AI_PROVIDER__FORMAT: return getFormat();
            case OrchestrationPackage.AI_PROVIDER__LOCAL: return isLocal();
            case OrchestrationPackage.AI_PROVIDER__DEFAULT_MODEL: return getDefaultModel();
        }
        return super.eGet(featureID, resolve, coreType);
    }

    @Override
    public void eSet(int featureID, Object newValue) {
        switch (featureID) {
            case OrchestrationPackage.AI_PROVIDER__NAME: setName((String)newValue); return;
            case OrchestrationPackage.AI_PROVIDER__URL: setUrl((String)newValue); return;
            case OrchestrationPackage.AI_PROVIDER__API_KEY: setApiKey((String)newValue); return;
            case OrchestrationPackage.AI_PROVIDER__FORMAT: setFormat((String)newValue); return;
            case OrchestrationPackage.AI_PROVIDER__LOCAL: setLocal((Boolean)newValue); return;
            case OrchestrationPackage.AI_PROVIDER__DEFAULT_MODEL: setDefaultModel((String)newValue); return;
        }
        super.eSet(featureID, newValue);
    }
}
