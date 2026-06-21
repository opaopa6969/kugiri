package org.unlaxer.kugiri.building.parser;

import org.unlaxer.kugiri.building.identity.BuildingLexicon;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * kugiri 統計版の建物パーサー。{@link RuleBuildingParser} の剥がしに加えて、
 * 「裸の末尾数字（号/階/ハイフン等のマーカーが無い数字）」が <b>棟</b>なのか<b>建物名の一部</b>なのかを、
 * 誘導語彙（{@link BuildingLexicon}）で辞書レスに判定する。
 *
 * <p>BH が cross-row 共起でしか解けなかった「森ビル10（名前の一部）」vs「グランドメゾン青葉台2（棟）」を、
 * コーパス由来の生産性で1件単位で判定する：基底名の末尾トークンが生産的な固有名なら数字は棟、
 * そうでなければ名前の一部として残す。
 */
public class LexiconBuildingParser implements BuildingParser {

    private static final Pattern TRAILING_BARE_NUM = Pattern.compile("^(.+?)(\\d{1,2})$");

    private final RuleBuildingParser rule = new RuleBuildingParser();
    private final BuildingLexicon lex;

    public LexiconBuildingParser(BuildingLexicon lex) { this.lex = lex; }

    @Override
    public String name() { return "lexicon"; }

    @Override
    public ParsedBuilding parse(String tail) {
        ParsedBuilding p = rule.parse(tail);
        // rule が棟/階/部屋を取った後、まだ棟が空で建物名が「…<数字>」で終わるなら語彙で判定
        if (!p.wing().isEmpty() || !p.name().matches(".+\\d{1,2}$")) return p;

        Matcher m = TRAILING_BARE_NUM.matcher(p.name());
        if (!m.matches()) return p;
        String base = m.group(1), digits = m.group(2);
        if (base.isEmpty()) return p;

        // 基底名の末尾トークンが生産的な固有名なら、数字は別単位＝棟
        List<String> toks = lex.segment(base);
        String lastTok = toks.isEmpty() ? base : toks.get(toks.size() - 1);
        if (lex.isProductive(lastTok)) {
            return p.withName(base).withWing(digits);
        }
        return p; // 例: 森ビル10（ビルは生産的固有名でない）→ 名前のまま
    }
}
