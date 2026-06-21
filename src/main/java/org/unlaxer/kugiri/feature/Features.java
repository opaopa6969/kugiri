package org.unlaxer.kugiri.feature;

import java.util.*;

import org.unlaxer.jaddress.model.character.BuiltinCharacterKind;
import org.unlaxer.jaddress.model.character.CharacterKindRegistry;

/**
 * codepoint 単位の素性抽出。BuiltinCharacterKind(住所特化文字種) + 窓 ±2 を与える。
 */
public final class Features {
    private Features() {}

    private static final CharacterKindRegistry REGISTRY = CharacterKindRegistry.getInstance();

    /** アドレス境界として重要な BuiltinCharacterKind。特別素性を生成する。 */
    private static final Set<BuiltinCharacterKind> BOUNDARY_KINDS = EnumSet.of(
        BuiltinCharacterKind.suffix丁目,
        BuiltinCharacterKind.suffix地番,
        BuiltinCharacterKind.suffix号,
        BuiltinCharacterKind.suffix号室,
        BuiltinCharacterKind.suffix棟,
        BuiltinCharacterKind.suffix階,
        BuiltinCharacterKind.japaneseAddressNumber,
        BuiltinCharacterKind.十干,
        BuiltinCharacterKind.十二支,
        BuiltinCharacterKind.delimitorJapanese,
        BuiltinCharacterKind.delimitorHyphen
    );

    public static String charType(String cp) {
        int c = cp.codePointAt(0);
        Set<org.unlaxer.jaddress.model.character.CharacterKind> kinds = REGISTRY.kindsOf(c);
        if (kinds.contains(BuiltinCharacterKind.delimitorSpace))   return "space";
        if (kinds.contains(BuiltinCharacterKind.arabicNumber))     return "digit";
        if (kinds.contains(BuiltinCharacterKind.japaneseAddressNumber)) return "jnum";
        if (kinds.contains(BuiltinCharacterKind.delimitorHyphen))  return "hyphen";
        if (kinds.contains(BuiltinCharacterKind.hiragana))         return "hira";
        if (kinds.contains(BuiltinCharacterKind.katakana))         return "kata";
        if (kinds.contains(BuiltinCharacterKind.alphabet))         return "latin";
        // normal の内訳: CJK は kanji、それ以外は other
        Character.UnicodeBlock b = Character.UnicodeBlock.of(c);
        if (b == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || b == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                || (c >= 0x20000 && c <= 0x2FFFF)) return "kanji";
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
        // 住所境界に強い BuiltinCharacterKind をそれぞれ個別素性として追加
        Set<org.unlaxer.jaddress.model.character.CharacterKind> kinds = REGISTRY.kindsOf(cp);
        for (BuiltinCharacterKind bk : BOUNDARY_KINDS) {
            if (kinds.contains(bk)) f.add("bk[" + p + "]=" + bk.name());
        }
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
