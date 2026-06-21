# Span F1 (Entity-Level Span F1)

[日本語版はこちら](span-f1.ja.md)

---

## What is it?

**Span F1 (entity-level span F1)** measures how well an address is split into parts **at the level of
whole spans**. A predicted span counts as correct **only when its label matches AND its start and end
positions exactly match the gold span**. Precision, recall, and F1 are then computed over whole spans.

Think of a teacher who grades an exam **by whole words, not by individual characters**. If `丸の内` is
cut as `丸の` + `内`, it may match almost perfectly character-by-character, but **as a span it is
wrong** — a single misplaced boundary makes the entire part incorrect. That is what reflects real
skill.

---

## Why does it matter?

Per-character accuracy (token accuracy) is **far too lenient and unreliable** for address evaluation.
In an address with many `O` tags (not part of any element), simply answering `O` everywhere already
yields high accuracy.

In fact, kugiri's `SynthDemo` reports a token accuracy of **1.000**, but that is because the
**synthetic data is too regular** — it is **not real skill** (see [synthetic data](synthetic-data.md)).
Span F1 asks whether a part was cut out correctly *as a whole*, so it exposes this kind of illusion.
That is why kugiri's definition of done is to **evaluate with span F1 and report figures that do not
rely on the synthetic 1.000**.

---

## How does it work?

1. Decode the predicted [BIOES](bioes.md) label sequence into a **set of spans**
   (`B-丁目 I-丁目 E-丁目` → `丁目:[3,6)`).
2. Decode the gold sequence into spans the same way.
3. Count as a TP (true positive) only the spans whose **label + start + end match exactly**.

```
  gold spans:  [都道府県:0-3] [市:3-7]   [町:7-10]      [丁目:10-13]
  pred spans:  [都道府県:0-3] [市:3-7]   [町:7-9] [?:9-10]  [丁目:10-13]
                  ✓TP          ✓TP        ✗(boundary off → FP+FN)  ✓TP

  precision = TP / (TP + FP)        ← fraction of predictions that were right
  recall    = TP / (TP + FN)        ← fraction of gold spans that were caught
  F1        = 2PR / (P + R)         ← harmonic mean of the two
```

Cutting `町:7-10` as `7-9` — off by one character — makes that part entirely wrong (both FP and FN).

---

## How does kugiri use it?

It is implemented in `eval/SpanEval.java` (entity-level span P/R/F1, confusion matrix, hold-out
evaluation) and exposed through `tagger/AddressParser.evaluateSpans`. `demo/EvalDemo` demonstrates it.

Evaluation is always done on a **hold-out** (data not used in training). Reporting **per-label span F1**
— rather than the synthetic 1.000 — is the correct yardstick for measuring a change. (The internal
boundary F1=0.829 in [aza induction](aza.md) is scored with this same span-match philosophy.)

---

## Learn more

- [perceptron-tagger.md](perceptron-tagger.md) — the sequence labeler that span F1 measures.
- [bioes.md](bioes.md) — the tag scheme decoded into spans before scoring.
- [synthetic-data.md](synthetic-data.md) — why a token accuracy of 1.000 must not be trusted.
- [aza.md](aza.md) — aza induction is also scored by span match.
- [Chapter 11: Evaluation — precision, recall, F1](../study/11-hyouka-f1.md)
