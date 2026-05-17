package org.ngengine.components.actions;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;



/**
 * Marks a component method as an action handler for a specific message type.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ComponentAction {
    /**
     * Message type matched by this handler.
     */
    Class<? extends ComponentActionEvent> type();

    /**
     * Bitmask from {@link ComponentActionFilter} controlling origin and authority constraints.
     */
    int filter() default ComponentActionFilter.LOCAL | ComponentActionFilter.REMOTE;

    /**
     * Optional tie-break score added to the runtime matching algorithm.
     */
    int priority() default 0;
}
