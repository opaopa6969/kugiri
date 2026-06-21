# ABR (Address Base Registry)

[日本語版はこちら](abr.ja.md)

---

## What is it?

**ABR (Address Base Registry / アドレス・ベース・レジストリ)** is Japan's public, authoritative
"address master." Maintained by the Digital Agency, it ships addresses **already decomposed**
into town/oaza, block, residential number, and parcel number, each carried by its own ID.

Think of a library catalog. Instead of storing a book as the raw text on its spine, the catalog
stores it as a **card broken into fields** — author, title, publisher, and so on. ABR is the
catalog for addresses: you receive an address not as one whole string, but already **split into
fields** — `oaza / chome / aza / block symbol / residential number`. That field-level split is
what makes ABR valuable to kugiri.

---

## Why does it matter?

kugiri is a learner that cuts an address into a token sequence of [hierarchy](hierarchy.md)
elements. Training needs gold labels — "this span is that layer" — but a raw, unsegmented address
string has none.

Because ABR is **already split into fields**, kugiri can auto-generate labeled training data for
the **head of the address (oaza through residential number, lv4–7)** with no human annotation.
This is the foundation of [weak supervision](weak-supervision.md). And once the head is fixed,
the scaffold is in place to infer — with no supervision — the unlabeled span left in the tail:
the [aza](aza.md).

---

## How does it work?

ABR has no ZIP code, so kugiri **looks up roughly via [KEN_ALL](ken-all.md), which has the ZIP,
then re-resolves that town with ABR** (the so-called "opa method"). Matching is done by
**string**, not by org code: KEN_ALL's org code is 5 digits and ABR's is 6, so joining on the
code directly will not align.

```
  (1) Look up KEN_ALL by ZIP
      020-0021 → Iwate / Morioka-shi / Ueda
                          │ match on town name (string)
                          ▼
  (2) Re-resolve with ABR (already split into fields)
      oaza=Ueda  chome=…  block=…  residential-no=…

  ★ KEN_ALL's merged field is split by ABR:
     千代田区        → 東京23区:千代田区
     横浜市西区      → 市:横浜市   + 区:西区
     虻田郡洞爺湖町  → 群:虻田郡   + 町村:洞爺湖町
```

ABR columns are read **by header name** (`get("列名")`). If a real dump's columns shift between
versions, the only code change is the **column-name string**.

---

## How does kugiri use it?

The implementation is `abr/Abr.java` (re-resolves KEN_ALL→ABR into a Component sequence of
[hierarchy](hierarchy.md) elements) and `abr/Csv.java` (header-name-based CSV reader).
`demo/AbrDemo` demonstrates the splits for the Tokyo 23 wards / city+ward / county+town cases.

> Note: raw ABR/KEN_ALL data is never committed to the repo (samples only). Real-data ingestion
> happens only after verifying column names against the actual schema.

---

## Learn more

- [ken-all.md](ken-all.md) — the ZIP-keyed partner; the entry point into ABR.
- [weak-supervision.md](weak-supervision.md) — using ABR as the teacher for head labels.
- [hierarchy.md](hierarchy.md) — the full set of layers ABR splits an address into.
- [aza.md](aza.md) — the tail-end aza inferred without supervision once the head is fixed.
- [study chapter 17](../study/17-weak-supervision-abr.md) — the big picture of weak supervision.
