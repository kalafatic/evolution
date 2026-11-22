package com.example.evo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

/**
 * Deterministic evaluation suite for the Darwin evolution fixture.
 *
 * This suite is the objective evidence source: any candidate implementation
 * proposed by Darwin is evaluated by running this exact suite.
 */
public class CollectionProcessorTest {

    @Test
    public void testDeduplicatesPreservingOrder() {
        List<String> input = Arrays.asList("b", "a", "b", "c", "a");
        List<String> result = CollectionProcessor.deduplicate(input);
        assertEquals(Arrays.asList("b", "a", "c"), result);
    }

    @Test
    public void testNullElementsIgnored() {
        List<String> input = Arrays.asList("a", null, "b", null, "a");
        List<String> result = CollectionProcessor.deduplicate(input);
        assertEquals(Arrays.asList("a", "b"), result);
    }

    @Test
    public void testEmptyInputProducesEmptyResult() {
        List<String> input = new ArrayList<String>();
        List<String> result = CollectionProcessor.deduplicate(input);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testInputIsNotModified() {
        List<String> input = new ArrayList<String>(Arrays.asList("x", "y", "x"));
        CollectionProcessor.deduplicate(input);
        assertEquals(Arrays.asList("x", "y", "x"), input);
    }

    @Test
    public void testResultIsNewInstance() {
        List<String> input = Arrays.asList("x", "y", "z");
        List<String> result = CollectionProcessor.deduplicate(input);
        assertNotSame(input, result);
        assertEquals(input, result);
    }

    @Test
    public void testLinearComplexityOnLargeInput() {
        List<String> base = new ArrayList<String>();
        for (int i = 0; i < 2000; i++) {
            base.add("item-" + (i % 100));
        }
        CountingList input = new CountingList(base);
        List<String> result = CollectionProcessor.deduplicate(input);
        assertEquals(100, result.size());
        long scans = input.getIndexScanCount();
        assertTrue(
            "Quadratic duplicate detection detected: " + scans
                + " index scans for 2000 elements (linear implementations stay well below 2500)",
            scans < 2500);
    }

    /**
     * Instruments List lookups so tests can objectively detect quadratic
     * duplicate-detection strategies without wall-clock timing.
     */
    private static final class CountingList extends AbstractList<String> {
        private final List<String> delegate;
        private long indexScanCount;

        CountingList(List<String> delegate) {
            this.delegate = delegate;
        }

        @Override
        public String get(int index) {
            return delegate.get(index);
        }

        @Override
        public int size() {
            return delegate.size();
        }

        @Override
        public int indexOf(Object o) {
            for (int i = 0; i < delegate.size(); i++) {
                indexScanCount++;
                if (o == null ? delegate.get(i) == null : o.equals(delegate.get(i))) {
                    return i;
                }
            }
            return -1;
        }

        @Override
        public int lastIndexOf(Object o) {
            for (int i = delegate.size() - 1; i >= 0; i--) {
                indexScanCount++;
                if (o == null ? delegate.get(i) == null : o.equals(delegate.get(i))) {
                    return i;
                }
            }
            return -1;
        }

        @Override
        public boolean contains(Object o) {
            return indexOf(o) >= 0;
        }

        long getIndexScanCount() {
            return indexScanCount;
        }
    }
}
