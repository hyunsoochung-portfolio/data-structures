# Subway Shortest-Path Finder

![Java](https://img.shields.io/badge/Java-SE-007396?logo=openjdk&logoColor=white)

A command-line tool that finds the fastest route between two stations on a subway network, where **both riding a train and changing lines cost time**. I built it to work through a problem I find genuinely interesting: a subway map looks like a graph, but the moment you account for transfers, the "obvious" modeling falls apart. Most of the work here was in the *modeling*, not the algorithm — getting the graph right made the rest fall into place.

This README is about the decisions, not the code. Below I walk through how I mapped a real transit network onto a weighted graph, why Dijkstra is the right algorithm, why I reached for a heap, and what the whole thing costs.

---

## The problem

Given a network of stations and lines, find the route from station **A** to station **B** that minimizes total travel time. The catch is that "time" has two sources:

- **Riding** between adjacent stations on a line takes time.
- **Transferring** between lines at an interchange takes time too — you walk to another platform and wait for a different train.

So the fastest route is *not* the one with the fewest stops. A path that stays on one line through a couple of extra stations can easily beat a path that saves stations by transferring twice. The cost function has to price both, and the shortest-path search has to optimize over their sum.

## Modeling: stations vs. platforms

The first real decision was deciding what a **vertex** is. The tempting answer — one vertex per station name — is wrong, and seeing why is the heart of this project.

If "Sindorim" (served by lines 1 and 2) is a single vertex, then arriving on line 1 and arriving on line 2 are indistinguishable. But they are *not* the same: continuing straight through on line 1 is free, while switching to line 2 costs a transfer. A single-vertex model has nowhere to put that cost, because the transfer happens *inside* the station, between two ways of being there.

So I modeled **platforms as vertices, not stations**. Each `Station` record carries a unique `id`, a human-readable `name`, and the `line` it belongs to. A station served by two lines becomes **two vertices** that share a name but differ in `id` and `line`.

The payoff: **transfers become explicit weighted edges** between co-named platforms. "Changing from line 1 to line 2 at Sindorim" is now a real edge — from the line-1 Sindorim vertex to the line-2 Sindorim vertex — whose weight is the transfer penalty for that station. Riding and transferring are now the *same kind of thing* (a weighted edge), so a single shortest-path search optimizes over both automatically. The hard part of the problem dissolved into a modeling choice.

One subtlety worth noting: I keep transfer edges *implicit* rather than materializing them all upfront. During the search, when the algorithm sits on a platform, it relaxes toward every other same-named platform at the configured transfer cost — but only when no real track edge already connects them (so a through-running line that happens to share a name isn't charged a bogus transfer). Logically these are still edges in the graph; I just generate them on demand, and a station with no configured transfer time falls back to a sensible default.

## Modeling: adjacency list, not a matrix

The second decision was the graph *representation*. The two standard options are an adjacency **matrix** (a V×V grid of weights) and an adjacency **list** (each vertex stores only its actual neighbors). I chose the list.

The reason is the shape of the data. A subway graph is **sparse**: a platform connects to its neighbor up the line, its neighbor down the line, and the handful of other platforms in the same station. Degree is essentially **constant**, independent of how large the network grows — adding stations in another city doesn't give any platform more neighbors. That means E grows linearly with V, not quadratically.

- An adjacency **matrix** costs **O(V²)** space no matter how sparse the graph, and it forces every neighbor scan to walk all V columns, most of them empty.
- An adjacency **list** costs **O(V + E)** space, which for a sparse graph is effectively **O(V)**, and it lets you iterate a vertex's *actual* neighbors in time proportional to its degree.

For a sparse, low-degree graph like this, the list wins on both space and traversal cost. A matrix would only pay off on a dense graph where most vertex pairs are connected — the opposite of a subway.

## Why Dijkstra

With the graph defined, the algorithm choice follows from one property: **every edge weight is non-negative.** Ride times are positive; transfer penalties are positive. There are no negative edges and no way to "gain time" by traversing one.

That is exactly the condition under which **Dijkstra's algorithm** is correct and optimal. Dijkstra is greedy — at each step it permanently settles the closest unsettled vertex and never revisits it. That greedy commitment is only safe when no future path can sneak back and undercut a settled distance, which is guaranteed precisely when edges are non-negative. The problem fits the algorithm's preconditions perfectly.

The natural alternatives don't fit as well:

- **BFS** finds the path with the fewest *edges*, treating every hop as cost 1. With weighted edges it simply solves the wrong problem — it can't tell that two stops on one line beat one transfer.
- **Bellman-Ford** handles negative edges and detects negative cycles, at a cost of **O(V·E)**. We have no negative edges, so that capability buys nothing and we'd be paying a strictly worse running time for robustness we don't need. Reaching for it here would be over-engineering.

Dijkstra is the tightest tool that exactly matches the problem's constraints.

## Why a heap

Dijkstra's correctness doesn't depend on *how* you find "the closest unsettled vertex" — but the performance does, and this is where the implementation choice matters.

The textbook-naive version scans the whole distance array on every iteration to find the minimum. That's **O(V)** per extraction, repeated V times, giving **O(V²)** overall. On a sparse graph that's wasteful: you spend most of each scan looking at vertices that aren't even relevant yet.

Instead I keep the frontier in a **binary-heap priority queue**, which serves up the minimum in **O(log V)**. Each of the E edge relaxations may push a new entry, and each extraction pops one, so the search runs in **O(E log V)**. For a sparse graph (E ≈ V), that's roughly **O(V log V)** — dramatically better than the array-scan's O(V²) as the network scales.

Two practical details fall out of using Java's heap, which has no decrease-key operation:

- **Lazy deletion.** Rather than updating a vertex's existing heap entry when I find a shorter distance, I just push a fresh entry. The stale, larger-distance copies are still in the heap, but when one is popped I check it against the best known distance and discard it if it's outdated. Simpler than maintaining heap positions, and it doesn't change the asymptotic cost.
- **Early exit.** The moment the destination is popped from the heap, its distance is final — that's the Dijkstra invariant — so the search stops immediately instead of settling the rest of the network. On a long network with a nearby target, this saves a lot of needless work.

## Path reconstruction & multi-platform endpoints

Finding the *cost* of the shortest path isn't enough; you want the actual route. I track this with a **predecessor map**: whenever relaxing an edge improves a vertex's distance, I record which vertex it came from. Once the search finishes, I walk that map backward from the destination to the source and reverse it to get the path in travel order. This is the standard predecessor-pointer technique — O(path length) to rebuild, and no extra cost during the search beyond one map write per relaxation.

There's one wrinkle specific to the platform model. A query names *stations* ("Gangnam to Sinwol"), but the search runs over *platforms*, and a station name can map to several platforms. So I **expand each endpoint into its platforms**: the start station becomes a set of source platforms and the destination a set of target platforms. I run the search across these and keep the global best, which guarantees the true fastest route no matter which platform the rider happens to board from or get off at. Since any real station has only a small, constant number of platforms, this expansion doesn't change the per-query complexity in any meaningful way.

When printing, consecutive same-name platforms (the two ends of a transfer edge) collapse back into a single station name, and genuine interchanges — where the line you arrive on differs from the line you leave on — get bracketed, e.g. `Gangnam [Sindorim] Sinwol`. The platform-level model is an internal detail; the rider sees stations.

## What this project demonstrates

- **Graph modeling under real-world constraints** — recognizing that the interesting cost (transfers) lives *between* representations of the same place, and choosing platform-level vertices so that cost becomes a first-class weighted edge.
- **Representation trade-offs** — picking an adjacency list over a matrix from the sparsity of the domain, and being able to justify it in space and traversal terms.
- **Matching algorithm to problem** — Dijkstra because weights are non-negative; knowing why BFS and Bellman-Ford are the wrong reach.
- **Priority queues and complexity analysis** — heap-based Dijkstra at O(E log V) vs. the naive O(V²), plus the practical heap patterns (lazy deletion, early exit) that come with no decrease-key.

---

## Build & run

No external dependencies — just the JDK and the standard library.

```bash
# Compile
javac *.java

# Run; the single argument is the network data file
java Subway subway.txt
```

The data file has **three blank-line-delimited sections**, in order:

```
# 1. Stations: id name line
S1 Gangnam 2
S2 Sindorim 1
S3 Sindorim 2

# 2. Edges: from to time   (directed — supply both directions for two-way track)
S1 S3 4
S3 S1 4

# 3. Transfer times: stationName time   (names omitted here use a default)
Sindorim 3
```

Queries are read from **stdin**, one per line, as two station **names** separated by a space. Each prints the fastest route (interchanges bracketed) followed by the total travel time. `QUIT` ends the session.

```
Gangnam Sinwol
Sindorim Gangnam
QUIT
```
