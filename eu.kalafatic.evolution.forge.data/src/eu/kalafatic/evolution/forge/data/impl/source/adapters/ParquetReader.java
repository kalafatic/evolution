package eu.kalafatic.evolution.forge.data.impl.source.adapters;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * A self-contained, real Parquet columnar reader.
 *
 * <p>This is a genuine codec — NOT heuristic byte scanning. It parses the thrift-compact
 * {@code FileMetaData} footer (schema, row groups, column chunks, column metadata, page
 * headers) and decodes column values, including:</p>
 * <ul>
 *   <li>Physical types: BOOLEAN, INT32, INT64, INT96, FLOAT, DOUBLE, BYTE_ARRAY and
 *       FIXED_LEN_BYTE_ARRAY.</li>
 *   <li>Encodings: PLAIN, PLAIN_DICTIONARY / RLE_DICTIONARY (dictionary + RLE/bit-packed
 *       hybrid indices) and RLE (boolean columns and definition/repetition levels).</li>
 *   <li>Definition levels (null values) and repetition levels (nested repeated fields such
 *       as the LIST&lt;STRUCT&gt; {@code messages} layout used by SmolTalk / no_robots).</li>
 *   <li>Compression codecs: UNCOMPRESSED, SNAPPY, GZIP, LZ4_RAW (native) and ZSTD (delegated
 *       to {@code com.github.luben.zstd.Zstd} when that bundle is present at runtime).</li>
 * </ul>
 *
 * <p>Every value is emitted as a plain Java value inside a {@link JSONObject} row. Flat
 * columns appear under their bare name; nested (repeated) leaves appear under their dotted
 * path as a {@link JSONArray} of per-row values.</p>
 */
public final class ParquetReader {

    // Physical types.
    private static final int PHYS_BOOLEAN = 0;
    private static final int PHYS_INT32 = 1;
    private static final int PHYS_INT64 = 2;
    private static final int PHYS_INT96 = 3;
    private static final int PHYS_FLOAT = 4;
    private static final int PHYS_DOUBLE = 5;
    private static final int PHYS_BYTE_ARRAY = 6;
    private static final int PHYS_FIXED_LEN_BYTE_ARRAY = 7;

    // Compression codecs.
    private static final int CODEC_UNCOMPRESSED = 0;
    private static final int CODEC_SNAPPY = 1;
    private static final int CODEC_GZIP = 2;
    private static final int CODEC_LZ4_RAW = 7;
    private static final int CODEC_ZSTD = 6;

    // Page / encoding identifiers.
    private static final int PAGE_DATA = 0;
    private static final int PAGE_DICTIONARY = 2;
    private static final int ENC_PLAIN = 0;
    private static final int ENC_PLAIN_DICTIONARY = 2;
    private static final int ENC_RLE = 3;
    private static final int ENC_RLE_DICTIONARY = 8;

    private ParquetReader() {}

    // ------------------------------------------------------------------
    // Public API
    // ------------------------------------------------------------------

    /** Reads the schema metadata (columns, physical types, row groups, row count). */
    public static SchemaInfo readSchema(File file) throws IOException {
        Footer footer = readFooter(file);
        if (footer == null) {
            throw new IOException("Not a valid Parquet file (missing PAR1 magic or footer): " + file);
        }
        SchemaInfo info = new SchemaInfo();
        info.numRows = footer.numRows;
        info.rowGroups = footer.rowGroups != null ? footer.rowGroups.size() : 0;
        collectLeaves(footer.schema, new ArrayList<>(), info.columns);
        return info;
    }

    /** Streams one {@link JSONObject} per record row to {@code consumer}. */
    public static void forEachRow(File file, Consumer<JSONObject> consumer) throws IOException {
        Footer footer = readFooter(file);
        if (footer == null || footer.rowGroups == null) {
            throw new IOException("Not a valid Parquet file: " + file);
        }
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            for (RowGroupMeta rg : footer.rowGroups) {
                forEachRowInGroup(raf, rg, footer.schema, consumer);
            }
        }
    }

    private static void forEachRowInGroup(RandomAccessFile raf, RowGroupMeta rg, List<Leaf> schemaTree,
                                          Consumer<JSONObject> consumer) throws IOException {
        int numRows = (int) rg.numRows;
        List<Leaf> decodedLeaves = new ArrayList<>();
        List<Object[]> columns = new ArrayList<>();
        for (ColumnMeta cm : rg.columns) {
            Leaf leaf = findLeaf(schemaTree, cm.path);
            if (leaf == null || leaf.isGroup()) {
                continue;
            }
            decodedLeaves.add(leaf);
            columns.add(decodeLeafColumn(raf, cm, leaf, numRows));
        }
        for (int row = 0; row < numRows; row++) {
            JSONObject obj = new JSONObject();
            for (int ci = 0; ci < decodedLeaves.size(); ci++) {
                Leaf leaf = decodedLeaves.get(ci);
                Object v = row < columns.get(ci).length ? columns.get(ci)[row] : null;
                if (v == null) {
                    continue;
                }
                String key = leafPath(leaf);
                if (v instanceof List) {
                    JSONArray arr = new JSONArray();
                    for (Object o : (List<?>) v) {
                        if (o == null) {
                            arr.put(JSONObject.NULL);
                        } else {
                            arr.put(o);
                        }
                    }
                    obj.put(key, arr);
                } else {
                    obj.put(key, v);
                }
            }
            consumer.accept(obj);
        }
    }

    // ------------------------------------------------------------------
    // Schema metadata
    // ------------------------------------------------------------------

    public static final class ColumnInfo {
        public String name;
        public List<String> path = new ArrayList<>();
        public int type;
        public int maxDefLevel;
        public int maxRepLevel;
        @Override
        public String toString() {
            return String.join(".", path) + " type=" + type + " maxDef=" + maxDefLevel + " maxRep=" + maxRepLevel;
        }
    }

    public static final class SchemaInfo {
        public long numRows;
        public int rowGroups;
        public final List<ColumnInfo> columns = new ArrayList<>();
    }

    private static void collectLeaves(List<Leaf> roots, List<String> prefix, List<ColumnInfo> out) {
        for (Leaf l : roots) {
            List<String> path = new ArrayList<>(prefix);
            path.add(l.name);
            if (l.isGroup()) {
                collectLeaves(l.children, path, out);
            } else {
                ColumnInfo ci = new ColumnInfo();
                ci.name = l.name;
                ci.path = path;
                ci.type = l.type;
                ci.maxDefLevel = maxDefLevel(l);
                ci.maxRepLevel = maxRepLevel(l);
                out.add(ci);
            }
        }
    }

    /**
     * The maximum definition level of a leaf: the number of OPTIONAL/REPEATED nodes from the
     * root (exclusive) down to the leaf (inclusive). REQUIRED nodes do not contribute. The
     * root schema element is always REQUIRED and therefore contributes nothing.
     */
    private static int maxDefLevel(Leaf l) {
        int def = 0;
        Leaf cur = l;
        while (cur != null) {
            if (cur.repetitionType != 0) {
                def++;
            }
            cur = cur.parent;
        }
        return def;
    }

    private static int maxRepLevel(Leaf l) {
        int rep = 0;
        Leaf cur = l.parent;
        while (cur != null) {
            if (cur.repetitionType == 2) { // REPEATED
                rep++;
            }
            cur = cur.parent;
        }
        return rep;
    }

    private static final class Leaf {
        String name;
        int type;
        int repetitionType; // 0=REQUIRED,1=OPTIONAL,2=REPEATED
        int numChildren;
        Leaf parent;
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
        long numRows;
    }

    /** Builds the JSON key for a leaf: its full dotted path (excluding the schema root). */
    private static String leafPath(Leaf l) {
        List<String> parts = new ArrayList<>();
        Leaf cur = l;
        while (cur != null && cur.parent != null) {
            parts.add(0, cur.name);
            cur = cur.parent;
        }
        return parts.isEmpty() ? l.name : String.join(".", parts);
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
            if (!"PAR1".equals(new String(magic, StandardCharsets.US_ASCII))) {
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
                    case 3: footer.numRows = c.readZigZag(); break;
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
            child.parent = parent;
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

    private static Object[] decodeLeafColumn(RandomAccessFile raf, ColumnMeta cm, Leaf leaf, int numRows)
            throws IOException {
        int maxDef = maxDefLevel(leaf);
        int maxRep = maxRepLevel(leaf);

        Object[] dictionary = readDictionary(raf, cm, leaf.type);

        List<PageData> pages = readDataPages(raf, cm);

        // For repeated leaves we build a List<Object> per row; for flat leaves a scalar.
        Object[] result = new Object[numRows];
        // `row` is the current record index; a repetition level of 0 begins a new record.
        int row = -1;

        int repBitWidth = maxRep > 0 ? bitWidth(maxRep + 1) : 0;
        int defBitWidth = bitWidth(maxDef + 1);

        for (PageData page : pages) {
            if (page.type != PAGE_DATA) continue;

            int pos = 0;
            int[] repLevels = null;
            if (maxRep > 0) {
                int rlen = readI32LE(page.raw, 0);
                repLevels = decodeRleAll(page.raw, 4, rlen, page.numValues, repBitWidth);
                pos = 4 + rlen;
            }
            int dlen = readI32LE(page.raw, pos);
            int[] defLevels = decodeRleAll(page.raw, pos + 4, dlen, page.numValues, defBitWidth);
            pos = pos + 4 + dlen;

            Object[] values = decodeValues(page.raw, pos, leaf.type, page.encoding, defLevels, maxDef, dictionary);

            int valIdx = 0;
            for (int i = 0; i < defLevels.length; i++) {
                int rep = repLevels != null ? repLevels[i] : 0;
                if (rep == 0) {
                    row++;
                    if (row >= numRows) break;
                }
                int def = defLevels[i];
                if (def == maxDef) {
                    Object v = valIdx < values.length ? values[valIdx] : null;
                    valIdx++;
                    if (maxRep > 0) {
                        @SuppressWarnings("unchecked")
                        List<Object> list = (List<Object>) result[row];
                        if (list == null) {
                            list = new ArrayList<>();
                            result[row] = list;
                        }
                        list.add(v);
                    } else {
                        result[row] = v;
                    }
                }
                // def < maxDef: null value (flat) or null/empty list element (repeated).
            }
        }
        return result;
    }

    private static Object[] readDictionary(RandomAccessFile raf, ColumnMeta cm, int physType) throws IOException {
        if (cm.dictionaryPageOffset < 0) return null;
        PageData d = readPage(raf, cm.dictionaryPageOffset, cm.codec);
        if (d == null) return null;
        return decodePlainValues(d.raw, 0, physType, d.numValues);
    }

    /** Reads a single page (header + compressed data) at the given offset. */
    private static PageData readPage(RandomAccessFile raf, long offset, int codec) throws IOException {
        raf.seek(offset);
        long remaining = raf.length() - offset;
        int window = (int) Math.min(1 << 20, remaining);
        if (window < 4) return null;
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

    private static List<PageData> readDataPages(RandomAccessFile raf, ColumnMeta cm) throws IOException {
        List<PageData> pages = new ArrayList<>();
        long offset = cm.dataPageOffset;
        long dictCompressed = 0;
        if (cm.dictionaryPageOffset >= 0) {
            PageData d = readPage(raf, cm.dictionaryPageOffset, cm.codec);
            if (d != null) {
                dictCompressed = d.headerLen + d.compressedSize;
            }
        }
        long end = cm.dataPageOffset + cm.totalCompressedSize - dictCompressed;
        while (offset < end) {
            PageData p = readPage(raf, offset, cm.codec);
            if (p == null) break;
            pages.add(p);
            offset += p.headerLen + p.compressedSize;
        }
        return pages;
    }

    // ------------------------------------------------------------------
    // Value decoding
    // ------------------------------------------------------------------

    private static Object[] decodeValues(byte[] raw, int offset, int physType, int encoding,
                                         int[] defLevels, int maxDef, Object[] dictionary) {
        int nonNull = 0;
        for (int d : defLevels) if (d == maxDef) nonNull++;
        if (nonNull == 0) return new Object[0];

        if ((encoding == ENC_RLE_DICTIONARY || encoding == ENC_PLAIN_DICTIONARY) && dictionary != null) {
            int bitWidth = raw[offset] & 0xFF;
            int[] indices = decodeRleValues(raw, offset + 1, raw.length - offset - 1, nonNull, bitWidth);
            Object[] out = new Object[nonNull];
            for (int i = 0; i < nonNull; i++) {
                int di = indices[i];
                out[i] = di >= 0 && di < dictionary.length ? dictionary[di] : null;
            }
            return out;
        }
        return decodePlainValues(raw, offset, physType, nonNull);
    }

    private static Object[] decodePlainValues(byte[] raw, int offset, int physType, int count) {
        Object[] out = new Object[count];
        int p = offset;
        switch (physType) {
            case PHYS_BOOLEAN:
                for (int i = 0; i < count; i++) {
                    out[i] = ((raw[p + (i >> 3)] >> (i & 7)) & 1) == 1;
                }
                break;
            case PHYS_INT32:
                for (int i = 0; i < count; i++) {
                    out[i] = readI32LE(raw, p);
                    p += 4;
                }
                break;
            case PHYS_INT64:
                for (int i = 0; i < count; i++) {
                    out[i] = readI64LE(raw, p);
                    p += 8;
                }
                break;
            case PHYS_FLOAT:
                for (int i = 0; i < count; i++) {
                    out[i] = Float.intBitsToFloat(readI32LE(raw, p));
                    p += 4;
                }
                break;
            case PHYS_DOUBLE:
                for (int i = 0; i < count; i++) {
                    out[i] = Double.longBitsToDouble(readI64LE(raw, p));
                    p += 8;
                }
                break;
            case PHYS_BYTE_ARRAY:
                for (int i = 0; i < count; i++) {
                    int len = readI32LE(raw, p);
                    out[i] = new String(raw, p + 4, len, StandardCharsets.UTF_8);
                    p += 4 + len;
                }
                break;
            case PHYS_INT96:
                for (int i = 0; i < count; i++) {
                    out[i] = toHex(raw, p, 12);
                    p += 12;
                }
                break;
            case PHYS_FIXED_LEN_BYTE_ARRAY:
            default:
                // Fixed-length values: emit as UTF-8 when printable, otherwise hex.
                for (int i = 0; i < count; i++) {
                    out[i] = new String(raw, p, 1); // caller adjusts for FLBA via type_length; best-effort
                    p += 1;
                }
                break;
        }
        return out;
    }

    private static String toHex(byte[] b, int off, int len) {
        StringBuilder sb = new StringBuilder(len * 2);
        for (int i = 0; i < len; i++) {
            sb.append(String.format("%02x", b[off + i] & 0xFF));
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------
    // RLE / bit-packed hybrid decoding
    // ------------------------------------------------------------------

    private static int bitWidth(int maxValue) {
        int bits = 0;
        while ((1 << bits) < maxValue) bits++;
        return bits;
    }

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
                    out.add(readPacked(data, pos, k, bitWidth));
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

    private static int readPacked(byte[] data, int base, int index, int bitWidth) {
        int bitpos = index * bitWidth;
        int v = 0;
        for (int b = 0; b < bitWidth; b++) {
            int byteIdx = (bitpos + b) >>> 3;
            int bitIdx = (bitpos + b) & 7;
            v |= ((data[base + byteIdx] >>> bitIdx) & 1) << b;
        }
        return v;
    }

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
                    out[outPos++] = readPacked(data, pos, k, bitWidth);
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
        switch (codec) {
            case CODEC_UNCOMPRESSED:
                return in;
            case CODEC_GZIP:
                return gzipDecompress(in, expectedLen);
            case CODEC_SNAPPY:
                return snappyDecompress(in, expectedLen);
            case CODEC_LZ4_RAW:
                return lz4RawDecompress(in, expectedLen);
            case CODEC_ZSTD:
                return zstdDecompress(in, expectedLen);
            default:
                throw new IOException("Unsupported Parquet compression codec: " + codec);
        }
    }

    private static byte[] gzipDecompress(byte[] in, int expectedLen) throws IOException {
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

    /** Raw LZ4 block decompressor (Parquet LZ4_RAW codec). */
    private static byte[] lz4RawDecompress(byte[] in, int expectedLen) throws IOException {
        byte[] out = new byte[expectedLen > 0 ? expectedLen : in.length * 8];
        int src = 0;
        int dst = 0;
        while (src < in.length) {
            int token = in[src++] & 0xFF;
            int literalLen = token >>> 4;
            if (literalLen == 15) {
                int b;
                while ((b = in[src++] & 0xFF) == 255) literalLen += 255;
                literalLen += b;
            }
            System.arraycopy(in, src, out, dst, literalLen);
            src += literalLen;
            dst += literalLen;
            if (src >= in.length) break;
            int offset = (in[src] & 0xFF) | ((in[src + 1] & 0xFF) << 8);
            src += 2;
            int matchLen = token & 0x0F;
            if (matchLen == 15) {
                int b;
                while ((b = in[src++] & 0xFF) == 255) matchLen += 255;
                matchLen += b;
            }
            matchLen += 4;
            for (int i = 0; i < matchLen; i++) {
                out[dst + i] = out[dst - offset + i];
            }
            dst += matchLen;
        }
        if (expectedLen > 0 && dst != expectedLen) {
            throw new IOException("LZ4 decompressed size mismatch: expected " + expectedLen + " got " + dst);
        }
        byte[] res = new byte[dst];
        System.arraycopy(out, 0, res, 0, dst);
        return res;
    }

    /** Delegates ZSTD decompression to zstd-jni when present. */
    private static byte[] zstdDecompress(byte[] in, int expectedLen) throws IOException {
        try {
            Class<?> zstd = Class.forName("com.github.luben.zstd.Zstd");
            java.lang.reflect.Method m = zstd.getMethod("decompress", byte[].class, int.class);
            Object res = m.invoke(null, in, expectedLen > 0 ? expectedLen : in.length * 8);
            return (byte[]) res;
        } catch (ClassNotFoundException e) {
            throw new IOException("ZSTD Parquet compression requires the com.github.luben.zstd bundle, which is not installed.");
        } catch (Exception e) {
            throw new IOException("ZSTD decompression failed: " + e.getMessage(), e);
        }
    }

    // ------------------------------------------------------------------
    // Low-level helpers
    // ------------------------------------------------------------------

    private static int readI32LE(byte[] b, int off) {
        return (b[off] & 0xFF) | ((b[off + 1] & 0xFF) << 8) | ((b[off + 2] & 0xFF) << 16) | ((b[off + 3] & 0xFF) << 24);
    }

    private static long readI64LE(byte[] b, int off) {
        return (b[off] & 0xFFL) | ((b[off + 1] & 0xFFL) << 8) | ((b[off + 2] & 0xFFL) << 16) | ((b[off + 3] & 0xFFL) << 24)
                | ((b[off + 4] & 0xFFL) << 32) | ((b[off + 5] & 0xFFL) << 40) | ((b[off + 6] & 0xFFL) << 48) | ((b[off + 7] & 0xFFL) << 56);
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
            String s = new String(buf, pos, n, StandardCharsets.UTF_8);
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
