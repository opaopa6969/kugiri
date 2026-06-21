package org.unlaxer.kugiri.building.parser;

/**
 * 建物テール（番地より後ろ）を分解した結果。
 *
 * @param name  建物名（固有名＋種別語。例: 府営柱本団地）
 * @param wing  棟（例: B2 / A / 4B / 南棟。空=なし）
 * @param floor 階（表層。例: 4F / 1階 / B1階。空=なし）
 * @param room  部屋番号（例: 407 / 101。空=なし）
 */
public record ParsedBuilding(String name, String wing, String floor, String room) {

    public static ParsedBuilding of(String name) {
        return new ParsedBuilding(name == null ? "" : name, "", "", "");
    }

    public ParsedBuilding withWing(String w)  { return new ParsedBuilding(name, w, floor, room); }
    public ParsedBuilding withFloor(String f) { return new ParsedBuilding(name, wing, f, room); }
    public ParsedBuilding withRoom(String r)  { return new ParsedBuilding(name, wing, floor, r); }
    public ParsedBuilding withName(String n)  { return new ParsedBuilding(n, wing, floor, room); }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append("建物名=").append(name.isEmpty() ? "-" : name);
        if (!wing.isEmpty())  b.append(" 棟=").append(wing);
        if (!floor.isEmpty()) b.append(" 階=").append(floor);
        if (!room.isEmpty())  b.append(" 部屋=").append(room);
        return b.toString();
    }
}
