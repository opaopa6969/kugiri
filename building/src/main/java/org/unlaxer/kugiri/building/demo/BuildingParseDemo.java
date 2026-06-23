package org.unlaxer.kugiri.building.demo;

import org.unlaxer.kugiri.building.identity.BuildingLexicon;
import org.unlaxer.kugiri.building.identity.SampleCorpus;
import org.unlaxer.kugiri.building.parser.BuildingParser;
import org.unlaxer.kugiri.building.parser.ParsedBuilding;

import java.util.List;

/**
 * 建物テール分解デモ（Phase1-1）。rule（決定的）と lexicon（kugiri 統計）を対比し、
 * 「裸の末尾数字＝棟か名前の一部か」を語彙で解く差分を見せる。
 */
public final class BuildingParseDemo {
    public static void main(String[] args) {
        BuildingLexicon lex = BuildingLexicon.learn(SampleCorpus.names());
        BuildingParser rule = BuildingParser.of("rule", lex);
        BuildingParser lexp = BuildingParser.of("lexicon", lex);
        BuildingParser perc = BuildingParser.of("perceptron", lex);

        List<String> tails = List.of(
                "川上ハイツ-101",
                "楠木マンション10号",
                "天王ビル4F",
                "藤田アパートB",
                "仁徳ビル4-B",
                "府営柱本団地B2-407",
                "杉の荘6号室",
                "島田文化1階-103号室",
                "ライオンズマンション梅田301号室",
                "グランドメゾン青葉台2",   // ← 2 は棟（青葉台は生産的固有名）
                "森ビル10"                 // ← 10 は名前の一部（ビルは固有名でない）
        );

        System.out.println("=== 建物テール分解：rule vs lexicon ===");
        for (String t : tails) {
            ParsedBuilding r = rule.parse(t);
            ParsedBuilding l = lexp.parse(t);
            String diff = r.equals(l) ? "" : "   ★差分";
            System.out.printf("入力: %s%n", t);
            System.out.printf("   rule       : %s%n", r);
            System.out.printf("   lexicon    : %s%s%n", l, diff);
            System.out.printf("   perceptron : %s%n", perc.parse(t));
        }
        System.out.println("\n※ 裸の末尾数字の棟/名前判定は、BHが cross-row 共起で解く所を");
        System.out.println("   kugiri は誘導語彙の生産性で1件単位で判定（辞書レス）。");
    }
}
