package org.unlaxer.kugiri.building.parser;

import org.unlaxer.kugiri.building.identity.BuildingLexicon;

/**
 * 建物テール（番地より後ろの文字列）を {@link ParsedBuilding} に分解する差し替え可能な層。
 *
 * <p>動作オプションで実装を切替（CLAUDE.md 差し替え層方針）：
 * <ul>
 *   <li>{@code "rule"}    — 末尾から優先順位パターンで貪欲に剥がす（BH 相当の決定的ベースライン）</li>
 *   <li>{@code "lexicon"} — rule＋誘導語彙で「裸の末尾数字＝棟か名前の一部か」を辞書レス判定（kugiri）</li>
 * </ul>
 */
public interface BuildingParser {

    ParsedBuilding parse(String tail);

    String name();

    static BuildingParser of(String name, BuildingLexicon lexicon) {
        return switch (name) {
            case "rule" -> new RuleBuildingParser();
            case "lexicon" -> new LexiconBuildingParser(lexicon);
            case "perceptron" -> new PerceptronBuildingParser();
            default -> throw new IllegalArgumentException(
                    "Unknown building parser: " + name + " (rule | lexicon | perceptron)");
        };
    }
}
