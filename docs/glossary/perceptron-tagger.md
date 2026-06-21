# Perceptron Tagger (Averaged Structured Perceptron)

[日本語版はこちら](perceptron-tagger.ja.md)

---

## What is it?

The **perceptron tagger** is a sequence labeler: it reads an address one character at a time and
asks "is this character part of the prefecture? the city? an aza?", attaching a label to each.
In kugiri it is implemented as an **averaged structured perceptron + [Viterbi](viterbi.md)**.

Think of it as an experienced postal sorting clerk. At first it knows nothing and guesses, but every
time it is shown the correct answer it nudges its internal rulebook (the weights): "I got this one
wrong, next time lean the other way." After thousands of practice items it sorts even unseen
addresses fairly accurately.

It belongs to the same family as a linear-chain CRF (conditional random field), but its distinguishing
trait is that it runs on **pure JDK with zero external libraries**.

---

## Why does it matter?

Japanese addresses have no spaces between words. To split `東京都千代田区丸の内一丁目` into parts,
a machine must decide, character by character, **where one [hierarchy](hierarchy.md) element starts
and ends**.

Fixed rules (regexes) can cut some of it, but they break the moment notation varies or an unknown
place-name appears. A learned sequence labeler can **generalize boundaries from cues (features)**,
so it stays robust on addresses it has never seen.

kugiri first wanted a reference implementation that runs with zero dependencies — no heavy ML library
in the core — yet keeps the CRF-like structure of choosing the best label sequence over the whole
input. This tagger is that answer.

---

## How does it work?

1. Each character is turned into a **feature vector** (a bag of cues), drawn from
   `feature/Features.java`: **window ±2 characters, character type, suffix keywords, and bigrams**.
2. The dot product of weights and features scores each label, and **[Viterbi](viterbi.md)** picks the
   best label sequence for the whole string. Illegal [BIOES](bioes.md) transitions (e.g. an `I-` tag
   starting on its own) are masked out.
3. When a prediction disagrees with the gold answer, **reward the gold features and penalize the wrong
   ones** (perceptron update).
4. **Average** the weights across all epochs before using them — this curbs overfitting and improves
   generalization.

```
  string:   東 京 都 千 代 田 区 ...
            │  │  │  │  │  │  │
  features: [window ±2 char / char-type / suffix KW / bigram]
            │  │  │  │  │  │  │
  scores:   dot product per label → Viterbi over the whole sequence (BIOES-legal only)
            ↓
  predict:  B-都道府県 I-都道府県 E-都道府県 B-市 ...
            ↓ compare to gold
  update:   gold features +1 / wrong features −1   →  average all weights at the end
```

---

## How does kugiri use it?

The core is `tagger/PerceptronTagger.java` (the sequence labeler and Viterbi decoder). Feature
extraction lives in `feature/Features.java`. The public window for training, parsing, and evaluation
is `tagger/AddressParser.java`.

The perceptron was chosen over a CRF to keep a **dependency-free pure-JDK reference implementation**.
When accuracy plateaus, the design lets you swap it — reusing the same feature functions — for a
**MALLET CRF** or a **character BERT + ONNX** model (the tagger is a replaceable slot).

---

## Learn more

- [viterbi.md](viterbi.md) — the dynamic program that picks the best label sequence instantly.
- [bioes.md](bioes.md) — the tag scheme the tagger emits and its legal transitions.
- [span-f1.md](span-f1.md) — the metric that measures the tagger's real skill at the span level.
- [codepoint.md](codepoint.md) — the smallest unit of text it operates on.
- [Chapter 8: Perceptron](../study/08-perceptron.md)
- [Chapter 9: Structured perceptron and averaging](../study/09-structured-perceptron.md)
