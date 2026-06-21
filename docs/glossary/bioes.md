# BIOES (Sequence-Label Encoding)

[日本語版はこちら](bioes.ja.md)

---

## What is it?

BIOES is a **way of encoding labels** over a sequence of characters. You raise one of five flags
on each character: `B` (Begin), `I` (Inside), `O` (Outside), `E` (End), and `S` (Single — a
span that is exactly one character). It is also known as BILOU.

Think of a train. The lead car is `B`, the middle cars are `I`, the rear car is `E`, a one-car
train is `S`, and someone standing off the tracks is `O`. Just by looking at each car (each
character), you can tell where one train starts and stops.

---

## Why does it matter?

Japanese addresses have no word spacing. `東京都千代田区丸の内一丁目` runs together with no
gaps or separators, yet we want a machine to answer "where does the *chome* start and end?"

If we only tag the *kind* of each character (`prefecture / ward / chome …`), the **boundary
disappears whenever the same kind sits next to itself**. Two adjacent `chome` characters give
no clue whether that is one chome or two. BIOES marks the start (`B`) and the end (`E`), so the
**boundary is baked right into the label**.

---

## How does it work?

We combine the kind of [hierarchy](hierarchy.md) element (chome, banchi, …) with the five
position flags. Each character — more precisely each [codepoint](codepoint.md) — gets exactly
one tag.

```
char:  東  京  都  千  代  田  区  丸  の  内  一  丁  目
tag:   B-  I-  E-  B-  I-  I-  E-  B-  I-  E-  B-  I-  E-
       prefecture | ward     | town(oaza)| chome
       ───────────┘ ─────────┘ ──────────┘ ──────────┘

A single-character span uses S-:
char:  9  ...
tag:   S-gaiku  ...
```

The flag sequence has rules. After `B-` only the same-kind `I-` or `E-` may follow, and an `I-`
is never allowed to start a sequence (an illegal sequence). This legality is masked by
[Viterbi](viterbi.md), so impossible sequences are ruled out from the start.

---

## How does kugiri use it?

`label/Bioes.java` defines the BIOES tag space and handles encoding (boundaries → tag sequence)
and decoding (tag sequence → spans). The [perceptron-tagger](perceptron-tagger.md) predicts a
BIOES tag for every codepoint, and [Viterbi](viterbi.md) applies the transition-legality mask so
it never emits an invalid sequence such as a lone leading `I-`. Decoding turns the tags back into
spans like `chome: 一丁目`.

---

## Learn more

- [codepoint.md](codepoint.md) — the unit a tag is raised on (a codepoint, not a char).
- [hierarchy.md](hierarchy.md) — the list of hierarchy elements that become tag "kinds".
- [viterbi.md](viterbi.md) — the mechanism that masks illegal BIOES sequences.
- [perceptron-tagger.md](perceptron-tagger.md) — the model that predicts the BIOES tags.
- [study ch.6 Sequence labeling and BIOES](../study/06-sequence-bioes.md)
