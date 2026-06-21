package org.unlaxer.kugiri.label;

import java.util.*;

/**
 * 階層要素(enum) を、表層スパンを持つ「ラベル」に写像する層。
 *
 * <p>原 enum には2種類あり、(a) 構造マーカー(国域Top/町域Top/建物Bottom, isTop=true)は
 * 文字列上のスパンを持たず level から導出できるので tokenizer の出力ラベルから外し、
 * (b) 表層リーフ(都道府県/市/区/丁目/字小字/地番...)だけを系列ラベルにする。
 *
 * <p>ラベルは codepoint 単位に BIOES で展開する({@link Bioes})。日本語住所は分かち書きが
 * 無いので codepoint 単位ラベリングが自然で、外字(PUA)・サロゲートペアも 1 単位で扱える。
 */
public final class Labels {
    private Labels() {}

    public static final String OUTSIDE = "O"; // 階層要素.空

    /** 表層スパンを持つリーフ・ラベル -> level。原 enum の isTop=false 群に対応。 */
    public static final Map<String, Integer> LEVEL;
    /** 表層ラベルの一覧(出力順)。 */
    public static final List<String> SURFACE;

    static {
        LinkedHashMap<String, Integer> m = new LinkedHashMap<>();
        m.put("ZIP", 0);
        m.put("都道府県", 1);
        m.put("群", 2);
        m.put("東京23区", 2);
        m.put("政令指定市", 2);
        m.put("市", 2);
        m.put("区", 3);
        m.put("町村", 4);
        m.put("町または大字", 4);
        m.put("丁目", 5);
        m.put("字小字", 5);
        m.put("地番", 6);
        m.put("街区符号", 6);
        m.put("住居番号", 7);
        m.put("支号", 7);
        m.put("枝番号", 8);
        m.put("区画", 10);
        m.put("棟", 11);
        m.put("階数", 12);
        m.put("部屋番号", 13);
        m.put("方書き", 14);
        LEVEL = Collections.unmodifiableMap(m);
        SURFACE = List.copyOf(m.keySet());
    }
}
