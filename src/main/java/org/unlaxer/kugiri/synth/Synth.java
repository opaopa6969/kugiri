package org.unlaxer.kugiri.synth;

import org.unlaxer.kugiri.label.*;
import org.unlaxer.kugiri.model.*;
import java.util.*;

/**
 * 学習データ生成エンジン。
 *
 * <p>住所を「階層コンポーネント列」として持ち、連結して文字列を作ると同時に codepoint 単位
 * ラベルが <em>アライメント不要で</em> 得られる、というのが中核。実データでは
 * {@code Abr}/{@code KenAll} アダプタが組んだコンポーネント列を {@link #fromRecords} に渡す。
 * 本クラスの合成器はアダプタ無しでもパイプラインを回すための代替。
 */
public final class Synth {
    private Synth() {}

    /** コンポーネント列 -> (codepoint列, BIOESタグ列)。 */
    public static Example buildExample(List<Component> comps) {
        List<String> chars = new ArrayList<>();
        List<String> tags = new ArrayList<>();
        for (Component c : comps) {
            List<String> cps = CodePoints.of(c.surface());
            chars.addAll(cps);
            tags.addAll(Bioes.encode(c.label(), cps.size()));
        }
        return new Example(chars, tags);
    }

    public static List<Example> fromRecords(List<List<Component>> records) {
        List<Example> out = new ArrayList<>();
        for (List<Component> r : records) out.add(buildExample(r));
        return out;
    }

    // ---------------- 表層オーグメント ----------------
    private static final String FW = "０１２３４５６７８９", HW = "0123456789";
    private static final String[] HYPHENS = {"-", "ー", "−", "－", "‐"};

    private static String digits(String s, String width) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            int fi = FW.indexOf(ch), hi = HW.indexOf(ch);
            if (width.equals("half") && fi >= 0) b.append(HW.charAt(fi));
            else if (width.equals("full") && hi >= 0) b.append(FW.charAt(hi));
            else b.append(ch);
        }
        return b.toString();
    }

    /** 同一意味で表層だけ揺らす(数字幅/ハイフン/丁目番号の記号化/〒)。ラベルは保持。 */
    public static List<Component> augment(List<Component> comps, Random rng) {
        String[] widths = {"half", "full", "keep", "keep"};
        String width = widths[rng.nextInt(widths.length)];
        String hyph = HYPHENS[rng.nextInt(HYPHENS.length)];
        List<Component> out = new ArrayList<>();
        for (Component c : comps) {
            String s = digits(c.surface(), width);
            if ((c.label().equals("丁目") || c.label().equals("街区符号")
                    || c.label().equals("住居番号")) && rng.nextDouble() < 0.35) {
                s = s.replace("丁目", hyph).replace("番地", hyph)
                     .replace("番", hyph).replace("号", hyph).replace("地割", hyph);
            }
            out.add(new Component(c.label(), s));
        }
        if (!out.isEmpty() && out.get(0).label().equals("ZIP") && rng.nextDouble() < 0.5)
            out.set(0, new Component("ZIP", "〒" + out.get(0).surface()));
        return out;
    }

    // ---------------- 合成住所ジェネレータ(代替データ) ----------------
    private static final String[] PREF = {"東京都","神奈川県","大阪府","岩手県","京都府","北海道"};
    private static final String[] CITY = {"千代田区","横浜市","大阪市","盛岡市","川崎市","京都市"};
    private static final String[] WARD = {"中区","北区","西区","都筑区","左京区"};
    private static final String[] OAZA = {"丸の内","本町","栄","みなとみらい","高松","桜ヶ丘","上田"};
    private static final String[] BLDG = {"%sビル","%sマンション","%sタワー","%sハイツ"};
    private static final String[] BLDG_NAME = {"朝日","中央","グランド","パーク","みなと"};
    private static final String KANJI_NUM = "一二三四五六七八九十";

    private static List<Component> genComponents(Random rng) {
        List<Component> c = new ArrayList<>();
        String zip5 = String.format("%03d-%04d", 100 + rng.nextInt(900), rng.nextInt(10000));
        c.add(new Component("ZIP", zip5));
        c.add(new Component("都道府県", PREF[rng.nextInt(PREF.length)]));
        c.add(new Component("市", CITY[rng.nextInt(CITY.length)]));
        if (rng.nextDouble() < 0.5) c.add(new Component("区", WARD[rng.nextInt(WARD.length)]));
        c.add(new Component("町または大字", OAZA[rng.nextInt(OAZA.length)]));
        if (rng.nextDouble() < 0.7)
            c.add(new Component("丁目", KANJI_NUM.charAt(rng.nextInt(KANJI_NUM.length())) + "丁目"));
        if (rng.nextDouble() < 0.6) {
            c.add(new Component("街区符号", (1 + rng.nextInt(30)) + "番"));
            c.add(new Component("住居番号", (1 + rng.nextInt(40)) + "号"));
            if (rng.nextDouble() < 0.2) c.add(new Component("枝番号", "-" + (1 + rng.nextInt(9))));
        } else {
            c.add(new Component("地番", (1 + rng.nextInt(2000)) + "番地"));
        }
        if (rng.nextDouble() < 0.5) {
            String bld = String.format(BLDG[rng.nextInt(BLDG.length)], BLDG_NAME[rng.nextInt(BLDG_NAME.length)]);
            c.add(new Component("棟", bld));
            if (rng.nextDouble() < 0.7)
                c.add(new Component("部屋番号", String.format("%d%02d号室", 1 + rng.nextInt(15), 1 + rng.nextInt(20))));
        }
        return c;
    }

    public static List<Example> makeDataset(int n, long seed) {
        Random rng = new Random(seed);
        List<Example> data = new ArrayList<>(n);
        for (int i = 0; i < n; i++) data.add(buildExample(augment(genComponents(rng), rng)));
        return data;
    }
}
