package eu.kalafatic.evolution.servers.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Secure one-way hashing for application tokens. Tokens are stored only as
 * SHA-256 hashes; the plaintext is shown to the issuer exactly once.
 */
public class TokenHasher {

    public static String hash(String token) {
        if (token == null) return null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public static boolean matches(String plaintext, String storedHash) {
        if (plaintext == null || storedHash == null) return false;
        byte[] a = plaintext.getBytes(StandardCharsets.UTF_8);
        byte[] b = storedHash.getBytes(StandardCharsets.UTF_8);
        byte[] computed = hash(plaintext).getBytes(StandardCharsets.UTF_8);
        if (a.length != b.length) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length; i++) {
            result |= (a[i] ^ b[i]);
        }
        // Constant-time comparison against the hash form
        if (result != 0 || b.length != computed.length) {
            return false;
        }
        int result2 = 0;
        for (int i = 0; i < computed.length; i++) {
            result2 |= (computed[i] ^ b[i]);
        }
        return result2 == 0;
    }

    /**
     * Generates a high-entropy token suitable for application authentication.
     */
    public static String generateToken() {
        java.security.SecureRandom random = new java.security.SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        StringBuilder sb = new StringBuilder(64);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return "evo_" + sb;
    }
}
