package org.unlaxer.kugiri.building.parser;

import java.util.*;

/**
 * 建物テールの合成学習データ生成（系列ラベラ用）。建物名/棟/階/部屋を組み立てつつ、
 * codepoint 単位の BIOES ラベルを<b>アライメント不要で</b>同時に得る（kugiri 第一部 Synth と同思想）。
 */
public final class BuildingTailSynth {
    private BuildingTailSynth() {}

    /** 1事例：codepoint 列とその BIOES タグ列。 */
    public record Tagged(List<String> chars, List<String> tags) {}

    private static final String[] BRAND = {
            "ライオンズ", "グランドメゾン", "白雲", "青雲", "朝日", "みなと", "さくら", "中央",
            "杉", "藤田", "天王", "島田", "楠木", "川上"};
    private static final String[] TYPE = {"マンション", "ハイツ", "コーポ", "ビル", "荘", "団地", "寮", "アパート", "文化"};
    private static final String[] PLACE = {"梅田", "難波", "青葉台", "本町", "柱本", ""};
    private static final String[] WING = {"", "A", "B", "C", "B2", "南棟", "北棟", "1棟", "2棟"};
    private static final String[] FLOOR = {"", "4F", "1F", "1階", "3階", "B1階"};

    public static List<Tagged> makeDataset(int n, long seed) {
        Random rng = new Random(seed);
        List<Tagged> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) out.add(one(rng));
        return out;
    }

    private static Tagged one(Random rng) {
        List<String> chars = new ArrayList<>();
        List<String> tags = new ArrayList<>();

        // 建物名 = ブランド(+種別)(+地名)
        StringBuilder name = new StringBuilder(BRAND[rng.nextInt(BRAND.length)]);
        if (rng.nextDouble() < 0.7) name.append(TYPE[rng.nextInt(TYPE.length)]);
        if (rng.nextDouble() < 0.4) name.append(PLACE[rng.nextInt(PLACE.length)]);
        emit(chars, tags, name.toString(), "建物名");

        // 棟
        if (rng.nextDouble() < 0.45) {
            String w = WING[rng.nextInt(WING.length)];
            if (!w.isEmpty()) emit(chars, tags, w, "棟");
        }
        // 階
        if (rng.nextDouble() < 0.35) {
            String f = FLOOR[rng.nextInt(FLOOR.length)];
            if (!f.isEmpty()) emit(chars, tags, f, "階");
        }
        // 部屋（ハイフン区切りは O）
        double pr = rng.nextDouble();
        if (pr < 0.55) {
            int room = 1 + rng.nextInt(999);
            if (rng.nextDouble() < 0.4) {            // -203 形式（先頭ハイフンは O）
                emitO(chars, tags, "-");
                emit(chars, tags, String.valueOf(room), "部屋");
            } else if (rng.nextDouble() < 0.5) {     // 101号室
                emit(chars, tags, String.valueOf(room), "部屋");
                emitO(chars, tags, "号室");
            } else {                                  // 10号
                emit(chars, tags, String.valueOf(room), "部屋");
                emitO(chars, tags, "号");
            }
        }
        return new Tagged(chars, tags);
    }

    /** span を BIOES で追加。 */
    private static void emit(List<String> chars, List<String> tags, String s, String label) {
        List<String> cps = codePoints(s);
        if (cps.isEmpty()) return;
        if (cps.size() == 1) { chars.add(cps.get(0)); tags.add("S-" + label); return; }
        for (int i = 0; i < cps.size(); i++) {
            chars.add(cps.get(i));
            tags.add(i == 0 ? "B-" + label : i == cps.size() - 1 ? "E-" + label : "I-" + label);
        }
    }

    /** O ラベルで追加（区切り記号・接尾辞）。 */
    private static void emitO(List<String> chars, List<String> tags, String s) {
        for (String cp : codePoints(s)) { chars.add(cp); tags.add("O"); }
    }

    static List<String> codePoints(String s) {
        List<String> out = new ArrayList<>();
        s.codePoints().forEach(cp -> out.add(new String(Character.toChars(cp))));
        return out;
    }
}
