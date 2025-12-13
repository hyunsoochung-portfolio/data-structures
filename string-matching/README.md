# Substring Matching — A Hash Table of AVL Trees

![Java](https://img.shields.io/badge/Java-ED8B00?style=flat&logo=openjdk&logoColor=white)

A text indexing engine that answers a single, deceptively demanding question fast: *given a keyword, where does it occur?* — and it must return **every** occurrence, as a list of `(line, column)` positions, over a body of text loaded from a file.

This README is about the reasoning behind the data structure I chose, not a tour of the methods. The headline decision is the one worth defending: the index is a **hash table whose buckets are AVL trees**. Below I explain why that hybrid is the right shape for this problem, and why each of the obvious simpler alternatives loses.

## The problem

I want interactive, repeated keyword lookups against the same text. The text is loaded once; queries come many times afterward. So this is fundamentally a **build-once, query-many** workload, which tells me where to spend effort: I can afford to pay for a richer index up front if it makes each query cheap and predictable.

Two properties shape the design:

- A query must return **all** matches, not just "found / not found". That rules out anything that collapses duplicates — the structure has to aggregate multiple positions under one key.
- Worst-case behavior matters. A search engine that is fast on average but degrades to a linear scan on adversarial or already-sorted input is not something I'd want behind an interactive prompt. I optimized for the worst case being *provably* good, not just the average case being acceptable.

## The centerpiece: why a hash table *of* AVL trees

The instinct is to reach for one structure. I deliberately composed two, because each covers the other's weakness.

**Why not a plain hash table alone?** Hashing gives me O(1) expected bucketing, which is exactly what I want for the first cut. But a hash table on its own has two problems here. First, collisions are inevitable — my keys are short text substrings and the address space is finite, so multiple distinct keys land in the same bucket. The classic answer is a collision chain (a linked list per bucket), but a chain is an **unordered linear structure**: once a bucket gets busy, locating the right key inside it degrades to O(chain length). Second, a hash table destroys ordering entirely. There's no meaningful way to enumerate keys in sorted order, inspect a bucket's contents predictably, or reason about its internal distribution. I wanted the per-bucket structure to be *searchable in logarithmic time and inspectable in order*, not a flat list.

**Why not a single balanced tree, then?** A balanced tree alone solves ordering and gives O(log n) search — but over *all* keys at once. Hashing first is what shrinks the search space: the hash partitions the entire key set into independent buckets in O(1), so any subsequent tree operation works against `n / (number of buckets)` keys on average, not `n`. The hash is the coarse, constant-time filter; the tree is the fine, logarithmic one. Composing them means each query does O(1) work to pick a bucket and then O(log m) work inside it, where m is that bucket's population — strictly better than searching one giant tree of size n.

So the hybrid is not two structures glued together for show. It's a deliberate division of labor: **the hash collapses the space cheaply; the tree keeps each bucket fast and ordered.** That directly fixes the collision-chain problem — a busy bucket is a balanced tree, not a linear list, so even a hot bucket stays logarithmic.

## Why AVL specifically — and AVL vs. red-black

Inside each bucket I needed a *self-balancing* search tree, and the word "self-balancing" is load-bearing.

A plain unbalanced BST has the right asymptotics only on random input. The moment keys arrive in sorted (or reverse-sorted) order — which is entirely plausible for substrings drawn from natural text, and trivial for an adversary to engineer — the tree degenerates into a linked list and every operation collapses to O(n). That would silently undo the entire point of bucketing: a "tree" bucket that's secretly a list is no better than the collision chain I rejected. I refused to accept a structure whose guarantee evaporates on ordered input.

**AVL trees** solve this by enforcing an invariant on insert: the heights of any node's two subtrees differ by at most one. I track each node's height and compute its balance factor; when an insertion pushes the factor past ±1, the tree restores balance with **rotations** — single (left or right) or double (left-right / right-left) — chosen by the sign of the balance factor and the direction of the offending insert. The payoff is a hard guarantee: height stays Θ(log n), so search, and the navigation part of insert, are O(log n) in the **worst case**, not merely on average. That worst-case guarantee is precisely the property I was unwilling to give up.

**AVL vs. red-black:** both are self-balancing and both give O(log n) worst-case operations, so the choice is about which constant factors to favor. Red-black trees balance more loosely — they tolerate longer paths in exchange for fewer rotations per write — which makes them the usual pick for write-heavy workloads. AVL trees are more rigidly balanced: they may rotate slightly more on insert, but they keep height tighter and therefore make **lookups faster**. Given that my workload is build-once/query-many — reads dominate — I chose AVL because I'm happy to pay a little extra at index-build time to get the shortest possible search paths during the query phase. The trade-off lines up with the access pattern.

## Why n-gram keys

I don't index whole lines or whole words — I index fixed-length substrings (n-grams; here, length-6 windows). Indexing every length-n window of every line means that *any* query of length ≥ n shares a known prefix with some indexed key, so I always have a constant-length handle to hash and look up, independent of how long the query is. It also keeps the keys uniform in length, which keeps comparisons and the hash function (a simple sum of character codes, folded into the table size) cheap and predictable. The cost is index size — a line of length L contributes L − n + 1 keys — but that's the up-front price I already decided I was willing to pay for cheap queries.

## Approach

The system runs in two conceptual phases.

**Index build.** When a file is loaded, I slide a fixed-length window across every line. Each window is an n-gram key; I hash it to choose a bucket, and insert the key into that bucket's AVL tree paired with the position `(line, column)` where it starts. Crucially, when the same n-gram appears again, the AVL node for that key already exists — so instead of duplicating the key, I **append the new position to a list hanging off that node**. This is how multiple occurrences aggregate cleanly: one key, one ordered tree node, and a chain of every position it was seen at.

**Query.** A search takes the query's leading n-gram, hashes it, and looks it up in the corresponding bucket's AVL tree in O(log m). That lookup yields a list of *candidate* positions — places where the query's first n characters match. The n-gram match is necessary but not sufficient (the query may be longer than n, and only its prefix was indexed), so each candidate is **verified** by comparing the full query against the original source text at that position. Verified positions are collected and returned together; if nothing survives verification, the query reports no match. The original lines are kept alongside the index precisely so this verification step has the ground truth to check against. (A query shorter than one n-gram has no indexable prefix, so it simply reports no match.)

## Complexity

Let n be the total number of indexed n-grams and m the population of a given bucket.

- **Hashing / bucket selection:** O(1) expected, O(1) work per key.
- **AVL search and the navigation in insert:** O(log m) worst case, guaranteed by the height invariant — the whole reason for choosing a self-balancing tree.
- **Build:** for a line of length L, sliding the window emits L − n + 1 keys, each an O(1) hash plus an O(log m) tree insert — so building the index is O(total-text-length × log m).
- **Query:** O(1) to hash the prefix, O(log m) to find the candidate list, then O(k · q) verification where k is the number of candidates and q is the query length (each candidate is checked against the source). Verification touches only true prefix-matches, so k is small in practice.

**Why the hybrid wins in the worst case.** A plain hash table with chaining degrades to O(bucket size) once a bucket is hot. A single unbalanced BST degrades to O(n) on sorted input. The hash-of-AVL design has no such failure mode: the hash bounds bucket size on average, and the AVL invariant bounds each bucket's search at O(log m) *even under adversarial, sorted, or skewed input*. The two layers cover exactly the cases where the other would have fallen over.

## What this demonstrates

- **Hashing** as a constant-time space-partitioning tool, and a clear-eyed view of its weakness (collisions, no ordering) rather than treating it as magic.
- **Self-balancing trees and rotations** — the AVL height invariant and how single/double rotations restore it, plus the judgment to pick AVL over red-black based on a read-heavy access pattern.
- **Worst-case vs. average-case reasoning** — the recurring theme that drove every decision: refusing structures whose guarantees hold only on benign input, and composing two structures so the worst case stays provably logarithmic.
- **Composing data structures** so each one's strength covers the other's weakness, rather than forcing a single structure to do everything.

## Build & run

```bash
javac *.java
java Matching
```

The program reads commands from standard input:

- `< filename` — load a text file and build the index over it.
- `? pattern` — search for a pattern; prints every `(line, column)` where it occurs, or `(0, 0)` if none.
- `@ index` — print the keys held in hash bucket `index` (a preorder walk of that bucket's AVL tree), or `EMPTY`.
- `QUIT` — exit.
