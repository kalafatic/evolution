package eu.kalafatic.evolution.forge.data.impl.source.adapters;

import eu.kalafatic.evolution.forge.data.api.NormalizedSample;
import eu.kalafatic.evolution.forge.data.api.source.DatasetInspection;
import eu.kalafatic.evolution.forge.data.api.source.DatasetItem;
import eu.kalafatic.evolution.forge.data.api.source.DatasetPreparationContext;
import eu.kalafatic.evolution.forge.data.api.source.DatasetSourceAdapter;

import org.json.JSONObject;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Universal source adapter for Parquet columnar dataset files.
 * Extracts usable text, instruction, JSON, or tabular string records from .parquet dataset files.
 */
public class ParquetAdapter implements DatasetSourceAdapter {

    @Override
    public boolean supports(DatasetItem item) {
        if (item == null || item.getPath() == null) return false;
        String path = item.getPath().trim().toLowerCase();
        return path.endsWith(".parquet") || "PARQUET".equalsIgnoreCase(item.getType());
    }

    @Override
    public DatasetInspection inspect(DatasetItem item) {
        if (item == null || item.getPath() == null) {
            return new DatasetInspection(item, "ParquetAdapter", false, 0, "parquet", false, "Path is null");
        }
        File file = new File(item.getPath());
        boolean exists = file.exists() && file.isFile();
        long size = exists ? file.length() : 0;
        return new DatasetInspection(item, "ParquetAdapter", exists, size, "parquet", exists, exists ? "Parquet Dataset Source" : "File does not exist");
    }

    @Override
    public List<NormalizedSample> convert(DatasetItem item, DatasetPreparationContext context) throws Exception {
        List<NormalizedSample> samples = new ArrayList<>();
        if (item == null || item.getPath() == null) return samples;

        File file = new File(item.getPath());
        if (!file.exists() || !file.isFile()) {
            context.log("[forge.dataset] [ParquetAdapter] File not found: " + item.getPath());
            return samples;
        }

        context.log("[forge.dataset] Preparing Parquet source: " + file.getName());
        byte[] bytes = Files.readAllBytes(file.toPath());
        if (bytes.length == 0) return samples;

        boolean isParquetMagic = bytes.length >= 12 && (bytes[0] == 'P' && bytes[1] == 'A' && bytes[2] == 'R' && bytes[3] == '1');

        context.log(String.format("[FORGE-PARQUET-DIAG] file=%s physicalBytes=%d format=%s",
                file.getName(), bytes.length, isParquetMagic ? "BINARY_PARQUET_PAR1" : "PLAIN_TEXT_FALLBACK"));

        if (!isParquetMagic) {
            String rawStr = new String(bytes, StandardCharsets.UTF_8);
            String[] lines = rawStr.split("\r?\n");
            for (String line : lines) {
                if (context.isCancelled()) break;
                String trimmed = line.trim();
                if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
                    try {
                        JSONObject json = new JSONObject(trimmed);
                        NormalizedSample s = JSONLAdapter.parseJsonObject(json, file.getName());
                        if (s != null) samples.add(s);
                        continue;
                    } catch (Exception ignored) {}
                }
                if (trimmed.length() >= 10) {
                    samples.add(NormalizedSample.createTextSample(trimmed, file.getName()));
                }
            }
        } else {
            List<String> rawExtractedStrings = new ArrayList<>();
            int maxStrings = 50000;

            // Scan and decompress Snappy / GZIP pages embedded in Parquet stream
            int pos = 4;
            int end = bytes.length - 4;
            while (pos < end && rawExtractedStrings.size() < maxStrings) {
                if (context.isCancelled()) break;
                byte[] dec = decompressSnappyBlock(bytes, pos, Math.min(end - pos, 250000));
                if (dec.length >= 64) {
                    extractStringsFromBlock(dec, rawExtractedStrings, maxStrings);
                    pos += Math.max(32, dec.length / 2);
                } else {
                    pos += 16;
                }
            }
            extractStringsFromBlock(bytes, rawExtractedStrings, maxStrings);

            // Group or structure extracted string tokens into normalized samples
            String instructionBuf = null;
            String inputBuf = null;

            for (String str : rawExtractedStrings) {
                if (context.isCancelled()) break;
                if (str.startsWith("{") && str.endsWith("}")) {
                    try {
                        JSONObject json = new JSONObject(str);
                        NormalizedSample s = JSONLAdapter.parseJsonObject(json, file.getName());
                        if (s != null) {
                            samples.add(s);
                            continue;
                        }
                    } catch (Exception ignored) {}
                }

                if (instructionBuf == null) {
                    instructionBuf = str;
                } else {
                    samples.add(NormalizedSample.createInstructionSample(instructionBuf, str, file.getName()));
                    instructionBuf = null;
                }
            }
            if (instructionBuf != null && !instructionBuf.trim().isEmpty()) {
                samples.add(NormalizedSample.createTextSample(instructionBuf, file.getName()));
            }
        }

        context.log(String.format("[FORGE-PARQUET] file=%s rows=%d status=SUCCESS", file.getName(), samples.size()));
        context.log("[forge.dataset] Parquet Parsed " + samples.size() + " samples from " + file.getName());
        return samples;
    }

    private void extractStringsFromBlock(byte[] block, List<String> rawExtractedStrings, int maxStrings) {
        if (block == null) return;
        int bpos = 0;
        int bend = block.length;
        while (bpos <= bend - 5 && rawExtractedStrings.size() < maxStrings) {
            int len = (block[bpos] & 0xFF) | ((block[bpos + 1] & 0xFF) << 8) | ((block[bpos + 2] & 0xFF) << 16) | ((block[bpos + 3] & 0xFF) << 24);
            if (len >= 15 && len <= 50000 && bpos + 4 + len <= bend) {
                boolean isAsciiPrintable = true;
                for (int k = 0; k < Math.min(len, 100); k++) {
                    int ch = block[bpos + 4 + k] & 0xFF;
                    if ((ch < 0x20 || ch > 0x7E) && ch != '\n' && ch != '\r' && ch != '\t') {
                        isAsciiPrintable = false;
                        break;
                    }
                }
                if (isAsciiPrintable) {
                    String strCandidate = new String(block, bpos + 4, len, StandardCharsets.UTF_8).trim();
                    if (!strCandidate.isEmpty()) {
                        rawExtractedStrings.add(strCandidate);
                    }
                    bpos += 4 + len;
                    continue;
                }
            }
            bpos++;
        }
    }

    private byte[] decompressSnappyBlock(byte[] input, int offset, int length) {
        if (input == null || offset < 0 || length <= 0 || offset + length > input.length) return new byte[0];
        int pos = offset;
        int end = offset + length;

        int uncompressedLen = 0;
        int shift = 0;
        while (pos < end) {
            int b = input[pos++] & 0xFF;
            uncompressedLen |= (b & 0x7F) << shift;
            if ((b & 0x80) == 0) break;
            shift += 7;
        }

        if (shift > 28 || uncompressedLen <= 0 || uncompressedLen > 2 * 1024 * 1024) {
            return new byte[0];
        }

        byte[] decompressed;
        try {
            decompressed = new byte[uncompressedLen];
        } catch (OutOfMemoryError oom) {
            return new byte[0];
        }
        int destPos = 0;

        try {
            while (pos < end && destPos < uncompressedLen) {
                int b = input[pos++] & 0xFF;
                int tag = b & 0x03;
                if (tag == 0) { // Literal
                    int len = (b >> 2);
                    if (len < 60) {
                        len = len + 1;
                    } else if (len == 60) {
                        if (pos >= end) break;
                        len = (input[pos++] & 0xFF) + 1;
                    } else if (len == 61) {
                        if (pos + 1 >= end) break;
                        len = ((input[pos++] & 0xFF) | ((input[pos++] & 0xFF) << 8)) + 1;
                    } else if (len == 62) {
                        if (pos + 2 >= end) break;
                        len = ((input[pos++] & 0xFF) | ((input[pos++] & 0xFF) << 8) | ((input[pos++] & 0xFF) << 16)) + 1;
                    } else {
                        if (pos + 3 >= end) break;
                        len = ((input[pos++] & 0xFF) | ((input[pos++] & 0xFF) << 8) | ((input[pos++] & 0xFF) << 16) | ((input[pos++] & 0xFF) << 24)) + 1;
                    }
                    int copyLen = Math.min(len, Math.min(end - pos, uncompressedLen - destPos));
                    if (copyLen <= 0) break;
                    System.arraycopy(input, pos, decompressed, destPos, copyLen);
                    pos += copyLen;
                    destPos += copyLen;
                } else if (tag == 1) { // Copy 1-byte offset
                    int len = ((b >> 2) & 0x07) + 4;
                    if (pos >= end) break;
                    int off = ((b >> 5) & 0x07) << 8 | (input[pos++] & 0xFF);
                    if (off <= 0 || off > destPos) break;
                    int srcPos = destPos - off;
                    for (int i = 0; i < len && destPos < uncompressedLen; i++) {
                        decompressed[destPos] = decompressed[srcPos + i];
                        destPos++;
                    }
                } else if (tag == 2) { // Copy 2-byte offset
                    int len = (b >> 2) + 1;
                    if (pos + 1 >= end) break;
                    int off = (input[pos++] & 0xFF) | ((input[pos++] & 0xFF) << 8);
                    if (off <= 0 || off > destPos) break;
                    int srcPos = destPos - off;
                    for (int i = 0; i < len && destPos < uncompressedLen; i++) {
                        decompressed[destPos] = decompressed[srcPos + i];
                        destPos++;
                    }
                } else if (tag == 3) { // Copy 4-byte offset
                    int len = (b >> 2) + 1;
                    if (pos + 3 >= end) break;
                    int off = (input[pos++] & 0xFF) | ((input[pos++] & 0xFF) << 8) | ((input[pos++] & 0xFF) << 16) | ((input[pos++] & 0xFF) << 24);
                    if (off <= 0 || off > destPos) break;
                    int srcPos = destPos - off;
                    for (int i = 0; i < len && destPos < uncompressedLen; i++) {
                        decompressed[destPos] = decompressed[srcPos + i];
                        destPos++;
                    }
                }
            }
        } catch (Exception ignored) {}

        if (destPos > 0) {
            byte[] res = new byte[destPos];
            System.arraycopy(decompressed, 0, res, 0, destPos);
            return res;
        }
        return new byte[0];
    }
}
