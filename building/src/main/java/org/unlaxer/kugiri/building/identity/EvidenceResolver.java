package org.unlaxer.kugiri.building.identity;

import org.unlaxer.kugiri.building.identity.BuildingIdentity.Decision;

import java.util.HashSet;
import java.util.Set;

/**
 * テキストだけでは断定できない同定（型F=略しすぎ衝突 / 型C=改名疑い＝{@link Decision#NEEDS_REVIEW}）を、
 * <b>部屋番号集合という非テキスト証拠</b>で確定する。設計 DESIGN §0.1 / §5 の「証拠による確定」。
 *
 * <ul>
 *   <li>同じ部屋番号を両名が持つ → 同じ物理部屋を2通りに呼んでいる ＝ <b>SAME</b>（表記ゆれ/略称）</li>
 *   <li>部屋集合が排他（重なりゼロ）→ 別の部屋群 ＝ <b>DISTINCT</b>（別建物/別施設）</li>
 *   <li>どちらかに部屋情報が無い → 証拠不足で <b>NEEDS_REVIEW</b> のまま（人手へ）</li>
 * </ul>
 * テキスト判定が SAME/DISTINCT で確信できている場合はそれを尊重する（証拠で覆さない）。
 */
public final class EvidenceResolver {
    private EvidenceResolver() {}

    public static Decision resolve(Decision textDecision, Set<String> roomsA, Set<String> roomsB) {
        if (textDecision != Decision.NEEDS_REVIEW) return textDecision;
        if (roomsA == null || roomsB == null || roomsA.isEmpty() || roomsB.isEmpty())
            return Decision.NEEDS_REVIEW; // 証拠なし
        Set<String> inter = new HashSet<>(roomsA);
        inter.retainAll(roomsB);
        return inter.isEmpty() ? Decision.DISTINCT : Decision.SAME;
    }
}
