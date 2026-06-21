package org.unlaxer.kugiri.building.store;

import org.unlaxer.kugiri.building.hierarchy.HierarchyAssembler;
import org.unlaxer.kugiri.building.hierarchy.HierarchyAssembler.Row;
import org.unlaxer.kugiri.building.hierarchy.HierarchyNode;
import org.unlaxer.kugiri.building.identity.BuildingLexicon;

import java.util.List;

/**
 * 行（住所キー＋分解済み建物）→ 同定して木に集約 → ストアへ upsert する取込パイプライン。
 * ストアは差し替え可能（{@link InMemoryStore} / {@link PostgresStore}）。
 */
public final class PersistencePipeline {
    private PersistencePipeline() {}

    /** rows を集約してストアへ保存し、保存した住所数を返す。 */
    public static int ingest(List<Row> rows, BuildingLexicon lex, BuildingStore store) {
        List<HierarchyNode> roots = HierarchyAssembler.assemble(rows, lex);
        store.saveAll(roots);
        return roots.size();
    }
}
