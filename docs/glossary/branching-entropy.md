# Branching Entropy

[日本語版はこちら](branching-entropy.ja.md)

---

## What is it?

**Branching entropy** measures "how many *different* characters can come right after (or before)
a given string."

A road analogy makes it click. On a straight road there is only one way forward. But at an
intersection, the number of directions you can take suddenly jumps. **"A place where the variety
of the next character suddenly increases" = an intersection = a word boundary** — that is the
idea behind branching entropy. Inside a word, the next character is nearly fixed (low entropy);
at the end of a word, what comes next is wide open (high entropy).

---

## Why does it matter?

kugiri's main goal is to carve out the unlabeled [aza](aza.md) without supervision. Since there
is no teacher telling it "the boundary is here," it must **find boundaries from the statistics of
the string itself**.

Branching entropy is the primary signal for that "boundary-ness." When an aza name recurs across
the corpus, all sorts of characters follow just outside its boundary (= high entropy). kugiri uses
this property to select candidates for the aza vocabulary.

---

## How does it work?

For a given substring, count the distribution of characters that appeared to its right
(or left) and measure the spread with an entropy based on [log](../study/05-log-to-jouhou.md).
The more spread out (diverse), the higher; the more concentrated (always the same character),
the lower.

```
  Count what follows "中里":
    中里[1] 中里[番] 中里[、] 中里[前] …  → all over = high entropy = looks like a boundary
  What follows "中":
    中[里] 中[里] 中[里] …               → always 里 = low entropy = mid-word

  * String endpoints (head/tail) are treated as a boundary token, BND
```

In `aza/AzaInducer`, this is used as a condition to **admit** a candidate into the vocabulary.

```
admit: frequency cnt >= minCount
       AND diverse left/right neighbors (>=2 kinds, or endpoint)
       AND mean branching entropy >= eMin
```

Roadmap item T4 will **explicitly combine** this boundary score with [PMI](pmi.md) pruning to
raise recall of internal boundaries.

---

## How does kugiri use it?

The implementation is the vocabulary-induction (fit) stage of `aza/AzaInducer`, which uses
branching entropy alongside frequency and left/right neighbor diversity as an admission
criterion. The admitted vocabulary is ultimately turned into aza boundaries by a unigram
maximum-likelihood split via [Viterbi](viterbi.md).

---

## Learn more

- [aza.md](aza.md) — the unsupervised aza pipeline where branching entropy shines.
- [pmi.md](pmi.md) — the other discriminating signal, used to prune glued-together runs.
- [viterbi.md](viterbi.md) — the dynamic program that splits using the admitted vocabulary.
- [study chapter 13](../study/13-bunki-entropy.md) — branching entropy in detail.
- [study chapter 5](../study/05-log-to-jouhou.md) — log and information (the basis of entropy).
