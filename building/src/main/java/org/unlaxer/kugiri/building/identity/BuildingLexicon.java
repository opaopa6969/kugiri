package org.unlaxer.kugiri.building.identity;

import org.unlaxer.jaddress.aza.AzaInducer;
import org.unlaxer.jaddress.normalizer.NFKC_CF;

import java.util.*;

/**
 * 建物名コーパスから、kugiri の教師なし語彙誘導（{@link AzaInducer}：分岐エントロピー＋PMI＋
 * 最尤分割）で「固有名トークンの生産性（対立度）」を学ぶ辞書。
 *
 * <p>同定の主役は<b>対立度（productive）・包含・enumerator＝辞書レス</b>。汎用語(種別/資格)は
 * 「略しすぎ衝突（型F）」の検出にのみ使い、最小 seed＋超高頻度で判定する（Phase1 で PMI 自動化）。
 */
public final class BuildingLexicon {

    /** 単独では建物を識別しない汎用語の最小 seed（型F=衝突検出にのみ使用）。 */
    static final Set<String> SEED_GENERIC = Set.of(
            "マンション", "ハイツ", "コーポ", "ビル", "アパート", "メゾン", "ハイム",
            "荘", "団地", "宿舎", "住宅", "寮", "館", "棟", "号棟", "施設", "会館",
            "センター", "号室", "号");

    private final AzaInducer inducer;
    private final Map<String, Integer> df = new HashMap<>(); // token -> 出現した建物名数
    private final int nNames;
    private final int genericDf; // これ以上の df かつ seed 的なら超汎用とみなす

    private BuildingLexicon(AzaInducer inducer, int nNames, int genericDf) {
        this.inducer = inducer;
        this.nNames = nNames;
        this.genericDf = genericDf;
    }

    /** コーパスから誘導する。 */
    public static BuildingLexicon learn(List<String> corpus) {
        AzaInducer inducer = new AzaInducer(2, 1, 8, 0.0, 0.4, 0.3, 0.5).fit(corpus);
        int genericDf = Math.max(5, (int) Math.round(corpus.size() * 0.30));
        BuildingLexicon lex = new BuildingLexicon(inducer, corpus.size(), genericDf);
        for (String name : corpus) {
            for (String tok : new HashSet<>(lex.segment(name))) lex.df.merge(tok, 1, Integer::sum);
        }
        return lex;
    }

    /** 名前をトークン分割（正規化してから AzaInducer.segment）。 */
    public List<String> segment(String name) {
        return inducer.segment(NFKC_CF.normalize(name));
    }

    /** あるトークンが現れた建物名数。 */
    public int df(String token) { return df.getOrDefault(token, 0); }

    /** 単独では建物を識別しない汎用語か（seed、または超高頻度）。 */
    public boolean isGeneric(String token) {
        return SEED_GENERIC.contains(token) || df(token) >= genericDf;
    }

    /** 生産的な固有名トークンか（複数の別建物に現れ＝対立的、かつ汎用語でない）。 */
    public boolean isProductive(String token) { return df(token) >= 2 && !isGeneric(token); }

    public int corpusSize() { return nNames; }
    public Map<String, Integer> documentFrequencies() { return Collections.unmodifiableMap(df); }
}
