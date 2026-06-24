package org.unlaxer.addressbench;

/**
 * ハイブリッド：基準の {@link BhSplitter}（高精度ルール）を主に、建物を検出できなかったときだけ
 * {@link KugiriSplitter}（辞書外カタカナ建物等を拾える ML）で補完する recall 重視の合成。
 */
public final class HybridSplitter implements AddressSplitter {

    private final AddressSplitter bh;
    private final AddressSplitter kugiri;

    public HybridSplitter(AddressSplitter bh, AddressSplitter kugiri) {
        this.bh = bh;
        this.kugiri = kugiri;
    }

    @Override
    public String name() { return "hybrid"; }

    @Override
    public Split split(String address) {
        Split b = bh.split(address);
        if (!b.buildingResidual().isEmpty()) return b;     // BH が建物を検出 → 採用
        Split k = kugiri.split(address);
        return k.buildingResidual().isEmpty() ? b : k;      // BH 取りこぼしを kugiri で補完
    }
}
