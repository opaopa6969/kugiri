package org.unlaxer.kugiri.building.demo;

import org.unlaxer.kugiri.building.hierarchy.HierarchyAssembler.Row;
import org.unlaxer.kugiri.building.identity.BuildingLexicon;
import org.unlaxer.kugiri.building.identity.SampleCorpus;
import org.unlaxer.kugiri.building.parser.BuildingParser;
import org.unlaxer.kugiri.building.store.BuildingStore;
import org.unlaxer.kugiri.building.store.InMemoryStore;
import org.unlaxer.kugiri.building.store.PersistencePipeline;

import java.util.ArrayList;
import java.util.List;

/**
 * 永続化デモ（Phase1-4・DBレス＝InMemoryStore）。行→木→ストア upsert→検索/件数/レビュー。
 * 本番は PostgresStore.open(jdbcUrl,user,pw)（Flyway 適用）に差し替えるだけ。
 */
public final class StoreDemo {
    public static void main(String[] args) {
        BuildingLexicon lex = BuildingLexicon.learn(SampleCorpus.names());
        BuildingParser parser = BuildingParser.of("lexicon", lex);

        String[][] raw = {
                {"霞ヶ浦1番", "ライオンズマンション梅田301号室"},
                {"霞ヶ浦1番", "ライオンズ梅田-302"},
                {"霞ヶ浦1番", "青雲荘B-101"},
                {"高松9番", "府営柱本団地B2-407"},
                {"高松9番", "杉の荘6号室"},
                {"本町5番", "寮-201"},               // 識別固有名なし → 要レビュー候補
        };
        List<Row> rows = new ArrayList<>();
        for (String[] r : raw) rows.add(new Row(r[0], parser.parse(r[1])));

        try (BuildingStore store = new InMemoryStore()) {
            int saved = PersistencePipeline.ingest(rows, lex, store);
            System.out.printf("保存住所数=%d  建物数=%d%n", saved, store.buildingCount());

            System.out.println("\n検索『梅田』: " + store.searchBuildings("梅田"));
            System.out.println("検索『荘』: " + store.searchBuildings("荘"));

            System.out.println("\n霞ヶ浦1番 の木:");
            store.address("霞ヶ浦1番").ifPresent(a -> System.out.print(a.pretty()));

            System.out.println("要レビュー: " + store.reviews());
            System.out.println("\n※ 本番は PostgresStore.open(...) に差し替え（Flyway で db/migration を適用）。");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
