package eu.kalafatic.evolution.model.orchestration;

import org.eclipse.emf.ecore.EObject;

public interface AIProvider extends EObject {
    String getName();
    void setName(String value);
    String getUrl();
    void setUrl(String value);
    String getApiKey();
    void setApiKey(String value);
    String getFormat();
    void setFormat(String value);
    boolean isLocal();
    void setLocal(boolean value);
    String getDefaultModel();
    void setDefaultModel(String value);
    boolean isApiKeyEncrypted();
    void setApiKeyEncrypted(boolean value);
    boolean isUseEnvVar();
    void setUseEnvVar(boolean value);
    String getEnvVarName();
    void setEnvVarName(String value);
}
