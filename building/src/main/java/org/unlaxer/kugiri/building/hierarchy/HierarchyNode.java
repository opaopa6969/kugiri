package org.unlaxer.kugiri.building.hierarchy;

import java.util.ArrayList;
import java.util.List;

/**
 * 可変深さの建物階層ノード。住所→建物→棟→階→部屋のうち、<b>存在するレベルだけ</b>ノード化する
 * （棟が無ければ建物直下に階、階が不明なら建物/棟直下に部屋）。
 */
public final class HierarchyNode {

    public enum Level { ADDRESS, BUILDING, WING, FLOOR, ROOM }

    private final Level level;
    private final String label;
    private final List<HierarchyNode> children = new ArrayList<>();
    /** 同定で別建物の疑いだが断定できない（型F）等のレビュー印。 */
    private boolean needsReview;

    public HierarchyNode(Level level, String label) {
        this.level = level;
        this.label = label;
    }

    public Level level() { return level; }
    public String label() { return label; }
    public List<HierarchyNode> children() { return children; }
    public boolean needsReview() { return needsReview; }
    public HierarchyNode markReview() { this.needsReview = true; return this; }

    public HierarchyNode child(Level lv, String lbl) {
        for (HierarchyNode c : children)
            if (c.level == lv && c.label.equals(lbl)) return c;
        HierarchyNode c = new HierarchyNode(lv, lbl);
        children.add(c);
        return c;
    }

    /** 末端（部屋）数を数える。 */
    public int leafCount() {
        if (children.isEmpty()) return 1;
        int n = 0;
        for (HierarchyNode c : children) n += c.leafCount();
        return n;
    }

    /** インデント付きの木表示。 */
    public String pretty() {
        StringBuilder sb = new StringBuilder();
        print(sb, 0);
        return sb.toString();
    }

    private void print(StringBuilder sb, int depth) {
        sb.append("  ".repeat(depth))
          .append(label.isEmpty() ? "(" + level + ")" : label)
          .append(needsReview ? "  ⚠要レビュー" : "")
          .append('\n');
        for (HierarchyNode c : children) c.print(sb, depth + 1);
    }
}
