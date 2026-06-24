package org.unlaxer.kugiri.building.ingest;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.unlaxer.kugiri.building.hierarchy.HierarchyAssembler.Row;
import org.unlaxer.kugiri.building.identity.BuildingLexicon;
import org.unlaxer.kugiri.building.identity.SampleCorpus;
import org.unlaxer.kugiri.building.ingest.BuildingIngestCli.Report;
import org.unlaxer.kugiri.building.parser.BuildingParser;
import org.unlaxer.kugiri.building.store.InMemoryStore;

class BuildingIngestCliTest {

    private Reader sample() {
        InputStream in = getClass().getResourceAsStream("/sample/building_rows.csv");
        assertNotNull(in, "sample csv not found");
        return new InputStreamReader(in, StandardCharsets.UTF_8);
    }

    @Test
    void ingestsSampleAndReports() throws IOException {
        BuildingLexicon lex = BuildingLexicon.learn(SampleCorpus.names());
        BuildingParser parser = BuildingParser.of("lexicon", lex);
        List<Row> rows = BuildingIngestCli.readRows(sample(), 0, 1, parser);

        Report r = BuildingIngestCli.ingest(rows, lex, new InMemoryStore());

        assertEquals(11, r.rows());
        assertEquals(4, r.addresses());       // 霞ヶ浦/高松/本町/基地
        assertTrue(r.buildings() >= 6, "建物数が少なすぎ: " + r.buildings());
        assertTrue(r.rooms() >= r.buildings(), "部屋数が建物数以上のはず");
        assertEquals(0, r.emptyName());        // 全行に建物名がある
        // 本町5番: 寮/さくら寮 が同部屋201 → 証拠で1棟に併合される（building<行数）
        assertTrue(r.buildings() < r.rows());
    }
}
