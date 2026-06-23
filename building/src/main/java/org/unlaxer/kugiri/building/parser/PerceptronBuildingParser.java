package org.unlaxer.kugiri.building.parser;

import org.unlaxer.kugiri.building.parser.BuildingTailSynth.Tagged;

import java.util.*;

/**
 * kugiri 統計版の建物テール・パーサー（系列ラベリング）。建物名/棟/階/部屋を BIOES で
 * codepoint ごとにラベル付けする、平均化パーセプトロン＋貪欲デコード（BIOES 合法性つき）。
 * 合成データ（{@link BuildingTailSynth}）で自己学習する。keyword 辞書に依存しない。
 *
 * <p>BH の keyword-lr もぐら叩きパーサに対する「統計で境界を出す」代替の実証。
 */
public class PerceptronBuildingParser implements BuildingParser {

    private static final String[] LABELS_BASE = {"建物名", "棟", "階", "部屋"};
    private final List<String> labels = new ArrayList<>();
    private final Map<String, Integer> labelIndex = new HashMap<>();
    private final Map<String, double[]> w = new HashMap<>();
    private final int L;

    public PerceptronBuildingParser() {
        labels.add("O");
        for (String b : LABELS_BASE) for (String p : new String[]{"B", "I", "E", "S"}) labels.add(p + "-" + b);
        L = labels.size();
        for (int i = 0; i < L; i++) labelIndex.put(labels.get(i), i);
        fit(BuildingTailSynth.makeDataset(3000, 7), 8);
    }

    @Override
    public String name() { return "perceptron"; }

    // ---- 特徴 ----
    private static String type(String c) {
        int cp = c.codePointAt(0);
        if (cp >= '0' && cp <= '9') return "d";
        if ((cp >= 'A' && cp <= 'Z') || (cp >= 'a' && cp <= 'z')) return "a";
        if (cp == '-') return "h";
        Character.UnicodeBlock b = Character.UnicodeBlock.of(cp);
        if (b == Character.UnicodeBlock.KATAKANA) return "K";
        if (b == Character.UnicodeBlock.HIRAGANA) return "H";
        if (b == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS) return "C";
        return "o";
    }

    private static List<String> feats(List<String> cs, int i, String prevTag) {
        List<String> f = new ArrayList<>();
        f.add("b");
        f.add("c=" + cs.get(i));
        f.add("t=" + type(cs.get(i)));
        f.add("c-1=" + (i > 0 ? cs.get(i - 1) : "^"));
        f.add("c+1=" + (i + 1 < cs.size() ? cs.get(i + 1) : "$"));
        f.add("t-1=" + (i > 0 ? type(cs.get(i - 1)) : "^"));
        f.add("t+1=" + (i + 1 < cs.size() ? type(cs.get(i + 1)) : "$"));
        f.add("p=" + prevTag);
        f.add("end=" + Math.min(cs.size() - 1 - i, 4)); // 末尾からの距離（部屋/階の手掛かり）
        return f;
    }

    private static String[] split(String t) {
        int d = t.indexOf('-');
        return d < 0 ? new String[]{t, ""} : new String[]{t.substring(0, d), t.substring(d + 1)};
    }

    private boolean legal(String prev, String cur) {
        String[] pc = split(cur);
        if (pc[0].equals("O") || pc[0].equals("B") || pc[0].equals("S")) return true;
        String[] pp = split(prev);
        return (pp[0].equals("B") || pp[0].equals("I")) && pp[1].equals(pc[1]);
    }

    private double[] wv(String f) { return w.computeIfAbsent(f, k -> new double[L]); }

    private int[] decode(List<String> cs) {
        int n = cs.size();
        int[] path = new int[n];
        String prev = "^";
        for (int i = 0; i < n; i++) {
            List<String> f = feats(cs, i, prev);
            int best = -1; double bestScore = Double.NEGATIVE_INFINITY;
            for (int k = 0; k < L; k++) {
                if (i == 0 ? !legal("^", labels.get(k)) : !legal(labels.get(path[i - 1]), labels.get(k))) continue;
                double s = 0;
                for (String ft : f) { double[] v = w.get(ft); if (v != null) s += v[k]; }
                if (s > bestScore) { bestScore = s; best = k; }
            }
            path[i] = best < 0 ? labelIndex.get("O") : best;
            prev = labels.get(path[i]);
        }
        return path;
    }

    private void fit(List<Tagged> data, int epochs) {
        Map<String, double[]> acc = new HashMap<>();
        Random rng = new Random(0);
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < data.size(); i++) order.add(i);
        for (int e = 0; e < epochs; e++) {
            Collections.shuffle(order, rng);
            for (int idx : order) {
                Tagged ex = data.get(idx);
                int[] gold = new int[ex.tags().size()];
                for (int i = 0; i < gold.length; i++) gold[i] = labelIndex.get(ex.tags().get(i));
                int[] pred = decode(ex.chars());
                String pg = "^", pp = "^";
                for (int i = 0; i < gold.length; i++) {
                    if (gold[i] != pred[i]) {
                        for (String f : feats(ex.chars(), i, pg)) wv(f)[gold[i]] += 1.0;
                        for (String f : feats(ex.chars(), i, pp)) wv(f)[pred[i]] -= 1.0;
                    }
                    pg = labels.get(gold[i]);
                    pp = labels.get(pred[i]);
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
    public ParsedBuilding parse(String tail) {
        String s = RuleBuildingParser.normalize(tail);
        List<String> cs = BuildingTailSynth.codePoints(s);
        if (cs.isEmpty()) return ParsedBuilding.of("");
        int[] path = decode(cs);
        // 連続する同一ラベルのランをスパンに（各ラベルは最初のスパンだけ採用）
        Map<String, String> first = new HashMap<>();
        StringBuilder buf = new StringBuilder();
        String cur = null;
        for (int i = 0; i <= cs.size(); i++) {
            String lab = (i < cs.size()) ? split(labels.get(path[i]))[1] : "";
            boolean isO = (i == cs.size()) || labels.get(path[i]).equals("O");
            String here = isO ? null : lab;
            if (!Objects.equals(here, cur)) {
                if (cur != null && buf.length() > 0) first.putIfAbsent(cur, buf.toString());
                buf.setLength(0);
                cur = here;
            }
            if (here != null) buf.append(cs.get(i));
        }
        return new ParsedBuilding(
                first.getOrDefault("建物名", ""), first.getOrDefault("棟", ""),
                first.getOrDefault("階", ""), first.getOrDefault("部屋", ""));
    }
}
