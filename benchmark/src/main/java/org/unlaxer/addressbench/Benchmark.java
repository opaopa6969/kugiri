package org.unlaxer.addressbench;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * 住所分割ベンチマークの中核。各 {@link AddressSplitter} を行集合に適用し、
 * gold があれば残差の完全一致率、無くても建物検出率・実装間の一致率を測る。
 */
public final class Benchmark {
    private Benchmark() {}

    /** 1行：住所と（あれば）正解の建物残差。 */
    public record Row(String address, String gold) {
        public boolean hasGold() { return gold != null; }
    }

    /** 1実装の集計。 */
    public record Result(String splitter, int n, int withGold, int exact, int detected) {
        public double exactRate() { return withGold == 0 ? Double.NaN : (double) exact / withGold; }
        public double detectRate() { return n == 0 ? 0 : (double) detected / n; }
    }

    public static List<Result> run(List<Row> rows, List<AddressSplitter> splitters) {
        List<Result> results = new ArrayList<>();
        for (AddressSplitter s : splitters) {
            int exact = 0, withGold = 0, detected = 0;
            for (Row r : rows) {
                AddressSplitter.Split sp = s.split(r.address());
                if (!sp.buildingResidual().isEmpty()) detected++;
                if (r.hasGold()) {
                    withGold++;
                    if (sp.buildingResidual().equals(r.gold())) exact++;
                }
            }
            results.add(new Result(s.name(), rows.size(), withGold, exact, detected));
        }
        return results;
    }

    /** 全実装が同一残差を返した行の割合（実装間一致率）。 */
    public static double agreementRate(List<Row> rows, List<AddressSplitter> splitters) {
        if (splitters.size() < 2) return 1.0;
        int agree = 0;
        for (Row r : rows) {
            Set<String> residuals = new HashSet<>();
            for (AddressSplitter s : splitters) residuals.add(s.split(r.address()).buildingResidual());
            if (residuals.size() == 1) agree++;
        }
        return rows.isEmpty() ? 1.0 : (double) agree / rows.size();
    }

    /** 実装間で残差が割れた行を最大 limit 件、各実装の残差つきで返す（選定の判断材料）。 */
    public static List<String> disagreements(List<Row> rows, List<AddressSplitter> splitters, int limit) {
        List<String> out = new ArrayList<>();
        for (Row r : rows) {
            Map<String, String> byName = new LinkedHashMap<>();
            Set<String> distinct = new HashSet<>();
            for (AddressSplitter s : splitters) {
                String res = s.split(r.address()).buildingResidual();
                byName.put(s.name(), res);
                distinct.add(res);
            }
            if (distinct.size() > 1) {
                StringBuilder sb = new StringBuilder(r.address()).append('\n');
                byName.forEach((k, v) -> sb.append(String.format("    %-7s残差=[%s]%n", k, v)));
                if (r.hasGold()) sb.append("    gold   残差=[").append(r.gold()).append("]\n");
                out.add(sb.toString());
                if (out.size() >= limit) break;
            }
        }
        return out;
    }

    // ---- CSV 読み込み（quote 対応の簡易パーサ） ----

    public static List<Row> readCsv(Path path, Charset cs, int addrCol, int goldCol,
                                    boolean skipHeader, int limit) throws IOException {
        List<Row> rows = new ArrayList<>();
        try (BufferedReader br = Files.newBufferedReader(path, cs)) {
            String line; boolean first = true;
            while ((line = br.readLine()) != null) {
                if (first && skipHeader) { first = false; continue; }
                first = false;
                if (line.isBlank()) continue;
                List<String> f = splitCsv(line);
                if (addrCol >= f.size()) continue;
                String addr = f.get(addrCol).strip();
                if (addr.isEmpty()) continue;
                String gold = (goldCol >= 0 && goldCol < f.size()) ? f.get(goldCol).strip() : null;
                rows.add(new Row(addr, gold));
                if (limit > 0 && rows.size() >= limit) break;
            }
        }
        return rows;
    }

    static List<String> splitCsv(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQ = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQ && i + 1 < line.length() && line.charAt(i + 1) == '"') { cur.append('"'); i++; }
                else inQ = !inQ;
            } else if (c == ',' && !inQ) {
                out.add(cur.toString()); cur.setLength(0);
            } else cur.append(c);
        }
        out.add(cur.toString());
        return out;
    }

    public static String formatResults(List<Result> results) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-8s %6s %8s %10s %8s%n", "splitter", "n", "建物検出", "gold一致", "gold数"));
        for (Result r : results) {
            String exact = r.withGold() == 0 ? "  -  " : String.format("%.3f", r.exactRate());
            sb.append(String.format("%-8s %6d %7.1f%% %10s %8d%n",
                    r.splitter(), r.n(), r.detectRate() * 100, exact, r.withGold()));
        }
        return sb.toString();
    }
}
