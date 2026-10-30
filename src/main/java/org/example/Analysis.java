package org.example;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.descriptive.moment.Mean;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Analysis {

    static double parseDouble(String s) {
        if (s == null || s.isBlank() || s.equals(".") || s.equalsIgnoreCase("NA")) return Double.NaN;
        s = s.replace("\"", "").replace("'", "").trim().replace(",", ".");
        try { return Double.parseDouble(s); } catch (Exception e) { return Double.NaN; }
    }

    static String findKey(Map<String, String> row, String target) {
        for (String k : row.keySet()) {
            if (k == null) continue;
            String cleaned = k.replace("\uFEFF", "").trim();
            if (cleaned.equalsIgnoreCase(target)) return k;
        }
        return null;
    }

    public static void main(String[] args) throws Exception {
        String dataFolder = "data/";

        List<Map<String, String>> demo = LoadData.readCsv(dataFolder + "DEMO_L.csv");
        List<Map<String, String>> bmx  = LoadData.readCsv(dataFolder + "BMX_L.csv");
        List<Map<String, String>> bpxo = LoadData.readCsv(dataFolder + "BPXO_L.csv");

        Map<String, Map<String, String>> demoIdx = MergeData.indexBySeqn(demo);
        Map<String, Map<String, String>> bmxIdx  = MergeData.indexBySeqn(bmx);
        Map<String, Map<String, String>> bpxoIdx = MergeData.indexBySeqn(bpxo);

        List<Map<String, String>> merged = new ArrayList<>();
        for (String seqn : demoIdx.keySet()) {
            if (bmxIdx.containsKey(seqn) && bpxoIdx.containsKey(seqn)) {
                Map<String, String> row = new HashMap<>();
                row.putAll(demoIdx.get(seqn));
                row.putAll(bmxIdx.get(seqn));
                row.putAll(bpxoIdx.get(seqn));
                merged.add(row);
            }
        }

        Map<String, String> firstRow = merged.get(0);
        final String AGE = findKey(firstRow, "RIDAGEYR");
        final String BMI = findKey(firstRow, "BMXBMI");
        final String SBP = findKey(firstRow, "BPXOSY1");
        final String WT  = findKey(firstRow, "WTMEC2YR");

        List<Map<String, String>> adults = merged.stream()
                .filter(r -> parseDouble(r.get(AGE)) >= 20)
                .collect(Collectors.toList());

        List<Map<String, String>> clean = adults.stream()
                .filter(r -> !Double.isNaN(parseDouble(r.get(BMI))))
                .filter(r -> !Double.isNaN(parseDouble(r.get(SBP))))
                .collect(Collectors.toList());

        System.out.println("=== خلاصه جمعیت تحلیلی ===");
        System.out.println("تعداد کل بعد از ادغام: " + merged.size());
        System.out.println("بعد از فیلتر سن (>=20): " + adults.size());
        System.out.println("بعد از حذف گمشده‌ها: " + clean.size());
        System.out.println();

        double[] bmiVals = clean.stream().mapToDouble(r -> parseDouble(r.get(BMI))).toArray();
        double[] sbpVals = clean.stream().mapToDouble(r -> parseDouble(r.get(SBP))).toArray();
        double[] ageVals = clean.stream().mapToDouble(r -> parseDouble(r.get(AGE))).toArray();
        double[] weights = clean.stream().mapToDouble(r -> parseDouble(r.get(WT))).toArray();

        DescriptiveStatistics bmiStats = new DescriptiveStatistics(bmiVals);
        DescriptiveStatistics sbpStats = new DescriptiveStatistics(sbpVals);
        DescriptiveStatistics ageStats = new DescriptiveStatistics(ageVals);

        System.out.println("=== آمار توصیفی BMI (kg/m2) ===");
        System.out.println("میانگین: " + String.format("%.2f", bmiStats.getMean()));
        System.out.println("میانه: " + String.format("%.2f", bmiStats.getPercentile(50)));
        System.out.println("انحراف معیار: " + String.format("%.2f", bmiStats.getStandardDeviation()));
        System.out.println("Q1: " + String.format("%.2f", bmiStats.getPercentile(25)));
        System.out.println("Q3: " + String.format("%.2f", bmiStats.getPercentile(75)));
        System.out.println();

        System.out.println("=== آمار توصیفی SBP (mmHg) ===");
        System.out.println("میانگین: " + String.format("%.2f", sbpStats.getMean()));
        System.out.println("میانه: " + String.format("%.2f", sbpStats.getPercentile(50)));
        System.out.println("انحراف معیار: " + String.format("%.2f", sbpStats.getStandardDeviation()));
        System.out.println("Q1: " + String.format("%.2f", sbpStats.getPercentile(25)));
        System.out.println("Q3: " + String.format("%.2f", sbpStats.getPercentile(75)));
        System.out.println();

        double weightedAge = new Mean().evaluate(ageVals, weights);
        System.out.println("=== مقایسه سن خام و وزن‌دار ===");
        System.out.println("میانگین سن خام: " + String.format("%.2f", ageStats.getMean()));
        System.out.println("میانگین سن وزن‌دار: " + String.format("%.2f", weightedAge));
        System.out.println();

        double weightedBMI = new Mean().evaluate(bmiVals, weights);
        double weightedSBP = new Mean().evaluate(sbpVals, weights);
        System.out.println("=== مقایسه خام و وزن‌دار ===");
        System.out.println("میانگین خام BMI: " + String.format("%.2f", bmiStats.getMean())
                + "  |  وزن‌دار BMI: " + String.format("%.2f", weightedBMI));
        System.out.println("میانگین خام SBP: " + String.format("%.2f", sbpStats.getMean())
                + "  |  وزن‌دار SBP: " + String.format("%.2f", weightedSBP));
        System.out.println();

        String keySex = findKey(firstRow, "RIAGENDR");
        long maleCount = clean.stream().filter(r -> parseDouble(r.get(keySex)) == 1.0).count();
        long femaleCount = clean.stream().filter(r -> parseDouble(r.get(keySex)) == 2.0).count();
        System.out.println("=== جدول فراوانی جنس ===");
        System.out.println("مرد: " + maleCount + " نفر (" + String.format("%.2f", (maleCount / (double)clean.size()) * 100) + "%)");
        System.out.println("زن: " + femaleCount + " نفر (" + String.format("%.2f", (femaleCount / (double)clean.size()) * 100) + "%)");
        System.out.println();

        List<Map<String, String>> group1 = clean.stream()
                .filter(r -> parseDouble(r.get(AGE)) >= 20 && parseDouble(r.get(AGE)) <= 39).collect(Collectors.toList());
        List<Map<String, String>> group2 = clean.stream()
                .filter(r -> parseDouble(r.get(AGE)) >= 40 && parseDouble(r.get(AGE)) <= 59).collect(Collectors.toList());
        List<Map<String, String>> group3 = clean.stream()
                .filter(r -> parseDouble(r.get(AGE)) >= 60).collect(Collectors.toList());

        System.out.println("=== جدول فراوانی و آمار توصیفی BMI بر حسب گروه سنی ===");
        for (var entry : Map.of("20-39", group1, "40-59", group2, "60+", group3).entrySet()) {
            double[] vals = entry.getValue().stream().mapToDouble(r -> parseDouble(r.get(BMI))).toArray();
            DescriptiveStatistics ds = new DescriptiveStatistics(vals);
            System.out.printf("%s سال: تعداد = %d | میانگین BMI = %.2f | انحراف معیار = %.2f%n",
                    entry.getKey(), vals.length, ds.getMean(), ds.getStandardDeviation());
        }
    }
}