package org.example;

import java.util.*;

public class MergeData {

    public static Map<String, Map<String, String>> indexBySeqn(List<Map<String, String>> rows) {
        Map<String, Map<String, String>> out = new HashMap<>();
        for (var row : rows) {
            out.put(row.get("SEQN"), row);
        }
        return out;
    }

    public static void main(String[] args) throws Exception {
        String dataFolder = "data/";

        // 1. خواندن فایل‌ها با استفاده از کلاس LoadData
        List<Map<String, String>> demo = LoadData.readCsv(dataFolder + "DEMO_L.csv");
        List<Map<String, String>> bmx  = LoadData.readCsv(dataFolder + "BMX_L.csv");
        List<Map<String, String>> bpxo = LoadData.readCsv(dataFolder + "BPXO_L.csv");

        // 2. ایندکس کردن فایل‌ها بر اساس شماره فرد (SEQN)
        Map<String, Map<String, String>> demoIdx = indexBySeqn(demo);
        Map<String, Map<String, String>> bmxIdx  = indexBySeqn(bmx);
        Map<String, Map<String, String>> bpxoIdx = indexBySeqn(bpxo);

        // 3. ادغام سه فایل
        List<Map<String, String>> merged = new ArrayList<>();
        for (String seqn : demoIdx.keySet()) {
            // فقط افرادی که در هر سه فایل وجود دارند
            if (bmxIdx.containsKey(seqn) && bpxoIdx.containsKey(seqn)) {
                Map<String, String> row = new HashMap<>();
                row.putAll(demoIdx.get(seqn));
                row.putAll(bmxIdx.get(seqn));
                row.putAll(bpxoIdx.get(seqn));
                merged.add(row);
            }
        }

        // 4. چاپ نتایج
        System.out.println("DEMO before merge: " + demo.size());
        System.out.println("BMX  before merge: " + bmx.size());
        System.out.println("BPXO before merge: " + bpxo.size());
        System.out.println("Merged after merge: " + merged.size());
    }
}