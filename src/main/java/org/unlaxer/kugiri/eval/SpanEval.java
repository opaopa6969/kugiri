package org.unlaxer.kugiri.eval;

import org.unlaxer.kugiri.label.Labels;
import java.util.*;

/**
 * entity-level（スパン一致）評価ユーティリティ。
 *
 * <p>token-level の正解率は O や規則的な部分が多いと高く出てしまい実力を測れない。
 * そこで BIOES タグ列を「(label, 開始, 終了) のスパン集合」に復元し、
 * <em>ラベルと範囲が完全一致したスパンだけ</em>を正解とみなして per-label / micro の
 * 適合率・再現率・F1 を測る。あわせて token-level の混同行列も提供する。
 */
public final class SpanEval {
    private SpanEval() {}

    /** codepoint 区間 [start,end) にラベル label を持つスパン。 */
    public record Span(String label, int start, int end) {}

    /** tp/fp/fn と、そこから導く P/R/F1。 */
    public static final class Counts {
        public long tp, fp, fn;
        public double p() { return tp + fp == 0 ? 1 : (double) tp / (tp + fp); }
        public double r() { return tp + fn == 0 ? 1 : (double) tp / (tp + fn); }
        public double f1() { double p = p(), r = r(); return p + r == 0 ? 0 : 2 * p * r / (p + r); }
    }

    /** BIOES タグ列から entity スパン集合を復元（壊れたタグも安全側に解釈）。 */
    public static List<Span> spans(List<String> tags) {
        List<Span> out = new ArrayList<>();
        String cur = null; int st = -1;
        for (int i = 0; i < tags.size(); i++) {
            String t = tags.get(i);
            if (t.equals(Labels.OUTSIDE)) {
                if (cur != null) { out.add(new Span(cur, st, i)); cur = null; }
                continue;
            }
            int d = t.indexOf('-');
            String pos = t.substring(0, d), lab = t.substring(d + 1);
            switch (pos) {
                case "S" -> {
                    if (cur != null) { out.add(new Span(cur, st, i)); cur = null; }
                    out.add(new Span(lab, i, i + 1));
                }
                case "B" -> {
                    if (cur != null) out.add(new Span(cur, st, i));
                    cur = lab; st = i;
                }
                case "I" -> {
                    if (cur == null || !cur.equals(lab)) {
                        if (cur != null) out.add(new Span(cur, st, i));
                        cur = lab; st = i;
                    }
                }
                case "E" -> {
                    if (cur == null || !cur.equals(lab)) {
                        if (cur != null) out.add(new Span(cur, st, i));
                        cur = lab; st = i;
                    }
                    out.add(new Span(cur, st, i + 1)); cur = null;
                }
                default -> { /* 未知 pos は無視 */ }
            }
        }
        if (cur != null) out.add(new Span(cur, st, tags.size()));
        return out;
    }

    /** gold/pred のタグ列ペア列から per-label スパン Counts を集計。 */
    public static Map<String, Counts> spanCounts(List<List<String>> golds, List<List<String>> preds) {
        Map<String, Counts> stat = new LinkedHashMap<>();
        for (String lab : Labels.SURFACE) stat.put(lab, new Counts());
        for (int k = 0; k < golds.size(); k++) {
            Set<Span> g = new HashSet<>(spans(golds.get(k)));
            Set<Span> p = new HashSet<>(spans(preds.get(k)));
            for (Span s : g) {
                Counts c = stat.computeIfAbsent(s.label(), x -> new Counts());
                if (p.contains(s)) c.tp++; else c.fn++;
            }
            for (Span s : p) {
                if (!g.contains(s)) stat.computeIfAbsent(s.label(), x -> new Counts()).fp++;
            }
        }
        return stat;
    }

    /** token-level 混同行列（gold ラベル -> pred ラベル -> 件数）。ラベルは表層名 or "O"。 */
    public static Map<String, Map<String, Long>> confusion(List<List<String>> golds, List<List<String>> preds) {
        Map<String, Map<String, Long>> m = new LinkedHashMap<>();
        for (int k = 0; k < golds.size(); k++) {
            List<String> g = golds.get(k), p = preds.get(k);
            for (int i = 0; i < g.size(); i++) {
                String gl = surface(g.get(i)), pl = surface(p.get(i));
                m.computeIfAbsent(gl, x -> new LinkedHashMap<>()).merge(pl, 1L, Long::sum);
            }
        }
        return m;
    }

    /** BIOES タグ -> 表層ラベル（O はそのまま "O"）。 */
    public static String surface(String tag) {
        if (tag.equals(Labels.OUTSIDE)) return "O";
        int d = tag.indexOf('-');
        return d < 0 ? tag : tag.substring(d + 1);
    }

    /** per-label スパン Counts を表に整形し、micro/macro F1 も付けて返す。 */
    public static String formatSpanReport(Map<String, Counts> stat) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-14s %6s %6s %6s %8s%n", "label", "P", "R", "F1", "支持"));
        long tp = 0, fp = 0, fn = 0; double macro = 0; int labs = 0;
        for (Map.Entry<String, Counts> e : stat.entrySet()) {
            Counts c = e.getValue();
            long support = c.tp + c.fn;
            if (c.tp + c.fp + c.fn == 0) continue;
            sb.append(String.format("%-14s %6.3f %6.3f %6.3f %8d%n",
                    e.getKey(), c.p(), c.r(), c.f1(), support));
            tp += c.tp; fp += c.fp; fn += c.fn; macro += c.f1(); labs++;
        }
        double microP = tp + fp == 0 ? 1 : (double) tp / (tp + fp);
        double microR = tp + fn == 0 ? 1 : (double) tp / (tp + fn);
        double microF1 = microP + microR == 0 ? 0 : 2 * microP * microR / (microP + microR);
        sb.append(String.format("%-14s %6.3f %6.3f %6.3f%n", "micro", microP, microR, microF1));
        sb.append(String.format("%-14s %6s %6s %6.3f%n", "macro", "", "", labs == 0 ? 0 : macro / labs));
        return sb.toString();
    }

    /** 非対角（誤り）の混同を多い順に最大 topN 行で整形。 */
    public static String formatConfusion(Map<String, Map<String, Long>> conf, int topN) {
        record E(String g, String p, long n) {}
        List<E> errs = new ArrayList<>();
        for (var ge : conf.entrySet())
            for (var pe : ge.getValue().entrySet())
                if (!ge.getKey().equals(pe.getKey())) errs.add(new E(ge.getKey(), pe.getKey(), pe.getValue()));
        errs.sort((a, b) -> Long.compare(b.n(), a.n()));
        StringBuilder sb = new StringBuilder();
        sb.append("混同 top（gold -> pred : 件数）\n");
        int i = 0;
        for (E e : errs) {
            if (i++ >= topN) break;
            sb.append(String.format("  %-12s -> %-12s : %d%n", e.g(), e.p(), e.n()));
        }
        if (errs.isEmpty()) sb.append("  （誤りなし）\n");
        return sb.toString();
    }
}
