package com.skyblockflipper.backend.instrumentation;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;
import java.time.Duration;

@ConfigurationProperties(prefix = "instrumentation")
public class InstrumentationProperties {

    private final Jfr jfr = new Jfr();
    private final Blocking blocking = new Blocking();
    private final Admin admin = new Admin();
    private final AsyncProfiler asyncProfiler = new AsyncProfiler();

    /**
 * Access JFR configuration settings.
 *
 * @return the {@link Jfr} instance containing Java Flight Recorder configuration values
 */
public Jfr getJfr() { return jfr; }
    /**
 * Accesses the Blocking instrumentation configuration.
 *
 * @return the Blocking configuration instance.
 */
public Blocking getBlocking() { return blocking; }
    /**
 * Access the admin instrumentation configuration.
 *
 * @return the {@link Admin} instance containing admin-related instrumentation properties
 */
public Admin getAdmin() { return admin; }
    /**
 * Access the async profiler configuration.
 *
 * @return the {@link AsyncProfiler} instance containing async-profiler configuration
 */
public AsyncProfiler getAsyncProfiler() { return asyncProfiler; }

    public static class Jfr {
        private boolean enabled = true;
        private Path outputDir = Path.of("var", "profiling", "jfr");
        private Duration retention = Duration.ofHours(2);
        private long maxSizeMb = 512;
        private int stackDepth = 256;
        private Duration snapshotWindow = Duration.ofMinutes(2);

        /**
 * Whether this component is enabled.
 *
 * @return `true` if enabled, `false` otherwise.
 */
public boolean isEnabled() { return enabled; }
        /**
 * Configure whether the component is enabled.
 *
 * @param enabled `true` to enable the component, `false` to disable it.
 */
public void setEnabled(boolean enabled) { this.enabled = enabled; }
        /**
 * Directory where profiling output files are written.
 *
 * @return the configured output directory path
 */
public Path getOutputDir() { return outputDir; }
        /**
 * Sets the directory where JFR recording files will be written.
 *
 * @param outputDir the target directory for JFR output
 */
public void setOutputDir(Path outputDir) { this.outputDir = outputDir; }
        /**
 * Retention period for JFR recordings.
 *
 * @return the duration JFR recordings are retained before being rotated or deleted
 */
public Duration getRetention() { return retention; }
        /**
 * Set the maximum age to retain JFR recordings before they are expired or cleaned up.
 *
 * @param retention the duration to keep recordings (e.g., Duration.ofHours(2))
 */
public void setRetention(Duration retention) { this.retention = retention; }
        /**
 * Maximum allowed recording size in megabytes.
 *
 * @return the maximum recording size in megabytes
 */
public long getMaxSizeMb() { return maxSizeMb; }
        /**
 * Set the maximum file size for JFR recordings in megabytes.
 *
 * @param maxSizeMb the maximum file size for JFR recordings in megabytes
 */
public void setMaxSizeMb(long maxSizeMb) { this.maxSizeMb = maxSizeMb; }
        /**
 * Maximum number of stack frames captured when sampling stack traces.
 *
 * @return the configured maximum stack depth (number of stack frames)
 */
public int getStackDepth() { return stackDepth; }
        /**
 * Sets the maximum stack depth captured for JFR stack traces.
 *
 * @param stackDepth maximum number of stack frames to capture per sample
 */
public void setStackDepth(int stackDepth) { this.stackDepth = stackDepth; }
        /**
 * The duration of the JFR snapshot window.
 *
 * @return the duration of the snapshot window
 */
public Duration getSnapshotWindow() { return snapshotWindow; }
        /**
 * Sets the duration of the JFR snapshot window.
 *
 * @param snapshotWindow the duration of the snapshot window for JFR recordings
 */
public void setSnapshotWindow(Duration snapshotWindow) { this.snapshotWindow = snapshotWindow; }
    }

    public static class Blocking {
        private long slowThresholdMillis = 100;
        private double stackSampleRate = 0.01;
        private Duration stackLogRateLimit = Duration.ofSeconds(30);

        /**
 * Threshold in milliseconds above which blocking operations are considered slow.
 *
 * @return the threshold in milliseconds; operations longer than this are considered slow
 */
public long getSlowThresholdMillis() { return slowThresholdMillis; }
        /**
 * Set the threshold, in milliseconds, used to classify an operation as slow.
 *
 * @param slowThresholdMillis the threshold duration in milliseconds for slow-operation detection
 */
public void setSlowThresholdMillis(long slowThresholdMillis) { this.slowThresholdMillis = slowThresholdMillis; }
        /**
 * Gets the sampling rate used when capturing stack traces for blocking detection.
 *
 * @return the sampling rate as a value between 0.0 and 1.0 representing the probability of sampling each event
 */
public double getStackSampleRate() { return stackSampleRate; }
        /**
 * Set the probability used to sample stack traces for blocking detection.
 *
 * @param stackSampleRate the sampling rate between 0.0 and 1.0 (e.g., 0.01 for 1% sampling)
 */
public void setStackSampleRate(double stackSampleRate) { this.stackSampleRate = stackSampleRate; }
        /**
 * Controls how frequently blocking stack traces may be logged.
 *
 * @return the minimum duration between logged blocking stack traces
 */
public Duration getStackLogRateLimit() { return stackLogRateLimit; }
        /**
 * Sets the minimum interval between logged stack samples.
 *
 * @param stackLogRateLimit the minimum duration to wait between consecutive stack log entries
 */
public void setStackLogRateLimit(Duration stackLogRateLimit) { this.stackLogRateLimit = stackLogRateLimit; }
    }

    public static class Admin {
        private boolean localOnly = true;
        private String token = "";

        /**
 * Indicates whether administrative endpoints are restricted to the local host.
 *
 * @return `true` if administration access is restricted to local requests, `false` otherwise.
 */
public boolean isLocalOnly() { return localOnly; }
        /**
 * Configure whether the admin endpoints are restricted to local access only.
 *
 * @param localOnly {@code true} to restrict admin access to the local host, {@code false} to allow non-local access
 */
public void setLocalOnly(boolean localOnly) { this.localOnly = localOnly; }
        /**
 * Retrieve the admin authentication token.
 *
 * @return the admin token, or an empty string if none is configured
 */
public String getToken() { return token; }
        /**
 * Set the admin access token used to authenticate administrative operations.
 *
 * @param token the admin token to use for authenticating admin requests
 */
public void setToken(String token) { this.token = token; }
    }

    public static class AsyncProfiler {
        private boolean enabled = false;
        private Path outputDir = Path.of("var", "profiling", "async-profiler");
        private String scriptPath = "scripts/run_async_profiler.sh";

        /**
 * Whether this component is enabled.
 *
 * @return `true` if enabled, `false` otherwise.
 */
public boolean isEnabled() { return enabled; }
        /**
 * Configure whether the component is enabled.
 *
 * @param enabled `true` to enable the component, `false` to disable it.
 */
public void setEnabled(boolean enabled) { this.enabled = enabled; }
        /**
 * Directory where profiling output files are written.
 *
 * @return the configured output directory path
 */
public Path getOutputDir() { return outputDir; }
        /**
 * Sets the directory where JFR recording files will be written.
 *
 * @param outputDir the target directory for JFR output
 */
public void setOutputDir(Path outputDir) { this.outputDir = outputDir; }
        /**
 * Location of the async-profiler helper script.
 *
 * @return the script path, either a relative application path or an absolute filesystem path
 */
public String getScriptPath() { return scriptPath; }
        /**
 * Sets the filesystem path to the async-profiler launch script used when starting the profiler.
 *
 * @param scriptPath the path to the script (absolute or relative to the application's working directory)
 */
public void setScriptPath(String scriptPath) { this.scriptPath = scriptPath; }
    }
}