# Hierarchy (階層要素)

[日本語版はこちら](hierarchy.ja.md)

---

## What is it?

The hierarchy (階層要素) is the list of **part kinds** that make up a Japanese address:
prefecture / city / ward / county (gun) / town-or-oaza / chome / aza-koaza / banchi /
gaiku (block) symbol / residence number / branch number / building / room number / addressee —
ordered from the largest down to the smallest.

Picture an address as a set of **nested boxes that get finer and finer**. The outermost box is
the prefecture, inside it the city, inside that the ward or town, then the chome and banchi…
What kugiri does is split a single run-on address string back into these boxes (kinds).

---

## Why does it matter?

Without a "which kind?" label on each extracted part, you cannot process an address
semantically. Only once you decide whether `千代田区` is a ward or a city, or whether `丸の内` is
a town or an aza, can name matching, record linkage, and normalization work.

And the kinds have two faces: those that have a real string span (prefecture, chome, banchi…)
and those that do not (abstract nodes like "top of the regional domain" or "top of the town
domain"). If you don't settle this distinction up front, you end up **forcing a label onto a
node with no substance**, which corrupts the set of output labels.

---

## How does it work?

There are two kinds of hierarchy element.

```
structural markers (isTop=true)     surface leaves (isTop=false)
───────────────────────────────     ────────────────────────────
regional-top / town-top /           prefecture / city / ward /
building-bottom, etc. — abstract     chome / aza-koaza / banchi /
nodes                                gaiku symbol, etc.
have no string span                  correspond to a real substring
derivable from level, so             only these become sequence labels
dropped from output labels           → used as [BIOES] tags
```

Only the "surface leaves" are used in the output. Structural markers are derivable from their
level (depth), so they are dropped from the labels. The surface-leaf → level mapping lives in
`label/Labels.java`.

---

## How does kugiri use it?

`label/Hierarchy.java` is a faithful port of the original `階層要素` enum, holding each element's
level and `isTop` flag (structural marker vs. surface leaf). The output label strings keep a
**one-to-one** correspondence with this enum. `label/Labels.java` provides the surface-leaf →
level mapping (the definition of the output label set), and the [BIOES](bioes.md) tag space is
built on top of it. The unlabeled `aza-koaza` ([aza](aza.md)) is inferred without supervision
from the [ABR](abr.md) head labels plus the residual slot.

---

## Learn more

- [bioes.md](bioes.md) — the encoding that adds position flags to each hierarchy kind.
- [codepoint.md](codepoint.md) — the unit each element's span rides on.
- [abr.md](abr.md) — the label source for the head layers (prefecture through oaza / gaiku).
- [aza.md](aza.md) — the core task: inferring the unlabeled aza-koaza without supervision.
- [study ch.2 What is "classification"?](../study/02-bunrui-towa.md)
