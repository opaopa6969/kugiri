# CLAUDE.md — kugiri

機械学習による日本住所の階層トークナイザ生成器。ZIP＋住所文字列を `階層要素`
（都道府県/市/区/群/町または大字/丁目/字小字/地番/街区符号/住居番号/枝番号/棟/部屋番号/方書き…）
ごとの token 列へ、**codepoint 単位の系列ラベリング**で切り出す。
**ラベルの無い「字(小字)」を教師なしで推定する**ことが主眼。依存ゼロの純 JDK 実装。

## まず読む
- `docs/DESIGN.md` … 要件・設計・アルゴリズム・結果・ロードマップ（全体像）
- `docs/HANDOFF.md` … 次タスク（優先順・受け入れ条件）と落とし穴

## ビルド & 実行
```bash
mvn -q compile
mvn -q exec:java -Dexec.mainClass=org.unlaxer.kugiri.demo.AzaDemo   # 教師なし字推定
mvn -q exec:java -Dexec.mainClass=org.unlaxer.kugiri.demo.AbrDemo   # KEN_ALL→ABR 弱教師
mvn -q exec:java -Dexec.mainClass=org.unlaxer.kugiri.demo.SynthDemo # 合成 end-to-end
```
依存ゼロなので `javac -encoding UTF-8` 直叩きでも可。出力化け時は `-Dstdout.encoding=UTF-8`。

## 構成
```
label/(Hierarchy,Labels,Bioes,CodePoints) model/(Component,Example)
synth/(Synth) feature/(Features) tagger/(PerceptronTagger,AddressParser)
abr/(Csv,KenAll,Abr) aza/(AzaInducer,Aza) demo/(Synth,Abr,Aza,Res)
```

## 設計原則（厳守）
- **依存は最小**。本体は純 JDK。ML ライブラリは tagger の差し替え層にのみ閉じ込める。
- **codepoint 単位**（`char` 不可、`String.codePoints()` / `CodePoints`）。外字/サロゲート対応。
- **日本語テキストの走査に regex（`String.matches`/`replaceAll`/`split` 等）を使わない**。codepoint 走査で書く。
  例外は ASCII 固定パターンや NFKC 正規化など限定用途のみ。サロゲート/外字を壊さないため。
- ラベルは `Labels` の表層文字列。`Hierarchy` enum と 1:1 を崩さない。
- 公開窓口は `AddressParser` / `Abr` / `AzaInducer` / `Aza`。
- 日本語コメント可。住所用語は原表記（群=郡 等、原 enum 準拠）。

## やってはいけない
- 実 ABR/KEN_ALL の生データをコミットしない（サンプルのみ）。
- `SynthDemo` の tag accuracy 1.000 を「実力」として扱わない（合成の規則性）。実評価は hold-out で。
- 本体へ重い依存を足さない。

## 完了の定義
- `mvn compile` が通り、3デモが実行できる。
- 変更がスパン F1（entity-level）で評価され、合成 1.000 に依存しない数値で示される。
