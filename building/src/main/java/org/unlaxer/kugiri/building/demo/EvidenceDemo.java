package org.unlaxer.kugiri.building.demo;

import org.unlaxer.kugiri.building.identity.BuildingIdentity;
import org.unlaxer.kugiri.building.identity.BuildingIdentity.Decision;
import org.unlaxer.kugiri.building.identity.BuildingLexicon;
import org.unlaxer.kugiri.building.identity.EvidenceResolver;
import org.unlaxer.kugiri.building.identity.SampleCorpus;

import java.util.Set;

/**
 * 証拠による型F/C確定デモ。テキストでは NEEDS_REVIEW（略しすぎ衝突）になるペアを、
 * 部屋番号集合の重なり/排他で SAME / DISTINCT に確定する。
 */
public final class EvidenceDemo {
    public static void main(String[] args) {
        BuildingLexicon lex = BuildingLexicon.learn(SampleCorpus.names());

        record Case(String a, String b, Set<String> roomsA, Set<String> roomsB, Decision truth, String note) {}
        Case[] cases = {
                // 「寮」と「さくら寮」：同じ部屋を両名で呼ぶ → 略称（同一）
                new Case("寮", "さくら寮", Set.of("201", "202"), Set.of("201", "202"),
                        Decision.SAME, "同じ部屋集合 → さくら寮の略称"),
                // 「寮」と「さくら寮」：部屋が排他 → 別建物
                new Case("寮", "さくら寮", Set.of("101", "102"), Set.of("201", "202"),
                        Decision.DISTINCT, "排他な部屋集合 → 別の寮"),
                // 証拠が無い（部屋不明）→ 人手レビューのまま
                new Case("寮", "さくら寮", Set.of(), Set.of("201"),
                        Decision.NEEDS_REVIEW, "片方に部屋情報なし → 証拠不足"),
        };

        System.out.println("=== 型F（略しすぎ衝突）を部屋証拠で確定 ===");
        for (Case c : cases) {
            Decision text = BuildingIdentity.contrastive(c.a(), c.b(), lex).decision();
            Decision fin = EvidenceResolver.resolve(text, c.roomsA(), c.roomsB());
            System.out.printf("%s / %s  rooms %s vs %s%n", c.a(), c.b(), c.roomsA(), c.roomsB());
            System.out.printf("   テキスト=%s → 証拠確定=%-12s %s  [%s]%n",
                    text, fin, fin == c.truth() ? "○" : "×", c.note());
        }
        System.out.println("\n※ テキストの天井（型F/C）を、部屋集合という非テキスト証拠で超える。");
        System.out.println("   証拠も無ければ NEEDS_REVIEW のまま人手レビューへ（断定しない）。");
    }
}
