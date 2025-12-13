# Datastructure_Sortingtest

![Java](https://img.shields.io/badge/Java-SE-007396?logo=openjdk&logoColor=white)

I built every major sorting algorithm from scratch — Bubble, Insertion, Heap, Merge, Quick, and Radix — in a single command-driven Java harness (`SortingTest.java`). The point was never just to make them work; it was to internalize *when each one wins and why*, and then to encode that judgment into an adaptive recommender that reads the shape of the data and picks the right tool for it. A measured benchmark write-up is included as `Report.pdf`.

## The problem

"Sort an array" sounds like one problem, but it's really a family of trade-offs. The same input can be sorted in microseconds or in seconds depending on which algorithm I reach for, and the right choice depends entirely on the data: How big is it? Is it nearly sorted already? How many duplicates? How wide are the keys? My goal here was to implement the canonical sorts honestly (no `Arrays.sort`, only `java.io` / `java.util`), benchmark them against each other on identical inputs, and demonstrate that I understand the theory well enough to choose between them automatically.

The harness loads one integer array — either random (`r <count> <min> <max>`, which reports wall-clock time per sort) or a fixed array read from stdin (which prints the sorted result for correctness checking) — and then runs whichever algorithm I ask for. Crucially, every sort runs on a fresh `clone()` of the loaded data, so all six compete on byte-identical input within a single session. That's what makes the timing comparisons meaningful.

## Why these algorithms differ

The interesting story is the trade-off space. Here's how I think about the six I implemented, on the axes that actually matter — asymptotic cost, memory, stability, and how they react to input that's already partly ordered.

| Algorithm | Best | Average | Worst | Space | Stable | In place |
|-----------|------|---------|-------|-------|--------|----------|
| Bubble    | O(n²) | O(n²) | O(n²) | O(1) | yes | yes |
| Insertion | O(n) | O(n²) | O(n²) | O(1) | yes | yes |
| Heap      | O(n log n) | O(n log n) | O(n log n) | O(1) | no | yes |
| Merge     | O(n log n) | O(n log n) | O(n log n) | O(n) | yes | no |
| Quick     | O(n log n) | O(n log n) | O(n²) | O(log n) | no | yes |
| Radix     | O(d·n) | O(d·n) | O(d·n) | O(n + k) | yes | no |

**Bubble and Insertion** are both O(n²) quadratic sorts, but they are not equivalent. My bubble sort is the textbook baseline — adjacent swaps with a shrinking inner bound, and deliberately no early-exit flag, so it's quadratic even on sorted input. I keep it as the honest worst case to benchmark against. Insertion is the one that earns its keep: because its inner loop stops the moment order holds, it runs in near-linear time on nearly-sorted data and has tiny constant factors. **That's exactly why Insertion beats Quick on small or almost-sorted arrays** — Quick's recursion, partitioning, and pivot bookkeeping cost more than Insertion's handful of shifts when n is small or the data barely needs moving. This is why real-world hybrid sorts (Timsort, introsort) fall back to insertion sort below a size threshold; I lean on the same insight in my recommender.

**Merge sort** is the safe O(n log n) choice. Its cost is O(n log n) in the best, average, *and* worst case — no input can degrade it — and it's stable, which matters when equal keys carry satellite data whose original order must survive. The price is memory: my implementation allocates scratch buffers to merge into, so it's O(n) extra space rather than in-place. When I can't tolerate a worst case and I can spare the memory, Merge is the default I trust.

**Quick sort** is usually the fastest in practice because of excellent cache behavior and a tight in-place partition (I use the Lomuto scheme with the last element as pivot). But pivot choice is its Achilles' heel: a fixed last-element pivot means already-sorted (or reverse-sorted) input produces maximally unbalanced partitions, collapsing it to **O(n²)** with O(n) recursion depth. The lesson I took from implementing it this way is precisely *why* production quicksorts randomize the pivot or use median-of-three — the average case is great, but the worst case is a real liability you have to engineer around.

**Heap sort** is the algorithm I reach for when I want guaranteed O(n log n) *and* O(1) extra space. It builds a max-heap bottom-up and repeatedly extracts the root. Unlike Quick it has no quadratic worst case, and unlike Merge it needs no auxiliary array — it's the best worst-case-bounded in-place comparison sort here. The trade-off is that it's not stable and its constant factors and cache behavior are worse than Quick's, so it tends to lose on average-case timing even though it can never blow up.

**Radix sort** is the outlier, and the most conceptually interesting. It's a **non-comparison** sort: instead of comparing elements to each other, LSD radix sort distributes them by digit through a stable counting sort, one decimal place at a time. That lets it escape the comparison lower bound entirely and run in O(d·n), where `d` is the number of digits — linear in n for fixed-width keys. The catch is that counting sort over digit buckets has no natural notion of a negative digit, so I handle signs explicitly: split the array into positives and negatives, radix-sort the negatives by **absolute value**, then write them back **reversed and re-signed** (so `-3` precedes `-1`) and concatenate them ahead of the sorted positives. The result is a correct fully-ascending order including negatives.

### The comparison-sort lower bound

It's worth being precise about why O(n log n) is the floor for the comparison sorts above. Any sort that only inspects elements via pairwise comparisons can be modeled as a binary decision tree: each comparison is an internal node with two outcomes, and every distinct permutation of the input must reach its own leaf. With n elements there are n! permutations, so the tree needs at least n! leaves, and a binary tree with n! leaves has height at least log₂(n!) = Ω(n log n) by Stirling's approximation. That height is the number of comparisons in the worst case — so no comparison-based sort can beat Ω(n log n). Radix sidesteps this not by being cleverer about comparisons but by *not comparing at all*, which is the only way to legitimately go faster.

## The adaptive recommender (the part I'm proudest of)

The `S` command runs a routine that profiles the data and returns a single letter — the algorithm I'd choose for it. This is where the theory above becomes engineering judgment: "choose the right tool for the data." It checks signals in priority order and returns on the first match:

1. **Key width first (→ Radix).** If the largest absolute value has **≤ 4 digits**, recommend Radix. The reasoning: radix's cost is O(d·n), so it only beats the O(n log n) comparison sorts when `d` is small. Capping at 4 digits is a conservative bet that the key width is narrow enough for the linear-time pass to pay off; wider keys make `d` large enough that a comparison sort wins again.
2. **Duplication next (→ Merge).** I estimate duplication by hashing values into a table of size n and counting collisions; a collision rate **> 0.75** signals heavy duplication. With many equal keys I want **stability** preserved and I want to avoid Quick's degenerate behavior on long runs of equal elements, so I pick Merge — its guaranteed O(n log n) and stability are the safe answer here.
3. **Sortedness (→ Insertion).** I measure the fraction of adjacent pairs already in order; **> 0.9** means the array is nearly sorted. That's Insertion's sweet spot — it goes near-linear on almost-ordered input — so it's the obvious pick over an O(n log n) sort that ignores existing order.
4. **Size (→ Insertion).** If none of the above fire but `n < 80`, still choose Insertion. Below this threshold the low constant factors of insertion sort beat the overhead of Quick's recursion and partitioning — the same hybrid-sort cutover real libraries use.
5. **Default (→ Quick).** Otherwise the data is large, varied, and unsorted, so I recommend Quick for its best average-case performance.

Each threshold is a deliberate encoding of a complexity trade-off rather than a magic number: digit count gates radix's linear regime, the collision rate guards stability, the sortedness ratio targets insertion's adaptivity, and the size cutoff respects constant factors. The recommender only advises — it doesn't run the sort — which keeps the profiling honest and separate from the timing.

## Build & run

No build tool or external dependencies — just the JDK.

```bash
javac SortingTest.java
java SortingTest
```

The first stdin line configures the dataset; each following line is a single-character command. Commands: `B` Bubble, `I` Insertion, `H` Heap, `M` Merge, `Q` Quick, `R` Radix, `S` recommend-an-algorithm, `X` exit. In random mode each command prints elapsed milliseconds; in fixed mode it prints the sorted output (or, for `S`, the recommended letter).

```text
# Timing mode: 100,000 random ints in [0, 1000000], time a Quick Sort, exit
r 100000 0 1000000
Q
X
```

```text
# Fixed mode: load 5 ints, Radix-sort and print, then ask the recommender, exit
5
9
1
7
3
2
R
S
X
```

A single command can also be passed as the first CLI argument instead of read from stdin.

## Performance report

`Report.pdf` (repository root) contains my measured benchmark analysis of these implementations across input sizes and distributions.
