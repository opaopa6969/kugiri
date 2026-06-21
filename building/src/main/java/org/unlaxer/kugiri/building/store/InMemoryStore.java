package org.unlaxer.kugiri.building.store;

import org.unlaxer.kugiri.building.hierarchy.HierarchyNode;
import org.unlaxer.kugiri.building.hierarchy.HierarchyNode.Level;

import java.util.*;

/**
 * 依存ゼロのインメモリ建物ストア。テスト/デモ/DBレス起動用。
 * {@link PostgresStore} と同じ {@link BuildingStore} 契約を満たす。
 */
public final class InMemoryStore implements BuildingStore {

    private final Map<String, HierarchyNode> byAddress = new LinkedHashMap<>();

    @Override
    public void save(HierarchyNode addressRoot) {
        byAddress.put(addressRoot.label(), addressRoot); // addressKey 単位で置換 = upsert
    }

    @Override
    public Optional<HierarchyNode> address(String addressKey) {
        return Optional.ofNullable(byAddress.get(addressKey));
    }

    @Override
    public List<String> searchBuildings(String namePart) {
        List<String> out = new ArrayList<>();
        for (HierarchyNode addr : byAddress.values())
            for (HierarchyNode b : addr.children())
                if (b.level() == Level.BUILDING && b.label().contains(namePart)) out.add(b.label());
        return out;
    }

    @Override
    public List<Review> reviews() {
        List<Review> out = new ArrayList<>();
        for (HierarchyNode addr : byAddress.values())
            for (HierarchyNode b : addr.children())
                if (b.level() == Level.BUILDING && b.needsReview())
                    out.add(new Review(addr.label(), b.label()));
        return out;
    }

    @Override
    public int buildingCount() {
        int n = 0;
        for (HierarchyNode addr : byAddress.values()) n += addr.children().size();
        return n;
    }
}
