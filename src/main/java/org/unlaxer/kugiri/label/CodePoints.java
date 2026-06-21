package org.unlaxer.kugiri.label;

import java.util.*;

/** 文字列を codepoint 単位(=unlaxer CodePoint 相当)で扱うユーティリティ。 */
public final class CodePoints {
    private CodePoints() {}

    /** 各 codepoint を 1 要素の文字列にした列。サロゲートペア/外字PUAも 1 要素。 */
    public static List<String> of(String s) {
        List<String> out = new ArrayList<>();
        s.codePoints().forEach(cp -> out.add(new String(Character.toChars(cp))));
        return out;
    }

    public static String join(List<String> cps) {
        return String.join("", cps);
    }
}
