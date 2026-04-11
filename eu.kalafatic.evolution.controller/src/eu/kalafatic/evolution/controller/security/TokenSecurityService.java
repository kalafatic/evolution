package eu.kalafatic.evolution.controller.security;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import eu.kalafatic.evolution.model.orchestration.AIProvider;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.controller.providers.AiProviders;
import eu.kalafatic.evolution.controller.providers.ProviderConfig;

public class TokenSecurityService {

    public static class ResolvedProvider {
        public String name;
        public String url;
        public String token;
        public String format;
        public String model;
        public boolean local;

        public ResolvedProvider(String name, String url, String token, String format, String model, boolean local) {
            this.name = name;
            this.url = url;
            this.token = token;
            this.format = format;
            this.model = model;
            this.local = local;
        }
    }

    private static final String ALGORITHM = "AES";
    // In a real scenario, this should be more secure/machine-specific
    private static final byte[] KEY = "EvolutionSecureK".getBytes(StandardCharsets.UTF_8);

    private static TokenSecurityService instance;

    public static TokenSecurityService getInstance() {
        if (instance == null) {
            instance = new TokenSecurityService();
        }
        return instance;
    }

    public String encrypt(String token) throws Exception {
        if (token == null || token.isEmpty()) return token;
        SecretKeySpec secretKey = new SecretKeySpec(KEY, ALGORITHM);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] encryptedBytes = cipher.doFinal(token.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    public String decrypt(String encryptedToken) throws Exception {
        if (encryptedToken == null || encryptedToken.isEmpty()) return encryptedToken;
        SecretKeySpec secretKey = new SecretKeySpec(KEY, ALGORITHM);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        byte[] decodedBytes = Base64.getDecoder().decode(encryptedToken);
        byte[] decryptedBytes = cipher.doFinal(decodedBytes);
        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }

    public String getToken(AIProvider provider) {
        if (provider == null) return null;

        if (provider.isUseEnvVar()) {
            String envName = provider.getEnvVarName();
            if (envName != null && !envName.isEmpty()) {
                String envValue = System.getenv(envName);
                if (envValue != null && !envValue.isEmpty()) {
                    return envValue;
                }
            }
        }

        String apiKey = provider.getApiKey();
        if (provider.isApiKeyEncrypted() && apiKey != null && !apiKey.isEmpty()) {
            try {
                return decrypt(apiKey);
            } catch (Exception e) {
                // Fallback to raw if decryption fails, or log error
                return apiKey;
            }
        }
        return apiKey;
    }

    public ResolvedProvider resolve(Orchestrator orchestrator, String providerName) {
        if (orchestrator == null) return null;
        String name = providerName != null ? providerName : orchestrator.getRemoteModel();

        // 1. Try Custom Provider from Model
        if (orchestrator.getAiProviders() != null && name != null) {
            AIProvider custom = orchestrator.getAiProviders().stream()
                    .filter(p -> p.getName().equalsIgnoreCase(name))
                    .findFirst().orElse(null);
            if (custom != null) {
                return new ResolvedProvider(
                        custom.getName(),
                        custom.getUrl(),
                        getToken(custom),
                        custom.getFormat(),
                        custom.getDefaultModel(),
                        custom.isLocal()
                );
            }
        }

        // 2. Try static config
        ProviderConfig config = (name != null) ? AiProviders.PROVIDERS.get(name.toLowerCase()) : null;
        if (config != null) {
            String token = config.getApiKey();
            // If it's the currently selected remote model in orchestrator, check openAiToken first
            if (name.equalsIgnoreCase(orchestrator.getRemoteModel()) &&
                orchestrator.getOpenAiToken() != null && !orchestrator.getOpenAiToken().isEmpty() &&
                !"YOUR_API_KEY".equals(orchestrator.getOpenAiToken())) {
                token = orchestrator.getOpenAiToken();
            }

            return new ResolvedProvider(
                    config.getName(),
                    config.getUrl(),
                    token,
                    config.getFormat(),
                    config.getDefaultModel(),
                    config.isLocal()
            );
        }

        // 3. Last fallback to legacy fields if name matches or is generic
        if (name == null || "openai".equalsIgnoreCase(name) || name.equalsIgnoreCase(orchestrator.getRemoteModel())) {
            String token = orchestrator.getOpenAiToken();
            if (token == null && orchestrator.getAiChat() != null) token = orchestrator.getAiChat().getToken();

            String url = (orchestrator.getAiChat() != null) ? orchestrator.getAiChat().getUrl() : null;
            String model = orchestrator.getOpenAiModel();

            return new ResolvedProvider(name != null ? name : "openai", url, token, "openai", model, false);
        }

        return null;
    }

    public void updateToken(Orchestrator orchestrator, String providerName, String token) {
        if (orchestrator == null || providerName == null) return;

        // 1. If custom provider exists in model, update it
        if (orchestrator.getAiProviders() != null) {
            AIProvider custom = orchestrator.getAiProviders().stream()
                    .filter(p -> p.getName().equalsIgnoreCase(providerName))
                    .findFirst().orElse(null);
            if (custom != null) {
                if (custom.isApiKeyEncrypted()) {
                    try {
                        custom.setApiKey(encrypt(token));
                    } catch (Exception e) {
                        custom.setApiKey(token);
                    }
                } else {
                    custom.setApiKey(token);
                }
                return;
            }
        }

        // 2. Otherwise update legacy field
        if ("openai".equalsIgnoreCase(providerName) || providerName.equalsIgnoreCase(orchestrator.getRemoteModel())) {
            orchestrator.setOpenAiToken(token);
        }

        // Also sync to aiChat token if it's the current chat model
        if (orchestrator.getAiChat() != null && providerName.equalsIgnoreCase(orchestrator.getRemoteModel())) {
             orchestrator.getAiChat().setToken(token);
        }
    }
}
