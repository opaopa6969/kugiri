package org.unlaxer.kugiri.building.identity;

import java.util.*;

/**
 * 同定プロトタイプ用の合成・建物名コーパス（Phase0）。
 *
 * <p>固有名（ブランド/地名）と汎用語（種別/資格）を意図的に反復させ、{@link BuildingLexicon} が
 * 「どの語が生産的な固有名か」をコーパス頻度から誘導できるようにする。実データ・gold は Phase2。
 */
public final class SampleCorpus {
    private SampleCorpus() {}

    /** 生産的な固有名（複数の建物名に登場するブランド）。 */
    static final String[] BRANDS = {
            "ライオンズ", "グランドメゾン", "白雲", "青雲", "朝日", "みなと", "さくら", "中央"
    };
    /** 地名（固有名・建物を識別する。梅田と難波は別建物）。 */
    static final String[] PLACES = {"梅田", "難波", "青葉台", "本町", "柱本"};
    /** 汎用語（種別/資格。単独では建物を識別しない）。 */
    static final String[] TYPES = {"マンション", "ハイツ", "コーポ", "ビル", "荘", "団地", "寮"};

    /** 反復のある建物名コーパスを生成する。 */
    public static List<String> names() {
        List<String> out = new ArrayList<>();
        for (String b : BRANDS) {
            for (String t : TYPES) out.add(b + t);             // ブランド×種別
            for (String p : PLACES) out.add(b + p);            // ブランド×地名
            for (String p : PLACES) out.add(b + "マンション" + p); // ブランド×種別×地名
        }
        // enumerator を持つ別建物群
        for (String n : new String[]{"第一", "第二", "第三"}) {
            out.add(n + "宿舎");
            out.add(n + "団地");
        }
        // 正式名（資格＋施設＋寮）と、その略称が現れるコーパス（型E）
        for (String b : new String[]{"さくら", "みなと", "朝日"}) {
            out.add("県立" + b + "施設" + b + "寮");
            out.add(b + "寮");
        }
        return out;
    }
}
