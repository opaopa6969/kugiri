package org.unlaxer.kugiri.building.identity;

import org.unlaxer.jaddress.aza.AzaInducer;
import org.unlaxer.jaddress.normalizer.NFKC_CF;

import java.util.*;

/**
 * 建物名コーパスから、kugiri の教師なし語彙誘導（{@link AzaInducer}：分岐エントロピー＋PMI＋
 * 最尤分割）で「種別語 vs 固有名核」と「対立的（生産的）トークン」を学ぶ辞書。
 *
 * <p>BH が手書きの {@code TYPE_KEYWORDS}(48語)＋Kuromoji で行う type/identity 分離を、
 * <b>辞書レス・コーパス駆動</b>で置き換えるのが狙い。
 */
public final class BuildingLexicon {

    private final AzaInducer inducer;
    private final Map<String, Integer> df = new HashMap<>(); // token -> 出現した建物名数(document frequency)
    private final int nNames;
    private final int typeDf; // これ以上の df を持つトークンは「ありふれた種別語」とみなす

    private BuildingLexicon(AzaInducer inducer, int nNames, int typeDf) {
        this.inducer = inducer;
        this.nNames = nNames;
        this.typeDf = typeDf;
    }

    /** コーパスから誘導する。 */
    public static BuildingLexicon learn(List<String> corpus) {
        // 建物名は字より長いので maxLen を広げ、minCount を下げて固有名2語も拾う。
        AzaInducer inducer = new AzaInducer(2, 1, 8, 0.0, 0.4, 0.3, 0.5).fit(corpus);
        // 種別語しきい値: コーパスの約15%以上の建物名に現れるトークン（ありふれている）。
        int typeDf = Math.max(3, (int) Math.round(corpus.size() * 0.15));
        BuildingLexicon lex = new BuildingLexicon(inducer, corpus.size(), typeDf);
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

    /** ありふれた種別語か（高 df）。 */
    public boolean isType(String token) { return df(token) >= typeDf; }

    /** 生産的な固有名トークンか（複数の別建物に現れる＝対立的だが、種別語ではない）。 */
    public boolean isProductive(String token) { return df(token) >= 2 && !isType(token); }

    public int corpusSize() { return nNames; }
    public int typeDfThreshold() { return typeDf; }
    public Map<String, Integer> documentFrequencies() { return Collections.unmodifiableMap(df); }
}
