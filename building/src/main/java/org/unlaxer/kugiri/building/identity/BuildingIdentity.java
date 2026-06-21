package org.unlaxer.kugiri.building.identity;

import org.unlaxer.jaddress.normalizer.NFKC_CF;

import java.util.*;
import java.util.regex.Pattern;

/**
 * 同一住所に並ぶ2つの建物名が「同一建物（表記ゆれ）」か「別建物」かを判定する。
 *
 * <p>3 方式を提供して対決できる（動作オプション）:
 * <ul>
 *   <li>{@link #editAbsolute}    — 絶対編集距離しきい値（BH legacy 相当のベースライン）</li>
 *   <li>{@link #editNormalized}  — 正規化編集距離しきい値</li>
 *   <li>{@link #contrastive}     — kugiri 統計：種別語を落とし、差分が「対立的（生産的/enumerator）」なら別建物、
 *       そうでなければ typo として編集距離にフォールバック</li>
 * </ul>
 */
public final class BuildingIdentity {
    private BuildingIdentity() {}

    public enum Decision { SAME, DISTINCT }

    public record Verdict(Decision decision, String reason) {}

    private static final Pattern ENUMERATOR = Pattern.compile(
            "^第[一二三四五六七八九十百千0-9]+$"      // 第一,第二…
          + "|^[0-9]+$"                                // 純数字（棟番号）
          + "|^[A-Za-z]$"                              // 単独英字（棟記号）
          + "|^[東西南北新本別].*棟$");                // 方位/新本別＋棟

    private static final Pattern WING_TAIL = Pattern.compile(
            "^[A-Za-z]$|^[0-9]{1,2}$|^[東西南北新本別]?棟$|.*棟$");

    // ---- 正規化（検索キー） ----

    /** NFKC_CF＋長音/各種ハイフン/中黒を畳んだ検索キー（決定的な表記ゆれ吸収）。 */
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

    // ---- ベースライン（編集距離） ----

    /** BH legacy 相当：検索キーの絶対編集距離 ≤ threshold なら同一。 */
    public static Verdict editAbsolute(String a, String b, int threshold) {
        int d = levenshtein(searchKey(a), searchKey(b));
        return d <= threshold
                ? new Verdict(Decision.SAME, "編集距離 " + d + " ≤ " + threshold)
                : new Verdict(Decision.DISTINCT, "編集距離 " + d + " > " + threshold);
    }

    /** 正規化編集距離（1 - d/maxlen）≥ t なら同一。 */
    public static Verdict editNormalized(String a, String b, double t) {
        String ka = searchKey(a), kb = searchKey(b);
        double sim = sim(ka, kb);
        return sim >= t
                ? new Verdict(Decision.SAME, String.format("正規化類似度 %.2f ≥ %.2f", sim, t))
                : new Verdict(Decision.DISTINCT, String.format("正規化類似度 %.2f < %.2f", sim, t));
    }

    // ---- kugiri 統計（対立度） ----

    /** kugiri 統計判定。 */
    public static Verdict contrastive(String a, String b, BuildingLexicon lex) {
        if (searchKey(a).equals(searchKey(b)))
            return new Verdict(Decision.SAME, "検索キー一致（notation）");

        Set<String> ca = identityCore(a, lex), cb = identityCore(b, lex);
        Set<String> extraA = new LinkedHashSet<>(ca); extraA.removeAll(cb);
        Set<String> extraB = new LinkedHashSet<>(cb); extraB.removeAll(ca);

        if (extraA.isEmpty() && extraB.isEmpty())
            return new Verdict(Decision.SAME, "固有名核が一致（種別/棟差のみ）: " + ca);

        boolean aContrast = allContrastive(extraA, lex);
        boolean bContrast = allContrastive(extraB, lex);
        if (!extraA.isEmpty() && !extraB.isEmpty() && aContrast && bContrast)
            return new Verdict(Decision.DISTINCT, "対立的差分 " + extraA + " vs " + extraB);

        // 片側が非生産的（typo の疑い）→ 編集距離で救済
        Verdict fb = editNormalized(a, b, 0.72);
        return new Verdict(fb.decision(), "非対立差分→編集距離: " + fb.reason());
    }

    /** 固有名核 = segment - 種別語 - 末尾の棟記号。 */
    static Set<String> identityCore(String name, BuildingLexicon lex) {
        List<String> toks = new ArrayList<>(lex.segment(name));
        // 末尾の棟記号（単英字・1-2桁数字・N棟）を1つ剥がす
        if (!toks.isEmpty()) {
            String last = toks.get(toks.size() - 1);
            if (WING_TAIL.matcher(last).matches() && toks.size() >= 2) toks.remove(toks.size() - 1);
        }
        Set<String> core = new LinkedHashSet<>();
        for (String t : toks) if (!lex.isType(t)) core.add(t);
        if (core.isEmpty()) core.addAll(toks); // 全部種別なら素のトークンで比較
        return core;
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
