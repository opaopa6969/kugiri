package org.unlaxer.kugiri.feature;

import java.util.*;

/**
 * codepoint 単位の素性抽出。区切りキーワード(都道府県郡市区町村丁目番地号字...)と文字種が
 * 強い手掛かりなので、窓 ±2 でそれらを与える。各素性は indicator(=存在=アクティブ)。
 */
public final class Features {
    private Features() {}

    private static final Set<Integer> SUFFIX_KW = codepointSet("都道府県郡市区町村丁目番地号字条線通甲乙丙丁戊割の之ノ");
    private static final Set<Integer> BLDG_KW = codepointSet("棟館階号室Ｆ階建ビルマンションタワーハイツ");

    private static Set<Integer> codepointSet(String s) {
        Set<Integer> set = new HashSet<>();
        s.codePoints().forEach(set::add);
        return set;
    }

    public static String charType(String cp) {
        int c = cp.codePointAt(0);
        if (Character.isWhitespace(c)) return "space";
        if ("0123456789".indexOf(c) >= 0) return "digit_h";
        if ("０１２３４５６７８９".indexOf(c) >= 0) return "digit_f";
        if ("-ー−－‐〒".indexOf(c) >= 0) return "sym";
        Character.UnicodeBlock b = Character.UnicodeBlock.of(c);
        if (b == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || b == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                || (c >= 0x20000 && c <= 0x2FFFF)) return "kanji";
        if (b == Character.UnicodeBlock.HIRAGANA) return "hira";
        if (b == Character.UnicodeBlock.KATAKANA) return "kata";
        if (b == Character.UnicodeBlock.BASIC_LATIN
                || b == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS) return "latin";
        return "other";
    }

    private static void cfeat(List<String> chars, int i, int off, List<String> f) {
        int j = i + off;
        String p = (off >= 0 ? "+" : "") + off;
        if (j < 0 || j >= chars.size()) { f.add("c[" + p + "]=__BND__"); return; }
        String ch = chars.get(j);
        int cp = ch.codePointAt(0);
        f.add("c[" + p + "]=" + ch);
        f.add("t[" + p + "]=" + charType(ch));
        if (SUFFIX_KW.contains(cp)) f.add("kw[" + p + "]");
        if (BLDG_KW.contains(cp)) f.add("bk[" + p + "]");
    }

    /** 位置 i のアクティブ素性キー列。 */
    public static List<String> charFeatures(List<String> chars, int i) {
        List<String> f = new ArrayList<>();
        f.add("bias");
        for (int off = -2; off <= 2; off++) cfeat(chars, i, off, f);
        if (i + 1 < chars.size()) f.add("bg=" + chars.get(i) + chars.get(i + 1));
        if (i == 0) f.add("BOS");
        if (i == chars.size() - 1) f.add("EOS");
        return f;
    }

    public static List<List<String>> sentFeatures(List<String> chars) {
        List<List<String>> out = new ArrayList<>(chars.size());
        for (int i = 0; i < chars.size(); i++) out.add(charFeatures(chars, i));
        return out;
    }
}
