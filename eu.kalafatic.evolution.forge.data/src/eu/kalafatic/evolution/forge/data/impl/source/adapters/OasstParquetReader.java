package eu.kalafatic.evolution.forge.data.impl.source.adapters;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * A self-contained, real Parquet reader for the HuggingFace OASST1 message dataset.
 *
 * <p>This is NOT heuristic byte scanning: it correctly implements the Parquet columnar
 * format for the flat OASST1 message schema, including:</p>
 * <ul>
 *   <li>Thrift-compact {@code FileMetaData} / {@code SchemaElement} / {@code RowGroup} /
 *       {@code ColumnChunk} / {@code ColumnMetaData} / {@code PageHeader} parsing.</li>
 *   <li>SNAPPY and GZIP page decompression, plus uncompressed pages.</li>
 *   <li>PLAIN encoding for BYTE_ARRAY, INT32 and BOOLEAN (bit-packed) values.</li>
 *   <li>PLAIN_DICTIONARY / RLE_DICTIONARY dictionary pages and RLE/bit-packed hybrid
 *       dictionary-index decoding.</li>
 *   <li>RLE/bit-packed hybrid definition-level decoding (repetition levels are absent
 *       because the top-level columns are flat).</li>
 * </ul>
 *
 * <p>The OASST1 dataset is a flat message list. Each row is one message with a
 * {@code message_id}, a {@code parent_id}, a {@code message_tree_id}, a {@code role}
 * ({@code "prompter"} or {@code "assistant"}), {@code text}, {@code lang}, {@code deleted},
 * {@code rank} and {@code tree_state}. Conversation trees are reconstructed from the
 * parent/child relationships by the caller ({@link OASST1Adapter}).</p>
 */
public final class OasstParquetReader {

    // Thrift compact protocol type codes.
    private static final int T_BOOL_TRUE = 1;
    private static final int T_BOOL_FALSE = 2;
    private static final int T_I8 = 3;
    private static final int T_I16 = 4;
    private static final int T_I32 = 5;
    private static final int T_I64 = 6;
    private static final int T_DOUBLE = 7;
    private static final int T_BINARY = 8;
    private static final int T_LIST = 9;
    private static final int T_SET = 10;
    private static final int T_MAP = 11;
    private static final int T_STRUCT = 12;

    // Parquet physical types.
    private static final int PHYS_BOOLEAN = 0;
    private static final int PHYS_INT32 = 1;
    private static final int PHYS_BYTE_ARRAY = 6;

    // Compression codecs.
    private static final int CODEC_UNCOMPRESSED = 0;
    private static final int CODEC_SNAPPY = 1;
    private static final int CODEC_GZIP = 2;

    // Parquet page / encoding identifiers.
    private static final int PAGE_DATA = 0;
    private static final int PAGE_DICTIONARY = 2;
    private static final int ENC_RLE_DICTIONARY = 8;

    // Column names we decode (top-level leaves only; nested groups are skipped).
    private static final List<String> WANTED = List.of(
            "message_id", "parent_id", "user_id", "text", "role", "lang",
            "deleted", "rank", "tree_state", "message_tree_id", "created_date");

    private OasstParquetReader() {}

    /**
     * Cheap check: does this file carry the OASST1 flat-message schema (message_id +
     * parent_id + text columns)? Only the footer metadata is read.
     */
    public static boolean isOasstParquet(File file) {
        try {
            Footer f = readFooter(file);
            if (f == null || f.schema == null) {
                return false;
            }
            boolean hasMessageId = false;
            boolean hasParentId = false;
            boolean hasText = false;
            for (Leaf leaf : f.schema) {
                if (leaf.isGroup()) {
                    continue;
                }
                if ("message_id".equals(leaf.name)) hasMessageId = true;
                if ("parent_id".equals(leaf.name)) hasParentId = true;
                if ("text".equals(leaf.name)) hasText = true;
            }
            return hasMessageId && hasParentId && hasText;
        } catch (Exception ex) {
            return false;
        }
    }

    /**
     * Streams one {@link JSONObject} per OASST1 message row to {@code consumer}. Emitted
     * keys are the decoded top-level columns (message_id, parent_id, user_id, text, role,
     * lang, deleted, rank, tree_state, message_tree_id, created_date).
     */
    public static void forEachMessage(File file, Consumer<JSONObject> consumer) throws IOException {
        Footer footer = readFooter(file);
        if (footer == null || footer.rowGroups == null) {
            throw new IOException("Not a valid Parquet file: " + file);
        }
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            for (RowGroupMeta rg : footer.rowGroups) {
                // Decode each wanted column once for the whole row group.
                List<String> names = new ArrayList<>();
                List<Object[]> columns = new ArrayList<>();
                for (ColumnMeta cm : rg.columns) {
                    Leaf leaf = findLeaf(footer.schema, cm.path);
                    if (leaf == null || leaf.isGroup()) {
                        continue;
                    }
                    if (!WANTED.contains(leaf.name)) {
                        continue;
                    }
                    names.add(leaf.name);
                    columns.add(decodeColumn(raf, cm, leaf, (int) rg.numRows));
                }
                for (int row = 0; row < rg.numRows; row++) {
                    JSONObject obj = new JSONObject();
                    boolean any = false;
                    for (int ci = 0; ci < names.size(); ci++) {
                        Object v = row < columns.get(ci).length ? columns.get(ci)[row] : null;
                        if (v != null) {
                            obj.put(names.get(ci), v);
                            any = true;
                        }
                    }
                    if (any) {
                        consumer.accept(obj);
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // Footer metadata
    // ------------------------------------------------------------------

    private static final class Leaf {
        String name;
        int type;
        int repetitionType;
        int numChildren;
        List<Leaf> children = new ArrayList<>();
        boolean isGroup() { return numChildren > 0; }
    }

    private static final class ColumnMeta {
        List<String> path = new ArrayList<>();
        int type;
        int codec;
        long numValues;
        long totalCompressedSize;
        long dataPageOffset;
        long dictionaryPageOffset = -1;
    }

    private static final class RowGroupMeta {
        long numRows;
        List<ColumnMeta> columns = new ArrayList<>();
    }

    private static final class Footer {
        List<Leaf> schema;
        List<RowGroupMeta> rowGroups;
    }

    private static Leaf findLeaf(List<Leaf> roots, List<String> path) {
        if (path == null || path.isEmpty()) {
            return null;
        }
        Leaf current = null;
        for (Leaf l : roots) {
            if (l.name.equals(path.get(0))) {
                current = l;
                break;
            }
        }
        if (current == null) return null;
        for (int i = 1; i < path.size() && current != null; i++) {
            Leaf next = null;
            for (Leaf child : current.children) {
                if (child.name.equals(path.get(i))) {
                    next = child;
                    break;
                }
            }
            current = next;
        }
        return current;
    }

    private static Footer readFooter(File file) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            long len = raf.length();
            if (len < 12) return null;
            raf.seek(0);
            byte[] magic = new byte[4];
            raf.readFully(magic);
            if (!"PAR1".equals(new String(magic, java.nio.charset.StandardCharsets.US_ASCII))) {
                return null;
            }
            raf.seek(len - 8);
            byte[] tail = new byte[8];
            raf.readFully(tail);
            long footerLen = readU32LE(tail, 0);
            if (footerLen <= 0 || footerLen > len) return null;
            raf.seek(len - 8 - footerLen);
            byte[] footerBytes = new byte[(int) footerLen];
            raf.readFully(footerBytes);

            Cur c = new Cur(footerBytes);
            Footer footer = new Footer();
            parseStruct(c, (fieldId, type) -> {
                switch (fieldId) {
                    case 1: c.readZigZag(); break; // version
                    case 2: footer.schema = parseSchema(c); break;
                    case 3: c.readZigZag(); break; // num_rows
                    case 4: footer.rowGroups = parseRowGroups(c); break;
                    default: c.skip(type);
                }
            });
            return footer;
        }
    }

    private static List<Leaf> parseSchema(Cur c) {
        int size = c.readListHeader();
        List<Leaf> flat = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            Leaf leaf = new Leaf();
            parseStruct(c, (fieldId, type) -> {
                switch (fieldId) {
                    case 1: leaf.type = (int) c.readZigZag(); break;
                    case 2: c.readZigZag(); break; // type_length
                    case 3: leaf.repetitionType = (int) c.readZigZag(); break;
                    case 4: leaf.name = c.readString(); break;
                    case 5: leaf.numChildren = (int) c.readZigZag(); break;
                    case 6: c.readZigZag(); break; // converted_type
                    case 9: c.readZigZag(); break; // field_id
                    case 10: c.skip(type); break; // logicalType struct
                    default: c.skip(type);
                }
            });
            flat.add(leaf);
        }
        int[] idx = {0};
        Leaf root = flat.get(idx[0]++);
        buildChildren(root, flat, idx);
        return root.children;
    }

    private static void buildChildren(Leaf parent, List<Leaf> flat, int[] idx) {
        for (int i = 0; i < parent.numChildren; i++) {
            Leaf child = flat.get(idx[0]++);
            parent.children.add(child);
            buildChildren(child, flat, idx);
        }
    }

    private static List<RowGroupMeta> parseRowGroups(Cur c) {
        int size = c.readListHeader();
        List<RowGroupMeta> groups = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            RowGroupMeta rg = new RowGroupMeta();
            parseStruct(c, (fieldId, type) -> {
                switch (fieldId) {
                    case 1: rg.columns = parseColumns(c); break;
                    case 2: c.readZigZag(); break; // total_byte_size
                    case 3: rg.numRows = c.readZigZag(); break;
                    case 4: c.skip(type); break; // sorting_columns
                    case 5: c.readZigZag(); break; // file_offset
                    case 6: c.readZigZag(); break; // total_compressed_size
                    case 7: c.readZigZag(); break; // ordinal (i16)
                    default: c.skip(type);
                }
            });
            groups.add(rg);
        }
        return groups;
    }

    private static List<ColumnMeta> parseColumns(Cur c) {
        int size = c.readListHeader();
        List<ColumnMeta> cols = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            ColumnMeta cm = new ColumnMeta();
            parseStruct(c, (fieldId, type) -> {
                switch (fieldId) {
                    case 1: c.readString(); break; // file_path
                    case 2: c.readZigZag(); break; // file_offset
                    case 3: parseColumnMetaData(c, cm); break;
                    default: c.skip(type);
                }
            });
            cols.add(cm);
        }
        return cols;
    }

    private static void parseColumnMetaData(Cur c, ColumnMeta cm) {
        parseStruct(c, (fieldId, type) -> {
            switch (fieldId) {
                case 1: cm.type = (int) c.readZigZag(); break;
                case 2: c.readI32List(); break; // encodings
                case 3: cm.path = c.readStringList(); break;
                case 4: cm.codec = (int) c.readZigZag(); break;
                case 5: cm.numValues = c.readZigZag(); break;
                case 6: c.readZigZag(); break; // total_uncompressed_size
                case 7: cm.totalCompressedSize = c.readZigZag(); break;
                case 8: c.skip(type); break; // key_value_metadata
                case 9: cm.dataPageOffset = c.readZigZag(); break;
                case 10: c.readZigZag(); break; // index_page_offset
                case 11: cm.dictionaryPageOffset = c.readZigZag(); break;
                case 12: c.skip(type); break; // statistics
                case 13: c.skip(type); break; // encoding_stats
                case 14: c.readZigZag(); break; // bloom_filter_offset
                default: c.skip(type);
            }
        });
    }

    // ------------------------------------------------------------------
    // Column decoding
    // ------------------------------------------------------------------

    private static final class PageData {
        final int type;
        final int numValues;
        final int encoding;
        final byte[] raw;
        final int headerLen;
        final int compressedSize;
        PageData(int type, int numValues, int encoding, byte[] raw, int headerLen, int compressedSize) {
            this.type = type;
            this.numValues = numValues;
            this.encoding = encoding;
            this.raw = raw;
            this.headerLen = headerLen;
            this.compressedSize = compressedSize;
        }
    }

    private static Object[] decodeColumn(RandomAccessFile raf, ColumnMeta cm, Leaf leaf, int numRows)
            throws IOException {
        switch (leaf.type) {
            case PHYS_BOOLEAN:
                return decodeBooleanColumn(raf, cm, numRows);
            case PHYS_INT32:
                return decodeIntColumn(raf, cm, numRows);
            case PHYS_BYTE_ARRAY:
                return decodeByteArrayColumn(raf, cm, numRows);
            default:
                return new Object[numRows];
        }
    }

    /** Reads a single page (header + compressed data) at the given offset. */
    private static PageData readPage(RandomAccessFile raf, long offset, int codec) throws IOException {
        raf.seek(offset);
        // Page headers are small, but their optional statistics can embed the column's
        // min/max values (full text for BYTE_ARRAY columns). Use a generous window.
        long remaining = raf.length() - offset;
        int window = (int) Math.min(1 << 20, remaining);
        byte[] buf = new byte[window];
        raf.readFully(buf);
        Cur c = new Cur(buf);
        final int[] pageType = {0};
        final long[] uncomp = {0};
        final long[] comp = {0};
        final int[] numValues = {0};
        final int[] encoding = {0};
        parseStruct(c, (fieldId, type) -> {
            switch (fieldId) {
                case 1: pageType[0] = (int) c.readZigZag(); break;
                case 2: uncomp[0] = c.readZigZag(); break;
                case 3: comp[0] = c.readZigZag(); break;
                case 4: c.readZigZag(); break; // crc
                case 5: parseDataPageHeader(c, numValues, encoding); break;
                case 7: parseDictionaryPageHeader(c, numValues, encoding); break;
                default: c.skip(type);
            }
        });
        int headerLen = c.pos;
        int compressedSize = (int) comp[0];
        raf.seek(offset + headerLen);
        byte[] compressed = new byte[compressedSize];
        raf.readFully(compressed);
        byte[] raw = decompress(compressed, (int) uncomp[0], codec);
        return new PageData(pageType[0], numValues[0], encoding[0], raw, headerLen, compressedSize);
    }

    private static void parseDataPageHeader(Cur c, int[] numValues, int[] encoding) {
        parseStruct(c, (fieldId, type) -> {
            switch (fieldId) {
                case 1: numValues[0] = (int) c.readZigZag(); break;
                case 2: encoding[0] = (int) c.readZigZag(); break;
                case 3: c.readZigZag(); break; // definition_level_encoding
                case 4: c.readZigZag(); break; // repetition_level_encoding
                case 5: c.skip(type); break; // statistics
                default: c.skip(type);
            }
        });
    }

    private static void parseDictionaryPageHeader(Cur c, int[] numValues, int[] encoding) {
        parseStruct(c, (fieldId, type) -> {
            switch (fieldId) {
                case 1: numValues[0] = (int) c.readZigZag(); break;
                case 2: encoding[0] = (int) c.readZigZag(); break;
                case 3: c.readBool(); break; // is_sorted
                default: c.skip(type);
            }
        });
    }

    /** Iterates the data pages of a column chunk in file order. */
    private static List<PageData> readDataPages(RandomAccessFile raf, ColumnMeta cm) throws IOException {
        List<PageData> pages = new ArrayList<>();
        long offset = cm.dataPageOffset;
        // The dictionary page is stored at dictionaryPageOffset; its compressed size must be
        // excluded from the data-page range covered by totalCompressedSize.
        long dictCompressed = 0;
        if (cm.dictionaryPageOffset >= 0) {
            PageData d = readPage(raf, cm.dictionaryPageOffset, cm.codec);
            dictCompressed = d.headerLen + d.compressedSize;
        }
        long end = cm.dataPageOffset + cm.totalCompressedSize - dictCompressed;
        while (offset < end) {
            PageData p = readPage(raf, offset, cm.codec);
            pages.add(p);
            offset += p.headerLen + p.compressedSize;
        }
        return pages;
    }

    private static String[] readStringDictionary(RandomAccessFile raf, ColumnMeta cm) throws IOException {
        if (cm.dictionaryPageOffset < 0) return null;
        PageData d = readPage(raf, cm.dictionaryPageOffset, cm.codec);
        return decodePlainStrings(d.raw, d.numValues);
    }

    private static int[] readIntDictionary(RandomAccessFile raf, ColumnMeta cm) throws IOException {
        if (cm.dictionaryPageOffset < 0) return null;
        PageData d = readPage(raf, cm.dictionaryPageOffset, cm.codec);
        return decodePlainInts(d.raw, d.numValues);
    }

    private static Object[] decodeByteArrayColumn(RandomAccessFile raf, ColumnMeta cm, int numRows)
            throws IOException {
        String[] dictionary = readStringDictionary(raf, cm);
        List<PageData> pages = readDataPages(raf, cm);
        Object[] result = new Object[numRows];
        int row = 0;
        for (PageData page : pages) {
            if (page.type != PAGE_DATA) continue;
            int[] defLevels = decodeDefinitionLevels(page.raw, numRows - row);
            int valuesOffset = valuesOffset(page.raw);
            int nonNull = countDefined(defLevels);
            if (page.encoding == ENC_RLE_DICTIONARY && dictionary != null) {
                int bitWidth = page.raw[valuesOffset] & 0xFF;
                int[] indices = decodeRleValues(page.raw, valuesOffset + 1, page.raw.length - valuesOffset - 1, nonNull, bitWidth);
                int idx = 0;
                for (int i = 0; i < defLevels.length && row < numRows; i++, row++) {
                    if (defLevels[i] == 1) {
                        int di = indices[idx];
                        result[row] = di >= 0 && di < dictionary.length ? dictionary[di] : null;
                        idx++;
                    }
                }
            } else {
                int p = valuesOffset;
                for (int i = 0; i < defLevels.length && row < numRows; i++, row++) {
                    if (defLevels[i] == 1) {
                        int l = readI32LE(page.raw, p);
                        result[row] = new String(page.raw, p + 4, l, java.nio.charset.StandardCharsets.UTF_8);
                        p += 4 + l;
                    }
                }
            }
        }
        return result;
    }

    private static Object[] decodeIntColumn(RandomAccessFile raf, ColumnMeta cm, int numRows)
            throws IOException {
        int[] dictionary = readIntDictionary(raf, cm);
        List<PageData> pages = readDataPages(raf, cm);
        Object[] result = new Object[numRows];
        int row = 0;
        for (PageData page : pages) {
            if (page.type != PAGE_DATA) continue;
            int[] defLevels = decodeDefinitionLevels(page.raw, numRows - row);
            int valuesOffset = valuesOffset(page.raw);
            int nonNull = countDefined(defLevels);
            if (page.encoding == ENC_RLE_DICTIONARY && dictionary != null) {
                int bitWidth = page.raw[valuesOffset] & 0xFF;
                int[] indices = decodeRleValues(page.raw, valuesOffset + 1, page.raw.length - valuesOffset - 1, nonNull, bitWidth);
                int idx = 0;
                for (int i = 0; i < defLevels.length && row < numRows; i++, row++) {
                    if (defLevels[i] == 1) {
                        int di = indices[idx];
                        result[row] = di >= 0 && di < dictionary.length ? dictionary[di] : null;
                        idx++;
                    }
                }
            } else {
                int p = valuesOffset;
                for (int i = 0; i < defLevels.length && row < numRows; i++, row++) {
                    if (defLevels[i] == 1) {
                        result[row] = readI32LE(page.raw, p);
                        p += 4;
                    }
                }
            }
        }
        return result;
    }

    private static Object[] decodeBooleanColumn(RandomAccessFile raf, ColumnMeta cm, int numRows)
            throws IOException {
        List<PageData> pages = readDataPages(raf, cm);
        Object[] result = new Object[numRows];
        int row = 0;
        for (PageData page : pages) {
            if (page.type != PAGE_DATA) continue;
            int[] defLevels = decodeDefinitionLevels(page.raw, numRows - row);
            int valuesOffset = valuesOffset(page.raw);
            int nonNull = countDefined(defLevels);
            int idx = 0;
            for (int i = 0; i < defLevels.length && row < numRows; i++, row++) {
                if (defLevels[i] == 1) {
                    int byteIdx = valuesOffset + idx / 8;
                    int bitIdx = idx % 8;
                    result[row] = ((page.raw[byteIdx] >> bitIdx) & 1) == 1;
                    idx++;
                }
            }
        }
        return result;
    }

    // ------------------------------------------------------------------
    // Encodings
    // ------------------------------------------------------------------

    private static String[] decodePlainStrings(byte[] raw, int count) {
        String[] out = new String[count];
        int p = 0;
        for (int i = 0; i < count; i++) {
            int len = readI32LE(raw, p);
            out[i] = new String(raw, p + 4, len, java.nio.charset.StandardCharsets.UTF_8);
            p += 4 + len;
        }
        return out;
    }

    private static int[] decodePlainInts(byte[] raw, int count) {
        int[] out = new int[count];
        int p = 0;
        for (int i = 0; i < count; i++) {
            out[i] = readI32LE(raw, p);
            p += 4;
        }
        return out;
    }

    private static int countDefined(int[] defLevels) {
        int n = 0;
        for (int d : defLevels) if (d == 1) n++;
        return n;
    }

    /** Offset of the encoded values, just after the 4-byte length prefix + definition levels. */
    private static int valuesOffset(byte[] raw) {
        return 4 + readI32LE(raw, 0);
    }

    /** Decodes all definition levels in a page (4-byte length prefix + RLE data, bit width 1). */
    private static int[] decodeDefinitionLevels(byte[] raw, int maxCount) {
        int len = readI32LE(raw, 0);
        return decodeRleAll(raw, 4, len, maxCount, 1);
    }

    /** Decodes every RLE/bit-packed hybrid value within [offset, offset+length). */
    private static int[] decodeRleAll(byte[] data, int offset, int length, int maxCount, int bitWidth) {
        List<Integer> out = new ArrayList<>(Math.min(maxCount, 1024));
        int pos = offset;
        int end = offset + length;
        while (pos < end && out.size() < maxCount) {
            int[] posRef = {pos};
            long header = readVarInt(data, posRef);
            pos = posRef[0];
            if ((header & 1) == 1) {
                int runCount = (int) ((header >>> 1) * 8);
                int nbytes = runCount * bitWidth / 8;
                for (int k = 0; k < runCount && out.size() < maxCount; k++) {
                    int bitpos = k * bitWidth;
                    int v = 0;
                    for (int b = 0; b < bitWidth; b++) {
                        int byteIdx = (bitpos + b) >>> 3;
                        int bitIdx = (bitpos + b) & 7;
                        v |= ((data[pos + byteIdx] >>> bitIdx) & 1) << b;
                    }
                    out.add(v);
                }
                pos += nbytes;
            } else {
                int runCount = (int) (header >>> 1);
                int nb = (bitWidth + 7) / 8;
                int v = 0;
                for (int k = 0; k < nb; k++) {
                    v |= (data[pos + k] & 0xFF) << (8 * k);
                }
                pos += nb;
                for (int k = 0; k < runCount && out.size() < maxCount; k++) {
                    out.add(v);
                }
            }
        }
        int[] r = new int[out.size()];
        for (int i = 0; i < r.length; i++) r[i] = out.get(i);
        return r;
    }

    /** Decodes exactly {@code count} RLE/bit-packed hybrid values (dictionary indices). */
    private static int[] decodeRleValues(byte[] data, int offset, int length, int count, int bitWidth) {
        int[] out = new int[count];
        int pos = offset;
        int end = offset + length;
        int outPos = 0;
        while (outPos < count && pos < end) {
            int[] posRef = {pos};
            long header = readVarInt(data, posRef);
            pos = posRef[0];
            if ((header & 1) == 1) {
                int runCount = (int) ((header >>> 1) * 8);
                int nbytes = runCount * bitWidth / 8;
                for (int k = 0; k < runCount && outPos < count; k++) {
                    int bitpos = k * bitWidth;
                    int v = 0;
                    for (int b = 0; b < bitWidth; b++) {
                        int byteIdx = (bitpos + b) >>> 3;
                        int bitIdx = (bitpos + b) & 7;
                        v |= ((data[pos + byteIdx] >>> bitIdx) & 1) << b;
                    }
                    out[outPos++] = v;
                }
                pos += nbytes;
            } else {
                int runCount = (int) (header >>> 1);
                int nb = (bitWidth + 7) / 8;
                int v = 0;
                for (int k = 0; k < nb; k++) {
                    v |= (data[pos + k] & 0xFF) << (8 * k);
                }
                pos += nb;
                for (int k = 0; k < runCount && outPos < count; k++) {
                    out[outPos++] = v;
                }
            }
        }
        return out;
    }

    // ------------------------------------------------------------------
    // Compression
    // ------------------------------------------------------------------

    private static byte[] decompress(byte[] in, int expectedLen, int codec) throws IOException {
        if (codec == CODEC_UNCOMPRESSED) {
            return in;
        }
        if (codec == CODEC_GZIP) {
            try (java.io.ByteArrayInputStream bis = new java.io.ByteArrayInputStream(in);
                 java.util.zip.GZIPInputStream gz = new java.util.zip.GZIPInputStream(bis);
                 java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream(expectedLen > 0 ? expectedLen : 1024)) {
                byte[] buf = new byte[8192];
                int n;
                while ((n = gz.read(buf)) != -1) {
                    bos.write(buf, 0, n);
                }
                return bos.toByteArray();
            }
        }
        if (codec == CODEC_SNAPPY) {
            return snappyDecompress(in, expectedLen);
        }
        throw new IOException("Unsupported Parquet compression codec: " + codec);
    }

    /** Raw Snappy block decompressor (Parquet SNAPPY codec). */
    private static byte[] snappyDecompress(byte[] in, int expectedLen) throws IOException {
        int[] pos = {0};
        long uncompressedLen = readVarInt(in, pos);
        int p = pos[0];
        byte[] out = new byte[(int) uncompressedLen];
        int outPos = 0;
        while (p < in.length) {
            int tag = in[p++] & 0xFF;
            int type = tag & 0x03;
            if (type == 0) {
                int len = tag >>> 2;
                if (len < 60) {
                    len += 1;
                } else if (len == 60) {
                    len = (in[p++] & 0xFF) + 1;
                } else if (len == 61) {
                    len = ((in[p] & 0xFF) | ((in[p + 1] & 0xFF) << 8)) + 1;
                    p += 2;
                } else if (len == 62) {
                    len = ((in[p] & 0xFF) | ((in[p + 1] & 0xFF) << 8) | ((in[p + 2] & 0xFF) << 16)) + 1;
                    p += 3;
                } else {
                    len = ((in[p] & 0xFF) | ((in[p + 1] & 0xFF) << 8) | ((in[p + 2] & 0xFF) << 16) | ((in[p + 3] & 0xFF) << 24)) + 1;
                    p += 4;
                }
                System.arraycopy(in, p, out, outPos, len);
                p += len;
                outPos += len;
            } else {
                int len;
                int off;
                if (type == 1) {
                    len = 4 + ((tag >>> 2) & 0x07);
                    off = ((tag >>> 5) << 8) | (in[p++] & 0xFF);
                } else if (type == 2) {
                    len = 1 + (tag >>> 2);
                    off = (in[p] & 0xFF) | ((in[p + 1] & 0xFF) << 8);
                    p += 2;
                } else {
                    len = 1 + (tag >>> 2);
                    off = (in[p] & 0xFF) | ((in[p + 1] & 0xFF) << 8)
                            | ((in[p + 2] & 0xFF) << 16) | ((in[p + 3] & 0xFF) << 24);
                    p += 4;
                }
                for (int k = 0; k < len; k++) {
                    out[outPos] = out[outPos - off];
                    outPos++;
                }
            }
        }
        if (expectedLen > 0 && outPos != expectedLen) {
            throw new IOException("Snappy decompressed size mismatch: expected " + expectedLen + " got " + outPos);
        }
        return out;
    }

    // ------------------------------------------------------------------
    // Low-level byte / varint helpers
    // ------------------------------------------------------------------

    private static int readI32LE(byte[] b, int off) {
        return (b[off] & 0xFF) | ((b[off + 1] & 0xFF) << 8) | ((b[off + 2] & 0xFF) << 16) | ((b[off + 3] & 0xFF) << 24);
    }

    private static long readU32LE(byte[] b, int off) {
        return (b[off] & 0xFFL) | ((b[off + 1] & 0xFFL) << 8) | ((b[off + 2] & 0xFFL) << 16) | ((b[off + 3] & 0xFFL) << 24);
    }

    private static long readVarInt(byte[] b, int[] pos) {
        long r = 0;
        int s = 0;
        while (true) {
            int x = b[pos[0]++] & 0xFF;
            r |= (long) (x & 0x7F) << s;
            if ((x & 0x80) == 0) return r;
            s += 7;
        }
    }

    // ------------------------------------------------------------------
    // Thrift compact protocol cursor
    // ------------------------------------------------------------------

    private static final class Cur {
        final byte[] buf;
        int pos;
        Cur(byte[] buf) { this.buf = buf; }

        long readZigZag() {
            long v = readVarInt();
            return (v >>> 1) ^ -(v & 1);
        }

        long readVarInt() {
            long r = 0;
            int s = 0;
            while (true) {
                int x = buf[pos++] & 0xFF;
                r |= (long) (x & 0x7F) << s;
                if ((x & 0x80) == 0) return r;
                s += 7;
            }
        }

        String readString() {
            int n = (int) readVarInt();
            String s = new String(buf, pos, n, java.nio.charset.StandardCharsets.UTF_8);
            pos += n;
            return s;
        }

        void readBool() {
            // boolean value is encoded in the field-header type code; nothing to consume.
        }

        List<String> readStringList() {
            int n = readListHeader();
            List<String> out = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                out.add(readString());
            }
            return out;
        }

        void readI32List() {
            int n = readListHeader();
            for (int i = 0; i < n; i++) {
                readZigZag();
            }
        }

        int readListHeader() {
            int b = buf[pos++] & 0xFF;
            int size = b >>> 4;
            if (size == 15) {
                size = (int) readVarInt();
            }
            return size;
        }

        void skip(int type) {
            switch (type) {
                case T_BOOL_TRUE:
                case T_BOOL_FALSE:
                    return;
                case T_I8:
                    pos += 1;
                    return;
                case T_I16:
                case T_I32:
                case T_I64:
                    readVarInt();
                    return;
                case T_DOUBLE:
                    pos += 8;
                    return;
                case T_BINARY: {
                    int n = (int) readVarInt();
                    pos += n;
                    return;
                }
                case T_LIST:
                case T_SET: {
                    int b = buf[pos++] & 0xFF;
                    int elemType = b & 0x0F;
                    int size = b >>> 4;
                    if (size == 15) {
                        size = (int) readVarInt();
                    }
                    for (int i = 0; i < size; i++) {
                        skip(elemType);
                    }
                    return;
                }
                case T_MAP: {
                    int b = buf[pos++] & 0xFF;
                    if (b == 0) {
                        return; // empty map
                    }
                    int keyType = b >>> 4;
                    int valType = b & 0x0F;
                    int size = (int) readVarInt();
                    for (int i = 0; i < size; i++) {
                        skip(keyType);
                        skip(valType);
                    }
                    return;
                }
                case T_STRUCT: {
                    int last = 0;
                    while (true) {
                        int fb = buf[pos++] & 0xFF;
                        if (fb == 0) return;
                        int delta = fb >>> 4;
                        int t = fb & 0x0F;
                        last += delta;
                        skip(t);
                    }
                }
                default:
                    return;
            }
        }
    }

    private interface FieldHandler {
        void handle(int fieldId, int type);
    }

    private static void parseStruct(Cur c, FieldHandler handler) {
        int last = 0;
        while (true) {
            int fb = c.buf[c.pos++] & 0xFF;
            if (fb == 0) {
                return;
            }
            int delta = fb >>> 4;
            int type = fb & 0x0F;
            last += delta;
            handler.handle(last, type);
        }
    }
}
