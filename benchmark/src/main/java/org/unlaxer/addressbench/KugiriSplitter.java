package org.unlaxer.addressbench;

import org.unlaxer.kugiri.model.Component;
import org.unlaxer.kugiri.synth.Synth;
import org.unlaxer.kugiri.tagger.AddressParser;

import java.util.List;
import java.util.Set;

/**
 * kugiri の系列ラベラ {@link AddressParser} を委譲する ML 実装。合成データで自己学習し、
 * parse 結果を「建物系ラベル（区画/棟/階数/部屋番号/方書き）」の最初の境界で2分割する。
 *
 * <p>学習が合成のみのため、実データ（語彙が辞書外）での強さがベンチで分かる＝対比の主役。
 */
public final class KugiriSplitter implements AddressSplitter {

    /** 建物以降とみなすラベル（これより前が住所部分）。 */
    private static final Set<String> BUILDING = Set.of("区画", "棟", "階数", "部屋番号", "方書き");

    private final AddressParser parser;

    public KugiriSplitter() {
        this.parser = new AddressParser().fit(Synth.makeDataset(4000, 42), 10);
    }

    @Override
    public String name() { return "kugiri"; }

    @Override
    public Split split(String address) {
        List<Component> comps = parser.parse(address);
        StringBuilder addr = new StringBuilder(), bld = new StringBuilder();
        boolean inBuilding = false;
        for (Component c : comps) {
            if (!inBuilding && BUILDING.contains(c.label())) inBuilding = true;
            (inBuilding ? bld : addr).append(c.surface());
        }
        return bld.length() == 0 ? Split.noBuilding(addr.toString())
                                 : new Split(addr.toString(), bld.toString());
    }
}
