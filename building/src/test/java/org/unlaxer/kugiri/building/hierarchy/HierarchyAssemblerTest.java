package org.unlaxer.kugiri.building.hierarchy;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.unlaxer.kugiri.building.hierarchy.HierarchyAssembler.Row;
import org.unlaxer.kugiri.building.hierarchy.HierarchyNode.Level;
import org.unlaxer.kugiri.building.identity.BuildingLexicon;
import org.unlaxer.kugiri.building.identity.SampleCorpus;
import org.unlaxer.kugiri.building.parser.BuildingParser;

public class HierarchyAssemblerTest {

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
    public void variantsMergeDistinctBuildingsSeparate() {
        List<Row> rows = rows("霞ヶ浦1番",
                "ライオンズマンション梅田301号室",
                "ライオンズ梅田-302",   // 表記ゆれ → 同一建物
                "青雲荘B-101",          // 別建物・棟B
                "青雲荘A-203");         // 同建物・棟A
        HierarchyNode addr = HierarchyAssembler.assemble(rows, lex).get(0);

        // 建物は2つ（ライオンズ系1 + 青雲荘1）
        assertEquals(2, addr.children().size(), addr.pretty());
        // 表記ゆれが1建物に束ねられ、部屋が2つ
        HierarchyNode lions = addr.children().stream()
                .filter(b -> b.label().contains("ライオンズ")).findFirst().orElseThrow();
        assertEquals(2, lions.leafCount());
        // 青雲荘は棟A/Bを持つ
        HierarchyNode seiun = addr.children().stream()
                .filter(b -> b.label().contains("青雲")).findFirst().orElseThrow();
        Set<String> wings = new HashSet<>();
        for (HierarchyNode w : seiun.children()) if (w.level() == Level.WING) wings.add(w.label());
        assertEquals(Set.of("A", "B"), wings);
    }

    @Test
    public void distinctBuildingsAtSameAddressAreCounted() {
        List<Row> rows = rows("高松9番",
                "府営柱本団地B2-407", "府営柱本団地B2-408", "杉の荘6号室");
        HierarchyNode addr = HierarchyAssembler.assemble(rows, lex).get(0);
        assertEquals(2, addr.children().size()); // 団地 + 荘
        assertEquals(3, addr.leafCount());       // 407,408,6
    }

    @Test
    public void variableDepthOmitsMissingLevels() {
        // 棟も階も無い → 建物直下に部屋
        List<Row> rows = rows("本町1番", "杉の荘6号室");
        HierarchyNode addr = HierarchyAssembler.assemble(rows, lex).get(0);
        HierarchyNode building = addr.children().get(0);
        assertEquals(1, building.children().size());
        assertEquals(Level.ROOM, building.children().get(0).level());
    }
}
