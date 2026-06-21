# KEN_ALL (Japan Post's postal-code data)

[日本語版はこちら](ken-all.ja.md)

---

## What is it?

**KEN_ALL.CSV** is the postal-code dataset distributed by Japan Post. Think of it as a giant
table where, for each ZIP code, you get `prefecture / municipality / town area` lined up.

It is like a reverse phone book. If you know the number, you can immediately look up the
"rough address" it belongs to. For kugiri, it is the handiest entry point for pulling out the
**head of an address (lv1–5: prefecture through town area)** using the ZIP as the key.

---

## Why does it matter?

[ABR](abr.md) splits an address neatly into fields, but it **has no ZIP code**. KEN_ALL, on the
other hand, does have the ZIP. So kugiri uses a two-stage approach:

1. Look up the "rough address (prefecture, municipality, town area)" from the ZIP using **KEN_ALL**, then
2. Re-resolve that town area with **ABR** to get the field-level split labels.

KEN_ALL plays the **first stage — the scaffold**. It is the starting point for generating
[weak-supervision](weak-supervision.md) labels.

---

## How does it work?

The head strings looked up from KEN_ALL are applied to the address by **longest-prefix match at
the head** of the string, assigning [hierarchy](hierarchy.md) labels.

```
  Address:  岩手県盛岡市上田1234番地
  ZIP:      020-0021
             │ look up KEN_ALL
             ▼
  prefecture=岩手県  municipality=盛岡市  town=上田
             │ label by longest-prefix match at the head
             ▼
  [都道府県:岩手県][市:盛岡市][町または大字:上田] 1234番地…
```

Two gotchas:

- **The encoding is CP932 (Windows-31J).** Reading it as UTF-8 produces mojibake (only the
  repo's samples are UTF-8).
- **The municipality field can be merged** (e.g. `横浜市西区`). [ABR](abr.md) splits this further
  into `市:横浜市 + 区:西区`.

Special town notations such as Kyoto street names and Iwate *jiwari* are normalized
case-by-case in `cleanTown`.

---

## How does kugiri use it?

The implementation is `abr/KenAll.java`, which provides `KEN_ALL.CSV → (zip7, prefecture,
municipality, town)`. The head labels obtained here are handed to [ABR](abr.md) for re-resolution
and turned into training data via `synth/Synth.fromRecords`. `demo/AbrDemo` shows the full flow.

> Raw KEN_ALL data is never committed (samples only). Real data is read as CP932.

---

## Learn more

- [abr.md](abr.md) — the partner that re-splits the town area looked up via KEN_ALL.
- [weak-supervision.md](weak-supervision.md) — making head labels via KEN_ALL→ABR.
- [hierarchy.md](hierarchy.md) — the layers (lv1–5) KEN_ALL fills in.
- [study chapter 17](../study/17-weak-supervision-abr.md) — the big picture of weak supervision.
