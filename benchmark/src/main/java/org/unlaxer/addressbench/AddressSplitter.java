package org.unlaxer.addressbench;

/**
 * 住所文字列を「住所部分（都道府県〜番地・号）」と「建物残差（建物名/棟/階/部屋…）」に分割する
 * 統一インターフェース。建物階層辞書づくりの前段：残差＝辞書に入れる建物文字列。
 */
public interface AddressSplitter {

    /**
     * @param addressPart      番地・号までの住所部分（原文）
     * @param buildingResidual 建物以降の残差（無ければ空文字）
     */
    record Split(String addressPart, String buildingResidual) {
        public static Split noBuilding(String full) { return new Split(full, ""); }
    }

    /** 実装名（kugiri / bh / hybrid / onigiri / abr …）。 */
    String name();

    /** 住所1本を (住所部分, 建物残差) に分割。 */
    Split split(String address);
}
