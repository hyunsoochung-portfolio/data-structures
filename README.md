# Data Structures & Algorithms (Java)

> Data-structure and algorithm coursework projects in Java. Each one implements the underlying structure by hand — no `java.util` collections for the core logic.

![Java](https://img.shields.io/badge/Java-007396?logo=openjdk&logoColor=white)

Each project lives in its own folder with its own README explaining the **design decisions and CS reasoning** behind it.

| Project | What it implements | Core data structures / algorithms |
|---------|--------------------|-----------------------------------|
| [**biginteger-calculator**](./biginteger-calculator) | Arbitrary-precision integer calculator (add / subtract / multiply) | Digit arrays with manual carry & borrow; sign/magnitude handling |
| [**string-matching**](./string-matching) | Substring search engine over text | Hash table of **AVL trees** keyed on n-grams; intrusive linked lists of match positions |
| [**movie-database**](./movie-database) | Genre-indexed movie database with add / search / delete | Custom generic **linked list** (sentinel head, safe-remove iterator) |
| [**sorting-benchmark**](./sorting-benchmark) | Test & timing harness for sorting algorithms | Bubble / Insertion / Heap / Merge / Quick / Radix, plus an adaptive algorithm selector |
| [**stack-calculator**](./stack-calculator) | Infix expression calculator | **Shunting-yard** infix→postfix conversion + stack evaluation; precedence & associativity |
| [**subway-shortest-path**](./subway-shortest-path) | Shortest-path finder over a subway network | **Dijkstra** over a weighted adjacency-list graph with a binary-heap priority queue |

## Building & Running

Each folder is a standalone Java program. From inside a project folder:

```bash
javac *.java
java <MainClass>     # see each project's README for the entry-point class
```

## Why this repo

These were coursework projects where the point was to implement the structures themselves — balancing an AVL tree on insert, running Dijkstra with a real priority queue, doing big-integer arithmetic on raw digit arrays — rather than reaching for built-in collections. I merged the individual assignment repos into one place so the work reads as a single, browsable collection; each folder's README focuses on *why* a given structure or algorithm was the right choice.
