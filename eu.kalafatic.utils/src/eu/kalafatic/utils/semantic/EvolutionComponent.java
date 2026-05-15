package eu.kalafatic.utils.semantic;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to enrich components with semantic AI metadata.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface EvolutionComponent {
    String domain() default "";
    String role() default "";
    String purpose() default "";
    String[] inputs() default {};
    String[] outputs() default {};
    Stability stability() default Stability.EVOLVING;
    EvolutionaryImpact evolutionaryImpact() default EvolutionaryImpact.MEDIUM;
    String lifecycleStage() default "active";
    String[] dependencies() default {};
    boolean mediationRelevance() default true;
    String visibility() default "public";
    String trajectoryCategory() default "general";
}
