package org.unlaxer.kugiri.building.api;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.unlaxer.kugiri.building.hierarchy.HierarchyAssembler.Row;
import org.unlaxer.kugiri.building.identity.BuildingLexicon;
import org.unlaxer.kugiri.building.identity.SampleCorpus;
import org.unlaxer.kugiri.building.parser.BuildingParser;
import org.unlaxer.kugiri.building.store.InMemoryStore;
import org.unlaxer.kugiri.building.store.PersistencePipeline;

class BuildingFacadeTest {

    private static BuildingFacade facade;

    @BeforeAll
    static void setup() {
        BuildingLexicon lex = BuildingLexicon.learn(SampleCorpus.names());
        BuildingParser parser = BuildingParser.of("lexicon", lex);
        InMemoryStore store = new InMemoryStore();
        List<Row> rows = List.of(
                new Row("霞ヶ浦1番", parser.parse("ライオンズマンション梅田301号室")),
                new Row("霞ヶ浦1番", parser.parse("青雲荘B-101")),
                new Row("高松9番", parser.parse("杉の荘6号室")));
        PersistencePipeline.ingest(rows, lex, store);
        facade = new BuildingFacade(store, lex, "lexicon");
    }

    @Test
    void parseReturnsStructuredFields() {
        var p = facade.parse("府営柱本団地B2-407");
        assertEquals("府営柱本団地", p.name());
        assertEquals("B2", p.wing());
        assertEquals("407", p.room());
    }

    @Test
    void searchFindsBuildings() {
        assertFalse(facade.search("梅田").isEmpty());
        assertTrue(facade.search("").isEmpty());
    }

    @Test
    void addressReturnsNestedTree() {
        Map<String, Object> t = facade.address("霞ヶ浦1番");
        assertNotNull(t);
        assertEquals("ADDRESS", t.get("level"));
        assertTrue(t.containsKey("children"));
        assertNull(facade.address("存在しない住所"));
    }

    @Test
    void statsReportsCounts() {
        assertTrue((int) facade.stats().get("buildings") >= 3);
    }
}
