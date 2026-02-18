package com.skyblockflipper.backend.instrumentation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

@Component
@Slf4j
public class BlockingTimeTracker {

    private final InstrumentationProperties properties;
    private final Clock clock;
    private final AtomicLong nextStackLogEpochMillis = new AtomicLong(0);

    /**
     * Creates a BlockingTimeTracker configured with the given instrumentation properties and the system UTC clock.
     *
     * @param properties configuration for blocking thresholds, sampling rate, and rate limits
     */
    @Autowired
    public BlockingTimeTracker(InstrumentationProperties properties) {
        this(properties, Clock.systemUTC());
    }

    /**
     * Create a BlockingTimeTracker using the provided instrumentation configuration and clock.
     *
     * @param properties configuration providing thresholds, sampling rates, and rate limits used by the tracker
     * @param clock      clock used for time-based decisions and rate-limiting (injected to allow test control) 
     */
    BlockingTimeTracker(InstrumentationProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    /**
     * Executes the provided supplier, records the elapsed blocking time for the given label and category, and returns the supplier's result.
     *
     * @param label     a short identifier for the blocking point
     * @param category  a category for grouping blocking points
     * @param supplier  the work to execute whose execution time will be recorded; may throw checked exceptions
     * @param <T>       the supplier result type
     * @return the value returned by the supplier
     * @throws RuntimeException if the supplier throws a RuntimeException (propagated) or a checked exception (wrapped)
     */
    public <T> T record(String label, String category, CheckedSupplier<T> supplier) {
        long start = System.nanoTime();
        try {
            return supplier.get();
        } catch (RuntimeException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        } finally {
            long elapsed = (System.nanoTime() - start) / 1_000_000L;
            capture(label, category, elapsed);
        }
    }

    /**
     * Measures the elapsed time while executing the provided runnable and records the measured blocking duration under the given label and category.
     *
     * @param label    a short identifier for the blocking point
     * @param category a category grouping for the blocking point
     * @throws RuntimeException if the runnable throws an exception; runtime exceptions are propagated and checked exceptions are wrapped in a {@code RuntimeException}
     */
    public void recordRunnable(String label, String category, CheckedRunnable runnable) {
        long start = System.nanoTime();
        try {
            runnable.run();
        } catch (RuntimeException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        } finally {
            long elapsed = (System.nanoTime() - start) / 1_000_000L;
            capture(label, category, elapsed);
        }
    }

    /**
     * Records a blocking measurement and, when it exceeds the configured slow threshold and sampling/rate limits permit,
     * emits a warning that includes the measured duration and a short stack trace.
     *
     * If a current CycleContext exists, a BlockingPoint for this measurement is appended to its blocking points.
     *
     * @param label        a short identifier for the measured blocking point
     * @param category     a category or grouping for the blocking point
     * @param blockedMillis the elapsed blocking time in milliseconds
     */
    private void capture(String label, String category, long blockedMillis) {
        CycleContext context = CycleContextHolder.get();
        if (context != null) {
            context.getBlockingPoints().add(new BlockingPoint(label, category, blockedMillis));
        }
        if (blockedMillis < properties.getBlocking().getSlowThresholdMillis()) {
            return;
        }
        if (!shouldCaptureStack()) {
            return;
        }
        log.warn("slow_blocking_point label={} category={} blockedMillis={} stack={}",
                label,
                category,
                blockedMillis,
                StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
                        .walk(stream -> stream.limit(12).map(StackWalker.StackFrame::toString).toList()));
    }

    /**
     * Decide whether a stack trace should be captured for the current blocking event,
     * applying probabilistic sampling and a time-based rate limit.
     *
     * @return `true` if sampling selected this event and the rate limiter permits a capture now, `false` otherwise.
     */
    private boolean shouldCaptureStack() {
        if (ThreadLocalRandom.current().nextDouble() > properties.getBlocking().getStackSampleRate()) {
            return false;
        }
        long now = clock.millis();
        long nextAllowed = nextStackLogEpochMillis.get();
        if (now < nextAllowed) {
            return false;
        }
        return nextStackLogEpochMillis.compareAndSet(nextAllowed,
                now + properties.getBlocking().getStackLogRateLimit().toMillis());
    }

    /**
     * Get the top blocking points from a CycleContext ordered by blocked duration.
     *
     * @param context the CycleContext to read blocking points from
     * @param limit the maximum number of blocking points to return
     * @return a list of BlockingPoint objects sorted by `blockedMillis` in descending order; contains at most `limit` entries
     */
    public List<BlockingPoint> topBlockingPoints(CycleContext context, int limit) {
        return context.getBlockingPoints()
                .stream()
                .sorted(Comparator.comparingLong(BlockingPoint::blockedMillis).reversed())
                .limit(limit)
                .toList();
    }

    public record BlockingPoint(String label, String category, long blockedMillis) {}

    @FunctionalInterface
    public interface CheckedSupplier<T> {
        /**
 * Produce a value, permitting callers to handle checked exceptions thrown during production.
 *
 * @return the produced value
 * @throws Exception if an error occurs while producing the value
 */
T get() throws Exception;
    }

    @FunctionalInterface
    public interface CheckedRunnable {
        /**
 * Performs the runnable action.
 *
 * @throws Exception if an error occurs during execution
 */
void run() throws Exception;
    }
}