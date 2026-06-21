package org.unlaxer.kugiri.building.identity;

import static org.junit.jupiter.api.Assertions.*;
import static org.unlaxer.kugiri.building.identity.BuildingIdentity.Decision.*;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.unlaxer.kugiri.building.hierarchy.BuildingClusterer;

public class IdentityResolverTest {

    private static BuildingLexicon lex;

    @BeforeAll
    static void setup() { lex = BuildingLexicon.learn(SampleCorpus.names()); }

    @Test
    public void resolversAreSwappable() {
        var contrastive = IdentityResolver.of("contrastive", lex);
        var editNorm = IdentityResolver.of("edit-normalized", lex);
        var editAbs = IdentityResolver.of("edit-absolute", lex);

        // contrastive のみ 3値（NEEDS_REVIEW）を出せる
        assertEquals(NEEDS_REVIEW, contrastive.decide("寮", "さくら寮").decision());
        // 編集距離系は SAME/DISTINCT のみ
        assertNotEquals(NEEDS_REVIEW, editNorm.decide("寮", "さくら寮").decision());
        assertNotEquals(NEEDS_REVIEW, editAbs.decide("寮", "さくら寮").decision());

        assertEquals("contrastive", contrastive.name());
        assertEquals("edit-normalized", editNorm.name());
        assertEquals("edit-absolute", editAbs.name());
    }

    @Test
    public void clustererUsesPluggableResolver() {
        List<String> names = List.of("ライオンズマンション梅田", "ライオンズ梅田", "白雲荘");
        // contrastive: 表記ゆれを束ね 2 クラスタ（ライオンズ系 + 白雲荘）
        var byContrastive = BuildingClusterer.cluster(names, IdentityResolver.of("contrastive", lex));
        assertEquals(2, byContrastive.size());
        // 絶対編集距離: ライオンズ系を別物にしがちで 3 クラスタ（差し替えで結果が変わることの確認）
        var byEdit = BuildingClusterer.cluster(names, IdentityResolver.of("edit-absolute", lex));
        assertEquals(3, byEdit.size());
    }
}
