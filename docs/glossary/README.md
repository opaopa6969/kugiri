# kugiri 用語集 / Glossary

[English](#english) | [日本語](#japanese)

> 機械学習による日本住所の階層トークナイザ「kugiri」で使う用語を、
> ML 未経験でも分かるように1用語=1記事で解説します。各記事は日本語(`.ja.md`)が主、
> 可能なものは英語(`.md`)も用意しています。

<a id="japanese"></a>

## 日本語

### DDE とは

**DDE（Document-Deficit Extraction）** は、ドキュメントから専門用語を抽出し、
「誰でも理解できる解説記事」を1用語1ファイルで生成し、元ドキュメントへ自動でリンクを
張り直す仕組みです（[DxE-suite](https://github.com/opaopa6969/DxE-suite) の一部）。
この `docs/glossary/` はその成果物——kugiri ドメインの用語集です。

### 記事の書式

各記事は実適用例（volta-auth-proxy）の書式に従います:

```
# 用語名
[English version](term.md)        ← 言語切替リンク
---
## これは何？                      ← なずなに（要約＋たとえ話）
---
## なぜ重要なのか？
---
## どう動くのか？                  ← 図・コード例
---
## kugiri ではどう使われている？    ← 実装クラスに即して
---
## さらに学ぶために                ← 関連用語・docs/study への相互リンク
```

### 収録用語

| 用語 | 日本語 | English | 概要 |
|------|--------|---------|------|
| 字（字小字） | [JA](aza.ja.md) | [EN](aza.md) | ラベルの無い「字」を教師なしで推定（kugiri の主眼） |
| 階層要素 | [JA](hierarchy.ja.md) | [EN](hierarchy.md) | 都道府県〜方書きの住所階層 enum |
| codepoint | [JA](codepoint.ja.md) | [EN](codepoint.md) | char ではなく codepoint 単位でラベリング |
| BIOES | [JA](bioes.ja.md) | [EN](bioes.md) | 系列ラベルの符号化（B/I/O/E/S） |
| 構造化パーセプトロン | [JA](perceptron-tagger.ja.md) | [EN](perceptron-tagger.md) | 平均化構造化パーセプトロン + Viterbi の参照実装 |
| Viterbi | [JA](viterbi.ja.md) | [EN](viterbi.md) | 最良ラベル列を求める動的計画法 |
| スパン F1 | [JA](span-f1.ja.md) | [EN](span-f1.md) | entity-level スパン F1（合成 1.000 を信じない） |
| ABR | [JA](abr.ja.md) | [EN](abr.md) | アドレス・ベース・レジストリ（弱教師） |
| KEN_ALL | [JA](ken-all.ja.md) | [EN](ken-all.md) | 日本郵便の郵便番号データ（弱教師） |
| 分岐エントロピー | [JA](branching-entropy.ja.md) | [EN](branching-entropy.md) | 字の区切り目を見つける主信号 |
| PMI | [JA](pmi.ja.md) | [EN](pmi.md) | 字の癒着を剪定する自己相互情報量 |
| 弱教師 | [JA](weak-supervision.ja.md) | [EN](weak-supervision.md) | KEN_ALL+ABR で「だいたいの正解」を作る |
| 合成データ | [JA](synthetic-data.ja.md) | [EN](synthetic-data.md) | 連結アライメントで教師を自動生成 |

### 使い方（リンク自動埋め込み）

`dde-link` CLI でドキュメントへ用語リンクを自動で埋め込めます（LLM 不要・CLI のみ）:

```bash
# ドキュメントにリンクを埋め込む
npx @unlaxer/dde-toolkit dde-link docs/DESIGN.md --glossary docs/glossary

# まず変更内容をプレビュー（上書きしない）
npx @unlaxer/dde-toolkit dde-link docs/DESIGN.md --glossary docs/glossary --dry-run

# CI: 未リンクを検出（不足があれば exit 1）
npx @unlaxer/dde-toolkit dde-link docs/DESIGN.md --glossary docs/glossary --check
```

リンク解決は `dictionary.yaml`（日本語/別表記マッピング）を優先し、
コードブロック・インラインコード・見出し・既存リンクはスキップ、1段落につき1回・最長一致で置換します。
日本語ドキュメント(`.ja.md`)に対しては、対応する `<term>.ja.md` へリンクされます。

<a id="english"></a>

## English

### What DDE is

**DDE (Document-Deficit Extraction)** extracts terms from your docs, generates one
easy-to-understand article per term, and auto-links them back into the source documents
(part of [DxE-suite](https://github.com/opaopa6969/DxE-suite)). This `docs/glossary/` is that
deliverable — the term glossary for the kugiri domain.

### Usage

```bash
npx @unlaxer/dde-toolkit dde-link docs/DESIGN.md --glossary docs/glossary            # embed links
npx @unlaxer/dde-toolkit dde-link docs/DESIGN.md --glossary docs/glossary --dry-run  # preview
npx @unlaxer/dde-toolkit dde-link docs/DESIGN.md --glossary docs/glossary --check    # CI gate
```

`dde-link` resolves terms from the `.md` filenames in this folder, overridden by
`dictionary.yaml`, and replaces longest-match-first, once per paragraph, skipping code blocks,
inline code, headings, and existing links.

See the index table in the Japanese section above for every term (JA + EN).
