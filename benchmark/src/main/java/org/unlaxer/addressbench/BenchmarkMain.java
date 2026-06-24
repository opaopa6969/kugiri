package org.unlaxer.addressbench;

import org.unlaxer.addressbench.Benchmark.Row;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.*;

/**
 * 住所→(住所部分, 建物残差) ベンチマーク実行。
 *
 * <pre>
 * mvn -q -f benchmark/pom.xml exec:java -Dexec.mainClass=org.unlaxer.addressbench.BenchmarkMain \
 *   -Dexec.args="--csv /tmp/sample-addresses.csv --addr-col 2 --skip-header false --limit 2000"
 *   --gold-col N : 正解残差の列（あれば完全一致率を測る）
 *   無指定なら同梱の手ラベル gold（少数）で完全一致率＋実装間一致率を出す。
 * </pre>
 */
public final class BenchmarkMain {

    /** 手ラベルの少数 gold（bh-sample / indexing-sample 由来。残差＝建物以降、原文保持）。 */
    private static List<Row> builtinGold() {
        return List.of(
                new Row("東京都千代田区千代田1-1サンハイツ101", "サンハイツ101"),
                new Row("東京都千代田区千代田1-1サンハイツ202", "サンハイツ202"),
                new Row("大阪府大阪市北区梅田1-1グランドビル3階", "グランドビル3階"),
                new Row("大阪市北区同心１丁目９−４川上ハイツ−１０１", "川上ハイツ−１０１"),
                new Row("高槻市柱本新町１番府営柱本団地Ｂ２−４０７", "府営柱本団地Ｂ２−４０７"),
                new Row("大阪市淀川区西三国３丁目２４−３", ""),
                new Row("伊都郡かつらぎ町大字佐野８７８−３ガーデンハイツかつらぎ１０４", "ガーデンハイツかつらぎ１０４"),
                new Row("美方郡香美町香住区下岡５２１", ""));
    }

    public static void main(String[] args) throws Exception {
        Map<String, String> a = parseArgs(args);

        System.out.println("パーサ初期化中（kugiri 学習）...");
        AddressSplitter bh = new BhSplitter();
        AddressSplitter kugiri = new KugiriSplitter();
        AddressSplitter hybrid = new HybridSplitter(bh, kugiri);
        List<AddressSplitter> splitters = List.of(bh, kugiri, hybrid);

        List<Row> rows;
        String source;
        if (a.containsKey("csv")) {
            rows = Benchmark.readCsv(Path.of(a.get("csv")),
                    Charset.forName(a.getOrDefault("encoding", "UTF-8")),
                    Integer.parseInt(a.getOrDefault("addr-col", "0")),
                    Integer.parseInt(a.getOrDefault("gold-col", "-1")),
                    !"false".equals(a.get("skip-header")),
                    Integer.parseInt(a.getOrDefault("limit", "2000")));
            source = a.get("csv") + " (" + rows.size() + "行)";
        } else {
            rows = builtinGold();
            source = "同梱 gold (" + rows.size() + "行)";
        }

        System.out.println("=== 住所分割ベンチマーク : " + source + " ===\n");
        System.out.print(Benchmark.formatResults(Benchmark.run(rows, splitters)));
        System.out.printf("%n実装間一致率（全実装が同一残差）: %.3f%n",
                Benchmark.agreementRate(rows, splitters));

        System.out.println("\n=== 残差が割れた例（最大8件） ===");
        List<String> diffs = Benchmark.disagreements(rows, splitters, 8);
        if (diffs.isEmpty()) System.out.println("（全実装一致）");
        else diffs.forEach(System.out::println);

        System.out.println("※ gold列があれば完全一致率を測る。onigiri/ABRは重依存(DB)で別プロファイル予定。");
    }

    private static Map<String, String> parseArgs(String[] args) {
        Map<String, String> m = new LinkedHashMap<>();
        for (int i = 0; i < args.length; i++)
            if (args[i].startsWith("--")) {
                String k = args[i].substring(2);
                if (i + 1 < args.length && !args[i + 1].startsWith("--")) m.put(k, args[++i]);
                else m.put(k, "true");
            }
        return m;
    }
}
