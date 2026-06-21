package org.unlaxer.kugiri.building.demo;

import org.unlaxer.kugiri.building.identity.BuildingIdentity;
import org.unlaxer.kugiri.building.identity.BuildingIdentity.Decision;
import org.unlaxer.kugiri.building.identity.BuildingIdentity.Verdict;
import org.unlaxer.kugiri.building.identity.BuildingLexicon;
import org.unlaxer.kugiri.building.identity.SampleCorpus;

import java.util.List;

/**
 * 建物同定プロトタイプ（Phase0）。「同一住所に並ぶ2名は同一建物（表記ゆれ）か別建物か」を、
 * kugiri 統計（対立度）と編集距離ベースラインで判定して対決させる。
 */
public final class IdentityProbeDemo {

    private record Case(String a, String b, Decision truth, String note) {}

    public static void main(String[] args) {
        BuildingLexicon lex = BuildingLexicon.learn(SampleCorpus.names());

        List<Case> cases = List.of(
                new Case("コーポ山田", "コ−ポ山田", Decision.SAME, "notation（長音/ハイフン）"),
                new Case("ライオンズマンション梅田", "ライオンズ梅田", Decision.SAME, "種別語マンションの有無"),
                new Case("白雲荘", "青雲荘", Decision.DISTINCT, "白雲/青雲＝対立的な別建物"),
                new Case("第一宿舎", "第二宿舎", Decision.DISTINCT, "enumerator 違い"),
                new Case("サンハイツ", "サソハイツ", Decision.SAME, "typo（サン→サソ）"),
                new Case("白雲荘", "白雲荘B", Decision.SAME, "B＝棟（同一建物の別棟）")
        );

        System.out.println("=== 建物同定 対決（同一建物=SAME / 別建物=DISTINCT） ===");
        System.out.printf("%-26s %-12s | %-10s %-12s %-12s%n",
                "ペア", "正解", "kugiri", "編集距離(絶対≤4)", "編集距離(正規化)");
        int kugiriHit = 0, absHit = 0, normHit = 0;
        for (Case c : cases) {
            Verdict k = BuildingIdentity.contrastive(c.a(), c.b(), lex);
            Verdict abs = BuildingIdentity.editAbsolute(c.a(), c.b(), 4);
            Verdict norm = BuildingIdentity.editNormalized(c.a(), c.b(), 0.76);
            kugiriHit += k.decision() == c.truth() ? 1 : 0;
            absHit += abs.decision() == c.truth() ? 1 : 0;
            normHit += norm.decision() == c.truth() ? 1 : 0;
            System.out.printf("%-26s %-12s | %-10s %-12s %-12s%n",
                    c.a() + " / " + c.b(), c.truth(),
                    mark(k.decision(), c.truth()), mark(abs.decision(), c.truth()), mark(norm.decision(), c.truth()));
        }
        int n = cases.size();
        System.out.printf("%n正答数: kugiri %d/%d, 編集距離(絶対) %d/%d, 編集距離(正規化) %d/%d%n",
                kugiriHit, n, absHit, n, normHit, n);

        System.out.println("\n=== kugiri 判定の理由 ===");
        for (Case c : cases) {
            Verdict k = BuildingIdentity.contrastive(c.a(), c.b(), lex);
            System.out.printf("  %-26s -> %-8s : %s%n", c.a() + " / " + c.b(), k.decision(), k.reason());
        }
        System.out.println("\n※ 合成コーパスでの概念実証。gold pairs・実データでの対決は Phase2。");
    }

    private static String mark(Decision got, Decision truth) {
        return (got == truth ? "○ " : "× ") + got;
    }
}
