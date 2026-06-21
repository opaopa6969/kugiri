package org.unlaxer.kugiri.label;

/**
 * 原 {@code 階層要素} enum の忠実移植(要件アーカイブ)。住所の階層タクソノミを
 * id / level / isTop(構造マーカーか) / description で保持する。
 *
 * <p>パイプライン(tagger/synth/abr/aza)は表層ラベル文字列({@link Labels})を直接使うため
 * 本 enum は参照・対応付け用。isTop=true は文字列スパンを持たない構造ノード(出力ラベル外)。
 */
public enum Hierarchy {
    ZIP("0", 0, false, "郵便番号"),
    全体("z", 0, true, "全体"),
    空("y", 0, false, "要素外、empty"),
    不明("x", 0, false, "不明"),

    国域Top1("1", 1, true, "都道府県"),
    国域Top2("2", 2, true, "市、群"),
    国域Top3("3", 3, true, "区"),
    国域Top4("4", 4, true, "町村"),

    都道府県("1", 1, false, "都道府県"),
    その他("1", 1, false, "どの市区町村にも所属しない土地(埋立地/鳥島/須美寿島)"),
    東京23区("2", 2, false, "特別区"),
    政令指定市("2", 2, false, "政令指定都市"),
    市("2", 2, false, "市"),
    群("2", 2, false, "郡"),
    区("3", 3, false, "政令市の区"),
    町村("4", 4, false, "町村"),
    町または大字("4", 4, false, "姫路市の区、長崎県の免/里など"),

    町域Top1("5", 5, true, "丁目(3階層)や番地(2階層)など"),
    町域Top2("6", 6, true, "無番地含む。番地(3階層)や号(2階層)など"),
    町域Top3("7", 7, true, "号(3階層)や枝番号(2階層)や部屋番号など"),
    町域Top4("8", 8, true, "枝番号(3階層)や枝番号"),

    丁目("5", 5, false, "丁目なしもあり"),
    字小字("5", 5, false, "字・小字"),
    地番("6", 6, false, "無番地含む"),
    街区符号("6", 6, false, "番など"),
    住居番号("7", 7, false, "号など"),
    支号("7", 7, false, "地番に対する枝番号"),
    枝番号("8", 8, false, "住居番号に対する枝番号"),

    建物("9", 9, true, "建物"),
    建物Bottom4("10", 10, true, "区画"),
    建物Bottom3("11", 11, true, "棟"),
    建物Bottom2("12", 12, true, "階数"),
    建物Bottom1("13", 13, true, "部屋番号"),

    区画("10", 10, false, "区画"),
    棟("11", 11, false, "棟"),
    階数("12", 12, false, "階数"),
    部屋番号("13", 13, false, "部屋番号"),

    方書き("14", 14, true, "方書き"),
    ダミーサフィックス("15", 15, true, "ダミー");

    public final String id;
    public final int level;
    public final boolean isTop;
    public final String description;

    Hierarchy(String id, int level, boolean isTop, String description) {
        this.id = id; this.level = level; this.isTop = isTop; this.description = description;
    }

    /** tokenizer の出力対象となる表層リーフか(= isTop=false かつ表層スパンを持つ)。 */
    public boolean isSurface() { return !isTop && Labels.LEVEL.containsKey(name()); }
}
