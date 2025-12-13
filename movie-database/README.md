# Movie Database

![Java](https://img.shields.io/badge/Java-ED8B00?style=flat&logo=openjdk&logoColor=white)

A genre-indexed movie database that supports adding, searching, and deleting movies, with every genre's titles kept **sorted** and **duplicate-free** at all times. The interesting part of this project is not the database itself — it's that the entire thing is built on a **generic singly linked list I implemented from scratch**, because the point of the assignment was to *build* the structure, not to call into one.

## The problem

I needed a small in-memory catalog where each movie is a `(genre, title)` pair, and the operations are:

- **add** a movie under its genre,
- **search** for movies whose title contains a query substring,
- **delete** a movie, and
- enumerate everything in a stable order.

Two requirements shaped almost every decision: titles within a genre must stay **sorted**, and the same title must never appear twice in the same genre. Once you commit to those as *invariants* — properties that are true after every operation rather than things you clean up later — the design largely falls out of them.

## Design decisions

### Building the linked list myself instead of reaching for `java.util.List`

The whole assignment is the data structure. Using `ArrayList` or `LinkedList` would have hidden exactly the thing I was supposed to demonstrate: that I understand how a sequence is actually stored, traversed, and mutated through references. So I wrote a generic `MyLinkedList<T>` backed by a `Node<T>` that holds an item and a `next` reference. Everything else — the genre index, the per-genre title lists — is layered on top of that one primitive.

Making it generic (`<T>`) rather than hardcoding it to strings was deliberate: the same list type stores genres at one level and titles at another, and it returns search results as a list too. One ADT, reused at every level of the design.

### A sentinel head node

`MyLinkedList` starts life holding a single dummy node (`head = new Node<T>(null)`) that never carries data. Real elements always live *after* the sentinel.

This is the decision I'd most want to be asked about. Without a sentinel, inserting or deleting at the front of a list is a special case: you have to reassign the list's `head` field, which means every insert/delete needs an "is this the first node?" branch, and an empty list is yet another branch. With a sentinel, there is *always* a node before the one you care about, so insertion is uniformly "splice a node after `prev`" and deletion is uniformly "unlink the node after `prev`." The empty list and the first-element cases stop being special — they're just the case where `prev` is the sentinel. Fewer branches means fewer places for an off-by-one or null-pointer bug to hide. `isEmpty()` becomes simply "does the sentinel point at anything?"

### Sorted insertion with dedup, kept as an invariant

Rather than appending and sorting later, I keep each title list sorted *as I insert*. Insertion walks the list, drops the new title at the first position where it belongs, and bails out early if it finds the title already present. The trade-off is explicit and I made it on purpose:

- **Cost:** insertion is O(n) — I may have to scan the whole list to find the insertion point and to confirm there's no duplicate.
- **Payoff:** search and full enumeration are trivially correct and already ordered. There is no separate sort step, no "is this sorted yet?" state to track, and no possibility of a duplicate slipping in, because the only way to add a title enforces both properties at once.

For this workload — a catalog you read and scan far more often than you bulk-load — paying on the write to keep reads clean is the right call. If the access pattern were write-heavy bulk loading, I'd revisit this (load unsorted, sort once), but that's not what this is.

### A safe-remove iterator

`MyLinkedList` exposes an `Iterator<T>` that tracks both the current node and the node before it. That `prev` pointer is the whole reason it exists: deleting from a singly linked list requires the predecessor so you can unlink without it, and an iterator that has just visited a node is the one place that already *knows* the predecessor for free.

The classic bug here is mutating a list mid-traversal with an external index or saved reference and then continuing to walk a structure you've already changed underneath yourself. By routing removal through the iterator — which fixes up its own `prev`/`curr` after unlinking and decrements the count in the same step — deletion-during-iteration stays consistent instead of leaving a dangling reference or a stale size. It also guards the obvious misuse: calling `remove()` before `next()` throws rather than corrupting the list silently.

### Two-level genre to titles indexing

The store is a list of genres, and each genre owns its own list of titles. The genre list is itself kept sorted and dedup'd, so there's exactly one bucket per genre.

This mirrors how the data is actually accessed. Every operation starts by naming a genre, so resolving the genre first and then working within its (smaller) title list matches the natural access path — you never scan titles of genres you don't care about for an add or delete. It's a hand-rolled version of the same idea as a map of genre to a sorted collection, built from the one list primitive I had.

## Approach

- **Add:** find the genre. If it doesn't exist, create it and splice it into the genre list in sorted order; then sorted-insert the title into that genre's list, which silently no-ops if the title is already there.
- **Search:** walk every genre, walk its titles, and collect the `(genre, title)` pairs whose title contains the query substring. Because each level is already sorted, results come out in a stable, predictable order.
- **Delete:** find the genre, remove the title from its list, and — this is the bit I like — if that was the genre's last movie, **prune the now-empty genre** so the index never accumulates dead buckets. The structure shrinks back exactly to what's actually stored.

## Complexity

Let *n* be the number of titles in a genre's list (or the number of genres for the index level).

| Operation | Cost | Why |
|-----------|------|-----|
| Sorted insert | O(n) | scan to the insertion point, also checking for a duplicate |
| Search (within a genre) | O(n) | every title is examined against the substring |
| Delete | O(n) | scan to find the node, then O(1) to unlink |
| `isEmpty` / front access | O(1) | sentinel makes these constant |

The honest trade-off versus an array-backed list: an array gives O(1) indexed access and better cache locality, but insertion or deletion in the middle costs O(n) shifting, and growth means reallocation. A linked list gives O(1) splice/unlink *once you hold the right node* and never reallocates. For this workload the dominant cost is the **scan** either way — to find the insertion point, the match, or the deletion target — so the array's indexing advantage never comes into play, while the linked list's clean mid-sequence mutation does. That's why the linked list is the better fit here, not just the assigned one.

## What this demonstrates

- **ADT design:** one generic list primitive, layered into a two-level index — separating the structure from what it stores.
- **Generics:** the same `MyLinkedList<T>` carries genres, titles, and result sets.
- **Iterators:** a custom `Iterator` with safe in-place removal, plus the predecessor-tracking that makes it work.
- **Invariants:** sorted order and no-duplicates held by construction, not patched up after the fact.
- **Pointer manipulation:** sentinel-based splicing and unlinking, done directly on nodes rather than delegated to a library.

## Build & run

Plain `javac` — no build tool or external dependencies:

```bash
javac *.java
```

Two honest notes:

- `MyLinkedList` implements a `ListInterface<T>` that is **not part of this repository** — it's the contract supplied by the assignment, so the sources here compile against it as an assumed external interface. Provide that interface on the classpath to build standalone.
- This is a **library**, not an application: there is **no `main` method**. The classes are meant to be driven by a test harness or instantiated directly (`new MovieDB().insert(new MovieDBItem("genre", "title"))`), not run on their own.
