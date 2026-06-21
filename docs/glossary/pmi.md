# PMI (Pointwise Mutual Information)

[日本語版はこちら](pmi.ja.md)

---

## What is it?

PMI (Pointwise Mutual Information) is a single number that tells you whether two things are
**just standing next to each other by chance** or are **a pair that always shows up together**.

```
PMI(a, b) = log( c(ab) * N / ( c(a) * c(b) ) )
```

Think of two students you always see together at recess. From the attendance records alone, can
you tell whether they are genuine best friends, or whether they just happened to be in the same
room? PMI answers exactly that kind of question for substrings.

- **High PMI** → like `田中`, a and b co-occur far more than chance can explain = **one unit**.
- **Low PMI** → like `中里前田`, they merely landed next to each other = **really two words**.

---

## Why does it matter?

When carving out [aza](aza.md) without supervision, a "residual slot" can contain several aza
glued together. `中里前田` is really `中里` + `前田` (two aza), yet looking only at frequency or
[branching entropy](branching-entropy.md) it can look like one frequently-seen word.

Relying on frequency alone would **mis-register that glued composite as a single aza**. PMI is the
**discriminating signal** that separates a "real unit (田中)" from an "accidental adjacency
(中里前田)", preventing this error.

---

## How does it work?

`AzaInducer` takes a candidate vocabulary entry `ab`, splits it into a known split `a + b`, and
computes its PMI.

```
        c(ab) * N
PMI = log ─────────────
        c(a) * c(b)

  c(ab) … how often "ab" appears
  c(a)  … how often "a"  appears
  c(b)  … how often "b"  appears
  N     … total number of residual records   ★ this matters a lot

PMI is low (<= tau)
  → a and b just happen to be adjacent, independently
  → "ab" is a composite word, so prune it from the vocabulary

  e.g. 中里前田  →  low PMI  →  pruned → splits into 中里 / 前田
       田中      →  high PMI →  kept   → stays one word
```

**Normalization pitfall (a real bug noted in the DESIGN appendix):**
The `N` in PMI must be normalized by the **number of residual records**. If you divide by `Z`, the
sum of all substring frequencies, PMI becomes uniformly over-large and **pruning stops working
entirely**. Mixing up `N` and `Z` breaks it silently.

---

## How does kugiri use it?

PMI pruning happens inside `fit()` of `aza/AzaInducer`. After candidate vocabulary is gathered by
frequency and branching entropy (admit), runs whose PMI is at or below `tau` are removed as
composite words (prune), and the surviving aza vocabulary drives a unigram language-model
maximum-likelihood split ([Viterbi](viterbi.md)).

`tau` is a threshold retuned on a real-data hold-out. The DESIGN decision log records two failures —
"maximize frequency × length → over-segmentation" and "raise the word penalty → gluing" — before
concluding that **the discriminating signal is PMI**.

---

## Learn more

- [aza.md](aza.md) — unsupervised aza induction, which PMI pruning underpins.
- [branching-entropy.md](branching-entropy.md) — the signal that gathers candidates (used with PMI).
- [viterbi.md](viterbi.md) — the dynamic program that splits with the pruned vocabulary.
- [Chapter 14: PMI](../study/14-pmi.md) — "together, or just by chance?" in dialogue form.
