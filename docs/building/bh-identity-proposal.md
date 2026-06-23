# 提案ドラフト：building-hierarchy の建物同定に「対立度・包含・3値・部屋証拠」を導入

> これは `building-hierarchy`（opaopa6969/building-hierarchy）へ投げる提案 issue のドラフト。
> 外部リポへの issue 作成は自動承認されないため、内容をここに用意した。投稿はオーナーが実施する。
>
> 投稿コマンド例（リポジトリ作業ディレクトリで）:
> ```
> gh issue create -R opaopa6969/building-hierarchy \
>   --title "建物同定に「対立度・包含・3値・部屋証拠」を導入して recall と衝突判定を改善する提案" \
>   --body-file docs/building/bh-identity-proposal.md
> ```
> （body はこの見出し以下を貼るか、本ファイルを BH 側にコピーして使う）

---

## 背景・課題（building-hierarchy の自認）

`docs/clustering-design.md` と `NormalizedScoreStrategy`（pair precision 0.959 / **recall 0.391**）の通り、現行同定は「正規化編集距離＋identityトークンJaccard の average-linkage」。既知の弱点も明記：

- 距離では `白雲荘`/`青雲荘`（距離1=別建物）や `第一宿舎`/`第二宿舎` を分けにくい（§既知の弱点①）。
- 種別語分離が手書き `BuildingTokenizer.TYPE_KEYWORDS`(48語)＋Kuromoji 依存。辞書外カタカナに弱い。
- **recall 0.39**：真の表記ゆれの多くが自動併合できずレビュー行き。
- 「正式名 vs 略称」（`県立○○施設○○寮` と `○○寮`）は編集距離では遠いが同一。
- 「略しすぎ衝突」（どちらも `寮`）はテキストだけでは原理的に解けない。

## 提案：4つの特徴量を足す（統計ファースト・辞書依存を減らす）

`kugiri`（教師なし語彙誘導＝分岐エントロピー/PMI）で概念実証したものを特徴量として移植する。
**clustering 本体（編集距離・連結法）は据え置き**、比較する特徴を足すだけ。

1. **対立度（productivity）**：コーパスから「その固有名トークンが複数の別建物に現れるか」を学習。
   `白`/`青` は共に生産的 → `白雲荘`/`青雲荘` は対立的＝別建物。`サソ`(typo) は非生産的 → 編集距離へフォールバック。
   §1 の「重み付き編集距離」を手書きでなくデータから。
2. **包含（Overlap=1.0）**：短核 ⊆ 長核 を略称として同一に（`県立○○施設○○寮` ⊇ `○○寮`、`マンション`省略も特例）。
   §1 で既に「Overlap 係数◎短名吸収に最適」と挙がっている指標の本採用。**recall 改善**に直結。
3. **3値化（SAME / DISTINCT / NEEDS_REVIEW）**：略しすぎ衝突（共有核が種別語のみ）は断定せずレビューへ。
4. **部屋集合の証拠**：NEEDS_REVIEW を、同一住所の部屋番号集合の重なり→同一 / 排他→別、で確定。
   テキストの天井（型F=衝突・型C=改名）を非テキスト証拠で超える。本リポは `totalRooms` を持つので接続容易。

## 概念実証（kugiri、合成コーパス）

9ペアで：対立度＋包含＋3値（提案）= **9/9** / 正規化編集距離（現行相当）= 5/9 / 絶対編集距離（legacy相当）= 3/9。
`白雲荘/青雲荘＝別`・`県立さくら施設さくら寮/さくら寮＝同一(略称)`・`ライオンズマンション梅田/ライオンズ梅田＝同一`・
`寮/さくら寮＝要レビュー→部屋証拠で確定` を正答。編集距離は3値を出せず型Fで必ず外す。

## 具体的な接続先（building-hierarchy 側）

- `store/BuildingTokenizer`：`TYPE_KEYWORDS` を「コーパス頻度＋PMI で誘導した generic 判定」で補強/置換。
- `store/NormalizedScoreStrategy`：合成スコアに **対立度重み** と **Overlap（包含）** を追加（重みは `ClusteringTuner` で再チューニング）。
- `store/BuildingIdentityResolver`：status に NEEDS_REVIEW の確定段（部屋集合証拠）を追加。
- 評価：`GoldStandardEvaluator` の gold pairs で precision/recall 再測（特に recall 改善の確認）。

## 期待効果

- recall 0.39 の底上げ（略称・種別語省略を包含で拾う）＋ precision 維持（対立度で誤併合を防ぐ）。
- 手書き辞書メンテの軽減。型F/C を断定せず証拠＋人手に正しく回す。

参考実装：kugiri-building の `identity/`（BuildingLexicon / BuildingIdentity / EvidenceResolver）。
移植の最小パッチは別途用意可能。
