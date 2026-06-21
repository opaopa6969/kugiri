# HANDOFF — Claude Code 引き継ぎ

このドキュメントは Claude Code が `kugiri` を継続実装するための作業指示書。
背景・設計の全体像は `docs/DESIGN.md` を必読。プロジェクト方針は ルート `CLAUDE.md`。

---

## 0. 一言で

ZIP＋住所文字列を `階層要素` ごとに切り出す ML トークナイザ。
**ラベルの無い「字(小字)」を教師なしで推定する**のが主眼。依存ゼロの純 JDK 実装。

---

## 1. 現状（動作確認済み）

- JDK 21 / Maven プロジェクト。**外部依存ゼロ**でコンパイル・実行可能。
- 3つのデモが動く:
  - `SynthDemo` … 合成データで end-to-end（tag accuracy 1.000 ※規則性ゆえ。実力ではない）
  - `AbrDemo` … KEN_ALL→ABR 弱教師ラベル生成（東京23区/市+区/群+町村 の分離を確認）
  - `AzaDemo` … 教師ゼロの字推定。従来 F1 0.829、分岐エントロピー併用(T4・既定)で F1 1.000 / 完全一致 1.000（合成コーパス。実データは別途要評価）
- 系列ラベラは平均化構造化パーセプトロン+Viterbi（BIOES 合法性マスク付き）。CRF 差し替え可能。

未実装（＝次タスク）: 実データ取込、実評価基盤、self-training、BE 併用、CRF/BERT 差し替え、
建物/方書き辞書、partial-CRF。

---

## 2. ビルド & 実行

```bash
# Maven（推奨・opa 環境）
mvn -q compile
mvn -q exec:java -Dexec.mainClass=org.unlaxer.kugiri.demo.AzaDemo
mvn -q exec:java -Dexec.mainClass=org.unlaxer.kugiri.demo.AbrDemo
mvn -q exec:java -Dexec.mainClass=org.unlaxer.kugiri.demo.SynthDemo

# 依存なしなので javac/java 直叩きでも可（Maven Central 不通環境向け）
find src/main/java -name '*.java' > /tmp/srcs.txt
javac -encoding UTF-8 -d target/classes @/tmp/srcs.txt
cp -r src/main/resources/* target/classes/
java -Dstdout.encoding=UTF-8 -cp target/classes org.unlaxer.kugiri.demo.AzaDemo
```

> 出力が文字化けする場合は `-Dstdout.encoding=UTF-8` を付ける。

> 注: 0.1.0 以降は `japanese-parser-common`(GitHub Packages, private)に依存するため、
> 「javac 直叩き」だけでは jpc を解決できない。まず jpc を `mvn install` で local .m2 に入れるか、
> `~/.m2/settings.xml` に unlaxer-bom registry の read:packages 認証を設定すること。
> 本体の*コード*は引き続き純 JDK + jpc のみ（重い ML 依存は持ち込まない）。

### CI（GitHub Actions）
- `.github/workflows/ci.yml`: JDK21 で `caulis/japanese-parser-common` を checkout→`mvn install`
  してから kugiri を `mvn test` し、3デモを smoke 実行する。
- **要 secret**: `PACKAGES_READ_TOKEN`（read:packages＋caulis repo の contents:read を持つ PAT）。
  unlaxer-bom registry(private)と jpc ソース取得に必要。未設定だと依存解決で失敗する。
- jpc 側(caulis/japanese-parser-common)は既に `ci.yml`（compile+test）と `publish.yml`
  （release で GitHub Packages へ deploy、要 `PACKAGES_PUBLISH_TOKEN`）を持つ。

---

## 3. リポジトリ地図

```
pom.xml                     依存ゼロ・JDK21
CLAUDE.md                   Claude Code 用プロジェクトメモリ
docs/DESIGN.md              設計全体（必読）
docs/HANDOFF.md             本書
src/main/java/org/unlaxer/kugiri/
  label/   Hierarchy Labels Bioes CodePoints
  model/   Component Example
  synth/   Synth
  feature/ Features
  tagger/  PerceptronTagger AddressParser
  abr/     Csv KenAll Abr
  aza/     Aza（字推定は jpc 0.3.0 org.unlaxer.jaddress.aza へ移設、本クラスはアダプタ）
  eval/    SpanEval（entity スパン F1・混同行列）
  ingest/  IngestCli（実 KEN_ALL/ABR 取込 CLI）
  tagger/  PerceptronTagger AddressParser SelfTrainer
  demo/    SynthDemo AbrDemo AzaDemo EvalDemo SelfTrainDemo Res
src/main/resources/sample_data/  KEN_ALL + ABR 4マスタ サンプル（実スキーマ準拠）
docs/glossary/  DDE 用語集（住所ドメイン13語・日英）
```

---

## 4. 次タスク（優先順・受け入れ条件つき）

### T1. 実 ABR/KEN_ALL 取込パイプライン
- `abr.Abr.buildRecords` を実ファイルで叩けるよう CLI/ローダを追加。
- KEN_ALL は **CP932**（`StandardCharsets` ではなく `Charset.forName("Windows-31J")`）。
- ABR 列名を実ダンプの版で検証（`mt_town_all` / `mt_rsdtdsp_blk` / `mt_rsdtdsp_rsdt` / `mt_parcel`）。
  列が違えば `Abr` 内の `get("列名")` の名前のみ修正。
- **受け入れ**: 1県（例: 神奈川の住居表示）を流して N>10万件の Component 列が生成され、
  破綻なく `Synth.fromRecords` → 学習まで通る。未マッチ率をログ出力。
  → 🟡 CLI `ingest.IngestCli` 実装済み（CP932 既定、未マッチ率ログ、`--train` で hold-out 学習＋
  スパン評価）。サンプル(UTF-8)で検証済み・`AbrTest` で弱教師の頭階層分離を回帰。
  **実データ1県(N>10万)の試走は運用者待ち**（生データは非コミットのため当環境で不可）。実行例:
  ```
  mvn -q exec:java -Dexec.mainClass=org.unlaxer.kugiri.ingest.IngestCli \
    -Dexec.args="--kenall /path/KEN_ALL.CSV --town /path/mt_town_all.csv \
      --blk /path/mt_rsdtdsp_blk.csv --rsdt /path/mt_rsdtdsp_rsdt.csv \
      --parcel /path/mt_parcel.csv --train --augment 5"
  ```

### T2. 実評価基盤
- 手ラベル少数（数百件）の hold-out を用意し、**entity-level（スパン一致）F1** を実装。
- `AddressParser.evaluate` を token-level に加えスパン F1・混同行列へ拡張。
- **受け入れ**: 実 hold-out で per-label スパン F1 が出力される。合成 1.000 に頼らない数値が出る。

### T3. self-training ループ
- 種 = T1 の頭ラベル + `AzaInducer` のゼロラベル字推定。
- CRF/タガーの**周辺確率**で高信頼トークンのみ擬似ラベル化（しきい値ゲート）→ 再学習を反復。
- パーセプトロンには周辺確率が無いので、(a) MALLET CRF へ差し替えてから、または
  (b) マージン/スコア差を信頼度代用、のどちらかで実装。
- **受け入れ**: 反復で尻尾（字・地番境界）のスパン F1 が単調改善（ドリフト監視つき）。
  → ✅ 実装済み（(b)案：`PerceptronTagger.predictWithConfidence` の max-marginal 文平均マージンを
  信頼度ゲートに、`tagger.SelfTrainer`＋`SelfTrainDemo`。悪化時は不採用で終了するドリフト監視つき。
  seed40件 0.966→自己学習 0.972）。実データ尻尾での効果は T1 投入後に再評価。

### T4. 分岐エントロピー併用（字推定の強化）
- `AzaInducer` に右/左分岐エントロピーの**境界スコア**を足し、PMI 剪定と併用。
- **受け入れ**: `AzaDemo` の内部境界 R が現状 0.7 台から改善し、精度 1.000 を維持。
  → ✅ 実装済み（jpc 0.3.0 `AzaInducer.boundaryWeight`、既定0.5で R 0.708→1.000・P 1.000 維持）。

### T5. CRF / 文字BERT 差し替え
- `PerceptronTagger` と同じ素性で MALLET CRF を実装、または 文字BERT を ONNX 化して
  onnxruntime-java 推論。前処理は `CodePoints.of` のみ。
- **受け入れ**: 既存 `AddressParser` API を保ったまま実装が差し替わり、デモが通る。

### T6. 建物・方書き辞書
- KEN_ALL/ABR に無い `棟/階数/部屋番号/方書き` を辞書＋パターンで合成し、`Synth.augment` に追加。
- **受け入れ**: 建物つき住所のスパン F1 が評価に乗る。
  → ✅ 実装済み（`Synth.genComponents` に 階数(B1階含む)・方書き(様方/方/気付)・建物辞書拡充。
  `EvalDemo` に 棟/階数/部屋番号/方書き が出力。`SynthTest` で生成と round-trip を検証）。

### T7. partial-CRF（周辺尤度）
- 頭=既知、尻尾=潜在として周辺尤度で学習する CRF を実装（Java で marginal CRF、または
  Python `pytorch-partial-crf` で学習→ONNX で Java 推論）。
- **受け入れ**: 教師の無い尻尾でスパン F1 が self-training と同等以上。

---

## 5. コーディング規約・設計原則

- **依存は最小**。本体は純 JDK を維持。ML 専用ライブラリは差し替え層（tagger）にのみ閉じ込める。
- **codepoint 単位**で扱う（`char` ではなく `String.codePoints()` / `CodePoints`）。外字/サロゲート対応。
- ラベルは表層文字列（`Labels` の定数）。`Hierarchy` enum と 1:1 対応を崩さない。
- 公開 API は `AddressParser` / `Abr` / `AzaInducer` / `Aza` を窓口に保つ。
- 日本語コメント可（既存に倣う）。住所ドメイン用語は原表記（群=郡 等、原 enum に合わせる）。

---

## 6. 落とし穴

- **KEN_ALL は CP932**。UTF-8 で読むと文字化け。サンプルのみ UTF-8。
- **ABR の列名は版で変わる**。ヘッダ名参照なので名前だけ直す。`住居表示フラグ` の有無で住居/地番分岐。
- **団体コードは KEN_ALL 5桁 / ABR 6桁**。コード結合せず文字列突合する（実装済み）。
- **閾値感度**: `AzaInducer` の `tau`/`wordPenalty`/`minCount` は実データで再調整。
- **合成データを過信しない**: `SynthDemo` の 1.000 は規則性の産物。実評価は T2 の hold-out で。
- **京都の通り名・岩手の地割・無番地** は `KenAll.cleanTown` とラベル拡張で個別対応が必要。
