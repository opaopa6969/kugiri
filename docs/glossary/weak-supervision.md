# Weak Supervision

[日本語版はこちら](weak-supervision.ja.md)

---

## What is it?

Weak supervision means **deriving "approximate, cheap" labels automatically from external
structured data, instead of hand-labeling every example one by one**.

Think of it as: rather than the teacher grading every paper by hand, you **machine-match the
papers against an answer key (an existing address database) and mark them roughly**. The labels
are not perfect (they carry noise), but you can produce **huge quantities of them for almost free**.

In kugiri, the labels for the **head** of an address — prefecture, city, ward, gun, town, chome,
[gaiku block](hierarchy.md), residence number — are assigned automatically from
[KEN_ALL](ken-all.md) and [ABR](abr.md).

---

## Why does it matter?

Inferring [aza](aza.md) without supervision needs a **scaffold**. Only once you know where the
head of an address ends and where the trailing digit run (banchi) begins can the elimination
logic — "the span left in between is the aza" — hold.

Weak supervision builds that scaffold with **zero human labeling**.

```
  Address:  岩手県盛岡市上田 小鳥沢 1234番地

  Labelable by weak supervision (KEN_ALL + ABR):
    岩手県=prefecture / 盛岡市=city / 上田=oaza ... 1234番地=banchi
                          ↑       ↑
                     head ends   tail begins
                              \   /
                          leftover = aza, inferred without supervision
```

The reason it is "weak" is that it **only covers the head**. The tail containing the aza has no
gold at all — which, in contrast to the fully-supervised [synthetic data](synthetic-data.md), is
left to unsupervised inference.

---

## How does it work?

KEN_ALL maps ZIP→(prefecture, municipality, town area) but carries only org codes; ABR has
decomposed town/block/residence but carries no ZIP. The **opa method** bridges them.

```
  ① Look up the town area roughly via KEN_ALL's ZIP
        020-0021 → 岩手県 / 盛岡市 / 上田
                       │
  ② Re-resolve that town area through ABR to split the head finely
        盛岡市 → city:盛岡市
        千代田区 (special ward)   → Tokyo-23-ward:千代田区
        横浜市西区 (designated city) → city:横浜市 + ward:西区
        虻田郡洞爺湖町 (gun)       → gun:虻田郡 + town:洞爺湖町
                       │
  ③ Match on STRINGS (org codes differ: KEN_ALL 5-digit / ABR 6-digit)
```

Thanks to concatenation alignment, joining a component list `[(label, surface), …]` fixes the
codepoint-level labels **with no alignment computation**.

---

## How does kugiri use it?

Head-label generation is fronted by `abr/Abr.java` (KEN_ALL→ABR re-resolution → component list),
supported by `abr/KenAll.java` and `abr/Csv.java`. `demo/AbrDemo` demonstrates splitting
Tokyo-23-wards / city+ward / gun+town (and logs the unmatched rate).

These head labels then become the **seed** for the zero-label aza induction in `aza/AzaInducer`.
For self-training (semi-supervised), the principled path seeds with these head labels plus the aza
inference, pseudo-labels only high-confidence predictions, and iterates retraining.

---

## Learn more

- [abr.md](abr.md) — the Address Base Registry that splits the head finely.
- [ken-all.md](ken-all.md) — Japan Post's address data mapping ZIP to town area.
- [aza.md](aza.md) — the aza inferred unsupervised on top of the weak-supervision scaffold.
- [synthetic-data.md](synthetic-data.md) — fully-supervised synthetic data (contrast).
- [Chapter 17: Weak supervision with ABR and KEN_ALL](../study/17-weak-supervision-abr.md).
