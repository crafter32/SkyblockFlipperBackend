package com.skyblockflipper.backend.instrumentation;

import org.slf4j.MDC;

public final class CycleContextHolder {
    public static final String MDC_KEY = "cycleId";
    private static final ThreadLocal<CycleContext> CONTEXT = new ThreadLocal<>();

    /**
 * Prevents instantiation of this utility class.
 */
private CycleContextHolder() {}

    /**
     * Associates the given CycleContext with the current thread and publishes its cycleId to the logging MDC.
     *
     * @param context the CycleContext to set for the current thread; must be non-null — its `cycleId` is placed into the MDC under {@link #MDC_KEY}
     */
    public static void set(CycleContext context) {
        CONTEXT.set(context);
        MDC.put(MDC_KEY, context.getCycleId());
    }

    /**
     * Retrieve the CycleContext associated with the current thread.
     *
     * @return the current thread's CycleContext, or {@code null} if none is set
     */
    public static CycleContext get() {
        return CONTEXT.get();
    }

    /**
     * Clears the current thread's CycleContext and removes the corresponding MDC entry.
     *
     * <p>Removes the value stored in the internal ThreadLocal and removes the SLF4J MDC value
     * stored under the key {@link #MDC_KEY}.</p>
     */
    public static void clear() {
        CONTEXT.remove();
        MDC.remove(MDC_KEY);
    }
}