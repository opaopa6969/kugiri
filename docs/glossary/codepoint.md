# Codepoint

[日本語版はこちら](codepoint.ja.md)

---

## What is it?

A codepoint is the **number** Unicode assigns to each individual character.
`A` is U+0041, `あ` is U+3042, `𠮷` (a variant of "yoshi") is U+20BB7.
Each "thing a human reads as one character" maps to exactly one number.

The catch: this is **not** the same as a programming language's `char`. A Java `char` is only
16 bits, so a character with a large number like `𠮷` is **split across two chars** (a surrogate
pair). A codepoint never splits — `𠮷` is one codepoint, period. When processing addresses,
counting in this "unsplittable unit" matters a great deal.

---

## Why does it matter?

Addresses are full of gaiji (external characters) and rare kanji: the `𠮷` in a name like `𠮷田`,
hard-to-read place-name kanji, private-use-area (PUA) characters used by companies, and so on.

If you label per `char`, `𠮷` splits in two, and you end up either putting a **meaningless
internal boundary** inside one character (`B-` on one half, `I-` on the other) or losing the
position because the indices shifted. Working per codepoint keeps `𠮷` whole from start to
finish: exactly one [BIOES](bioes.md) flag is raised, and no offset drift occurs.

---

## How does it work?

We convert a string into a sequence of codepoints — not chars — before processing. In Java,
`String.codePoints()` provides this out of the box.

```
"𠮷田" seen as chars (length 3):
  [\uD842, \uDFB7, '田']   ← 𠮷 is split in two!

seen as codepoints (length 2):
  [0x20BB7, 0x7530]        ← 𠮷 is one, 田 is one
   └─ 𠮷 ─┘ └─ 田 ─┘

labels then map one-to-one onto these two elements:
  [S-name, ...]
```

PUA characters and large-numbered kanji all flow naturally as "one codepoint = one element".

---

## How does kugiri use it?

`label/CodePoints.java` (`CodePoints.of(...)`) converts between a string and its codepoint-unit
sequence. kugiri's core policy is **per-codepoint sequence labeling**; it never works in `char`
units. This matches unlaxer's CodePoint foundation, lets surrogate pairs and gaiji (PUA) flow as
a single unit, and aligns cleanly with the gaiji conversion tables used for name matching. Both
the [BIOES](bioes.md) tags and the [hierarchy](hierarchy.md) spans ride on top of this codepoint
sequence.

---

## Learn more

- [bioes.md](bioes.md) — the position flag raised on each codepoint.
- [hierarchy.md](hierarchy.md) — the hierarchy elements that codepoint spans correspond to.
- [study ch.1 How a computer sees characters](../study/01-moji-to-codepoint.md)
