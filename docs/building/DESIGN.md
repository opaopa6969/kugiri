# kugiri-building 設計（建物名・建物階層・建物同定）

> kugiri の統計アルゴリズムで、building-hierarchy(BH) が解きたいことを「統計ファースト＋
> 差し替え可能」で解く拡張。本体 kugiri の純度（純JDK＋jpc）を守るため**別モジュール
> `building/`（artifactId: kugiri-building）**に分離。同一リポジトリ。

## 0. 何を解くか（BH の課題の引き受け）

1. **建物名抽出**：住所文字列から 建物名 / 棟 / 階 / 部屋番号 を切り出す。
   - BHの主力はルールベース（数十語の固定キーワード＋手書きパターン）で、本人が「もぐら叩きの限界・keyword依存・辞書外カタカナに弱い」と自認。
   - kugiri 案：**境界＝分岐エントロピー（辞書レス）＋ 系列ラベラ（perceptron+Viterbi、棟/階/部屋ラベルは既存）**。
2. **建物同定（本丸）**：同一住所に複数の建物名が並ぶとき「**本当に別建物か／表記ゆれで実は1棟か**」を見分ける。
   - BHは「正規化編集距離＋トークンJaccard の average-linkage（precision 0.959 / recall 0.391）」。recall が低く、`白雲荘/青雲荘`(距離1=別建物)・`第一/第二宿舎` を距離では分けられない。
   - kugiri 案：**コーパス誘導の対立度（PMI/分岐エントロピー）**で「違っている部分が対立的か（→別建物）／notation・種別か（→同一）」を判定。Phase0 で実証する。
3. **建物階層の構築・永続化**：複数行を 建物→棟→階→部屋 の木に集約し、Postgres に永続化＋検索。

## 0.1 「建物が複数」とは何か（ドメイン類型）

同一住所に複数の建物名が並ぶとき、意味は一様でない。型を分けて扱う：

| 型 | 例 | 正しい扱い | 判別の鍵 |
|----|----|-----------|---------|
| **A. 複数施設の敷地** | 自衛隊基地内の「宿舎／コンビニ／○○棟」、大学キャンパス、団地 | **別建物が複数（敷地＝site の子）** | 種別/固有名が別、相互に部分集合でない |
| **B. 本当に2棟** | 同一住所にアパートが2つ | **別建物** | 固有名核が対立的 |
| **C. 改名・別名** | 旧名／新名（同一物） | **同一建物（時系列エイリアス）** | 住所・部屋集合が一致、片方が他方の言い換え |
| **D. notation 揺れ** | コーポ山田／コ−ポ山田 | **同一** | 検索キー一致 |
| **E. 正式名 vs 略称/部分名** | 「県立○○施設○○寮」 と 「○○寮」 | **同一（略称は正式名の部分）** | **包含**（短核 ⊆ 長核）＋共有核が強い固有名 |
| **F. 略しすぎ衝突** | どちらも「寮」だけ | **テキストでは不能 → 要レビュー** | 共有核が種別語のみ。住所粒度/部屋集合/敷地で判断 |

設計上の含意：
- **A** は階層に **敷地(site)レベル**が要る（§5）。site 直下に複数 building。施設どうしは併合しない。
- **E（包含/略称）** は編集距離では遠いが同一。**Overlap 係数＝1.0（短核が長核に内包）**で拾う（BH clustering-design も Overlap を「短名吸収に最適」と明記）。ただし内包の共有部が**強い固有名**であること（`○○寮 ⊆ 県立○○施設○○寮` は ○○ が固有）。
- **F（衝突）** は原理的にテキスト単独では解けない（略称が識別部を落とす）。**SAME/DISTINCT を断定せず NEEDS_REVIEW**にし、**非テキスト証拠**（住所の粒度、部屋番号集合の排他/重複、同一敷地か）で確定する。
- **C（改名）** はテキストだけでは別名と区別しにくい。部屋集合一致＋人手 must-link（レビュー）で確定。`renamed` フラグで履歴を残す。

> 結論：identity の出力は **SAME / DISTINCT / NEEDS_REVIEW** の3値。テキスト統計（対立度・包含）で
> 取れる所を取り、取れない F/C は証拠レイヤ（住所・部屋・敷地）と人手レビューに渡す。

## 1. アルゴリズムの対応（BH → kugiri）

| 課題 | BH（現行） | kugiri 案（統計ファースト） |
|------|-----------|----------------------------|
| 住所/建物の境界 | 固定キーワード38+12語＋番地末尾検出 | 分岐エントロピー境界＋（任意で keyword 併用） |
| 建物名/棟/階/部屋 の分解 | 末尾から優先順位パターンで貪欲剥がし | 系列ラベラ（BIOES: 棟/階数/部屋番号/方書き）＝kugiri 既存 |
| 種別語 vs 固有名核 | 手書き TYPE_KEYWORDS(48)＋Kuromoji | PMI で自動分離（辞書レス） |
| 表記ゆれ畳み込み | 決定的 search key（NFKC/長音/数値合成…） | 同左を流用（jpc 正規化）＋誘導語彙の正規形 |
| variant vs distinct | 正規化編集距離＋Jaccard の閾値 | **対立度重み付き**＝誘導語彙で「対立的差分か」を判定 |
| typo 救済 | 編集距離（◎） | 同左（kugiri は置換しない＝編集距離にフォールバック） |
| 階の復元 | cross-row 桁ルール検証 | 同方式を移植（差し替え可能化） |

**要点**：kugiri は clustering/編集距離を**置き換えない**。clustering が走る**特徴量（identity核・対立度）を統計で供給**して precision と recall を同時に上げる。typo は編集距離に委ねる。

## 2. モジュール構成（同一リポ・別モジュール）

```
kugiri/                    ← 本体（純JDK＋jpc）。変更しない
building/                  ← 本モジュール kugiri-building（kugiri＋jpc に依存）
  identity/   建物同定（variant vs distinct）。Phase0 の主役
  parser/     建物名・棟・階・部屋 抽出（kugiri 系列ラベラ＋境界）
  hierarchy/  行→木 集約（建物→棟→階→部屋）
  store/      永続化（Postgres+Flyway）＋スナップショット。Phase1
  api/        REST（Javalin, optional）。Phase1
  demo/ /algo/（差し替え層）
docs/building/   本設計
docs/study2/     Study 第二部（妖精＝姿/スガタ）
```

Phase1 で `building/pom.xml` に kugiri / postgresql / flyway-core / jackson / javalin を追加（**重い依存は本モジュールにのみ**）。

## 3. 差し替え可能なアルゴリズム（動作オプション）

kugiri の `SequenceTagger` / BH の `ClusteringStrategy.of(name)` に倣い、各段をインターフェース化し
CLI/設定で選択。**同じ土俵で kugiri-統計 vs BH-ルール を対決**できるのが狙い。

```
BoundaryDetector   : entropy | keyword | hybrid
BuildingParser     : perceptron | rule | reverse
IdentityResolver   : contrastive(kugiri) | normalized(BH) | legacy(BH) | edit-baseline
FloorResolver      : digit-rule | explicit-only
```

各 IF は `of(String name)` で実装を返す。比較ハーネス（gold pairs に対する precision/recall）で
切替評価する（BH の `ClusteringTuner` / `GoldStandardEvaluator` に相当）。

## 4. 同定アルゴリズム（contrastive・Phase0 で実装）

```
decide(a, b, lexicon):           # lexicon は建物名コーパスから AzaInducer.fit で誘導
  ka, kb = searchKey(a), searchKey(b)         # jpc 正規化(NFKC/長音/数値合成…)
  if ka == kb: return SAME(notation)          # 決定的畳み込み（BH search key 相当）
  ta, tb = segment(a), segment(b)             # AzaInducer.segment
  # 種別語(高頻度・汎用)を PMI/頻度で落とし、末尾の棟記号(単英字/N棟)を分離
  ca, cb = identityCore(ta), identityCore(tb)
  D = symdiff(ca, cb)
  extraA, extraB = ca - cb, cb - ca
  if extraA empty and extraB empty: return SAME(type/wing差のみ)  # ライオンズマンション梅田 vs ライオンズ梅田
  if exactly one of (extraA, extraB) is empty:                    # 包含＝略称（型E）
      return (intersection に強い固有名あり) ? SAME(略称)
                                            : NEEDS_REVIEW(衝突の恐れ・型F)
  if allContrastive(extraA) and allContrastive(extraB): return DISTINCT
      # 白雲荘 vs 青雲荘（白雲/青雲が共に生産的）, 第一/第二宿舎
  return editDistanceFallback(a, b)            # 残差はtypo救済を編集距離に委ねる
```

- **3値出力**：SAME / DISTINCT / **NEEDS_REVIEW**。型F（略しすぎ衝突）と低信頼は review へ。
- **包含（Overlap=1.0）**で型E（正式名⊇略称）を SAME に。共有核が種別語のみなら断定せず review。
- **天井**：型F/C はテキスト単独で解けない。§5 の証拠（住所粒度・部屋集合の排他/重複・同一敷地）と
  人手 must/cannot-link で確定する。identity はテキストチャネルの最善を返すに留める。

- **productive(t)**：誘導語彙でその固有名トークンが複数の別名に現れる＝対立的。
- **enumerator**：`第[一二三…0-9]`、単独英字、東西南北新本別＋棟。
- これが BH の弱点（`白雲荘/青雲荘`誤併合・recall低）に対する処方。

## 5. 永続化（Phase1・Postgres+Flyway）

- 階層：**`site`(敷地, 型A) → `building` → `wing`(棟) → `floor` → `room`**。
  基地/キャンパス/団地のように1住所に複数施設がある場合は site 直下に複数 building（施設は併合しない）。
  単独建物では site を省略可（存在するレベルだけノード化＝可変深さ）。
- スキーマ：`site` / `building`（site_id, addressKey, canonical, renamed_from, …）/
  `building_alias`（表記ゆれ・略称→building_id, kind: notation|abbrev|rename）/
  `wing` / `floor` / `room` / `identity_review`（NEEDS_REVIEW・人手 must/cannot-link）。
- **証拠による確定**（型F/C）：部屋番号集合の排他/重複・階の重なり・住所粒度を building 単位で集計し、
  テキスト identity が NEEDS_REVIEW のペアを確定/分離する（CrossRow 証拠）。
- Flyway でマイグレーション管理。BH のスナップショット（StringPool+Deflate, DBレス）も
  読み出し高速化として併設可（差し替え）。
- 取込はバッチ：CSV → 境界/分解 → 同定 → 木集約 → upsert。BH の行→木と同構造。

## 6. Study 第二部（妖精＝姿/スガタ）

第一部（アザミ＝ラベルが無く見えない「字」の妖精）に対し、第二部は **スガタ**＝
**いくつもの姿（表記ゆれ）で現れる同一性の妖精**。問いは「その姿たちは“1人”なのか“別人”なのか」。
これは variant-vs-distinct そのもの。章立て案：

- 第二部プロローグ：同じ顔、ちがう名前（スガタ登場）
- 文字列距離だけでは解けない（白雲荘/青雲荘 の罠）
- 種別語と固有名核（PMI で“ありふれた語”を消す）
- 対立度：その違いは意味があるか、ノイズか（分岐エントロピー再訪）
- クラスタリングのきほん（編集距離・連結法・しきい値）— BHの簡単アルゴリズム
- 建物名を切り出す（境界＝分岐エントロピー、系列ラベラ再訪）
- 行を木に組む（建物→棟→階→部屋）
- 永続化と検索（Postgres、なぜ状態が要るか）
- アルゴリズムを切り替える（差し替え層・対決ハーネス）

> Phase0 では本 DESIGN にアウトラインを置き、章の本文は実装と並走して書く。

## 7. フェーズ

- **Phase0（本コミット）**：本設計＋同定プロトタイプ（`identity` パッケージ）で
  variant-vs-distinct を実証（編集距離ベースラインと対決）。
- **Phase1（進行中）**：
  - ✅ 1-1 建物名抽出 `parser`（rule＋kugiri語彙版＋**系列ラベラ版** `perceptron`。裸末尾数字の棟判定）。
    `BuildingParser.of("rule"|"lexicon"|"perceptron")`。perceptron は合成学習(BuildingTailSynth)で辞書レスに建物名/棟/階/部屋を BIOES ラベリング。
  - ✅ 1-2/3 行→木 `hierarchy`（同定で建物を束ね、住所→建物→棟→階→部屋・可変深さ）。
  - ✅ 差し替え層 `IdentityResolver.of` / `BuildingParser.of`（動作オプション）。
  - ✅ 1-4 永続化 `store`：`BuildingStore` IF＋`InMemoryStore`(DBレス・テスト)＋`PostgresStore`(JDBC＋Flyway, db/migration/V1__init.sql)＋`PersistencePipeline`。
  - ⏳ Study第二部本文（0〜2章済み、3〜9章は実装と並走）／系列ラベラ版パーサ／kugiri本体依存。
- **Phase2**：gold pairs での対決評価・証拠(部屋集合)で型F/C確定・BH との相互運用（提案 issue）・REST/UI。
