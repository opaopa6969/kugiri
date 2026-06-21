package org.unlaxer.kugiri.tagger;

import org.unlaxer.kugiri.label.*;
import org.unlaxer.kugiri.model.*;
import java.util.*;

/** 住所 tokenizer の窓口: 学習 / parse / 簡易評価。 */
public final class AddressParser {
    private final PerceptronTagger tagger = new PerceptronTagger();

    public AddressParser fit(List<Example> data, int epochs) { tagger.fit(data, epochs); return this; }
    public AddressParser fit(List<Example> data) { return fit(data, 8); }

    /** 文字列 -> [(token,label)...] (Component 風)。 */
    public List<Component> parse(String text) {
        List<String> chars = CodePoints.of(text);
        List<String> tags = tagger.predict(chars);
        List<Component> out = new ArrayList<>();
        for (String[] tl : Bioes.decode(chars, tags)) out.add(new Component(tl[1], tl[0]));
        return out;
    }

    /** タグ正解率と、主要表層ラベルのトークンF1を文字列で返す。 */
    public String evaluate(List<Example> data) {
        long correct = 0, total = 0;
        Map<String, long[]> stat = new LinkedHashMap<>(); // label -> [tp,fp,fn]
        for (String lab : Labels.SURFACE) stat.put(lab, new long[3]);
        for (Example ex : data) {
            List<String> pred = tagger.predict(ex.chars());
            for (int i = 0; i < ex.tags().size(); i++) {
                String g = ex.tags().get(i), p = pred.get(i);
                total++; if (g.equals(p)) correct++;
                String gl = surface(g), pl = surface(p);
                if (gl != null && gl.equals(pl)) stat.get(gl)[0]++;
                else {
                    if (pl != null) stat.get(pl)[1]++;
                    if (gl != null) stat.get(gl)[2]++;
                }
            }
        }
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("tag accuracy = %.4f (%d/%d)%n", (double) correct / total, correct, total));
        sb.append(String.format("%-14s %6s %6s %6s%n", "label", "P", "R", "F1"));
        for (Map.Entry<String, long[]> e : stat.entrySet()) {
            long tp = e.getValue()[0], fp = e.getValue()[1], fn = e.getValue()[2];
            if (tp + fp + fn == 0) continue;
            double pr = tp + fp == 0 ? 1 : (double) tp / (tp + fp);
            double rc = tp + fn == 0 ? 1 : (double) tp / (tp + fn);
            double f1 = pr + rc == 0 ? 0 : 2 * pr * rc / (pr + rc);
            sb.append(String.format("%-14s %6.3f %6.3f %6.3f%n", e.getKey(), pr, rc, f1));
        }
        return sb.toString();
    }

    private static String surface(String tag) {
        if (tag.equals(Labels.OUTSIDE)) return null;
        return tag.substring(tag.indexOf('-') + 1);
    }
}
