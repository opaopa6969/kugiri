# kugiri-address-benchmark

住所文字列を **「住所部分（都道府県〜番地・号）」と「建物残差（建物名/棟/階/部屋）」** に分割する
各パーサを統一 IF で叩き、実データで比較するベンチマーク。**建物階層辞書づくりの前段検証**用。

> 残差の定義：番地・号まで除去した「建物以降」の原文（＝辞書に入れる建物文字列）。

## 統一インターフェース

```java
interface AddressSplitter { Split split(String address); record Split(String addressPart, String buildingResidual){} }
```

| 実装 | 元 | 方式 | 依存 | 位置づけ |
|---|---|---|---|---|
| `BhSplitter` | building-hierarchy `UnlaxerBuildingParser.splitBuildingTail` | ルール＋KW辞書(codepoint) | 純JDK・即時 | **基準（最有力）** F1 96.8%/R98.9% を主張 |
| `KugiriSplitter` | kugiri `AddressParser`（合成自己学習） | ML 系列ラベリング | 軽（要学習） | ML 対抗。辞書外語に強い理屈だが**実データ未学習で弱い** |
| `HybridSplitter` | BH 主＋kugiri 補完 | 合成 | — | recall 重視（BH取りこぼしを kugiri で拾う） |
| onigiri | onigiri-parser | 辞書＋突合＋ルール | 重（PG/Flyway/Lucene） | 残差は range ノードから。DB必須で別プロファイル |
| ABRUtils | レジストリ突合 | 突合 | 重（~558MBスナップショット） | 残差は出せない → **住所部分の実在検証オラクル**に回す |

## いまの結果（暫定）

同梱手ラベル gold（8件・明確例）:

| splitter | 建物検出 | gold完全一致 |
|---|---|---|
| bh | 75% | **1.000** |
| kugiri | 62.5% | 0.375 |
| hybrid | 75% | **1.000** |

実データ `/tmp/sample-addresses.csv` 3000行（gold無し・検出率と実装間一致）:

| splitter | 建物検出 | 
|---|---|
| bh | 56.5% |
| kugiri | 38.7% |
| hybrid | **57.5%** |

実装間一致率 0.451。割れる主因は **kugiri が番地の数字を残差に巻き込む**（例 `２２オーナーズマンション…`）・
**辞書外の府営住宅等を取りこぼす**。BH はルールで安定。

### 暫定の結論

- **辞書づくりの splitter は building-hierarchy（`splitBuildingTail`）が現状ベスト**（純JDK・即時・高精度）。
- **kugiri 単体は実データ未学習で不利**（合成のみ）。実データ＋ABR由来ラベルで再学習すれば対立し得る。
- **ハイブリッドは BH の取りこぼしを少し補う**（検出 +1pt）。本命は「BH 主＋kugiri/onigiri を fallback」。
- **ABRUtils は住所部分の実在検証**（町字・番地が実在するか、郵便/座標）に使うのが最適。
- **onigiri は最細粒度だが DB 前提**で重く、スタンドアロン辞書づくりには過剰。

## 実行

```bash
# 同梱 gold（完全一致率＋実装間一致＋差分例）
mvn -q -f benchmark/pom.xml exec:java -Dexec.mainClass=org.unlaxer.addressbench.BenchmarkMain -Dstdout.encoding=UTF-8

# 実データ（gold無し＝検出率・一致率・差分）
mvn -q -f benchmark/pom.xml exec:java -Dexec.mainClass=org.unlaxer.addressbench.BenchmarkMain \
  -Dstdout.encoding=UTF-8 -Dexec.args="--csv /tmp/sample-addresses.csv --addr-col 2 --skip-header false --limit 3000"

# gold 列つき CSV（完全一致率）
#   --gold-col N で正解残差の列を指定
```

## 次（方式1 の gold 生成）

実データの正解残差は **ABR/KEN_ALL 由来で半自動生成**する（zip＋住所→ABRで町字・番地まで確定→残り＝建物残差）。
ABRUtils をオラクルに、住所部分が実在する境界を gold とし、難しい数百件だけ手で確認する。
→ その gold で各 splitter の**完全一致率**を実測し、kugiri 再学習の効果も測る。
（実データ・gold はリポジトリにコミットしない。CSV パスを差し替えて実行）
