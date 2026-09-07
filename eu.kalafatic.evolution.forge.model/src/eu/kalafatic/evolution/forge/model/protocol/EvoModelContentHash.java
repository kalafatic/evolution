package eu.kalafatic.evolution.forge.model.protocol;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Deterministic SHA-256 Model Content Hash calculator for EVO Native Model Protocol.
 * Uses chunked buffer updates to handle large tensors memory-efficiently.
 */
public class EvoModelContentHash {

    private static final String SHA256_ALGORITHM = "SHA-256";
    private static final int BUFFER_CAPACITY = 8192; // 8 KB chunk size

    public static String calculateContentHash(EvoArchitectureDescriptor arch,
                                               EvoTokenizerDescriptor tokenizer,
                                               List<EvoTensorDescriptor> manifest,
                                               List<float[]> tensorDataPayloads) {
        try {
            MessageDigest digest = MessageDigest.getInstance(SHA256_ALGORITHM);

            if (arch != null) {
                byte[] archBytes = arch.toJsonObject().toString().getBytes(StandardCharsets.UTF_8);
                digest.update(archBytes);
            }

            if (tokenizer != null) {
                byte[] tokBytes = tokenizer.toJsonObject().toString().getBytes(StandardCharsets.UTF_8);
                digest.update(tokBytes);
            }

            if (manifest != null && !manifest.isEmpty()) {
                List<EvoTensorDescriptor> sortedManifest = new ArrayList<>(manifest);
                sortedManifest.sort(Comparator.comparing(EvoTensorDescriptor::getCanonicalName));

                for (EvoTensorDescriptor desc : sortedManifest) {
                    digest.update(desc.getCanonicalName().getBytes(StandardCharsets.UTF_8));
                    digest.update((byte) desc.getDtype().getCode());
                    ByteBuffer bb = ByteBuffer.allocate(Long.BYTES).order(ByteOrder.BIG_ENDIAN);
                    for (long dim : desc.getShape()) {
                        bb.clear();
                        bb.putLong(dim);
                        digest.update(bb.array());
                    }
                }
            }

            if (tensorDataPayloads != null) {
                ByteBuffer chunk = ByteBuffer.allocate(BUFFER_CAPACITY).order(ByteOrder.BIG_ENDIAN);
                for (float[] data : tensorDataPayloads) {
                    if (data != null) {
                        for (float val : data) {
                            if (chunk.remaining() < Float.BYTES) {
                                digest.update(chunk.array(), 0, chunk.position());
                                chunk.clear();
                            }
                            chunk.putFloat(val);
                        }
                        if (chunk.position() > 0) {
                            digest.update(chunk.array(), 0, chunk.position());
                            chunk.clear();
                        }
                    }
                }
            }

            return bytesToHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", e);
        }
    }

    public static String calculateFloatArrayChecksum(float[] data) {
        if (data == null) return "";
        try {
            MessageDigest digest = MessageDigest.getInstance(SHA256_ALGORITHM);
            ByteBuffer chunk = ByteBuffer.allocate(BUFFER_CAPACITY).order(ByteOrder.BIG_ENDIAN);
            for (float val : data) {
                if (chunk.remaining() < Float.BYTES) {
                    digest.update(chunk.array(), 0, chunk.position());
                    chunk.clear();
                }
                chunk.putFloat(val);
            }
            if (chunk.position() > 0) {
                digest.update(chunk.array(), 0, chunk.position());
                chunk.clear();
            }
            return bytesToHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", e);
        }
    }

    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
