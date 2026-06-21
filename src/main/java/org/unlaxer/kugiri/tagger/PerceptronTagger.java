package org.unlaxer.kugiri.tagger;

import org.unlaxer.kugiri.feature.Features;
import org.unlaxer.kugiri.label.Bioes;
import org.unlaxer.kugiri.model.Example;
import java.util.*;

/**
 * 平均化構造化パーセプトロン + Viterbi の系列ラベラ(線形CRFと同族・依存ゼロ)。
 *
 * <p>本番では MALLET の CRF や、文字BERTを ONNX 化して onnxruntime-java で推論する
 * スロットに差し替える前提。ここは「動く参照実装」。BIOES の遷移合法性を Viterbi で
 * マスクして不正系列(I-が単独で始まる等)を排除する。
 */
public final class PerceptronTagger {
    private final List<String> labels = Bioes.tags();
    private final Map<String, Integer> labelIndex = new HashMap<>();
    private final int L = labels.size();
    private final boolean[][] allowed = new boolean[L][L]; // 遷移合法性
    private final boolean[] allowedStart = new boolean[L];
    private final boolean[] allowedEnd = new boolean[L];

    private final Map<String, double[]> emis = new HashMap<>(); // 素性 -> ラベル別重み
    private double[][] trans = new double[L][L];
    private double[] start = new double[L];

    public PerceptronTagger() {
        for (int i = 0; i < L; i++) labelIndex.put(labels.get(i), i);
        for (int j = 0; j < L; j++) {
            allowedStart[j] = startOk(labels.get(j));
            allowedEnd[j] = endOk(labels.get(j));
            for (int k = 0; k < L; k++) allowed[j][k] = transOk(labels.get(j), labels.get(k));
        }
    }

    // --- BIOES 合法性 ---
    private static String[] split(String t) {
        if (t.equals("O")) return new String[]{"O", ""};
        int d = t.indexOf('-');
        return new String[]{t.substring(0, d), t.substring(d + 1)};
    }
    private static boolean startOk(String cur) { String p = split(cur)[0]; return p.equals("O") || p.equals("B") || p.equals("S"); }
    private static boolean endOk(String cur) { String p = split(cur)[0]; return p.equals("O") || p.equals("E") || p.equals("S"); }
    private static boolean transOk(String prev, String cur) {
        String[] pp = split(prev), pc = split(cur);
        String ppos = pp[0], cpos = pc[0];
        if (cpos.equals("O") || cpos.equals("B") || cpos.equals("S")) return true;
        // I/E は同ラベルの B/I の後のみ
        boolean cont = (ppos.equals("B") || ppos.equals("I")) && pp[1].equals(pc[1]);
        return cont;
    }

    private double[] w(String feat) { return emis.computeIfAbsent(feat, k -> new double[L]); }

    public void fit(List<Example> data, int epochs) {
        // 素性を事前計算
        List<List<List<String>>> feats = new ArrayList<>(data.size());
        List<int[]> golds = new ArrayList<>(data.size());
        for (Example ex : data) {
            feats.add(Features.sentFeatures(ex.chars()));
            int[] g = new int[ex.tags().size()];
            for (int i = 0; i < g.length; i++) g[i] = labelIndex.get(ex.tags().get(i));
            golds.add(g);
        }
        // 平均化(エポック毎の重みを加算 -> 平均)
        Map<String, double[]> emisAcc = new HashMap<>();
        double[][] transAcc = new double[L][L];
        double[] startAcc = new double[L];
        Random rng = new Random(0);
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < data.size(); i++) order.add(i);

        for (int e = 0; e < epochs; e++) {
            Collections.shuffle(order, rng);
            for (int idx : order) {
                List<List<String>> F = feats.get(idx);
                int[] gold = golds.get(idx);
                int[] pred = viterbi(F);
                if (!Arrays.equals(pred, gold)) update(F, gold, pred);
            }
            // accumulate
            for (Map.Entry<String, double[]> en : emis.entrySet()) {
                double[] acc = emisAcc.computeIfAbsent(en.getKey(), k -> new double[L]);
                for (int k = 0; k < L; k++) acc[k] += en.getValue()[k];
            }
            for (int a = 0; a < L; a++) {
                startAcc[a] += start[a];
                for (int b = 0; b < L; b++) transAcc[a][b] += trans[a][b];
            }
        }
        // 平均を確定
        for (Map.Entry<String, double[]> en : emisAcc.entrySet()) {
            double[] avg = new double[L];
            for (int k = 0; k < L; k++) avg[k] = en.getValue()[k] / epochs;
            emis.put(en.getKey(), avg);
        }
        for (int a = 0; a < L; a++) {
            start[a] = startAcc[a] / epochs;
            for (int b = 0; b < L; b++) trans[a][b] = transAcc[a][b] / epochs;
        }
    }

    private void update(List<List<String>> F, int[] gold, int[] pred) {
        int n = gold.length;
        for (int i = 0; i < n; i++) {
            if (gold[i] == pred[i]) continue;
            for (String f : F.get(i)) {
                double[] wv = w(f);
                wv[gold[i]] += 1.0;
                wv[pred[i]] -= 1.0;
            }
        }
        start[gold[0]] += 1.0; start[pred[0]] -= 1.0;
        for (int i = 1; i < n; i++) {
            trans[gold[i - 1]][gold[i]] += 1.0;
            trans[pred[i - 1]][pred[i]] -= 1.0;
        }
    }

    private double emission(List<String> activeFeats, int label) {
        double s = 0;
        for (String f : activeFeats) {
            double[] wv = emis.get(f);
            if (wv != null) s += wv[label];
        }
        return s;
    }

    private int[] viterbi(List<List<String>> F) {
        int n = F.size();
        double[][] score = new double[n][L];
        int[][] back = new int[n][L];
        double NEG = Double.NEGATIVE_INFINITY;
        double[] emit0 = new double[L];
        for (int k = 0; k < L; k++) emit0[k] = emission(F.get(0), k);
        for (int k = 0; k < L; k++)
            score[0][k] = allowedStart[k] ? start[k] + emit0[k] : NEG;
        for (int i = 1; i < n; i++) {
            double[] emit = new double[L];
            for (int k = 0; k < L; k++) emit[k] = emission(F.get(i), k);
            for (int k = 0; k < L; k++) {
                double best = NEG; int arg = 0;
                for (int j = 0; j < L; j++) {
                    if (!allowed[j][k] || score[i - 1][j] == NEG) continue;
                    double v = score[i - 1][j] + trans[j][k];
                    if (v > best) { best = v; arg = j; }
                }
                score[i][k] = (best == NEG) ? NEG : best + emit[k];
                back[i][k] = arg;
            }
        }
        double best = NEG; int last = 0;
        for (int k = 0; k < L; k++) {
            if (!allowedEnd[k] || score[n - 1][k] == NEG) continue;
            if (score[n - 1][k] > best) { best = score[n - 1][k]; last = k; }
        }
        int[] path = new int[n];
        path[n - 1] = last;
        for (int i = n - 1; i > 0; i--) path[i - 1] = back[i][path[i]];
        return path;
    }

    public List<String> predict(List<String> chars) {
        List<List<String>> F = Features.sentFeatures(chars);
        int[] p = viterbi(F);
        List<String> out = new ArrayList<>(p.length);
        for (int idx : p) out.add(labels.get(idx));
        return out;
    }
}
