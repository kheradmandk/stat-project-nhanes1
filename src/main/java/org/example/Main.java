package org.example;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Runs the whole Phase 1 pipeline in order.
 * Needs data/DEMO_L.csv, data/BMX_L.csv and data/BPXO_L.csv (see data/README.md).
 */
public class Main {
    public static void main(String[] args) throws Exception {
        Files.createDirectories(Path.of("figures"));

        System.out.println(">>> Step 1/3: data audit");
        Audit.main(args);

        System.out.println("\n>>> Step 2/3: crude vs weighted estimates");
        Analysis.main(args);

        System.out.println("\n>>> Step 3/3: figures (saved in figures/)");
        Plots.main(args);

        System.out.println("\nAll steps finished.");
    }
}
