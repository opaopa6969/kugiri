package org.unlaxer.kugiri.aza;

import org.unlaxer.kugiri.model.Component;
import java.util.*;
import java.util.regex.*;

/** 大字辞書＋末尾数字で残差スロットを切り出し、誘導字彙で字を推定する。教師ラベル不使用。 */
public final class Aza {
    private Aza() {}

    private static final String NUM = "0-9０-９一二三四五六七八九十百千";
    private static final Pattern TAIL_NUM = Pattern.compile(
            "[" + NUM + "][" + NUM + "\\-ー‐−－のノ]*(?:番地|地割|番|号|丁目)?.*$");
    private static final Pattern MARK = Pattern.compile("^(?:大字|字)?");

    public static String longestPrefix(String text, Set<String> dict) {
        String best = "";
        for (String w : dict) if (text.startsWith(w) && w.length() > best.length()) best = w;
        return best;
    }

    /** (大字, 残差=字候補, 末尾数字=地番)。頭の ZIP/都道府県/市区町村は除去済み想定。 */
    public static String[] peel(String text, Set<String> oazaDict) {
        String oaza = longestPrefix(text, oazaDict);
        String rest = text.substring(oaza.length());
        Matcher m = TAIL_NUM.matcher(rest);
        if (m.find()) return new String[]{oaza, rest.substring(0, m.start()), rest.substring(m.start())};
        return new String[]{oaza, rest, ""};
    }

    public static List<Component> inferComponents(String text, Set<String> oazaDict, AzaInducer inducer) {
        String[] p = peel(text, oazaDict);
        List<Component> comps = new ArrayList<>();
        if (!p[0].isEmpty()) comps.add(new Component("町または大字", p[0]));
        String name = MARK.matcher(p[1]).replaceFirst("");
        if (!name.isEmpty())
            for (String piece : inducer.segment(name)) comps.add(new Component("字小字", piece));
        if (!p[2].isEmpty()) comps.add(new Component("地番", p[2]));
        return comps;
    }
}
