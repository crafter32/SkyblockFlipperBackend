package com.skyblockflipper.backend.instrumentation;

import java.nio.file.Path;

public final class JfrReportCli {

    /**
     * Prevents instantiation of this utility class.
     */
    private JfrReportCli() {
    }

    /**
     * Command-line entry point that generates and prints a summary for a JFR recording.
     *
     * <p>If no arguments are provided, prints a usage message to standard error and exits with status
     * code 1. Otherwise treats {@code args[0]} as the path to a {@code .jfr} file, computes a summary,
     * and writes the summary to standard output.
     *
     * @param args command-line arguments; expects the first element to be the path to a `.jfr` file
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Usage: JfrReportCli <recording.jfr>");
            System.exit(1);
        }
        JfrBlockingReportService service = new JfrBlockingReportService();
        System.out.println(service.summarize(Path.of(args[0])));
    }
}