package org.unlaxer.kugiri.aza;

import org.unlaxer.kugiri.model.Component;
import org.unlaxer.jaddress.aza.AzaInducer;
import org.unlaxer.jaddress.aza.AzaParse;
import java.util.*;

/**
 * 教師なし字推定の kugiri 側アダプタ。
 *
 * <p>字推定の本体は japanese-parser-common (>=0.2.0) の {@code org.unlaxer.jaddress.aza}
 * （{@link AzaInducer} / {@code org.unlaxer.jaddress.aza.Aza}）へ移設済み。本クラスは
 * その結果({@link AzaParse})を kugiri の {@link Component} 列へ橋渡しする薄い層。
 */
public final class Aza {
    private Aza() {}

    /** jpc の {@code Aza.peel} へ委譲。(大字, 残差=字候補, 末尾数字=地番)。 */
    public static String[] peel(String text, Set<String> oazaDict) {
        return org.unlaxer.jaddress.aza.Aza.peel(text, oazaDict);
    }

    /** jpc の最長一致へ委譲。 */
    public static String longestPrefix(String text, Set<String> dict) {
        return org.unlaxer.jaddress.aza.Aza.longestPrefix(text, dict);
    }

    /** text を字推定し、kugiri の Component 列(町または大字 / 字小字×n / 地番)に変換。 */
    public static List<Component> inferComponents(String text, Set<String> oazaDict, AzaInducer inducer) {
        AzaParse p = org.unlaxer.jaddress.aza.Aza.parse(text, oazaDict, inducer);
        List<Component> comps = new ArrayList<>();
        if (!p.oaza().isEmpty()) comps.add(new Component("町または大字", p.oaza()));
        for (String piece : p.aza()) comps.add(new Component("字小字", piece));
        if (!p.banchi().isEmpty()) comps.add(new Component("地番", p.banchi()));
        return comps;
    }
}
