package org.unlaxer.kugiri.tagger;

import org.unlaxer.kugiri.feature.Features;
import org.unlaxer.kugiri.label.Bioes;
import org.unlaxer.kugiri.model.Example;
import java.util.*;

/**
 * 差し替え層の動作実証用の第2実装。平均化パーセプトロンだが、Viterbi を使わず
 * 左から貪欲(greedy)に decode する系列ラベラ。直前に予測したタグを素性に足し、
 * BIOES 合法性で候補を制限する。
 *
 * <p>{@link PerceptronTagger} より精度は劣るが、{@link SequenceTagger} を実装していれば
 * {@link AddressParser} の API を変えずに丸ごと差し替えられることを示す（CRF/BERT 実装の
 * 差し込み口が機能している証拠）。本番では MALLET CRF / ONNX BERT がこの位置に入る。
 */
public final class GreedyTagger implements SequenceTagger {
    private final List<String> labels = Bioes.tags();
    private final Map<String, Integer> labelIndex = new HashMap<>();
    private final int L = labels.size();
    private final Map<String, double[]> w = new HashMap<>(); // 素性 -> ラベル別重み

    public GreedyTagger() {
        for (int i = 0; i < L; i++) labelIndex.put(labels.get(i), i);
    }

    private static String[] split(String t) {
        if (t.equals("O")) return new String[]{"O", ""};
        int d = t.indexOf('-');
        return new String[]{t.substring(0, d), t.substring(d + 1)};
    }

    /** prev タグの後に cur タグが BIOES 的に合法か。 */
    private static boolean ok(String prev, String cur) {
        String[] pc = split(cur);
        if (pc[0].equals("O") || pc[0].equals("B") || pc[0].equals("S")) return true;
        String[] pp = split(prev);
        return (pp[0].equals("B") || pp[0].equals("I")) && pp[1].equals(pc[1]);
    }

    private static boolean startOk(String cur) {
        String p = split(cur)[0];
        return p.equals("O") || p.equals("B") || p.equals("S");
    }

    private double[] wv(String f) { return w.computeIfAbsent(f, k -> new double[L]); }

    /** 位置 i の素性に「直前タグ」を加えたもの。 */
    private List<String> feats(List<List<String>> base, int i, String prevTag) {
        List<String> f = new ArrayList<>(base.get(i));
        f.add("prev=" + prevTag);
        return f;
    }

    private int[] decode(List<List<String>> base) {
        int n = base.size();
        int[] path = new int[n];
        String prev = "<s>";
        for (int i = 0; i < n; i++) {
            List<String> f = feats(base, i, prev);
            int best = -1; double bestScore = Double.NEGATIVE_INFINITY;
            for (int k = 0; k < L; k++) {
                boolean legal = (i == 0) ? startOk(labels.get(k)) : ok(labels.get(path[i - 1]), labels.get(k));
                if (!legal) continue;
                double s = 0;
                for (String feat : f) { double[] v = w.get(feat); if (v != null) s += v[k]; }
                if (s > bestScore) { bestScore = s; best = k; }
            }
            if (best < 0) best = labelIndex.get("O");
            path[i] = best;
            prev = labels.get(best);
        }
        return path;
    }

    @Override
    public void fit(List<Example> data, int epochs) {
        List<List<List<String>>> feats = new ArrayList<>();
        List<int[]> golds = new ArrayList<>();
        for (Example ex : data) {
            feats.add(Features.sentFeatures(ex.chars()));
            int[] g = new int[ex.tags().size()];
            for (int i = 0; i < g.length; i++) g[i] = labelIndex.get(ex.tags().get(i));
            golds.add(g);
        }
        Map<String, double[]> acc = new HashMap<>();
        Random rng = new Random(0);
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < data.size(); i++) order.add(i);
        for (int e = 0; e < epochs; e++) {
            Collections.shuffle(order, rng);
            for (int idx : order) {
                List<List<String>> base = feats.get(idx);
                int[] gold = golds.get(idx);
                int[] pred = decode(base);
                String prevG = "<s>", prevP = "<s>";
                for (int i = 0; i < gold.length; i++) {
                    if (gold[i] != pred[i]) {
                        for (String f : feats(base, i, prevG)) wv(f)[gold[i]] += 1.0;
                        for (String f : feats(base, i, prevP)) wv(f)[pred[i]] -= 1.0;
                    }
                    prevG = labels.get(gold[i]);
                    prevP = labels.get(pred[i]);
                }
            }
            for (var en : w.entrySet()) {
                double[] a = acc.computeIfAbsent(en.getKey(), k -> new double[L]);
                for (int k = 0; k < L; k++) a[k] += en.getValue()[k];
            }
        }
        for (var en : acc.entrySet()) {
            double[] avg = new double[L];
            for (int k = 0; k < L; k++) avg[k] = en.getValue()[k] / epochs;
            w.put(en.getKey(), avg);
        }
    }

    @Override
    public List<String> predict(List<String> chars) {
        int[] p = decode(Features.sentFeatures(chars));
        List<String> out = new ArrayList<>(p.length);
        for (int idx : p) out.add(labels.get(idx));
        return out;
    }
}
