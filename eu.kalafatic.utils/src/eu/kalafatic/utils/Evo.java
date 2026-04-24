package eu.kalafatic.utils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotation for self-development traceability.
 */
@Retention(RetentionPolicy.SOURCE)
public @interface Evo {
    int iteration();
    String variant();
    String reason();
}
