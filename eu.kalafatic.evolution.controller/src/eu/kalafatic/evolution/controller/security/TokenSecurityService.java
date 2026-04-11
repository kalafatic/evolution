package eu.kalafatic.evolution.controller.security;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import eu.kalafatic.evolution.model.orchestration.AIProvider;

public class TokenSecurityService {

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
}
