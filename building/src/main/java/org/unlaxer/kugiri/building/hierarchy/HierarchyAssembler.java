package org.unlaxer.kugiri.building.hierarchy;

import org.unlaxer.kugiri.building.hierarchy.EvidenceClusterer.Cluster;
import org.unlaxer.kugiri.building.hierarchy.EvidenceClusterer.NamedRooms;
import org.unlaxer.kugiri.building.hierarchy.HierarchyNode.Level;
import org.unlaxer.kugiri.building.identity.BuildingLexicon;
import org.unlaxer.kugiri.building.identity.IdentityResolver;
import org.unlaxer.kugiri.building.parser.ParsedBuilding;

import java.util.*;

/**
 * 行（住所キー＋分解済み建物）を 住所→建物→棟→階→部屋 の木に集約する。
 * 同一住所の複数建物名は {@link BuildingClusterer} で同定して建物数を決める。
 * 存在するレベルだけノード化する（可変深さ）。
 */
public final class HierarchyAssembler {
    private HierarchyAssembler() {}

    /** 1行＝住所キー＋その行の建物分解。 */
    public record Row(String addressKey, ParsedBuilding building) {}

    private static final String UNKNOWN = "（建物名不明）";

    /** 住所キーごとに ADDRESS ルートを作って返す（出現順）。 */
    public static List<HierarchyNode> assemble(List<Row> rows, BuildingLexicon lex) {
        Map<String, List<Row>> byAddr = new LinkedHashMap<>();
        for (Row r : rows) byAddr.computeIfAbsent(r.addressKey(), k -> new ArrayList<>()).add(r);

        List<HierarchyNode> roots = new ArrayList<>();
        for (var e : byAddr.entrySet()) {
            HierarchyNode addr = new HierarchyNode(Level.ADDRESS, e.getKey());

            // 建物名と、その名前で観測された部屋番号集合（証拠）
            List<NamedRooms> items = new ArrayList<>();
            for (Row r : e.getValue()) {
                if (r.building().name().isEmpty()) continue;
                Set<String> rooms = r.building().room().isEmpty() ? Set.of() : Set.of(r.building().room());
                items.add(new NamedRooms(r.building().name(), rooms));
            }
            List<Cluster> clusters = EvidenceClusterer.cluster(items, IdentityResolver.of("contrastive", lex));

            // 名前 → (代表名, レビュー印)
            Map<String, Cluster> byName = new HashMap<>();
            for (var c : clusters) for (String m : c.members()) byName.put(m, c);

            for (Row r : e.getValue()) {
                ParsedBuilding p = r.building();
                String canon = p.name().isEmpty() ? UNKNOWN : byName.get(p.name()).canonical();
                HierarchyNode b = addr.child(Level.BUILDING, canon);
                if (!p.name().isEmpty() && byName.get(p.name()).needsReview()) b.markReview();
                HierarchyNode cur = b;
                if (!p.wing().isEmpty())  cur = cur.child(Level.WING, p.wing());
                if (!p.floor().isEmpty()) cur = cur.child(Level.FLOOR, p.floor());
                if (!p.room().isEmpty())  cur = cur.child(Level.ROOM, p.room());
            }
            roots.add(addr);
        }
        return roots;
    }
}
