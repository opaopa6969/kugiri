package org.unlaxer.kugiri.tagger;

import org.unlaxer.kugiri.label.*;
import org.unlaxer.kugiri.model.*;
import org.unlaxer.kugiri.eval.SpanEval;
import java.util.*;

/**
 * 住所 tokenizer の窓口: 学習 / parse / 評価。
 *
 * <p>系列ラベラは {@link SequenceTagger} として差し替え可能（既定は純 JDK の
 * {@link PerceptronTagger}）。MALLET CRF / 文字BERT+ONNX 等の実装を注入しても
 * 本クラスの API は不変（CLAUDE.md の差し替え層方針）。
 */
public final class AddressParser {
    private final SequenceTagger tagger;

    /** 既定: 平均化構造化パーセプトロン。 */
    public AddressParser() { this(new PerceptronTagger()); }

    /** 任意のタガー実装を注入。 */
    public AddressParser(SequenceTagger tagger) { this.tagger = tagger; }

    public AddressParser fit(List<Example> data, int epochs) { tagger.fit(data, epochs); return this; }
    public AddressParser fit(List<Example> data) { return fit(data, 8); }

    /**
     * codepoint 列に対する信頼度つき推論（self-training 用）。
     * タガーが {@link ConfidenceTagger} を実装していない場合は非対応。
     */
    public Confidence predictConfidence(List<String> chars) {
        if (tagger instanceof ConfidenceTagger ct) return ct.predictWithConfidence(chars);
        throw new UnsupportedOperationException(
                tagger.getClass().getSimpleName() + " は信頼度（ConfidenceTagger）に未対応");
    }

    /** このタガーが信頼度を出せるか（self-training 可否）。 */
    public boolean supportsConfidence() { return tagger instanceof ConfidenceTagger; }

    /** 文字列 -> [(token,label)...] (Component 風)。 */
    public List<Component> parse(String text) {
        List<String> chars = CodePoints.of(text);
        List<String> tags = tagger.predict(chars);
        List<Component> out = new ArrayList<>();
        for (String[] tl : Bioes.decode(chars, tags)) out.add(new Component(tl[1], tl[0]));
        return out;
    }

    /** タグ正解率と、主要表層ラベルのトークンF1を文字列で返す。 */
    public String evaluate(List<Example> data) {
        long correct = 0, total = 0;
        Map<String, long[]> stat = new LinkedHashMap<>(); // label -> [tp,fp,fn]
        for (String lab : Labels.SURFACE) stat.put(lab, new long[3]);
        for (Example ex : data) {
            List<String> pred = tagger.predict(ex.chars());
            for (int i = 0; i < ex.tags().size(); i++) {
                String g = ex.tags().get(i), p = pred.get(i);
                total++; if (g.equals(p)) correct++;
                String gl = surface(g), pl = surface(p);
                if (gl != null && gl.equals(pl)) stat.get(gl)[0]++;
                else {
                    if (pl != null) stat.get(pl)[1]++;
                    if (gl != null) stat.get(gl)[2]++;
                }
            }
        }
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("tag accuracy = %.4f (%d/%d)%n", (double) correct / total, correct, total));
        sb.append(String.format("%-14s %6s %6s %6s%n", "label", "P", "R", "F1"));
        for (Map.Entry<String, long[]> e : stat.entrySet()) {
            long tp = e.getValue()[0], fp = e.getValue()[1], fn = e.getValue()[2];
            if (tp + fp + fn == 0) continue;
            double pr = tp + fp == 0 ? 1 : (double) tp / (tp + fp);
            double rc = tp + fn == 0 ? 1 : (double) tp / (tp + fn);
            double f1 = pr + rc == 0 ? 0 : 2 * pr * rc / (pr + rc);
            sb.append(String.format("%-14s %6.3f %6.3f %6.3f%n", e.getKey(), pr, rc, f1));
        }
        return sb.toString();
    }

    /** 全事例を推論し gold/pred のタグ列ペアを返す。 */
    private void collect(List<Example> data, List<List<String>> golds, List<List<String>> preds) {
        for (Example ex : data) {
            golds.add(ex.tags());
            preds.add(tagger.predict(ex.chars()));
        }
    }

    /**
     * entity-level（スパン一致）評価。per-label / micro / macro のスパン P/R/F1 と
     * token-level 混同行列（上位）を文字列で返す。合成 1.000 に依存しない指標。
     */
    public String evaluateSpans(List<Example> data) {
        List<List<String>> golds = new ArrayList<>(), preds = new ArrayList<>();
        collect(data, golds, preds);
        StringBuilder sb = new StringBuilder();
        sb.append("=== entity-level スパン評価 ===\n");
        sb.append(SpanEval.formatSpanReport(SpanEval.spanCounts(golds, preds)));
        sb.append("\n");
        sb.append(SpanEval.formatConfusion(SpanEval.confusion(golds, preds), 10));
        return sb.toString();
    }

    /** スパン Counts を構造化して返す（プログラムから使う用）。 */
    public Map<String, SpanEval.Counts> spanCounts(List<Example> data) {
        List<List<String>> golds = new ArrayList<>(), preds = new ArrayList<>();
        collect(data, golds, preds);
        return SpanEval.spanCounts(golds, preds);
    }

    /** train/test の hold-out 分割結果。 */
    public record Split(List<Example> train, List<Example> test) {}

    /** データを trainRatio:残り で hold-out 分割（seed 固定でシャッフル）。 */
    public static Split holdout(List<Example> data, double trainRatio, long seed) {
        List<Example> shuffled = new ArrayList<>(data);
        Collections.shuffle(shuffled, new Random(seed));
        int n = (int) Math.round(shuffled.size() * trainRatio);
        return new Split(new ArrayList<>(shuffled.subList(0, n)),
                         new ArrayList<>(shuffled.subList(n, shuffled.size())));
    }

    private static String surface(String tag) {
        if (tag.equals(Labels.OUTSIDE)) return null;
        return tag.substring(tag.indexOf('-') + 1);
    }
}
