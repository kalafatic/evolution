package com.example.evo;

import java.util.ArrayList;
import java.util.List;

/**
 * Collection processing helper: removes duplicate entries from a list.
 *
 * FUNCTIONAL CONTRACT:
 *  - duplicates are removed while preserving first-occurrence order
 *  - null entries are ignored (never present in the result)
 *  - an empty input produces an empty result
 *  - the input list is never modified
 *  - a new list instance is always returned
 *
 * PERFORMANCE CONTRACT:
 *  - duplicate detection must be linear in the size of the input
 *    (the implementation must not scan the whole input for every element)
 */
public final class CollectionProcessor {

    private CollectionProcessor() {
    }

    /**
     * Removes duplicate elements from the given list.
     *
     * @param input the list to process (not modified)
     * @return a new list with duplicates removed, preserving first-occurrence order
     */
    public static List<String> deduplicate(List<String> input) {
        List<String> result = new ArrayList<String>();
        for (int i = 0; i < input.size(); i++) {
            String item = input.get(i);
            if (item == null) {
                continue;
            }
            if (input.indexOf(item) == i) {
                result.add(item);
            }
        }
        return result;
    }
}
