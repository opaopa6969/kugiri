package org.unlaxer.kugiri.ingest;

import org.unlaxer.kugiri.abr.Abr;
import org.unlaxer.kugiri.abr.KenAll;
import org.unlaxer.kugiri.model.Component;
import org.unlaxer.kugiri.model.Example;
import org.unlaxer.kugiri.synth.Synth;
import org.unlaxer.kugiri.tagger.AddressParser;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * 実 ABR/KEN_ALL 取込 CLI（T1）。実ファイルから (KEN_ALL + ABR 4マスタ) を読み、
 * {@link Abr#buildRecords} で Component 列（弱教師ラベル）を生成して統計を出す。
 * 任意で hold-out 学習＋entity スパン評価まで行う。
 *
 * <pre>
 * 使い方:
 *   IngestCli --kenall KEN_ALL.CSV [--kenall-encoding CP932]
 *             --town mt_town_all.csv --blk mt_rsdtdsp_blk.csv
 *             --rsdt mt_rsdtdsp_rsdt.csv --parcel mt_parcel.csv
 *             [--max-units 4] [--train] [--augment 0]
 *
 * メモ:
 *   - 実 KEN_ALL は CP932。--kenall-encoding CP932（既定）。サンプルは UTF-8 なので --kenall-encoding UTF-8。
 *   - ABR 4マスタは UTF-8 前提。列名は版差があれば abr.Abr の get("列名") を直す。
 *   - 生データはリポジトリにコミットしない（サンプルのみ同梱）。
 * </pre>
 */
public final class IngestCli {

    public static void main(String[] args) throws IOException {
        Map<String, String> a = parseArgs(args);
        if (a.containsKey("help") || !a.containsKey("kenall")) { usage(); return; }

        Path kenall = Path.of(a.get("kenall"));
        Charset kenallCs = Charset.forName(a.getOrDefault("kenall-encoding", "Windows-31J"));
        Path town = req(a, "town"), blk = req(a, "blk"), rsdt = req(a, "rsdt"), parcel = req(a, "parcel");
        int maxUnits = Integer.parseInt(a.getOrDefault("max-units", "4"));

        // KEN_ALL 総行数（未マッチ率の分母）
        int kenAllRows;
        try (InputStream in = Files.newInputStream(kenall)) {
            kenAllRows = KenAll.load(in, kenallCs).size();
        }

        Abr.Result res;
        try (InputStream k = Files.newInputStream(kenall);
             InputStream t = Files.newInputStream(town);
             InputStream b = Files.newInputStream(blk);
             InputStream r = Files.newInputStream(rsdt);
             InputStream p = Files.newInputStream(parcel)) {
            res = Abr.buildRecords(k, kenallCs, t, b, r, p, maxUnits);
        }

        double missRate = kenAllRows == 0 ? 0 : (double) res.misses() / kenAllRows;
        System.out.println("=== 取込結果 ===");
        System.out.printf("KEN_ALL 行数        : %d%n", kenAllRows);
        System.out.printf("生成レコード(教師)  : %d%n", res.records().size());
        System.out.printf("未マッチ(町字未解決): %d  (未マッチ率 %.2f%%)%n",
                res.misses(), missRate * 100);

        if (res.records().isEmpty()) {
            System.out.println("レコードが0件。列名/エンコーディング/マスタ整合を確認してください。");
            return;
        }

        if (a.containsKey("train")) {
            int augment = Integer.parseInt(a.getOrDefault("augment", "0"));
            trainAndEvaluate(res.records(), augment);
        } else {
            System.out.println("\n（--train を付けると hold-out 学習＋スパン評価まで実行）");
        }
    }

    private static void trainAndEvaluate(List<List<Component>> records, int augmentPer) {
        Random rng = new Random(7);
        List<Example> data = new ArrayList<>();
        for (List<Component> c : records) {
            data.add(Synth.buildExample(c));
            for (int i = 0; i < augmentPer; i++) data.add(Synth.buildExample(Synth.augment(c, rng)));
        }
        AddressParser.Split sp = AddressParser.holdout(data, 0.8, 7);
        System.out.printf("%n=== 学習/評価 ===%ntrain=%d test=%d (augment x%d/件)%n",
                sp.train().size(), sp.test().size(), augmentPer);
        AddressParser parser = new AddressParser().fit(sp.train(), 10);
        System.out.print(parser.evaluateSpans(sp.test()));
        System.out.println("\n※ 弱教師ラベル自体のノイズは別途実ラベル hold-out（T2）で評価のこと。");
    }

    private static Path req(Map<String, String> a, String key) {
        String v = a.get(key);
        if (v == null) { usage(); throw new IllegalArgumentException("--" + key + " が必要です"); }
        return Path.of(v);
    }

    private static Map<String, String> parseArgs(String[] args) {
        Map<String, String> m = new LinkedHashMap<>();
        for (int i = 0; i < args.length; i++) {
            String s = args[i];
            if (s.startsWith("--")) {
                String key = s.substring(2);
                if (i + 1 < args.length && !args[i + 1].startsWith("--")) m.put(key, args[++i]);
                else m.put(key, "true");
            }
        }
        return m;
    }

    private static void usage() {
        System.out.println("""
            IngestCli --kenall KEN_ALL.CSV [--kenall-encoding CP932]
                      --town mt_town_all.csv --blk mt_rsdtdsp_blk.csv
                      --rsdt mt_rsdtdsp_rsdt.csv --parcel mt_parcel.csv
                      [--max-units 4] [--train] [--augment 0]
            サンプルでの試走例(UTF-8):
              IngestCli --kenall src/main/resources/sample_data/ken_all_sample.csv \\
                        --kenall-encoding UTF-8 \\
                        --town src/main/resources/sample_data/abr_town.csv \\
                        --blk  src/main/resources/sample_data/abr_blk.csv \\
                        --rsdt src/main/resources/sample_data/abr_rsdt.csv \\
                        --parcel src/main/resources/sample_data/abr_parcel.csv --train --augment 20
            """);
    }
}
