package org.example;

import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.CategoryChart;
import org.knowm.xchart.CategoryChartBuilder;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.BoxChart;
import org.knowm.xchart.BoxChartBuilder;
import org.knowm.xchart.style.Styler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Plots {

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

    // تابع کمکی برای ساخت هیستوگرام با XYChart
    static void saveHistogram(List<Double> data, double binWidth, String title,
                              String xLabel, String path) throws Exception {
        double min = data.stream().mapToDouble(Double::doubleValue).min().orElse(0);
        double max = data.stream().mapToDouble(Double::doubleValue).max().orElse(100);

        int numBins = (int) Math.ceil((max - min) / binWidth);
        int[] counts = new int[numBins];
        double[] centers = new double[numBins];

        for (int i = 0; i < numBins; i++) {
            centers[i] = min + (i + 0.5) * binWidth;
        }
        for (double v : data) {
            int idx = (int) ((v - min) / binWidth);
            if (idx >= numBins) idx = numBins - 1;
            if (idx < 0) idx = 0;
            counts[idx]++;
        }

        List<Integer> yData = new ArrayList<>();
        List<Double> xData = new ArrayList<>();
        for (int i = 0; i < numBins; i++) {
            xData.add(centers[i]);
            yData.add(counts[i]);
        }

        CategoryChart chart = new CategoryChartBuilder()
                .width(800).height(600)
                .title(title)
                .xAxisTitle(xLabel)
                .yAxisTitle("Frequency")
                .build();

        List<String> labels = new ArrayList<>();
        for (double c : centers) labels.add(String.format("%.0f", c));
        chart.addSeries("Frequency", labels, yData);

        BitmapEncoder.saveBitmap(chart, path, BitmapEncoder.BitmapFormat.PNG);
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
        final String SEX = findKey(firstRow, "RIAGENDR");

        List<Map<String, String>> clean = merged.stream()
                .filter(r -> parseDouble(r.get(AGE)) >= 20)
                .filter(r -> !Double.isNaN(parseDouble(r.get(BMI))))
                .filter(r -> !Double.isNaN(parseDouble(r.get(SBP))))
                .collect(Collectors.toList());

        List<Double> bmiVals = new ArrayList<>();
        List<Double> sbpVals = new ArrayList<>();
        List<Double> ageVals = new ArrayList<>();
        List<Double> maleBmi = new ArrayList<>();
        List<Double> femaleBmi = new ArrayList<>();

        for (Map<String, String> r : clean) {
            double bmi = parseDouble(r.get(BMI));
            double sbp = parseDouble(r.get(SBP));
            double age = parseDouble(r.get(AGE));
            double sex = parseDouble(r.get(SEX));

            bmiVals.add(bmi);
            sbpVals.add(sbp);
            ageVals.add(age);
            if (sex == 1.0) maleBmi.add(bmi);
            else if (sex == 2.0) femaleBmi.add(bmi);
        }

        // ===== ۱. هیستوگرام BMI =====
        saveHistogram(bmiVals, 2.0,
                "Distribution of BMI (NHANES 2021-2023)",
                "BMI (kg/m2)",
                "figures/bmi_histogram");
        System.out.println("Saved: figures/bmi_histogram.png");

        // ===== ۲. هیستوگرام SBP =====
        saveHistogram(sbpVals, 5.0,
                "Distribution of Systolic Blood Pressure (NHANES 2021-2023)",
                "Systolic BP (mmHg)",
                "figures/sbp_histogram");
        System.out.println("Saved: figures/sbp_histogram.png");

        // ===== ۳. Scatter سن و BMI =====
        XYChart scatter = new XYChartBuilder()
                .width(800).height(600)
                .title("Age vs BMI (NHANES 2021-2023)")
                .xAxisTitle("Age (years)")
                .yAxisTitle("BMI (kg/m2)")
                .build();
        scatter.getStyler().setMarkerSize(2);
        scatter.getStyler().setDefaultSeriesRenderStyle(org.knowm.xchart.XYSeries.XYSeriesRenderStyle.Scatter);
        scatter.addSeries("Age-BMI", ageVals, bmiVals);
        BitmapEncoder.saveBitmap(scatter, "figures/age_bmi_scatter", BitmapEncoder.BitmapFormat.PNG);
        System.out.println("Saved: figures/age_bmi_scatter.png");

        // ===== ۴. Boxplot BMI بر حسب جنس =====
        BoxChart boxChart = new BoxChartBuilder()
                .width(800).height(600)
                .title("BMI by Sex (1=Male, 2=Female)")
                .yAxisTitle("BMI (kg/m2)")
                .build();
        boxChart.addSeries("Male", maleBmi);
        boxChart.addSeries("Female", femaleBmi);
        BitmapEncoder.saveBitmap(boxChart, "figures/bmi_by_sex", BitmapEncoder.BitmapFormat.PNG);
        System.out.println("Saved: figures/bmi_by_sex.png");

        System.out.println("\nتمام نمودارها با موفقیت در پوشه figures ذخیره شدند.");
    }
}