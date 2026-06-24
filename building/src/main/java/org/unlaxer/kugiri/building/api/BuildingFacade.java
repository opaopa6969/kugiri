package org.unlaxer.kugiri.building.api;

import org.unlaxer.kugiri.building.hierarchy.HierarchyNode;
import org.unlaxer.kugiri.building.identity.BuildingLexicon;
import org.unlaxer.kugiri.building.parser.BuildingParser;
import org.unlaxer.kugiri.building.parser.ParsedBuilding;
import org.unlaxer.kugiri.building.store.BuildingStore;

import java.util.*;

/**
 * REST から呼ぶ業務ロジック（HTTP 非依存・単体テスト可能）。
 * 解析/検索/住所ツリー/レビュー/統計を JSON 化しやすい Map・record で返す。
 */
public final class BuildingFacade {

    private final BuildingStore store;
    private final BuildingParser parser;

    public BuildingFacade(BuildingStore store, BuildingLexicon lex, String parserName) {
        this.store = store;
        this.parser = BuildingParser.of(parserName, lex);
    }

    /** 建物テールを分解。 */
    public ParsedBuilding parse(String tail) {
        return parser.parse(tail == null ? "" : tail);
    }

    /** 建物名の部分一致検索。 */
    public List<String> search(String q) {
        return q == null || q.isBlank() ? List.of() : store.searchBuildings(q);
    }

    /** 住所サブツリーを入れ子 Map で返す（無ければ null）。 */
    public Map<String, Object> address(String key) {
        return store.address(key).map(BuildingFacade::toMap).orElse(null);
    }

    /** 要レビュー一覧。 */
    public List<Map<String, String>> reviews() {
        List<Map<String, String>> out = new ArrayList<>();
        for (BuildingStore.Review r : store.reviews())
            out.add(Map.of("addressKey", r.addressKey(), "building", r.buildingLabel()));
        return out;
    }

    /** 統計。 */
    public Map<String, Object> stats() {
        return Map.of("buildings", store.buildingCount(), "reviews", store.reviews().size());
    }

    static Map<String, Object> toMap(HierarchyNode n) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("level", n.level().name());
        m.put("label", n.label());
        if (n.needsReview()) m.put("needsReview", true);
        if (!n.children().isEmpty()) {
            List<Map<String, Object>> kids = new ArrayList<>();
            for (HierarchyNode c : n.children()) kids.add(toMap(c));
            m.put("children", kids);
        }
        return m;
    }
}
