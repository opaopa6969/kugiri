package org.unlaxer.kugiri.building.parser;

import java.text.Normalizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 末尾から優先順位パターンで「部屋→階→棟」を1つずつ剥がし、残りを建物名とする決定的パーサー。
 * BH の bottom-up 剥がし（UnlaxerBuildingParser Step2-3）を最小構成で再実装したベースライン。
 *
 * <p>正規化は NFKC（幅畳み・大小は保持）＋各種ハイフンを {@code -} に統一。
 */
public class RuleBuildingParser implements BuildingParser {

    private static final Pattern[] ROOM = {
            Pattern.compile("(\\d{1,4})号室$"),
            Pattern.compile("(\\d{1,4})号$"),
            Pattern.compile("-(\\d{2,5})$"),
    };
    private static final Pattern[] FLOOR = {
            Pattern.compile("(B?\\d{1,2})階$"),
            Pattern.compile("(B?\\d{1,2})F$"),
    };
    private static final Pattern[] WING = {
            Pattern.compile("(\\d{1,2})-([A-Z])$"),       // 4-B → 4B
            Pattern.compile("([A-Z]\\d{0,2})$"),          // B / B2
            Pattern.compile("(\\d{1,2})棟$"),
            Pattern.compile("([東西南北新本別]棟)$"),
    };

    @Override
    public String name() { return "rule"; }

    @Override
    public ParsedBuilding parse(String tail) {
        String s = normalize(tail);
        String room = "", floor = "", wing = "";

        s = stripTrailingSep(s);
        for (Pattern p : ROOM) {
            Matcher m = p.matcher(s);
            if (m.find()) { room = m.group(1); s = stripTrailingSep(s.substring(0, m.start())); break; }
        }
        for (Pattern p : FLOOR) {
            Matcher m = p.matcher(s);
            if (m.find()) { floor = m.group(); s = stripTrailingSep(s.substring(0, m.start())); break; }
        }
        for (Pattern p : WING) {
            Matcher m = p.matcher(s);
            if (m.find()) {
                wing = m.groupCount() == 2 ? m.group(1) + m.group(2) : m.group(1);
                s = stripTrailingSep(s.substring(0, m.start()));
                break;
            }
        }
        return new ParsedBuilding(stripTrailingSep(s), wing, floor, room);
    }

    /**
     * NFKC で幅を畳む（大小は保持）。各種ハイフン/ダッシュのみ {@code -} に統一する。
     * カタカナ長音「ー」(U+30FC)・半角長音「ｰ」(U+FF70) は語の一部なので畳まない（アパート等）。
     */
    static String normalize(String tail) {
        String s = Normalizer.normalize(tail == null ? "" : tail, Normalizer.Form.NFKC);
        StringBuilder b = new StringBuilder();
        s.codePoints().forEach(cp -> b.appendCodePoint(switch (cp) {
            case '−', '‐', '―', '–', '—' -> '-';   // U+2212/2010/2015/2013/2014（長音は含めない）
            default -> cp;
        }));
        return b.toString().strip();
    }

    static String stripTrailingSep(String s) {
        int end = s.length();
        while (end > 0) {
            char c = s.charAt(end - 1);
            if (c == '-' || c == ' ' || c == '　') end--; else break;
        }
        return s.substring(0, end);
    }
}
