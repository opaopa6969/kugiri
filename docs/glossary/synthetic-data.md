# Synthetic Data

[日本語版はこちら](synthetic-data.ja.md)

---

## What is it?

Synthetic data means **building training samples artificially from parts whose correct labels are
already known, instead of collecting real data**.

In kugiri, an address is not held as a raw string up front. It is held as a **component list
`[(label, surface), …]`**. Concatenating it produces the string and, **at the same time**, fixes
the codepoint-level [BIOES](bioes.md) labels **with no alignment computation**.

```
[(pref,東京都),(Tokyo-23,千代田区),(town,丸の内),(chome,一丁目),(gaiku,9番),(residence,1号)]
        │ concatenate
        ▼
  "東京都千代田区丸の内一丁目9番1号"
        │ from the join positions
        ▼
  B-pref I-pref E-pref B-Tokyo-23 ... S-residence  ← gold falls out automatically
```

It is like assembling LEGO bricks: because **you know each brick's color from the start**, the
labels of the finished model are determined automatically.

---

## Why does it matter?

Training a sequence labeler (such as the [perceptron tagger](perceptron-tagger.md)) needs **large
amounts of gold where every character carries a label**. Real data does not come with such labels.

With synthetic data you can produce labeled gold **infinitely and exactly**, which is ideal for
wiring checks (does the code run end to end?) and for initial validation of features and models.
Whereas [weak supervision](weak-supervision.md) can only label the head, synthetic data is
**fully supervised**.

---

## How does it work?

`synth/Synth.augment()` augments data by perturbing **only the surface** while keeping labels, to
make the model robust to the notational variation in real input.

```
  Same meaning, same labels — only the appearance changes:

    digits     full-width ⇄ half-width    一 ⇄ 1
    hyphens    - ー − － ‐                 swap variants randomly
    chome/banchi/go  → symbolized          1丁目 ⇄ 1-
    ZIP        〒 present or not            〒020-0021 ⇄ 020-0021

  ★ Labels are preserved, so the gold alignment survives augmentation
```

### ⚠️ Do not believe the "1.000 accuracy"

`SynthDemo` reports a tag accuracy of **1.000**, but this is **an artifact of synthetic data being
too regular** — it is **not real skill**. Real evaluation must always use a hold-out of real labels
and measure entity-level [span F1](span-f1.md). DESIGN and HANDOFF warn about this repeatedly.

---

## How does kugiri use it?

The synthesizer lives in `synth/Synth.java` (`buildExample()` / `fromRecords()` / `augment()`),
and `demo/SynthDemo.java` demonstrates the end-to-end run (synthesize → train → parse).

`fromRecords()` can also generate training samples from real-data-derived component lists produced
by [weak supervision](weak-supervision.md), so synthetic and real data flow through the same path.

---

## Learn more

- [weak-supervision.md](weak-supervision.md) — real-data-derived head labels (contrast).
- [bioes.md](bioes.md) — the span labeling scheme fixed automatically by concatenation.
- [codepoint.md](codepoint.md) — the unit labels attach to (codepoint, not char).
- [span-f1.md](span-f1.md) — the real-evaluation metric you use instead of trusting "1.000".
- [Chapter 18: Synthetic data and "don't believe 1.000 accuracy"](../study/18-synth-data.md)
- [Chapter 19: Wiring it all together — end-to-end](../study/19-zenbu-tsunagu.md)
