package org.unlaxer.kugiri.building.parser;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * kugiri 統計（系列ラベリング）版パーサーの回帰。合成学習だけで、keyword 辞書なしに
 * 建物テールの主要フィールドを取り切れることを確認する。
 */
public class PerceptronBuildingParserTest {

    private static BuildingParser perc;

    @BeforeAll
    static void setup() { perc = BuildingParser.of("perceptron", null); }

    @Test
    public void extractsRoomFloorWingName() {
        assertEquals(new ParsedBuilding("天王ビル", "", "4F", ""), perc.parse("天王ビル4F"));
        assertEquals(new ParsedBuilding("藤田アパート", "B", "", ""), perc.parse("藤田アパートB"));
        assertEquals(new ParsedBuilding("府営柱本団地", "B2", "", "407"), perc.parse("府営柱本団地B2-407"));
        assertEquals(new ParsedBuilding("杉の荘", "", "", "6"), perc.parse("杉の荘6号室"));
        assertEquals(new ParsedBuilding("ライオンズマンション梅田", "", "", "301"),
                perc.parse("ライオンズマンション梅田301号室"));
    }

    @Test
    public void agreesWithRuleOnClearCases() {
        BuildingParser rule = BuildingParser.of("rule", null);
        for (String t : new String[]{"天王ビル4F", "府営柱本団地B2-407", "杉の荘6号室",
                "ライオンズマンション梅田301号室", "藤田アパートB"}) {
            assertEquals(rule.parse(t), perc.parse(t), "不一致: " + t);
        }
    }
}
