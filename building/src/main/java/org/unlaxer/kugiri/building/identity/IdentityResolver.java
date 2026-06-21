package org.unlaxer.kugiri.building.identity;

import org.unlaxer.kugiri.building.identity.BuildingIdentity.Verdict;

/**
 * 「2つの建物名は同一か別か」を判定する差し替え可能な層（動作オプション）。
 * {@link BuildingClusterer} や CLI はこの interface 越しにアルゴリズムを切り替える。
 *
 * <ul>
 *   <li>{@code "contrastive"} — kugiri 統計（包含＋対立度＋enumerator、3値）</li>
 *   <li>{@code "edit-normalized"} — 正規化編集距離（BH 現行相当・2値）</li>
 *   <li>{@code "edit-absolute"}   — 絶対編集距離（BH legacy 相当・2値）</li>
 * </ul>
 */
public interface IdentityResolver {

    Verdict decide(String a, String b);

    String name();

    static IdentityResolver of(String name, BuildingLexicon lex) {
        return switch (name) {
            case "contrastive" -> new IdentityResolver() {
                public Verdict decide(String a, String b) { return BuildingIdentity.contrastive(a, b, lex); }
                public String name() { return "contrastive"; }
            };
            case "edit-normalized" -> new IdentityResolver() {
                public Verdict decide(String a, String b) { return BuildingIdentity.editNormalized(a, b, 0.76); }
                public String name() { return "edit-normalized"; }
            };
            case "edit-absolute" -> new IdentityResolver() {
                public Verdict decide(String a, String b) { return BuildingIdentity.editAbsolute(a, b, 4); }
                public String name() { return "edit-absolute"; }
            };
            default -> throw new IllegalArgumentException(
                    "Unknown identity resolver: " + name + " (contrastive | edit-normalized | edit-absolute)");
        };
    }
}
