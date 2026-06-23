package org.unlaxer.kugiri.building.hierarchy;

import org.unlaxer.kugiri.building.identity.BuildingIdentity.Decision;
import org.unlaxer.kugiri.building.identity.EvidenceResolver;
import org.unlaxer.kugiri.building.identity.IdentityResolver;

import java.util.*;

/**
 * 部屋番号証拠を併用した建物名クラスタリング。テキスト同定が NEEDS_REVIEW（型F/C）になったペアを、
 * {@link EvidenceResolver}（部屋集合の重なり/排他）で確定してから束ねる。
 */
public final class EvidenceClusterer {
    private EvidenceClusterer() {}

    /** 建物名と、その名前で観測された部屋番号集合。 */
    public record NamedRooms(String name, Set<String> rooms) {}

    /** @param canonical 代表名（最頻出・同数は長い方） */
    public record Cluster(String canonical, Set<String> members, boolean needsReview) {}

    public static List<Cluster> cluster(List<NamedRooms> items, IdentityResolver resolver) {
        // 名前→頻度・部屋（同名の重複を集約）
        Map<String, Integer> freq = new LinkedHashMap<>();
        Map<String, Set<String>> rooms = new LinkedHashMap<>();
        for (NamedRooms it : items) {
            if (it.name().isEmpty()) continue;
            freq.merge(it.name(), 1, Integer::sum);
            rooms.computeIfAbsent(it.name(), k -> new LinkedHashSet<>()).addAll(it.rooms());
        }

        List<List<String>> groups = new ArrayList<>();
        List<Set<String>> groupRooms = new ArrayList<>();
        List<Boolean> review = new ArrayList<>();

        for (String n : freq.keySet()) {
            int joined = -1;
            boolean reviewHit = false;
            for (int g = 0; g < groups.size(); g++) {
                Decision d = resolver.decide(n, groups.get(g).get(0)).decision();
                if (d == Decision.NEEDS_REVIEW)
                    d = EvidenceResolver.resolve(d, rooms.get(n), groupRooms.get(g)); // 証拠で確定
                if (d == Decision.SAME) { joined = g; break; }
                if (d == Decision.NEEDS_REVIEW) reviewHit = true;
            }
            if (joined >= 0) {
                groups.get(joined).add(n);
                groupRooms.get(joined).addAll(rooms.get(n));
            } else {
                groups.add(new ArrayList<>(List.of(n)));
                groupRooms.add(new LinkedHashSet<>(rooms.get(n)));
                review.add(reviewHit);
            }
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
