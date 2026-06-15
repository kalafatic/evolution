package eu.kalafatic.evolution.forge.data.api;

import java.util.Iterator;

public interface Dataset<T> extends Iterable<T> {
    String getId();
    long getSize();
    Iterator<T> stream();
}
