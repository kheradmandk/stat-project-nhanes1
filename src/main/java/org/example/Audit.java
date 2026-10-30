package org.example;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Audit.java  -  Phase 1 data audit (NHANES 2021-2023)
 *
 * Reads the three CSV files in data/, checks their quality, builds the analytic
 * population step by step and prints every number needed for the Phase 1 report.
 * It does NOT change any other file. Just run main().
 */
public class Audit {

    // One NHANES participant (one row of DEMO_L, plus BMI and SBP if available)
    record P(String id, double age, double sex, double wt, double bmi, double sbp,
             boolean inBmx, boolean inBpxo) {}

    // ---------- small helpers ----------

    static double num(String s) {
        if (s == null) return Double.NaN;
        String t = s.replace("\"", "").replace("'", "").trim();
        if (t.isEmpty() || t.equals(".") || t.equalsIgnoreCase("NA")) return Double.NaN;
        try {
            return Double.parseDouble(t);
        } catch (Exception e) {
            return Double.NaN;
        }
    }

    // true if the cell has text that is not a number (and is not blank / "." / NA)
    static boolean unparseable(String s) {
        if (s == null) return false;
        String t = s.replace("\"", "").replace("'", "").trim();
        if (t.isEmpty() || t.equals(".") || t.equalsIgnoreCase("NA")) return false;
        try {
            Double.parseDouble(t);
            return false;
        } catch (Exception e) {
            return true;
        }
    }

    // SEQN is stored like "130378.0": turn it into "130378" so the files always match
    static String key(Map<String, String> row) {
        double v = num(row.get("SEQN"));
        return Double.isNaN(v) ? null : String.valueOf((long) v);
    }

    static Map<String, Map<String, String>> index(String name, List<Map<String, String>> rows) {
        Map<String, Map<String, String>> out = new HashMap<>();
        int noKey = 0, dup = 0;
        for (var r : rows) {
            String k = key(r);
            if (k == null) { noKey++; continue; }
            if (out.containsKey(k)) { dup++; continue; }
            out.put(k, r);
        }
        System.out.printf("%-5s rows=%d | unique SEQN=%d | duplicate SEQN rows=%d | rows without valid SEQN=%d%n",
                name, rows.size(), out.size(), dup, noKey);
        return out;
    }

    static void checkColumn(String file, List<Map<String, String>> rows, String col) {
        boolean ok = !rows.isEmpty() && rows.get(0).containsKey(col);
        System.out.println("  " + file + " has column " + col + ": " + (ok ? "YES" : "NO  <-- PROBLEM"));
    }

    static int countUnparseable(List<Map<String, String>> rows, String col) {
        int c = 0;
        for (var r : rows) if (unparseable(r.get(col))) c++;
        return c;
    }

    static double[] arr(List<P> g, java.util.function.ToDoubleFunction<P> f) {
        return g.stream().mapToDouble(f).toArray();
    }

    static void describe(String label, double[] x) {
        DescriptiveStatistics s = new DescriptiveStatistics(x);
        double q1 = s.getPercentile(25), q3 = s.getPercentile(75);
        System.out.printf("%-12s n=%d | mean=%.2f | median=%.2f | SD=%.2f | Q1=%.2f | Q3=%.2f | IQR=%.2f | min=%.2f | max=%.2f%n",
                label, x.length, s.getMean(), s.getPercentile(50), s.getStandardDeviation(),
                q1, q3, q3 - q1, s.getMin(), s.getMax());
    }

    static double wmean(List<P> g) {
        double sw = 0, swx = 0;
        for (P p : g) {
            if (!Double.isNaN(p.wt()) && p.wt() > 0 && !Double.isNaN(p.bmi())) {
                sw += p.wt();
                swx += p.wt() * p.bmi();
            }
        }
        return sw == 0 ? Double.NaN : swx / sw;
    }

    static void groupLine(String label, List<P> g) {
        double[] b = arr(g, P::bmi);
        DescriptiveStatistics s = new DescriptiveStatistics(b);
        System.out.printf("%-14s n=%d | crude mean BMI=%.2f | median=%.2f | SD=%.2f | weighted mean BMI=%.2f%n",
                label, b.length, s.getMean(), s.getPercentile(50), s.getStandardDeviation(), wmean(g));
    }

    static int ageGroup(double age) {
        if (age < 40) return 0;
        if (age < 60) return 1;
        return 2;
    }

    static final String[] AGE_LABELS = {"20-39", "40-59", "60+"};

    static void pct(String label, long k, long n) {
        System.out.printf("  %-28s %d  (%.2f%%)%n", label, k, 100.0 * k / n);
    }

    // ---------- main ----------

    public static void main(String[] args) throws Exception {
        String dir = "data/";
        var demo = LoadData.readCsv(dir + "DEMO_L.csv");
        var bmx = LoadData.readCsv(dir + "BMX_L.csv");
        var bpxo = LoadData.readCsv(dir + "BPXO_L.csv");

        System.out.println("=== 1. COLUMN CHECK ===");
        for (String c : new String[]{"SEQN", "RIDAGEYR", "RIAGENDR", "WTMEC2YR", "SDMVSTRA", "SDMVPSU"})
            checkColumn("DEMO_L", demo, c);
        checkColumn("BMX_L", bmx, "SEQN");
        checkColumn("BMX_L", bmx, "BMXBMI");
        checkColumn("BPXO_L", bpxo, "SEQN");
        checkColumn("BPXO_L", bpxo, "BPXOSY1");

        System.out.println("\n=== 2. FILES AND DUPLICATES ===");
        var d = index("DEMO", demo);
        var b = index("BMX", bmx);
        var p = index("BPXO", bpxo);

        System.out.println("\nCells with text that is not a number (should be 0):");
        System.out.println("  DEMO RIDAGEYR: " + countUnparseable(demo, "RIDAGEYR"));
        System.out.println("  DEMO RIAGENDR: " + countUnparseable(demo, "RIAGENDR"));
        System.out.println("  DEMO WTMEC2YR: " + countUnparseable(demo, "WTMEC2YR"));
        System.out.println("  BMX  BMXBMI  : " + countUnparseable(bmx, "BMXBMI"));
        System.out.println("  BPXO BPXOSY1 : " + countUnparseable(bpxo, "BPXOSY1"));

        System.out.println("\n=== 3. MERGE FLOW (key = SEQN) ===");
        int demoBmx = 0, demoBpxo = 0, all3 = 0;
        for (String id : d.keySet()) {
            boolean inB = b.containsKey(id), inP = p.containsKey(id);
            if (inB) demoBmx++;
            if (inP) demoBpxo++;
            if (inB && inP) all3++;
        }
        int bmxNotDemo = 0, bpxoNotDemo = 0;
        for (String id : b.keySet()) if (!d.containsKey(id)) bmxNotDemo++;
        for (String id : p.keySet()) if (!d.containsKey(id)) bpxoNotDemo++;
        System.out.println("Unique people in DEMO: " + d.size());
        System.out.println("Unique people in BMX : " + b.size() + "  (not in DEMO: " + bmxNotDemo + ")");
        System.out.println("Unique people in BPXO: " + p.size() + "  (not in DEMO: " + bpxoNotDemo + ")");
        System.out.println("In DEMO and BMX      : " + demoBmx);
        System.out.println("In DEMO and BPXO     : " + demoBpxo);
        System.out.println("In all three files   : " + all3);

        // build one object per DEMO participant
        List<P> all = new ArrayList<>();
        for (var e : d.entrySet()) {
            var dr = e.getValue();
            var br = b.get(e.getKey());
            var pr = p.get(e.getKey());
            all.add(new P(e.getKey(),
                    num(dr.get("RIDAGEYR")), num(dr.get("RIAGENDR")), num(dr.get("WTMEC2YR")),
                    br == null ? Double.NaN : num(br.get("BMXBMI")),
                    pr == null ? Double.NaN : num(pr.get("BPXOSY1")),
                    br != null, pr != null));
        }

        System.out.println("\n=== 4. ANALYTIC POPULATION (step by step, counts after each step) ===");
        long ageMissing = all.stream().filter(x -> Double.isNaN(x.age())).count();
        List<P> adults = all.stream().filter(x -> x.age() >= 20).collect(Collectors.toList());
        List<P> adBmi = adults.stream().filter(x -> !Double.isNaN(x.bmi())).collect(Collectors.toList());
        List<P> adBmiSbp = adBmi.stream().filter(x -> !Double.isNaN(x.sbp())).collect(Collectors.toList());
        List<P> finalA = adBmiSbp.stream().filter(x -> !Double.isNaN(x.wt()) && x.wt() > 0).collect(Collectors.toList());

        System.out.println("Step 0: everyone in DEMO                          : " + all.size());
        System.out.println("        (age missing: " + ageMissing + ")");
        System.out.println("Step 1: age >= 20                                 : " + adults.size()
                + "   (removed " + (all.size() - adults.size()) + ")");
        System.out.println("Step 2: + BMI available                           : " + adBmi.size()
                + "   (removed " + (adults.size() - adBmi.size()) + ")");
        System.out.println("Step 3: + systolic BP available                   : " + adBmiSbp.size()
                + "   (removed " + (adBmi.size() - adBmiSbp.size()) + ")");
        System.out.println("Step 4: + exam weight present and > 0  = SAMPLE A : " + finalA.size()
                + "   (removed " + (adBmiSbp.size() - finalA.size()) + ")");
        System.out.println("(The old report had 5805 after step 3; compare with step 3 above.)");

        System.out.println("\n=== 5. MISSING DATA AMONG ADULTS (n = " + adults.size() + ") ===");
        pct("age missing", ageMissing, all.size());
        pct("sex missing", adults.stream().filter(x -> Double.isNaN(x.sex())).count(), adults.size());
        pct("BMI missing", adults.stream().filter(x -> Double.isNaN(x.bmi())).count(), adults.size());
        pct("  of those: not in BMX file", adults.stream().filter(x -> !x.inBmx()).count(), adults.size());
        pct("SBP missing", adults.stream().filter(x -> Double.isNaN(x.sbp())).count(), adults.size());
        pct("  of those: not in BPXO file", adults.stream().filter(x -> !x.inBpxo()).count(), adults.size());
        pct("exam weight missing or <= 0", adults.stream().filter(x -> Double.isNaN(x.wt()) || x.wt() <= 0).count(), adults.size());

        System.out.println("\n=== 6. IMPOSSIBLE / SPECIAL VALUES IN SAMPLE A ===");
        System.out.println("Age: min=" + finalA.stream().mapToDouble(P::age).min().orElse(Double.NaN)
                + ", max=" + finalA.stream().mapToDouble(P::age).max().orElse(Double.NaN)
                + ", people coded exactly 80 (top-coded '80 and over'): "
                + finalA.stream().filter(x -> x.age() >= 80).count());
        System.out.println("BMI outside 10-80   : " + finalA.stream().filter(x -> x.bmi() < 10 || x.bmi() > 80).count());
        System.out.println("SBP outside 60-260  : " + finalA.stream().filter(x -> x.sbp() < 60 || x.sbp() > 260).count());
        System.out.println("Weight <= 0         : " + finalA.stream().filter(x -> x.wt() <= 0).count());
        System.out.println("Sex not 1 or 2      : " + finalA.stream().filter(x -> x.sex() != 1.0 && x.sex() != 2.0).count());
        System.out.println("Unusual codes (>= 777 in BMI or SBP): "
                + finalA.stream().filter(x -> x.bmi() >= 777 || x.sbp() >= 777).count());

        System.out.println("\n=== 7. DESCRIPTIVE STATISTICS, SAMPLE A (crude) ===");
        describe("Age", arr(finalA, P::age));
        describe("BMI", arr(finalA, P::bmi));
        describe("SBP", arr(finalA, P::sbp));

        System.out.println("\nSex frequency (SAMPLE A):");
        long males = finalA.stream().filter(x -> x.sex() == 1.0).count();
        long females = finalA.stream().filter(x -> x.sex() == 2.0).count();
        pct("Male (1)", males, finalA.size());
        pct("Female (2)", females, finalA.size());

        System.out.println("\nAge-group frequency (SAMPLE A):");
        for (int g = 0; g < 3; g++) {
            final int gg = g;
            pct(AGE_LABELS[g], finalA.stream().filter(x -> ageGroup(x.age()) == gg).count(), finalA.size());
        }

        System.out.println("\n=== 8. DESCRIPTIVE COMPARISON OF BMI (SAMPLE A) ===");
        groupLine("All", finalA);
        groupLine("Male", finalA.stream().filter(x -> x.sex() == 1.0).collect(Collectors.toList()));
        groupLine("Female", finalA.stream().filter(x -> x.sex() == 2.0).collect(Collectors.toList()));
        for (int g = 0; g < 3; g++) {
            final int gg = g;
            groupLine("Age " + AGE_LABELS[g], finalA.stream().filter(x -> ageGroup(x.age()) == gg).collect(Collectors.toList()));
        }

        System.out.println("\n=== 9. WHO IS EXCLUDED BECAUSE OF NO BLOOD PRESSURE? (GROUP B) ===");
        // Group B = adults with BMI and a valid weight but no systolic BP
        List<P> groupB = adBmi.stream()
                .filter(x -> Double.isNaN(x.sbp()))
                .filter(x -> !Double.isNaN(x.wt()) && x.wt() > 0)
                .collect(Collectors.toList());
        System.out.println("Group B size: " + groupB.size());
        System.out.println("  of which not in the BPXO file at all: " + groupB.stream().filter(x -> !x.inBpxo()).count());
        System.out.println("  of which in BPXO but SBP missing    : " + groupB.stream().filter(x -> x.inBpxo()).count());
        System.out.println("(Adults with BMI but weight missing/<=0: "
                + adBmi.stream().filter(x -> Double.isNaN(x.wt()) || x.wt() <= 0).count() + ")");

        System.out.println("\nComparison  SAMPLE A  vs  GROUP B:");
        for (var pair : List.of(Map.entry("SAMPLE A", finalA), Map.entry("GROUP B", groupB))) {
            List<P> g = pair.getValue();
            double[] ages = arr(g, P::age);
            double[] bm = arr(g, P::bmi);
            DescriptiveStatistics sa = new DescriptiveStatistics(ages);
            DescriptiveStatistics sb = new DescriptiveStatistics(bm);
            long fem = g.stream().filter(x -> x.sex() == 2.0).count();
            long old = g.stream().filter(x -> x.age() >= 60).count();
            System.out.printf("%-9s n=%d | mean age=%.1f | median age=%.1f | %% female=%.1f | %% age 60+=%.1f | mean BMI=%.2f | median BMI=%.2f%n",
                    pair.getKey(), g.size(), sa.getMean(), sa.getPercentile(50),
                    100.0 * fem / g.size(), 100.0 * old / g.size(), sb.getMean(), sb.getPercentile(50));
        }

        System.out.println("\n=== DONE. Copy everything above and send it. ===");
    }
}
