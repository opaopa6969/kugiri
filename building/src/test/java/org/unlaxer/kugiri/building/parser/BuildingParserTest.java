package org.unlaxer.kugiri.building.parser;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.unlaxer.kugiri.building.identity.BuildingLexicon;
import org.unlaxer.kugiri.building.identity.SampleCorpus;

public class BuildingParserTest {

    private static BuildingLexicon lex;

    @BeforeAll
    static void setup() { lex = BuildingLexicon.learn(SampleCorpus.names()); }

    private ParsedBuilding rule(String tail) { return BuildingParser.of("rule", lex).parse(tail); }
    private ParsedBuilding lexp(String tail) { return BuildingParser.of("lexicon", lex).parse(tail); }

    @Test
    public void ruleDecomposesCommonForms() {
        assertEquals(new ParsedBuilding("川上ハイツ", "", "", "101"), rule("川上ハイツ-101"));
        assertEquals(new ParsedBuilding("楠木マンション", "", "", "10"), rule("楠木マンション10号"));
        assertEquals(new ParsedBuilding("天王ビル", "", "4F", ""), rule("天王ビル4F"));
        assertEquals(new ParsedBuilding("藤田アパート", "B", "", ""), rule("藤田アパートB"));
        assertEquals(new ParsedBuilding("仁徳ビル", "4B", "", ""), rule("仁徳ビル4-B"));
        assertEquals(new ParsedBuilding("府営柱本団地", "B2", "", "407"), rule("府営柱本団地B2-407"));
        assertEquals(new ParsedBuilding("杉の荘", "", "", "6"), rule("杉の荘6号室"));
        assertEquals(new ParsedBuilding("島田文化", "", "1階", "103"), rule("島田文化1階-103号室"));
    }

    @Test
    public void longVowelKatakanaIsPreserved() {
        // 長音「ー」をハイフンに畳まない（アパート/ハイツ が壊れない）
        assertEquals("藤田アパート", rule("藤田アパートB").name());
        assertEquals("川上ハイツ", rule("川上ハイツ-101").name());
    }

    @Test
    public void fullwidthIsNormalized() {
        // 全角入力でも半角同様に分解できる
        ParsedBuilding p = rule("府営柱本団地Ｂ２−４０７");
        assertEquals("府営柱本団地", p.name());
        assertEquals("B2", p.wing());
        assertEquals("407", p.room());
    }

    @Test
    public void lexiconResolvesBareTrailingNumber() {
        // 生産的固有名(青葉台)の後の裸数字は棟、固有名でない(ビル)の後は名前の一部
        ParsedBuilding aoba = lexp("グランドメゾン青葉台2");
        assertEquals("グランドメゾン青葉台", aoba.name());
        assertEquals("2", aoba.wing());

        ParsedBuilding mori = lexp("森ビル10");
        assertEquals("森ビル10", mori.name());
        assertEquals("", mori.wing());

        // rule は裸数字を判定できず名前に残す（差分）
        assertEquals("グランドメゾン青葉台2", rule("グランドメゾン青葉台2").name());
    }
}
