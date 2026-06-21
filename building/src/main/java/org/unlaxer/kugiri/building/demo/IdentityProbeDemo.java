package org.unlaxer.kugiri.building.demo;

import org.unlaxer.kugiri.building.identity.BuildingIdentity;
import org.unlaxer.kugiri.building.identity.BuildingIdentity.Decision;
import org.unlaxer.kugiri.building.identity.BuildingIdentity.Verdict;
import org.unlaxer.kugiri.building.identity.BuildingLexicon;
import org.unlaxer.kugiri.building.identity.SampleCorpus;

import java.util.List;

/**
 * 建物同定プロトタイプ（Phase0）。「同一住所の2名は同一建物（表記ゆれ/略称）か別建物か」を、
 * kugiri 統計（包含＋対立度）と編集距離ベースラインで判定して対決させる。
 */
public final class IdentityProbeDemo {

    private record Case(String a, String b, Decision truth, String note) {}

    public static void main(String[] args) {
        BuildingLexicon lex = BuildingLexicon.learn(SampleCorpus.names());

        List<Case> cases = List.of(
                new Case("コーポ山田", "コ−ポ山田", Decision.SAME, "型D notation"),
                new Case("ライオンズマンション梅田", "ライオンズ梅田", Decision.SAME, "型E 種別語の省略（包含）"),
                new Case("県立さくら施設さくら寮", "さくら寮", Decision.SAME, "型E 正式名⊇略称"),
                new Case("白雲荘", "青雲荘", Decision.DISTINCT, "型B 白雲/青雲＝対立的"),
                new Case("第一宿舎", "第二宿舎", Decision.DISTINCT, "型B enumerator"),
                new Case("ライオンズ梅田", "ライオンズ難波", Decision.DISTINCT, "型A/B 地名が識別"),
                new Case("サンハイツ", "サソハイツ", Decision.SAME, "typo 救済"),
                new Case("白雲荘", "白雲荘B", Decision.SAME, "B＝棟（同一建物の別棟）"),
                new Case("寮", "さくら寮", Decision.NEEDS_REVIEW, "型F 略しすぎ衝突")
        );

        System.out.println("=== 建物同定 対決（SAME=同一 / DISTINCT=別 / NEEDS_REVIEW=要レビュー） ===");
        System.out.printf("%-30s %-13s | %-14s %-12s %-12s%n",
                "ペア", "正解", "kugiri", "編集(絶対≤4)", "編集(正規化)");
        int kHit = 0, aHit = 0, nHit = 0;
        for (Case c : cases) {
            Verdict k = BuildingIdentity.contrastive(c.a(), c.b(), lex);
            Verdict abs = BuildingIdentity.editAbsolute(c.a(), c.b(), 4);
            Verdict norm = BuildingIdentity.editNormalized(c.a(), c.b(), 0.76);
            kHit += k.decision() == c.truth() ? 1 : 0;
            aHit += abs.decision() == c.truth() ? 1 : 0;
            nHit += norm.decision() == c.truth() ? 1 : 0;
            System.out.printf("%-30s %-13s | %-14s %-12s %-12s%n",
                    c.a() + " / " + c.b(), c.truth(),
                    mark(k.decision(), c.truth()), mark(abs.decision(), c.truth()), mark(norm.decision(), c.truth()));
        }
        int n = cases.size();
        System.out.printf("%n正答数: kugiri %d/%d, 編集(絶対) %d/%d, 編集(正規化) %d/%d%n",
                kHit, n, aHit, n, nHit, n);
        System.out.println("（編集距離は3値判定できず NEEDS_REVIEW を出せない＝型Fで必ず外す）");

        System.out.println("\n=== kugiri 判定の理由 ===");
        for (Case c : cases) {
            Verdict k = BuildingIdentity.contrastive(c.a(), c.b(), lex);
            System.out.printf("  %-30s -> %-12s : %s%n", c.a() + " / " + c.b(), k.decision(), k.reason());
        }
        System.out.println("\n※ 合成コーパスでの概念実証。型F/Cの最終確定は住所粒度・部屋集合（Phase1）と人手レビュー。");
    }

    private static String mark(Decision got, Decision truth) {
        return (got == truth ? "○ " : "× ") + got;
    }
}
