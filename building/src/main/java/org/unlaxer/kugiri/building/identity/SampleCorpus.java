package org.unlaxer.kugiri.building.identity;

import java.util.*;

/**
 * 同定プロトタイプ用の合成・建物名コーパス（Phase0）。
 *
 * <p>固有名（ブランド/地名）と種別語を意図的に反復させ、{@link BuildingLexicon} が
 * 「どの語が生産的な固有名か／どの語がありふれた種別語か」をコーパス頻度から誘導できるようにする。
 * 実データ・gold pairs での検証は Phase2。
 */
public final class SampleCorpus {
    private SampleCorpus() {}

    /** 生産的な固有名（複数の建物名に登場するブランド・地名）。 */
    static final String[] BRANDS = {
            "ライオンズ", "グランドメゾン", "白雲", "青雲", "朝日", "みなと", "さくら", "中央"
    };
    static final String[] PLACES = {"梅田", "難波", "青葉台", "本町", "柱本"};
    /** ありふれた種別語（多数のブランドに付く＝高頻度）。 */
    static final String[] TYPES = {"マンション", "ハイツ", "コーポ", "ビル", "荘", "団地", "宿舎"};

    /** 反復のある建物名コーパスを生成する。 */
    public static List<String> names() {
        List<String> out = new ArrayList<>();
        for (String b : BRANDS) {
            for (String t : TYPES) out.add(b + t);            // ブランド×種別（種別を高頻度に）
            for (String p : PLACES) out.add(b + p);            // ブランド×地名（地名を高頻度に）
            for (String p : PLACES) out.add(b + "マンション" + p); // ブランド×種別×地名
        }
        // enumerator を持つ別建物群（同種別・番号違い）
        for (String n : new String[]{"第一", "第二", "第三"}) {
            out.add(n + "宿舎");
            out.add(n + "団地");
        }
        return out;
    }
}
