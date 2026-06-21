package org.unlaxer.kugiri.building.demo;

import org.unlaxer.kugiri.building.hierarchy.HierarchyAssembler;
import org.unlaxer.kugiri.building.hierarchy.HierarchyAssembler.Row;
import org.unlaxer.kugiri.building.hierarchy.HierarchyNode;
import org.unlaxer.kugiri.building.identity.BuildingLexicon;
import org.unlaxer.kugiri.building.identity.SampleCorpus;
import org.unlaxer.kugiri.building.parser.BuildingParser;
import org.unlaxer.kugiri.building.parser.ParsedBuilding;

import java.util.ArrayList;
import java.util.List;

/**
 * 行→木 集約デモ（Phase1-2/3）。同一住所に複数施設（型A）や表記ゆれ（型D/E）が
 * 混ざった行を、建物同定で束ねて 住所→建物→棟→階→部屋 の木に組む。
 */
public final class HierarchyDemo {
    public static void main(String[] args) {
        BuildingLexicon lex = BuildingLexicon.learn(SampleCorpus.names());
        BuildingParser parser = BuildingParser.of("lexicon", lex);

        // 住所キーと建物テールのペア（基地のような複数施設＋表記ゆれを含む）
        String[][] raw = {
                {"霞ヶ浦1番", "ライオンズマンション梅田301号室"},
                {"霞ヶ浦1番", "ライオンズ梅田-302"},        // 表記ゆれ（同一建物）
                {"霞ヶ浦1番", "青雲荘B-101"},               // 別建物・棟B
                {"霞ヶ浦1番", "青雲荘A-203"},               // 同建物・棟A
                {"高松9番", "府営柱本団地B2-407"},
                {"高松9番", "府営柱本団地B2-408"},
                {"高松9番", "杉の荘6号室"},                  // 別建物
        };

        List<Row> rows = new ArrayList<>();
        for (String[] r : raw) {
            ParsedBuilding pb = parser.parse(r[1]);
            rows.add(new Row(r[0], pb));
        }

        System.out.println("=== 行→木 集約（住所→建物→棟→階→部屋） ===");
        List<HierarchyNode> roots = HierarchyAssembler.assemble(rows, lex);
        for (HierarchyNode root : roots) {
            System.out.print(root.pretty());
            long buildings = root.children().size();
            System.out.printf("  → この住所の建物数 = %d / 部屋数 = %d%n%n", buildings, root.leafCount());
        }
        System.out.println("※ 同一住所の複数建物名は同定で束ねて建物数を決める（表記ゆれは1棟、別建物は別ノード）。");
    }
}
