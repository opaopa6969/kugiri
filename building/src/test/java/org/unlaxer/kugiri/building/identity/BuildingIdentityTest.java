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
                new Case("コーポ山田", "コ−ポ山田", SAME),
                new Case("ライオンズマンション梅田", "ライオンズ梅田", SAME),
                new Case("白雲荘", "青雲荘", DISTINCT),
                new Case("第一宿舎", "第二宿舎", DISTINCT),
                new Case("サンハイツ", "サソハイツ", SAME),
                new Case("白雲荘", "白雲荘B", SAME));
    }

    @Test
    public void contrastiveMatchesTruthOnAllCases() {
        for (Case c : cases()) {
            Decision got = BuildingIdentity.contrastive(c.a(), c.b(), lex).decision();
            assertEquals(c.truth(), got, "kugiri 判定不一致: " + c.a() + " / " + c.b());
        }
    }

    @Test
    public void contrastiveBeatsAbsoluteEditDistance() {
        int kugiri = 0, abs = 0;
        for (Case c : cases()) {
            if (BuildingIdentity.contrastive(c.a(), c.b(), lex).decision() == c.truth()) kugiri++;
            if (BuildingIdentity.editAbsolute(c.a(), c.b(), 4).decision() == c.truth()) abs++;
        }
        assertEquals(cases().size(), kugiri, "kugiri は全問正解のはず");
        assertTrue(kugiri > abs, "kugiri が絶対編集距離を上回るはず: kugiri=" + kugiri + " abs=" + abs);
    }

    @Test
    public void documentedFailurePairsAreFixed() {
        // BH が距離では誤る2大ケースを kugiri は正す
        assertEquals(DISTINCT, BuildingIdentity.contrastive("白雲荘", "青雲荘", lex).decision());
        assertEquals(DISTINCT, BuildingIdentity.contrastive("第一宿舎", "第二宿舎", lex).decision());
        // 絶対編集距離(≤4)はこれらを誤って同一にする
        assertEquals(SAME, BuildingIdentity.editAbsolute("白雲荘", "青雲荘", 4).decision());
    }

    @Test
    public void typeWordIsLearnedFromCorpusNotHardcoded() {
        // 「マンション」はコーパス頻度から種別語として学習される（手書き辞書に依存しない）
        assertTrue(lex.isType("マンション"), "マンションが種別語として誘導されていない");
        // ブランドは生産的な固有名として識別される
        assertTrue(lex.isProductive("白雲") || lex.df("白雲") >= 2, "白雲が固有名として誘導されていない");
    }
}
