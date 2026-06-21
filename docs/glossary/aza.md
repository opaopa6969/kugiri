# Aza (Aza / Koaza — Unsupervised Aza Induction)

[日本語版はこちら](aza.ja.md)

---

## What is it?

**Aza (字) / Koaza (小字)** are the small, historical place-names embedded in Japanese addresses.
For example, in `岩手県盛岡市上田字小鳥沢`, the `小鳥沢` part is an *aza*.
Unlike the other address layers — prefecture, city, ward, town, chome, banchi — which come with
a label someone has already assigned, *aza* are **only partially covered even by authoritative
labeled data (ABR / KEN_ALL)**.

kugiri's main goal is to **infer these unlabeled *aza* with zero supervision (no gold labels)**.
It carves out a region where supervised learning has no "answer key" to lean on, using only
corpus-wide statistics over addresses. In the study course this is framed as a quest to find
*Azami*, a spirit who became invisible because no one gave her a label.

---

## Why does it matter?

When splitting an address into [hierarchy](hierarchy.md) elements, the head (prefecture through
oaza) and the tail (banchi / parcel numbers) are decided mechanically by external dictionaries
and regular expressions. The hard part is the **span that remains in between**.

```
  岩手県盛岡市上田 小鳥沢 1234番地
  └ head from ABR ┘ └ ? ┘ └ digit run = banchi ┘
                    ↑
              this leftover is the "aza"
              but nobody holds a gold label for it
```

If this span is cut wrong, downstream address processing (record linkage, matching,
normalization) trips at the tail. And because there are a vast number of *aza*, manual labeling
is infeasible — so getting it right **without supervision** is what makes it valuable.

---

## How does it work?

kugiri infers *aza* from three structural grounds (see study chapters
[12 residual slot](../study/12-zansa-slot.md) through [16 full pipeline](../study/16-aza-zentai.md)).

### 1. Residual slot (elimination)

[ABR](abr.md) fixes the oaza, and the trailing digit run fixes the start of the
[banchi](hierarchy.md). The **non-empty span left in between is structurally an *aza*** even
though no one labels it (`Aza.peel()` cuts it out).

### 2. Repetition (unsupervised aza-vocabulary induction)

*Aza* names recur across addresses nationwide. By collecting residuals across the whole corpus,
substrings that are **frequent and diversely adjacent (high [branching entropy](branching-entropy.md))**
are **induced as the *aza* vocabulary**.

### 3. PMI pruning

A run like `中里前田`, where the two pieces merely happen to be adjacent (low [PMI](pmi.md)), is
removed from the vocabulary as a composite word. A run like `田中` that always co-occurs (high PMI)
is kept as one unit. The surviving vocabulary then drives a unigram-LM maximum-likelihood split
([Viterbi](viterbi.md)) to decide the *aza* boundaries.

```
fit(residuals):
  count cnt and left/right adjacency for every substring
  admit: cnt>=minCount AND diverse neighbors AND mean branching entropy >= eMin
  prune: drop runs whose PMI <= tau as composite words
segment(name):
  Viterbi maximizing unigram logp − wordPenalty
```

ABR/KEN_ALL only supply the scaffold (oaza dictionary + digit boundary); **not a single *aza*
label is required**.

---

## How does kugiri use it?

The public entry points are `aza/Aza` (residual-slot extraction) and `aza/AzaInducer`
(vocabulary induction + PMI pruning + ML split). `demo/AzaDemo` runs the end-to-end pipeline on
synthetic data with the *aza* hidden.

Measured `AzaDemo` results (**zero gold labels**):

```
internal boundary (aza splits) P=1.000 R=0.708 F1=0.829
exact aza-span match           353/400 = 0.883
```

Precision is 1.000 (it never inserts a wrong boundary); recall depends on the thresholds
(`tau` / `wordPenalty` / `minCount`). Real data has tens of millions of records where the
repetition of *aza* is orders of magnitude stronger, so it is far more robust than this toy corpus.

---

## Learn more

- [hierarchy.md](hierarchy.md) — the full set of address layers an *aza* lives in.
- [branching-entropy.md](branching-entropy.md) — the main signal for finding *aza* boundaries.
- [pmi.md](pmi.md) — the discriminating signal that prunes glued-together *aza*.
- [viterbi.md](viterbi.md) — the dynamic program for the maximum-likelihood split.
- [weak-supervision.md](weak-supervision.md) — how the head labels that scaffold *aza* are made.
- [study chapters 12–16](../study/16-aza-zentai.md) — the full unsupervised *aza* pipeline.
