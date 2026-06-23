package org.unlaxer.kugiri.building.eval;

import org.unlaxer.kugiri.building.identity.BuildingIdentity.Decision;
import org.unlaxer.kugiri.building.identity.IdentityResolver;

import java.util.List;

/**
 * gold pairs に対して同定アルゴリズム（{@link IdentityResolver}）を採点する対決ハーネス。
 * BH の {@code GoldStandardEvaluator} に相当。SAME クラスの P/R/F1 と全体正答率・レビュー率を出す。
 */
public final class IdentityEvaluator {
    private IdentityEvaluator() {}

    /**
     * @param accuracy   全ペアで予測＝正解の割合
     * @param sameP      SAME 適合率（SAMEと言った中で本当にSAME）
     * @param sameR      SAME 再現率（本当のSAMEのうち拾えた割合）
     * @param sameF1     SAME の F1
     * @param reviewRate NEEDS_REVIEW を返した割合（人手へ回す率）
     */
    public record Score(String resolver, int n, double accuracy,
                        double sameP, double sameR, double sameF1, double reviewRate) {}

    public static Score evaluate(List<GoldPair> gold, IdentityResolver resolver) {
        int n = gold.size();
        int correct = 0, review = 0;
        long tp = 0, fp = 0, fn = 0;
        for (GoldPair g : gold) {
            Decision pred = resolver.decide(g.a(), g.b()).decision();
            if (pred == g.expected()) correct++;
            if (pred == Decision.NEEDS_REVIEW) review++;
            boolean predSame = pred == Decision.SAME;
            boolean goldSame = g.expected() == Decision.SAME;
            if (predSame && goldSame) tp++;
            else if (predSame) fp++;
            else if (goldSame) fn++;
        }
        double p = tp + fp == 0 ? 1 : (double) tp / (tp + fp);
        double r = tp + fn == 0 ? 1 : (double) tp / (tp + fn);
        double f1 = p + r == 0 ? 0 : 2 * p * r / (p + r);
        return new Score(resolver.name(), n, (double) correct / n, p, r, f1, (double) review / n);
    }

    public static String format(Score s) {
        return String.format("%-16s n=%d  正答率=%.3f  SAME[P=%.3f R=%.3f F1=%.3f]  レビュー率=%.3f",
                s.resolver(), s.n(), s.accuracy(), s.sameP(), s.sameR(), s.sameF1(), s.reviewRate());
    }
}
