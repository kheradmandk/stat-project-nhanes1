package org.example;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.FileReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LoadData {

    public static List<Map<String, String>> readCsv(String path) throws Exception {
        try (Reader reader = new FileReader(path);
             CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT)) {

            List<CSVRecord> records = parser.getRecords();
            if (records.isEmpty()) return new ArrayList<>();

            // خواندن سربرگ
            CSVRecord headerRecord = records.get(0);
            List<String> headers = new ArrayList<>();
            for (String h : headerRecord) {
                headers.add(h.trim().replace("\uFEFF", ""));
            }

            // بررسی اینکه داده‌ها ستون اضافی (شماره ردیف) دارند یا نه
            CSVRecord firstData = records.get(1);
            boolean hasExtraColumn = firstData.size() > headers.size();

            // ساخت لیست رکوردها
            List<Map<String, String>> rows = new ArrayList<>();
            for (int i = 1; i < records.size(); i++) {
                CSVRecord record = records.get(i);
                Map<String, String> map = new HashMap<>();
                for (int j = 0; j < headers.size(); j++) {
                    int idx = j + (hasExtraColumn ? 1 : 0);
                    String val = (idx < record.size()) ? record.get(idx) : "";
                    map.put(headers.get(j), val);
                }
                rows.add(map);
            }
            return rows;
        }
    }

    public static void main(String[] args) throws Exception {
        String dataFolder = "data/";

        List<Map<String, String>> demo = readCsv(dataFolder + "DEMO_L.csv");
        List<Map<String, String>> bmx  = readCsv(dataFolder + "BMX_L.csv");
        List<Map<String, String>> bpxo = readCsv(dataFolder + "BPXO_L.csv");

        System.out.println("DEMO: " + demo.size() + " records");
        System.out.println("BMX : " + bmx.size() + " records");
        System.out.println("BPXO: " + bpxo.size() + " records");
    }
}