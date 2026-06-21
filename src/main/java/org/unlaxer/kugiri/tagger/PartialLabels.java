package org.unlaxer.kugiri.tagger;

import org.unlaxer.kugiri.eval.SpanEval;
import org.unlaxer.kugiri.model.Example;
import java.util.*;

/**
 * 部分ラベル（頭=既知 / 尻尾=潜在）ユーティリティ。
 *
 * <p>実データの弱教師は ABR/KEN_ALL から「頭」（都道府県〜丁目）の正解は得られるが、
 * 「尻尾」（番地・号・建物・字 等の細部）は分離されない。その状況を模擬するため、
 * 各事例の最初の尻尾ラベル以降を {@link PerceptronTagger#LATENT} に伏せる。
 */
public final class PartialLabels {
    private PartialLabels() {}

    /** 既定の「尻尾」ラベル（番地系＋建物系）。これらより前が「頭」。 */
    public static final Set<String> TAIL = Set.of(
            "字小字", "地番", "街区符号", "住居番号", "支号", "枝番号",
            "区画", "棟", "階数", "部屋番号", "方書き");

    private static String labelOf(String tag) {
        if (tag.equals("O")) return "O";
        int d = tag.indexOf('-');
        return d < 0 ? tag : tag.substring(d + 1);
    }

    /**
     * 最初の尻尾トークン以降（その位置を含む）を LATENT に伏せた部分ラベル事例を返す。
     * 頭（尻尾が始まる前）は gold のまま残す。
     */
    public static Example maskTail(Example full, Set<String> tailLabels) {
        List<String> tags = full.tags();
        int cut = tags.size();
        for (int i = 0; i < tags.size(); i++) {
            if (tailLabels.contains(labelOf(tags.get(i)))) { cut = i; break; }
        }
        List<String> masked = new ArrayList<>(tags.size());
        for (int i = 0; i < tags.size(); i++)
            masked.add(i < cut ? tags.get(i) : PerceptronTagger.LATENT);
        return new Example(full.chars(), masked);
    }

    /** 尻尾ラベルだけに限定したスパン micro-F1。 */
    public static double tailMicroF1(AddressParser parser, List<Example> test, Set<String> tailLabels) {
        Map<String, SpanEval.Counts> stat = parser.spanCounts(test);
        long tp = 0, fp = 0, fn = 0;
        for (Map.Entry<String, SpanEval.Counts> e : stat.entrySet()) {
            if (!tailLabels.contains(e.getKey())) continue;
            tp += e.getValue().tp; fp += e.getValue().fp; fn += e.getValue().fn;
        }
        double p = tp + fp == 0 ? 1 : (double) tp / (tp + fp);
        double r = tp + fn == 0 ? 1 : (double) tp / (tp + fn);
        return p + r == 0 ? 0 : 2 * p * r / (p + r);
    }
}
