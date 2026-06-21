package org.unlaxer.kugiri.building.identity;

import org.unlaxer.jaddress.normalizer.NFKC_CF;

import java.util.*;
import java.util.regex.Pattern;

/**
 * 同一住所に並ぶ2つの建物名が「同一建物（表記ゆれ/略称）」か「別建物」かを判定する。
 * テキストで断定できない衝突（型F）は {@link Decision#NEEDS_REVIEW} を返し、上位の証拠
 * （住所粒度・部屋集合・敷地）と人手レビューに委ねる。
 *
 * <p>3 方式（動作オプション）：
 * <ul>
 *   <li>{@link #editAbsolute}   — 絶対編集距離（BH legacy 相当ベースライン）</li>
 *   <li>{@link #editNormalized} — 正規化編集距離（BH 現行相当ベースライン）</li>
 *   <li>{@link #contrastive}    — kugiri 統計：包含＋対立度＋enumerator（辞書は最小 seed のみ）</li>
 * </ul>
 */
public final class BuildingIdentity {
    private BuildingIdentity() {}

    public enum Decision { SAME, DISTINCT, NEEDS_REVIEW }

    public record Verdict(Decision decision, String reason) {}

    private static final Pattern ENUMERATOR = Pattern.compile(
            "^第[一二三四五六七八九十百千0-9]+$|^[0-9]+$|^[A-Za-z]$|^[東西南北新本別].*棟$");

    private static final Pattern WING_TAIL = Pattern.compile("^[A-Za-z]$|^[0-9]{1,2}$|.*棟$");

    // ---- 正規化（検索キー） ----

    public static String searchKey(String name) {
        String s = NFKC_CF.normalize(name);
        StringBuilder b = new StringBuilder();
        s.codePoints().forEach(cp -> {
            switch (cp) {
                case 'ー', '−', '－', '‐', '―', '-', '・', '･', ' ', '　' -> { /* drop */ }
                default -> b.appendCodePoint(cp);
            }
        });
        return b.toString();
    }

    // ---- ベースライン ----

    public static Verdict editAbsolute(String a, String b, int threshold) {
        int d = levenshtein(searchKey(a), searchKey(b));
        return d <= threshold
                ? new Verdict(Decision.SAME, "編集距離 " + d + " ≤ " + threshold)
                : new Verdict(Decision.DISTINCT, "編集距離 " + d + " > " + threshold);
    }

    public static Verdict editNormalized(String a, String b, double t) {
        double sim = sim(searchKey(a), searchKey(b));
        return sim >= t
                ? new Verdict(Decision.SAME, String.format("正規化類似度 %.2f ≥ %.2f", sim, t))
                : new Verdict(Decision.DISTINCT, String.format("正規化類似度 %.2f < %.2f", sim, t));
    }

    // ---- kugiri 統計 ----

    public static Verdict contrastive(String a, String b, BuildingLexicon lex) {
        if (searchKey(a).equals(searchKey(b)))
            return new Verdict(Decision.SAME, "検索キー一致（notation・型D）");

        Set<String> ca = core(a, lex), cb = core(b, lex);
        Set<String> inter = new LinkedHashSet<>(ca); inter.retainAll(cb);
        Set<String> extraA = new LinkedHashSet<>(ca); extraA.removeAll(cb);
        Set<String> extraB = new LinkedHashSet<>(cb); extraB.removeAll(ca);

        if (extraA.isEmpty() && extraB.isEmpty())
            return new Verdict(Decision.SAME, "固有名核が一致（種別/棟差のみ）: " + ca);

        // 包含（型E＝略称 or 種別語の省略）：一方の核が他方に内包される
        if (extraA.isEmpty() ^ extraB.isEmpty()) {
            boolean sharedStrong = inter.stream().anyMatch(lex::isProductive);
            return sharedStrong
                    ? new Verdict(Decision.SAME, "包含＝略称（型E）共有核 " + inter)
                    : new Verdict(Decision.NEEDS_REVIEW, "包含だが共有核が汎用語のみ（衝突の恐れ・型F）: " + inter);
        }

        // 双方に差分：両方が対立的（生産的 or enumerator）なら別建物
        boolean aC = allContrastive(extraA, lex), bC = allContrastive(extraB, lex);
        if (aC && bC)
            return new Verdict(Decision.DISTINCT, "対立的差分 " + extraA + " vs " + extraB);

        // 片側が非生産的（typo の疑い）→ 編集距離で救済
        Verdict fb = editNormalized(a, b, 0.72);
        return new Verdict(fb.decision(), "非対立差分→編集距離: " + fb.reason());
    }

    /** 固有名核 = segment - 末尾の棟記号（汎用語ストリップはしない＝包含で扱う）。 */
    static Set<String> core(String name, BuildingLexicon lex) {
        List<String> toks = new ArrayList<>(lex.segment(name));
        if (toks.size() >= 2) {
            String last = toks.get(toks.size() - 1);
            if (WING_TAIL.matcher(last).matches()) toks.remove(toks.size() - 1);
        }
        return new LinkedHashSet<>(toks);
    }

    private static boolean allContrastive(Set<String> tokens, BuildingLexicon lex) {
        if (tokens.isEmpty()) return false;
        for (String t : tokens)
            if (!(ENUMERATOR.matcher(t).matches() || lex.isProductive(t))) return false;
        return true;
    }

    // ---- 文字列距離 ----

    static double sim(String a, String b) {
        int max = Math.max(a.length(), b.length());
        return max == 0 ? 1.0 : 1.0 - (double) levenshtein(a, b) / max;
    }

    static int levenshtein(String a, String b) {
        int[] prev = new int[b.length() + 1], cur = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) prev[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            cur[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                cur[j] = Math.min(Math.min(cur[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] t = prev; prev = cur; cur = t;
        }
        return prev[b.length()];
    }
}
