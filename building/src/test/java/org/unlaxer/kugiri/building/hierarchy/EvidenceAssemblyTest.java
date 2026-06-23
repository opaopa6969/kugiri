package org.unlaxer.kugiri.building.hierarchy;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.unlaxer.kugiri.building.hierarchy.HierarchyAssembler.Row;
import org.unlaxer.kugiri.building.identity.BuildingLexicon;
import org.unlaxer.kugiri.building.identity.SampleCorpus;
import org.unlaxer.kugiri.building.parser.BuildingParser;

public class EvidenceAssemblyTest {

    private static BuildingLexicon lex;
    private static BuildingParser parser;

    @BeforeAll
    static void setup() {
        lex = BuildingLexicon.learn(SampleCorpus.names());
        parser = BuildingParser.of("lexicon", lex);
    }

    private List<Row> rows(String addr, String... tails) {
        List<Row> rs = new ArrayList<>();
        for (String t : tails) rs.add(new Row(addr, parser.parse(t)));
        return rs;
    }

    @Test
    public void collisionWithSharedRoomMergesToOneBuilding() {
        // 寮 と さくら寮：テキストは衝突(NEEDS_REVIEW)。同じ部屋201 → 略称＝同一建物
        HierarchyNode addr = HierarchyAssembler.assemble(
                rows("基地1番", "寮201号室", "さくら寮201号室"), lex).get(0);
        assertEquals(1, addr.children().size(), addr.pretty());
    }

    @Test
    public void collisionWithDisjointRoomsSplitsToTwoBuildings() {
        // 寮 と さくら寮：部屋が排他(101 vs 201) → 別建物
        HierarchyNode addr = HierarchyAssembler.assemble(
                rows("基地2番", "寮101号室", "さくら寮201号室"), lex).get(0);
        assertEquals(2, addr.children().size(), addr.pretty());
    }
}
