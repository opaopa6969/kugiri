package org.unlaxer.addressbench;

import org.unlaxer.building.parser.UnlaxerBuildingParser;

/**
 * building-hierarchy の現役パーサ {@link UnlaxerBuildingParser#splitBuildingTail} を委譲する基準実装。
 * 純JDK・ステートレス・即時。境界未検出時は残差なし。
 */
public final class BhSplitter implements AddressSplitter {

    private final UnlaxerBuildingParser parser = new UnlaxerBuildingParser();

    @Override
    public String name() { return "bh"; }

    @Override
    public Split split(String address) {
        return parser.splitBuildingTail(address)
                .map(t -> new Split(t.addressPart(), t.buildingTail()))
                .orElseGet(() -> Split.noBuilding(address));
    }
}
