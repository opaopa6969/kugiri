package org.unlaxer.kugiri.tagger;

import org.unlaxer.kugiri.eval.SpanEval;
import org.unlaxer.kugiri.model.Example;
import java.util.*;

/**
 * self-training（自己学習）ループ。少数の正解（seed）＋大量の未ラベル住所から、
 * 高信頼の予測だけを擬似ラベルとして取り込み再学習を反復する。
 *
 * <p>パーセプトロンは確率を出さないので、信頼度は max-marginal の文平均マージン
 * （{@link Confidence#meanMargin}）で代用する（HANDOFF T3 の (b) 案）。
 * 反復ごとに hold-out（test）のスパン F1 を測り、悪化したら採用せず止める（ドリフト監視）。
 */
public final class SelfTrainer {
    private SelfTrainer() {}

    /** 1反復ぶんの記録。 */
    public record Round(int iter, int pseudoAdded, double testMicroF1) {}

    /** self-training の結果。 */
    public record Result(AddressParser parser, List<Round> history, double seedOnlyF1, double bestF1) {}

    /**
     * @param seed             正解つき少数データ
     * @param unlabeled        未ラベル住所（codepoint 列のみ）
     * @param test             評価用 hold-out（正解つき）
     * @param marginThreshold  文平均マージンがこの値以上の予測のみ擬似ラベル採用
     * @param maxIters         最大反復数
     * @param epochs           各学習のエポック数
     */
    public static Result run(List<Example> seed, List<List<String>> unlabeled, List<Example> test,
                             double marginThreshold, int maxIters, int epochs) {
        AddressParser best = new AddressParser().fit(seed, epochs);
        double seedF1 = microF1(best, test);
        double bestF1 = seedF1;
        List<Round> history = new ArrayList<>();

        List<List<String>> pool = new ArrayList<>(unlabeled);
        List<Example> trainAll = new ArrayList<>(seed);

        for (int it = 1; it <= maxIters; it++) {
            AddressParser current = best; // 現在の最良で擬似ラベル付け
            List<Example> accepted = new ArrayList<>();
            List<List<String>> remaining = new ArrayList<>();
            for (List<String> chars : pool) {
                Confidence c = current.predictConfidence(chars);
                if (c.meanMargin() >= marginThreshold) accepted.add(new Example(chars, c.tags()));
                else remaining.add(chars);
            }
            if (accepted.isEmpty()) {
                history.add(new Round(it, 0, bestF1));
                break; // これ以上取り込めるものが無い
            }
            List<Example> candidateTrain = new ArrayList<>(trainAll);
            candidateTrain.addAll(accepted);
            AddressParser retrained = new AddressParser().fit(candidateTrain, epochs);
            double f1 = microF1(retrained, test);
            history.add(new Round(it, accepted.size(), f1));
            if (f1 + 1e-9 < bestF1) break; // ドリフト: 悪化したら採用せず終了
            best = retrained;
            bestF1 = f1;
            trainAll = candidateTrain;
            pool = remaining;
            if (pool.isEmpty()) break;
        }
        return new Result(best, history, seedF1, bestF1);
    }

    /** test のスパン micro-F1。 */
    public static double microF1(AddressParser parser, List<Example> test) {
        Map<String, SpanEval.Counts> stat = parser.spanCounts(test);
        long tp = 0, fp = 0, fn = 0;
        for (SpanEval.Counts c : stat.values()) { tp += c.tp; fp += c.fp; fn += c.fn; }
        double p = tp + fp == 0 ? 1 : (double) tp / (tp + fp);
        double r = tp + fn == 0 ? 1 : (double) tp / (tp + fn);
        return p + r == 0 ? 0 : 2 * p * r / (p + r);
    }
}
