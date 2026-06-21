package org.unlaxer.kugiri.building.store;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.unlaxer.kugiri.building.hierarchy.HierarchyAssembler.Row;
import org.unlaxer.kugiri.building.identity.BuildingLexicon;
import org.unlaxer.kugiri.building.identity.SampleCorpus;
import org.unlaxer.kugiri.building.parser.BuildingParser;

public class InMemoryStoreTest {

    private static BuildingLexicon lex;
    private static BuildingParser parser;

    @BeforeAll
    static void setup() {
        lex = BuildingLexicon.learn(SampleCorpus.names());
        parser = BuildingParser.of("lexicon", lex);
    }

    private List<Row> rows(Object[]... pairs) {
        List<Row> rs = new ArrayList<>();
        for (Object[] p : pairs) rs.add(new Row((String) p[0], parser.parse((String) p[1])));
        return rs;
    }

    @Test
    public void ingestAndQuery() {
        BuildingStore store = new InMemoryStore();
        int saved = PersistencePipeline.ingest(rows(
                new Object[]{"霞ヶ浦1番", "ライオンズマンション梅田301号室"},
                new Object[]{"霞ヶ浦1番", "ライオンズ梅田-302"},
                new Object[]{"霞ヶ浦1番", "青雲荘B-101"},
                new Object[]{"高松9番", "府営柱本団地B2-407"},
                new Object[]{"高松9番", "杉の荘6号室"}), lex, store);

        assertEquals(2, saved);                 // 2 住所
        assertEquals(4, store.buildingCount()); // ライオンズ系1+青雲荘1+団地1+荘1
        assertFalse(store.searchBuildings("梅田").isEmpty());
        assertTrue(store.address("高松9番").isPresent());
        assertEquals(2, store.address("高松9番").get().children().size());
    }

    @Test
    public void upsertReplacesByAddressKey() {
        BuildingStore store = new InMemoryStore();
        PersistencePipeline.ingest(rows(new Object[]{"A町1", "杉の荘6号室"}), lex, store);
        assertEquals(1, store.buildingCount());
        // 同じ住所キーを入れ直すと置換（重複しない）
        PersistencePipeline.ingest(rows(
                new Object[]{"A町1", "杉の荘6号室"},
                new Object[]{"A町1", "白雲荘A-1"}), lex, store);
        assertEquals(2, store.buildingCount());
    }
}
