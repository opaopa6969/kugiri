package org.unlaxer.kugiri.building.store;

import org.unlaxer.kugiri.building.hierarchy.HierarchyNode;

import java.util.List;
import java.util.Optional;

/**
 * 建物階層の永続化層（差し替え可能）。
 *
 * <ul>
 *   <li>{@link InMemoryStore}  — 依存ゼロ。テスト/デモ/DBレス起動用。</li>
 *   <li>{@link PostgresStore}  — 本番。Flyway マイグレーション＋JDBC。要 Postgres。</li>
 * </ul>
 */
public interface BuildingStore extends AutoCloseable {

    /** 住所ルート（ADDRESS ノード）を1件 upsert（同一 addressKey は置換）。 */
    void save(HierarchyNode addressRoot);

    default void saveAll(List<HierarchyNode> roots) { roots.forEach(this::save); }

    /** addressKey の住所サブツリーを復元。 */
    Optional<HierarchyNode> address(String addressKey);

    /** 建物名の部分一致検索（代表名にヒット）。 */
    List<String> searchBuildings(String namePart);

    /** 要レビューの建物（型F=衝突の疑い等）。 */
    List<Review> reviews();

    /** 登録ずみ建物数。 */
    int buildingCount();

    @Override
    default void close() {}

    record Review(String addressKey, String buildingLabel) {}
}
