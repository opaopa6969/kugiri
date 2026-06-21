package org.unlaxer.kugiri.aza;

import java.util.*;

import org.unlaxer.jaddress.normalizer.NFKC_CF;

/**
 * 教師ゼロで「字(小字)」を推定する中核。残差スパン群から、頻出 × 高分岐エントロピーの
 * 部分文字列を字彙として誘導し、PMI で合成語(癒着)を剪定、ユニグラムLM で最尤分割する。
 *
 * <p>成立根拠: (1) ABR が大字を、末尾数字が地番開始を決めるので「間の残差」は構造的に字。
 * (2) 字名は全国で反復するので頻出部分文字列＝字、と教師なし誘導できる。
 * (3) 中里前田 のような独立共起(PMI低)は合成語として除去、田中 のような高PMIは1単位で保持。
 */
public final class AzaInducer {
    private final int minCount, minLen, maxLen;
    private final double eMin, wordPenalty, tau;
    private final Map<String, Integer> lex = new HashMap<>(); // 字彙 -> 出現回数
    private double Z = 1.0;
    private int N = 1;
    private static final String BND = "\u0000";

    public AzaInducer() { this(3, 1, 6, 0.0, 0.5, 0.2); }
    public AzaInducer(int minCount, int minLen, int maxLen, double eMin, double wordPenalty, double tau) {
        this.minCount = minCount; this.minLen = minLen; this.maxLen = maxLen;
        this.eMin = eMin; this.wordPenalty = wordPenalty; this.tau = tau;
    }

    public Map<String, Integer> lexicon() { return lex; }

    public AzaInducer fit(List<String> residuals) {
        N = Math.max(1, residuals.size());
        Map<String, Integer> cnt = new HashMap<>();
        Map<String, Map<String, Integer>> left = new HashMap<>(), right = new HashMap<>();
        for (String r : residuals) {
            String name = stripMark(r);
            String s = BND + name + BND;
            for (int len = minLen; len <= maxLen; len++) {
                for (int i = 1; i < s.length() - len; i++) {
                    String sub = s.substring(i, i + len);
                    if (sub.contains(BND)) continue;
                    cnt.merge(sub, 1, Integer::sum);
                    left.computeIfAbsent(sub, k -> new HashMap<>()).merge(String.valueOf(s.charAt(i - 1)), 1, Integer::sum);
                    right.computeIfAbsent(sub, k -> new HashMap<>()).merge(String.valueOf(s.charAt(i + len)), 1, Integer::sum);
                }
            }
        }
        for (Map.Entry<String, Integer> e : cnt.entrySet()) {
            String sub = e.getKey(); int c = e.getValue();
            if (c < minCount) continue;
            Map<String, Integer> lm = left.get(sub), rm = right.get(sub);
            boolean lvar = lm.size() >= 2 || lm.containsKey(BND);
            boolean rvar = rm.size() >= 2 || rm.containsKey(BND);
            double ent = (entropy(lm) + entropy(rm)) / 2;
            if (lvar && rvar && ent >= eMin) lex.put(sub, c);
        }
        Z = sum(lex); pruneCollocations(); Z = sum(lex);
        return this;
    }

    /** 既知2単位 u1+u2 に分解でき、その結合が偶然並み(PMI<=tau)の単位を剪定。 */
    private void pruneCollocations() {
        List<String> multi = new ArrayList<>();
        for (String w : lex.keySet()) if (w.length() >= 2) multi.add(w);
        for (String u : multi) {
            int cu = lex.get(u);
            double weakest = Double.POSITIVE_INFINITY;
            for (int k = 1; k < u.length(); k++) {
                Integer ca = lex.get(u.substring(0, k)), cb = lex.get(u.substring(k));
                if (ca != null && cb != null) {
                    double pmi = Math.log(((double) cu * N) / ((double) ca * cb));
                    weakest = Math.min(weakest, pmi);
                }
            }
            if (weakest <= tau) lex.remove(u);
        }
    }

    private double logp(String piece) {
        Integer c = lex.get(piece);
        if (c != null) return Math.log(c / Z);
        return Math.log(1.0 / (Z * 1000)) * piece.length(); // 未知のフロア
    }

    /** ユニグラムLM + 単語ペナルティで最尤分割(Viterbi)。 */
    public List<String> segment(String name) {
        int n = name.length();
        List<String> out = new ArrayList<>();
        if (n == 0) return out;
        double[] best = new double[n + 1];
        int[] bp = new int[n + 1];
        Arrays.fill(best, Double.NEGATIVE_INFINITY);
        best[0] = 0; bp[0] = -1;
        for (int i = 1; i <= n; i++) {
            for (int j = Math.max(0, i - maxLen); j < i; j++) {
                if (best[j] == Double.NEGATIVE_INFINITY) continue;
                String piece = name.substring(j, i);
                double cand = best[j] + logp(piece) - wordPenalty;
                if (cand > best[i]) { best[i] = cand; bp[i] = j; }
            }
        }
        int i = n;
        Deque<String> stack = new ArrayDeque<>();
        while (i > 0) { int j = bp[i]; stack.push(name.substring(j, i)); i = j; }
        out.addAll(stack);
        return out;
    }

    static String stripMark(String r) {
        String s = NFKC_CF.normalize(r);
        if (s.startsWith("大字")) return s.substring(2);
        if (s.startsWith("字")) return s.substring(1);
        return s;
    }

    private static double entropy(Map<String, Integer> counter) {
        int tot = 0; for (int v : counter.values()) tot += v;
        if (tot == 0) return 0;
        double h = 0;
        for (int v : counter.values()) { double p = (double) v / tot; h -= p * (Math.log(p) / Math.log(2)); }
        return h;
    }
    private static double sum(Map<String, Integer> m) { double s = 0; for (int v : m.values()) s += v; return s == 0 ? 1 : s; }
}
