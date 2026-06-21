package org.unlaxer.kugiri.label;

import java.util.*;

/** BIOES(=BILOU) タグ空間と、コンポーネント長 -> タグ列の展開・集約。 */
public final class Bioes {
    private Bioes() {}

    /** 全タグ(O + 各表層ラベルの B/I/E/S)。順序は安定。 */
    public static List<String> tags() {
        List<String> t = new ArrayList<>();
        t.add(Labels.OUTSIDE);
        for (String lab : Labels.SURFACE) {
            t.add("B-" + lab);
            t.add("I-" + lab);
            t.add("E-" + lab);
            t.add("S-" + lab);
        }
        return t;
    }

    /** 1コンポーネント(label,長さ length)を BIOES タグ列に展開。 */
    public static List<String> encode(String label, int length) {
        List<String> out = new ArrayList<>(Math.max(0, length));
        if (label.equals(Labels.OUTSIDE) || length == 0) {
            for (int i = 0; i < length; i++) out.add(Labels.OUTSIDE);
            return out;
        }
        if (length == 1) { out.add("S-" + label); return out; }
        out.add("B-" + label);
        for (int i = 0; i < length - 2; i++) out.add("I-" + label);
        out.add("E-" + label);
        return out;
    }

    /** codepoint 列 + BIOES タグ列 -> [(token,label)...]。壊れたタグも安全側に集約。 */
    public static List<String[]> decode(List<String> chars, List<String> tagsSeq) {
        List<String[]> out = new ArrayList<>();
        StringBuilder buf = new StringBuilder();
        String cur = null;
        for (int i = 0; i < chars.size(); i++) {
            String ch = chars.get(i), tag = tagsSeq.get(i);
            if (tag.equals(Labels.OUTSIDE)) {
                if (cur != null && !cur.equals(Labels.OUTSIDE)) { flush(out, buf, cur); cur = null; }
                cur = Labels.OUTSIDE; buf.append(ch); continue;
            }
            int dash = tag.indexOf('-');
            String pos = tag.substring(0, dash), lab = tag.substring(dash + 1);
            if (pos.equals("B") || pos.equals("S")) {
                flush(out, buf, cur); cur = lab; buf.append(ch);
                if (pos.equals("S")) { flush(out, buf, cur); cur = null; }
            } else { // I / E
                if (cur == null || !cur.equals(lab)) { flush(out, buf, cur); cur = lab; }
                buf.append(ch);
                if (pos.equals("E")) { flush(out, buf, cur); cur = null; }
            }
        }
        flush(out, buf, cur);
        // 連続する O を畳む
        List<String[]> merged = new ArrayList<>();
        for (String[] tl : out) {
            if (!merged.isEmpty() && tl[1].equals(Labels.OUTSIDE)
                    && merged.get(merged.size() - 1)[1].equals(Labels.OUTSIDE)) {
                merged.get(merged.size() - 1)[0] += tl[0];
            } else merged.add(tl);
        }
        return merged;
    }

    private static void flush(List<String[]> out, StringBuilder buf, String cur) {
        if (buf.length() > 0) {
            out.add(new String[]{buf.toString(), cur == null ? Labels.OUTSIDE : cur});
            buf.setLength(0);
        }
    }
}
