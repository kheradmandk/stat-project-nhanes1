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
        try {
            return Double.parseDouble(s);
        } catch (Exception e) {
            return Double.NaN;
        }
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

        // ===== آمار توصیفی BMI =====
        double[] bmiVals = clean.stream().mapToDouble(r -> parseDouble(r.get(BMI))).toArray();
        double[] sbpVals = clean.stream().mapToDouble(r -> parseDouble(r.get(SBP))).toArray();
        double[] weights = clean.stream().mapToDouble(r -> parseDouble(r.get(WT))).toArray();

        DescriptiveStatistics bmiStats = new DescriptiveStatistics(bmiVals);
        DescriptiveStatistics sbpStats = new DescriptiveStatistics(sbpVals);

        System.out.println("=== آمار توصیفی BMI (kg/m2) ===");
        System.out.println("میانگین خام: " + String.format("%.2f", bmiStats.getMean()));
        System.out.println("میانه: " + String.format("%.2f", bmiStats.getPercentile(50)));
        System.out.println("انحراف معیار: " + String.format("%.2f", bmiStats.getStandardDeviation()));
        System.out.println("Q1: " + String.format("%.2f", bmiStats.getPercentile(25)));
        System.out.println("Q3: " + String.format("%.2f", bmiStats.getPercentile(75)));
        System.out.println("حداقل: " + String.format("%.2f", bmiStats.getMin()));
        System.out.println("حداکثر: " + String.format("%.2f", bmiStats.getMax()));
        System.out.println();

        System.out.println("=== آمار توصیفی فشار خون سیستولیک (mmHg) ===");
        System.out.println("میانگین خام: " + String.format("%.2f", sbpStats.getMean()));
        System.out.println("میانه: " + String.format("%.2f", sbpStats.getPercentile(50)));
        System.out.println("انحراف معیار: " + String.format("%.2f", sbpStats.getStandardDeviation()));
        System.out.println("حداقل: " + String.format("%.2f", sbpStats.getMin()));
        System.out.println("حداکثر: " + String.format("%.2f", sbpStats.getMax()));
        System.out.println();

        // ===== میانگین وزن‌دار BMI =====
        List<Double> bmiL = new ArrayList<>();
        List<Double> sbpL = new ArrayList<>();
        List<Double> wtL  = new ArrayList<>();
        for (int i = 0; i < bmiVals.length; i++) {
            if (!Double.isNaN(weights[i]) && !Double.isNaN(bmiVals[i]) && !Double.isNaN(sbpVals[i])) {
                bmiL.add(bmiVals[i]);
                sbpL.add(sbpVals[i]);
                wtL.add(weights[i]);
            }
        }
        double[] bmiV2 = bmiL.stream().mapToDouble(Double::doubleValue).toArray();
        double[] sbpV2 = sbpL.stream().mapToDouble(Double::doubleValue).toArray();
        double[] wtV2  = wtL.stream().mapToDouble(Double::doubleValue).toArray();

        double weightedBMI = new Mean().evaluate(bmiV2, wtV2);
        double weightedSBP = new Mean().evaluate(sbpV2, wtV2);
        System.out.println("=== مقایسه خام و وزن‌دار ===");
        System.out.println("میانگین خام BMI: " + String.format("%.2f", bmiStats.getMean())
                + "  |  میانگین وزن‌دار BMI: " + String.format("%.2f", weightedBMI));
        System.out.println("میانگین خام SBP: " + String.format("%.2f", sbpStats.getMean())
                + "  |  میانگین وزن‌دار SBP: " + String.format("%.2f", weightedSBP));
        System.out.println();

        // ===== نسبت چاقی (BMI >= 30) =====
        int obeseCount = 0;
        double sumWtTotal = 0.0;
        double sumWtObese = 0.0;
        for (int i = 0; i < bmiV2.length; i++) {
            sumWtTotal += wtV2[i];
            if (bmiV2[i] >= 30.0) {
                obeseCount++;
                sumWtObese += wtV2[i];
            }
        }
        double crudePrev = (double) obeseCount / bmiV2.length;
        double weightedPrev = sumWtObese / sumWtTotal;

        System.out.println("=== نسبت چاقی (BMI >= 30) ===");
        System.out.println("تعداد چاق: " + obeseCount + " از " + bmiV2.length);
        System.out.println("شیوع خام: " + String.format("%.4f", crudePrev)
                + "  (یعنی " + String.format("%.2f", crudePrev * 100) + "%)");
        System.out.println("شیوع وزن‌دار: " + String.format("%.4f", weightedPrev)
                + "  (یعنی " + String.format("%.2f", weightedPrev * 100) + "%)");
        System.out.println("تفاوت: " + String.format("%.4f", crudePrev - weightedPrev));

        // =====  جدول فراوانی جنس (Sex) =====
        String keySex = findKey(firstRow, "RIAGENDR");
        long maleCount = clean.stream()
                .filter(r -> parseDouble(r.get(keySex)) == 1.0)
                .count();
        long femaleCount = clean.stream()
                .filter(r -> parseDouble(r.get(keySex)) == 2.0)
                .count();
        double totalCount = clean.size();

        System.out.println("\n=== جدول فراوانی جنس ===");
        System.out.println("مرد (کد 1): " + maleCount + " نفر (" + String.format("%.2f", (maleCount / totalCount) * 100) + " درصد)");
        System.out.println("زن (کد 2): " + femaleCount + " نفر (" + String.format("%.2f", (femaleCount / totalCount) * 100) + " درصد)");
        System.out.println("مجموع: " + (int)totalCount + " نفر");
    }
}