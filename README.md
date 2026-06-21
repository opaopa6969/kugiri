# kugiri（区切り）

機械学習による**日本住所の階層トークナイザ生成器**。
郵便番号＋住所文字列を `階層要素`（都道府県 / 市 / 区 / 群 / 町または大字 / 丁目 /
字小字 / 地番 / 街区符号 / 住居番号 / 枝番号 / 棟 / 部屋番号 / 方書き …）ごとの token に、
**codepoint 単位の系列ラベリング**で切り出す。

```
東京都千代田区丸の内一丁目9番1号朝日ビル1203号室
└都道府県┘└─区─┘└大字┘└丁目┘└街┘└住┘└─棟─┘└部屋番号┘
```

## 特長

- **弱教師ラベル生成**: KEN_ALL の ZIP で大まかに引き、ABR で町域を引き直して頭を自動ラベル化。
  KEN_ALL の連結フィールド（`横浜市西区`/`虻田郡洞爺湖町`）が ABR で `市+区`/`群+町村` に割れる。
- **教師なし字(小字)推定（主眼）**: ABR が網羅しない `字` を、残差スロット＋字彙の教師なし誘導＋
  PMI 剪定＋ユニグラム最尤分割＋分岐エントロピー境界スコアで、**ラベル無しで**推定する
  （合成コーパスで内部境界 F1 0.83→1.0）。本体は japanese-parser-common 0.3.0 の
  `org.unlaxer.jaddress.aza` に移設し共有。
- **実評価基盤**: entity-level スパン P/R/F1・混同行列・hold-out（`eval.SpanEval` / `EvalDemo`）。
- **半教師あり**: self-training（`SelfTrainer`）と partial-CRF 近似＝潜在変数perceptron（`fitPartial`）。
- **差し替え層**: 系列ラベラは `tagger.SequenceTagger` で差し替え可能（既定=平均化構造化
  パーセプトロン+Viterbi）。MALLET CRF / 文字BERT(ONNX) を同 API で差し込める。
- **依存方針**: 本体コードは純 JDK（Java 21）＋ japanese-parser-common のみ。
  重い ML 依存は差し替え層に閉じ込める。

## 使い方

```bash
mvn -q compile
mvn -q exec:java -Dexec.mainClass=org.unlaxer.kugiri.demo.AzaDemo        # 教師なし字推定
mvn -q exec:java -Dexec.mainClass=org.unlaxer.kugiri.demo.AbrDemo        # KEN_ALL→ABR 弱教師
mvn -q exec:java -Dexec.mainClass=org.unlaxer.kugiri.demo.SynthDemo      # 合成 end-to-end
mvn -q exec:java -Dexec.mainClass=org.unlaxer.kugiri.demo.EvalDemo       # スパンF1評価(hold-out)
mvn -q exec:java -Dexec.mainClass=org.unlaxer.kugiri.demo.SelfTrainDemo  # self-training
mvn -q exec:java -Dexec.mainClass=org.unlaxer.kugiri.demo.TaggerSwapDemo # タガー差し替え
mvn -q exec:java -Dexec.mainClass=org.unlaxer.kugiri.demo.PartialCrfDemo # partial-CRF近似
```

実データ取込は `ingest.IngestCli`（`docs/HANDOFF.md` §4 T1 参照、KEN_ALL は CP932）。

## ドキュメント

- **学習コース（ML未経験者向け・会話形式の全25章）**: [`docs/study/`](docs/study/README.md)
- 用語集（DDE）: [`docs/glossary/`](docs/glossary/README.md)
- 設計全体: [`docs/DESIGN.md`](docs/DESIGN.md)
- 継続開発の引き継ぎ: [`docs/HANDOFF.md`](docs/HANDOFF.md)
- Claude Code 用メモリ: [`CLAUDE.md`](CLAUDE.md)

— `org.unlaxer:kugiri` / unlaxer ecosystem
