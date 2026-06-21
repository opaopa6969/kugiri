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
  PMI 剪定＋ユニグラム最尤分割で、**ラベル無しで**推定する。
- **依存ゼロ**: 純 JDK（Java 21）。系列ラベラは平均化構造化パーセプトロン+Viterbi（CRF 差し替え可）。

## 使い方

```bash
mvn -q compile
mvn -q exec:java -Dexec.mainClass=org.unlaxer.kugiri.demo.AzaDemo
```

## ドキュメント

- 設計全体: [`docs/DESIGN.md`](docs/DESIGN.md)
- 継続開発の引き継ぎ: [`docs/HANDOFF.md`](docs/HANDOFF.md)
- Claude Code 用メモリ: [`CLAUDE.md`](CLAUDE.md)

— `org.unlaxer:kugiri` / unlaxer ecosystem
