# Viterbi (Dynamic Programming)

[日本語版はこちら](viterbi.ja.md)

---

## What is it?

**Viterbi** is a dynamic-programming algorithm that finds the **single highest-scoring path** among
a huge number of combinations — without brute-forcing them all.

Picture a mountain trail with many forks, and you want the one best route to the summit. Instead of
**walking every route and comparing**, you advance while remembering only **the best way to reach each
point so far**. No matter how many branches appear, all you keep is "the best score at each point and
where it came from." So even when the candidates grow exponentially, the computation stays roughly
proportional to the length of the sequence.

In kugiri, it picks the **one best label sequence** out of the astronomically many ways to tag each
character of an address.

---

## Why does it matter?

For a sequence of length N with L possible labels, there are **L^N** possible label sequences. Even a
single address yields an astronomical number, so brute force is impossible.

Viterbi exploits the property that "the best path up to a point is determined by the best path one step
earlier," collapsing this explosion into an **N×L×L table computation**. This lets it choose, while
considering the whole sequence, a **coherent label sequence** (one that does not cut a part in the
middle or make an illegal transition) — quickly.

---

## How does it work?

At each step it records, in a table, "the best score for reaching each label" and "which label came
just before (a back-pointer)." After reaching the end, it walks backward from the best final score to
reconstruct the best label sequence.

```
  char:        丸    の    内    一    丁    目
  candidates:  B-町  I-町  E-町  B-丁  I-丁  E-丁  ...
               ↓ store best score and "where it came from" in each cell
  table:      [t=0]→[t=1]→[t=2]→ ... →[t=N-1]
               └ each cell = max(prev cell + transition + emission score)
               × illegal [BIOES] transitions masked with score −∞
  backtrace:  follow back-pointers from the best final cell
               → B-町 I-町 E-町 B-丁目 I-丁目 E-丁目
```

Because illegal transitions (e.g. an `I-` right after an `O`) are **masked with −∞**, structurally
impossible sequences are never even candidates.

---

## How does kugiri use it?

It is central in two places:

- **`tagger/PerceptronTagger`**: decodes the [BIOES](bioes.md) label sequence scored by the learned
  weights. Masking illegal transitions guarantees the output is always a legal label sequence.
  (See [perceptron tagger](perceptron-tagger.md).)
- **`aza/AzaInducer.segment`**: using the unsupervised-induced aza vocabulary, it solves the unigram
  language model's **maximum-likelihood split** (maximizing log-probability − wordPenalty) with Viterbi.
  (See [aza induction](aza.md).)

---

## Learn more

- [bioes.md](bioes.md) — the tag scheme of the sequences Viterbi selects, and its legal transitions.
- [perceptron-tagger.md](perceptron-tagger.md) — the sequence labeler that uses Viterbi as its decoder.
- [aza.md](aza.md) — where Viterbi performs the maximum-likelihood split of aza names.
- [Chapter 10: Viterbi and dynamic programming](../study/10-viterbi.md)
- [Chapter 15: Language models and maximum-likelihood splitting](../study/15-gengo-model-viterbi.md)
