package com.skyblockflipper.backend.instrumentation.admin;

import com.skyblockflipper.backend.instrumentation.InstrumentationProperties;
import com.skyblockflipper.backend.instrumentation.JfrBlockingReportService;
import com.skyblockflipper.backend.instrumentation.JfrRecordingManager;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/internal/admin/instrumentation")
public class InstrumentationAdminController {

    private final AdminAccessGuard adminAccessGuard;
    private final JfrRecordingManager jfrRecordingManager;
    private final JfrBlockingReportService jfrBlockingReportService;
    private final InstrumentationProperties properties;

    /**
     * Creates the controller that exposes admin instrumentation endpoints.
     *
     * @param adminAccessGuard validates admin access for incoming requests
     * @param jfrRecordingManager handles JFR recording operations (snapshots and latest recordings)
     * @param jfrBlockingReportService generates summaries/reports from JFR data
     * @param properties instrumentation configuration (including async-profiler settings)
     */
    public InstrumentationAdminController(AdminAccessGuard adminAccessGuard,
                                          JfrRecordingManager jfrRecordingManager,
                                          JfrBlockingReportService jfrBlockingReportService,
                                          InstrumentationProperties properties) {
        this.adminAccessGuard = adminAccessGuard;
        this.jfrRecordingManager = jfrRecordingManager;
        this.jfrBlockingReportService = jfrBlockingReportService;
        this.properties = properties;
    }

    /**
     * Creates a JFR snapshot and returns metadata about the created file.
     *
     * @param request the incoming HTTP request (used for access validation)
     * @return a map with entries:
     *         "snapshot" — the file system path of the created snapshot as a string;
     *         "createdAt" — the snapshot creation timestamp as an ISO-8601 string
     */
    @PostMapping("/jfr/snapshot")
    public Map<String, Object> dumpSnapshot(HttpServletRequest request) {
        adminAccessGuard.validate(request);
        Path dump = jfrRecordingManager.dumpSnapshot();
        return Map.of("snapshot", dump.toString(), "createdAt", Instant.now().toString());
    }

    /**
     * Return a summarized report for the latest Java Flight Recorder (JFR) recording.
     *
     * @param request the incoming HTTP request; used to validate admin access
     * @return a map containing the report produced by summarizing the latest JFR recording
     */
    @GetMapping("/jfr/report/latest")
    public Map<String, Object> latestReport(HttpServletRequest request) {
        adminAccessGuard.validate(request);
        return jfrBlockingReportService.summarize(jfrRecordingManager.latestRecordingFile());
    }

    /**
     * Run the configured async-profiler integration and return execution results and generated artifacts.
     *
     * @param request HTTP request used to validate admin access
     * @return a map with keys:
     *         <ul>
     *           <li>"status" — the string "completed"</li>
     *           <li>"latestArtifacts" — a list of up to three newest artifact file paths (strings)</li>
     *           <li>"scriptOutput" — the complete textual output produced by the profiler script</li>
     *         </ul>
     * @throws ResponseStatusException HTTP 404 when async-profiler integration is disabled; HTTP 500 for I/O errors, interrupted execution, or when the profiler script exits with a non-zero status
     */
    @PostMapping("/async-profiler/run")
    public Map<String, Object> runAsyncProfiler(HttpServletRequest request) {
        adminAccessGuard.validate(request);
        if (!properties.getAsyncProfiler().isEnabled()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "async-profiler integration is disabled");
        }
        try {
            Files.createDirectories(properties.getAsyncProfiler().getOutputDir());
            String pid = ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
            Process process = new ProcessBuilder(
                    properties.getAsyncProfiler().getScriptPath(),
                    pid,
                    properties.getAsyncProfiler().getOutputDir().toString(),
                    Instant.now().toString().replace(':', '-')
            ).redirectErrorStream(true).start();
            int exit = process.waitFor();
            String output = new String(process.getInputStream().readAllBytes());
            if (exit != 0) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, output);
            }
            List<Path> files = Files.list(properties.getAsyncProfiler().getOutputDir())
                    .sorted((a, b) -> Long.compare(b.toFile().lastModified(), a.toFile().lastModified()))
                    .limit(3)
                    .toList();
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("status", "completed");
            result.put("latestArtifacts", files.stream().map(Path::toString).toList());
            result.put("scriptOutput", output);
            return result;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, exception.getMessage(), exception);
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, exception.getMessage(), exception);
        }
    }
}