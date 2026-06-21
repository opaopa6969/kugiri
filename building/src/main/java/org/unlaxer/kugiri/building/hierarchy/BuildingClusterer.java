package org.unlaxer.kugiri.building.hierarchy;

import org.unlaxer.kugiri.building.identity.BuildingIdentity;
import org.unlaxer.kugiri.building.identity.BuildingIdentity.Decision;
import org.unlaxer.kugiri.building.identity.BuildingLexicon;

import java.util.*;

/**
 * 同一住所内の建物名を {@link BuildingIdentity} で束ねる（single-linkage 近似）。
 * SAME は併合、DISTINCT は別、NEEDS_REVIEW は併合せず印を付ける（型F＝住所/部屋証拠待ち）。
 * 「その住所に建物が実際いくつあるか」を決める層。
 */
public final class BuildingClusterer {
    private BuildingClusterer() {}

    /** @param canonical 代表名（最頻出、同数は長い方） */
    public record Cluster(String canonical, Set<String> members, boolean needsReview) {}

    public static List<Cluster> cluster(List<String> names, BuildingLexicon lex) {
        // 出現頻度（代表名選出用）
        Map<String, Integer> freq = new LinkedHashMap<>();
        for (String n : names) if (!n.isEmpty()) freq.merge(n, 1, Integer::sum);

        List<List<String>> groups = new ArrayList<>(); // 各クラスタの member 名
        List<Boolean> review = new ArrayList<>();
        for (String n : freq.keySet()) {
            int joined = -1;
            boolean reviewHit = false;
            for (int g = 0; g < groups.size(); g++) {
                String rep = groups.get(g).get(0);
                Decision d = BuildingIdentity.contrastive(n, rep, lex).decision();
                if (d == Decision.SAME) { joined = g; break; }
                if (d == Decision.NEEDS_REVIEW) reviewHit = true;
            }
            if (joined >= 0) groups.get(joined).add(n);
            else { groups.add(new ArrayList<>(List.of(n))); review.add(reviewHit); }
        }

        List<Cluster> out = new ArrayList<>();
        for (int g = 0; g < groups.size(); g++) {
            List<String> mem = groups.get(g);
            String canonical = mem.get(0);
            int best = -1;
            for (String m : mem) {
                int f = freq.getOrDefault(m, 0);
                if (f > best || (f == best && m.length() > canonical.length())) { best = f; canonical = m; }
            }
            out.add(new Cluster(canonical, new LinkedHashSet<>(mem), review.get(g)));
        }
        return out;
    }
}
