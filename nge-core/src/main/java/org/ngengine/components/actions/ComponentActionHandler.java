package org.ngengine.components.actions;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.ngengine.components.Component;
import org.ngengine.components.fragments.ActionBasedFragment;

import jakarta.annotation.Nullable;

/**
 * Reflection-backed action handler metadata + selection runtime.
 */
public final class ComponentActionHandler {
    private static final Logger log = Logger.getLogger(ComponentActionHandler.class.getName());
    private static final Map<Class<?>, List<ComponentActionHandler>> CACHE = new ConcurrentHashMap<>();

    @FunctionalInterface
    public interface AuthorityResolver {
        boolean hasAuthority();
    }

    public static final class Selection {
        private final ActionBasedFragment component;
        private final ComponentActionHandler handler;

        private Selection(ActionBasedFragment component, ComponentActionHandler handler) {
            this.component = component;
            this.handler = handler;
        }

        public ActionBasedFragment getComponent() {
            return component;
        }

        public ComponentActionHandler getHandler() {
            return handler;
        }
    }

    private static final class Candidate implements Comparable<Candidate> {
        final ActionBasedFragment component;
        final ComponentActionHandler handler;
        final int score;

        Candidate(ActionBasedFragment component, ComponentActionHandler handler, int score) {
            this.component = component;
            this.handler = handler;
            this.score = score;
        }

        @Override
        public int compareTo(Candidate other) {
            if (other == null) {
                return 1;
            }
            int byScore = Integer.compare(this.score, other.score);
            if (byScore != 0) {
                return byScore;
            }
            return -this.handler.signature.compareTo(other.handler.signature);
        }
    }

    private final Method method;
    private final Class<? extends ComponentActionEvent> annotationActionType;
    private final int filter;
    private final int priority;
    private final String signature;

    private ComponentActionHandler(
        Method method,
        Class<? extends ComponentActionEvent> annotationActionType,
        int filter,
        int priority,
        String signature
    ) {
        this.method = method;
        this.annotationActionType = annotationActionType;
        this.filter = filter;
        this.priority = priority;
        this.signature = signature;
    }

    public static List<ComponentActionHandler> handlersOf(Class<?> type) {
        return CACHE.computeIfAbsent(type, ComponentActionHandler::scanHandlers);
    }

    public static @Nullable <T extends ComponentActionEvent> Selection selectBest(
        Iterable<? extends Component> components,
        @Nullable String requiredComponentId,
        @Nullable ActionBasedFragment preferredSource,
        T message,
        ComponentActionOrigin origin,
        AuthorityResolver authorityResolver
    ) {
        ActionBasedFragment source = preferredSource;
        if (source == null && components != null) {
            for (Component component : components) {
                if (component instanceof ActionBasedFragment) {
                    ActionBasedFragment fragment = (ActionBasedFragment) component;
                    source = fragment;
                    break;
                }
            }
        }
        return selectBest(source, requiredComponentId, message, origin, authorityResolver);
    }

    public static @Nullable <T extends ComponentActionEvent> Selection selectBest(
        @Nullable ActionBasedFragment<?> source,
        @Nullable String requiredComponentId,
        T message,
        ComponentActionOrigin origin,
        AuthorityResolver authorityResolver
    ) {
        if (source == null) {
            return null;
        }
        if (requiredComponentId != null
            && !requiredComponentId.trim().isEmpty()
            && !requiredComponentId.equals(source.getComponentId())) {
            return null;
        }

        Candidate best = null;
        for (ComponentActionHandler handler : handlersOf(source.getClass())) {
            int score = handler.score(source, message, origin, authorityResolver);
            if (score < 0) {
                continue;
            }
            Candidate current = new Candidate(source, handler, score);
            if (best == null || current.compareTo(best) > 0) {
                best = current;
            }
        }
        return best == null ? null : new Selection(source, best.handler);
    }

    public <T extends ComponentActionEvent> void invoke(ActionBasedFragment<?> component, T message) {
        try {
            method.invoke(component, message);
        } catch (Exception ex) {
            log.log(
                Level.WARNING,
                "Failed to invoke component action handler " + signature + " for " + component.getClass().getName(),
                ex
            );
        }
    }

    private int score(
        ActionBasedFragment candidate,
        ComponentActionEvent message,
        ComponentActionOrigin origin,
        AuthorityResolver authorityResolver
    ) {
        Class<?> messageClass = message.getClass();
        if (!annotationActionType.isAssignableFrom(messageClass)) {
            return -1;
        }

        boolean allowLocal = (filter & ComponentActionFilter.LOCAL) != 0;
        boolean allowRemote = (filter & ComponentActionFilter.REMOTE) != 0;
        if (!allowLocal && !allowRemote) {
            return -1;
        }
        if (origin == ComponentActionOrigin.LOCAL && !allowLocal) {
            return -1;
        }
        if (origin == ComponentActionOrigin.REMOTE && !allowRemote) {
            return -1;
        }

        boolean hasWithAuth = (filter & ComponentActionFilter.WITH_AUTHORITY) != 0;
        boolean hasWithoutAuth = (filter & ComponentActionFilter.WITHOUT_AUTHORITY) != 0;
        boolean hasAuthority = authorityResolver.hasAuthority();
        if (hasWithAuth && !hasWithoutAuth && !hasAuthority) {
            return -1;
        }
        if (!hasWithAuth && hasWithoutAuth && hasAuthority) {
            return -1;
        }

        int score = priority;
        int distance = inheritanceDistance(messageClass, annotationActionType);
        if (distance == 0) {
            score += 10_000;
        } else if (distance == Integer.MAX_VALUE) {
            return -1;
        } else if (annotationActionType == ComponentActionEvent.class) {
            score -= 5_000;
        } else {
            score += Math.max(50, 2_000 - (distance * 50));
        }
        if (allowLocal ^ allowRemote) {
            score += 20;
        }
        if (hasWithAuth ^ hasWithoutAuth) {
            score += 10;
        }
        return score;
    }

    private static List<ComponentActionHandler> scanHandlers(Class<?> type) {
        List<ComponentActionHandler> out = new ArrayList<>();
        for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
            for (Method method : current.getDeclaredMethods()) {
                ComponentAction action = method.getAnnotation(ComponentAction.class);
                if (action == null) {
                    continue;
                }
                Class<?>[] params = method.getParameterTypes();
                if (params.length != 1 || !ComponentActionEvent.class.isAssignableFrom(params[0])) {
                    log.warning("Ignoring @ComponentAction " + method + " because it must accept exactly one ComponentActionEvent parameter.");
                    continue;
                }
                if (!params[0].isAssignableFrom(action.type())) {
                    log.warning("Ignoring @ComponentAction " + method + " because parameter type is incompatible with annotation message.");
                    continue;
                }
                method.setAccessible(true);
                out.add(
                    new ComponentActionHandler(
                        method,
                        action.type(),
                        action.filter(),
                        action.priority(),
                        signatureOf(current, method)
                    )
                );
            }
        }
        Collections.sort(out, Comparator.comparing(h -> h.signature));
        return out;
    }

    private static int inheritanceDistance(Class<?> actual, Class<?> target) {
        if (actual == null || target == null || !target.isAssignableFrom(actual)) {
            return Integer.MAX_VALUE;
        }
        if (actual.equals(target)) {
            return 0;
        }
        int best = Integer.MAX_VALUE;
        Class<?> superClass = actual.getSuperclass();
        if (superClass != null) {
            int d = inheritanceDistance(superClass, target);
            if (d != Integer.MAX_VALUE) {
                best = Math.min(best, d + 1);
            }
        }
        for (Class<?> itf : actual.getInterfaces()) {
            int d = inheritanceDistance(itf, target);
            if (d != Integer.MAX_VALUE) {
                best = Math.min(best, d + 1);
            }
        }
        return best;
    }

    private static String signatureOf(Class<?> owner, Method method) {
        StringBuilder sb = new StringBuilder(owner.getName()).append('#').append(method.getName()).append('(');
        Class<?>[] params = method.getParameterTypes();
        for (int i = 0; i < params.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(params[i].getName());
        }
        sb.append(')');
        return sb.toString();
    }
}
