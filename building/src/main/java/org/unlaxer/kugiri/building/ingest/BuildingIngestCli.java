package org.unlaxer.kugiri.building.ingest;

import org.unlaxer.kugiri.building.hierarchy.HierarchyAssembler.Row;
import org.unlaxer.kugiri.building.hierarchy.HierarchyNode;
import org.unlaxer.kugiri.building.identity.BuildingLexicon;
import org.unlaxer.kugiri.building.identity.SampleCorpus;
import org.unlaxer.kugiri.building.parser.BuildingParser;
import org.unlaxer.kugiri.building.store.BuildingStore;
import org.unlaxer.kugiri.building.store.InMemoryStore;
import org.unlaxer.kugiri.building.store.PersistencePipeline;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * 建物データ投入・検証 CLI（Phase2-c）。CSV（addressKey,tail）を読み、分解→同定→木集約→ストアへ。
 * 投入の健全性（件数・建物数・部屋数・要レビュー・空建物名）をレポートする。
 *
 * <pre>
 * 使い方:
 *   BuildingIngestCli --csv rows.csv [--encoding UTF-8] [--addr-col 0] [--tail-col 1]
 *                     [--parser lexicon|rule|perceptron] [--samples]
 *   --samples : 同梱サンプル(resources/sample/building_rows.csv)で試走
 *   実 KEN_ALL 等が CP932 のときは --encoding Windows-31J。生データはコミットしない。
 * </pre>
 */
public final class BuildingIngestCli {

    /** 投入の検証レポート。 */
    public record Report(int rows, int addresses, int buildings, int rooms,
                         int needsReview, int emptyName) {}

    public static void main(String[] args) throws IOException {
        Map<String, String> a = parseArgs(args);
        BuildingLexicon lex = BuildingLexicon.learn(SampleCorpus.names());
        BuildingParser parser = BuildingParser.of(a.getOrDefault("parser", "lexicon"), lex);
        int addrCol = Integer.parseInt(a.getOrDefault("addr-col", "0"));
        int tailCol = Integer.parseInt(a.getOrDefault("tail-col", "1"));

        Reader reader;
        if (a.containsKey("samples")) {
            InputStream in = BuildingIngestCli.class.getResourceAsStream("/sample/building_rows.csv");
            if (in == null) throw new IllegalStateException("sample resource not found");
            reader = new InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8);
        } else if (a.containsKey("csv")) {
            Charset cs = Charset.forName(a.getOrDefault("encoding", "UTF-8"));
            reader = Files.newBufferedReader(Path.of(a.get("csv")), cs);
        } else {
            System.out.println("--csv PATH か --samples を指定してください");
            return;
        }

        List<Row> rows = readRows(reader, addrCol, tailCol, parser);
        BuildingStore store = new InMemoryStore();
        Report report = ingest(rows, lex, store);

        System.out.println("=== 投入レポート ===");
        System.out.printf("行数=%d  住所=%d  建物=%d  部屋=%d  要レビュー=%d  建物名空=%d%n",
                report.rows(), report.addresses(), report.buildings(), report.rooms(),
                report.needsReview(), report.emptyName());
        System.out.println("\n=== サンプル住所ツリー ===");
        int shown = 0;
        for (String key : store instanceof InMemoryStore ? sampleKeys(rows) : List.<String>of()) {
            if (shown++ >= 3) break;
            store.address(key).ifPresent(t -> System.out.print(t.pretty()));
        }
        System.out.println("要レビュー: " + store.reviews());
    }

    /** rows を集約・保存し、検証レポートを返す。 */
    public static Report ingest(List<Row> rows, BuildingLexicon lex, BuildingStore store) {
        int empty = 0;
        for (Row r : rows) if (r.building().name().isEmpty()) empty++;
        int addresses = PersistencePipeline.ingest(rows, lex, store);
        int buildings = store.buildingCount();
        int rooms = 0, review = store.reviews().size();
        for (String k : sampleKeys(rows)) {
            HierarchyNode addr = store.address(k).orElse(null);
            if (addr != null) for (HierarchyNode b : addr.children()) rooms += b.leafCount();
        }
        return new Report(rows.size(), addresses, buildings, rooms, review, empty);
    }

    static List<Row> readRows(Reader reader, int addrCol, int tailCol, BuildingParser parser) throws IOException {
        List<Row> rows = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(reader)) {
            String line; boolean header = true;
            while ((line = br.readLine()) != null) {
                if (line.isBlank()) continue;
                if (header) { header = false; continue; }
                String[] f = line.split(",");
                if (Math.max(addrCol, tailCol) >= f.length) continue;
                rows.add(new Row(f[addrCol].strip(), parser.parse(f[tailCol].strip())));
            }
        }
        return rows;
    }

    private static List<String> sampleKeys(List<Row> rows) {
        LinkedHashSet<String> keys = new LinkedHashSet<>();
        for (Row r : rows) keys.add(r.addressKey());
        return new ArrayList<>(keys);
    }

    private static Map<String, String> parseArgs(String[] args) {
        Map<String, String> m = new LinkedHashMap<>();
        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("--")) {
                String k = args[i].substring(2);
                if (i + 1 < args.length && !args[i + 1].startsWith("--")) m.put(k, args[++i]);
                else m.put(k, "true");
            }
        }
        return m;
    }
}
