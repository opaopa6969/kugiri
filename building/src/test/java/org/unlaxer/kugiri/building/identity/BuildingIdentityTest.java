package org.unlaxer.kugiri.building.identity;

import static org.junit.jupiter.api.Assertions.*;
import static org.unlaxer.kugiri.building.identity.BuildingIdentity.Decision.*;

import java.util.*;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.unlaxer.kugiri.building.identity.BuildingIdentity.Decision;

public class BuildingIdentityTest {

    private static BuildingLexicon lex;

    @BeforeAll
    static void setup() { lex = BuildingLexicon.learn(SampleCorpus.names()); }

    private record Case(String a, String b, Decision truth) {}

    private static List<Case> cases() {
        return List.of(
                new Case("コーポ山田", "コ−ポ山田", SAME),                         // notation
                new Case("ライオンズマンション梅田", "ライオンズ梅田", SAME),          // 種別語省略=包含
                new Case("県立さくら施設さくら寮", "さくら寮", SAME),                // 正式名⊇略称
                new Case("白雲荘", "青雲荘", DISTINCT),                           // 対立的
                new Case("第一宿舎", "第二宿舎", DISTINCT),                        // enumerator
                new Case("ライオンズ梅田", "ライオンズ難波", DISTINCT),              // 地名で別建物
                new Case("サンハイツ", "サソハイツ", SAME),                        // typo
                new Case("白雲荘", "白雲荘B", SAME),                             // 棟差
                new Case("寮", "さくら寮", NEEDS_REVIEW));                       // 略しすぎ衝突
    }

    @Test
    public void contrastiveMatchesTruthOnAllCases() {
        for (Case c : cases()) {
            Decision got = BuildingIdentity.contrastive(c.a(), c.b(), lex).decision();
            assertEquals(c.truth(), got, "判定不一致: " + c.a() + " / " + c.b());
        }
    }

    @Test
    public void contrastiveBeatsEditDistanceBaselines() {
        int kugiri = 0, abs = 0, norm = 0;
        for (Case c : cases()) {
            if (BuildingIdentity.contrastive(c.a(), c.b(), lex).decision() == c.truth()) kugiri++;
            if (BuildingIdentity.editAbsolute(c.a(), c.b(), 4).decision() == c.truth()) abs++;
            if (BuildingIdentity.editNormalized(c.a(), c.b(), 0.76).decision() == c.truth()) norm++;
        }
        assertEquals(cases().size(), kugiri, "kugiri は全問正解のはず");
        assertTrue(kugiri > norm && norm >= abs,
                "kugiri > 正規化編集距離 ≥ 絶対編集距離 のはず: k=" + kugiri + " norm=" + norm + " abs=" + abs);
    }

    @Test
    public void abbreviationIsRecognizedByContainment() {
        // 正式名 ⊇ 略称 は包含で同一
        assertEquals(SAME, BuildingIdentity.contrastive("県立さくら施設さくら寮", "さくら寮", lex).decision());
        // 種別語の有無も包含の特例として同一
        assertEquals(SAME, BuildingIdentity.contrastive("ライオンズマンション梅田", "ライオンズ梅田", lex).decision());
    }

    @Test
    public void placeNameDistinguishesBuildings() {
        // 同ブランド・別地名は別建物
        assertEquals(DISTINCT, BuildingIdentity.contrastive("ライオンズ梅田", "ライオンズ難波", lex).decision());
    }

    @Test
    public void collisionGoesToReviewNotForcedDecision() {
        // 共有核が汎用語のみ＝テキストでは断定不能 → 要レビュー
        assertEquals(NEEDS_REVIEW, BuildingIdentity.contrastive("寮", "さくら寮", lex).decision());
    }

    @Test
    public void documentedDistanceFailuresAreFixed() {
        assertEquals(DISTINCT, BuildingIdentity.contrastive("白雲荘", "青雲荘", lex).decision());
        assertEquals(SAME, BuildingIdentity.editAbsolute("白雲荘", "青雲荘", 4).decision()); // 距離は誤る
    }
}
