# Stack-Based Expression Calculator

![Java](https://img.shields.io/badge/Java-SE-007396?logo=openjdk&logoColor=white)

A small command-line calculator that evaluates infix arithmetic expressions — `3 + 4 * 2`, `-(5 - 8) ^ 2`, `2 ^ 3 ^ 2` — and respects operator precedence, associativity, and parentheses. I wrote it as a focused study of how stacks turn a hard parsing problem into an easy one.

## The problem

Infix is how humans write math, but it is ambiguous to a machine until you pin down three rules: **precedence** (`*` binds tighter than `+`, so `3 + 4 * 2` is `11`, not `14`), **associativity** (`8 - 3 - 1` groups left as `(8 - 3) - 1`, while `2 ^ 3 ^ 2` groups right), and **parentheses** that override both. A correct evaluator has to honor all three at once, from a flat left-to-right stream of characters.

## Why a stack is the right tool

The whole project hinges on one decision: don't evaluate infix directly — convert it to **postfix** (Reverse Polish Notation) first, then evaluate that. I made this choice because it splits one tangled problem into two clean ones, and a stack is the natural data structure for both halves.

**Conversion via the shunting-yard algorithm.** Dijkstra's shunting-yard keeps an operator stack while scanning tokens left to right. Operands go straight to the output; an incoming operator first pops any stacked operators that should bind first, then pushes itself. That single rule is where precedence and associativity actually live — and it's why I don't need a grammar or any backtracking to resolve `3 + 4 * 2`. When `+` is on the stack and `*` arrives, `*` outranks it, so `+` waits; `4 * 2` emits first. The stack *remembers deferred work* exactly as long as it should, which is precisely what a LIFO structure is for.

The precedence/associativity decision collapses to a tiny table plus one comparison:

| Operators | Precedence | Associativity |
|---|:--:|---|
| `+` `-` | 1 | left |
| `*` `/` `%` | 2 | left |
| `~` (unary minus) | 3 | right |
| `^` (power) | 4 | right |

The associativity distinction is just the comparison operator. For a **left**-associative incoming operator I pop the stack while the top's precedence is `>=` the incoming one — popping equals is what makes `a - b - c` group left. For a **right**-associative one I pop only while it's strictly `>`, which leaves equal-precedence operators on the stack and yields right grouping, so `2 ^ 3 ^ 2` correctly evaluates the upper `^` first (`3^2 = 9`, then `2^9 = 512`). One table, one comparison, all three rules satisfied — no special cases bolted on later.

**Parentheses as stack scoping.** A `(` is pushed as a sentinel; a `)` pops operators back to output until that sentinel is found and discarded. Parentheses never produce output of their own — they just carve out a local scope on the stack, and an unmatched one (sentinel missing, or a leftover at the end) is exactly how I detect malformed input.

**Evaluation is then trivial.** Postfix has no precedence left to reason about: scan it once with a single value stack, push numbers, and when an operator appears pop its operands, apply it, push the result. A well-formed expression ends with exactly one value on the stack — anything else is an error. This is the payoff of converting first: the evaluator has no notion of precedence at all.

### The unary-minus trick

`-` is genuinely two different operators: binary subtraction (`5 - 3`) and unary negation (`-5`, `-(a + b)`). They share a glyph but have different arity and precedence, which is the kind of thing that breeds ugly special cases. I resolve it with a clean rewrite: I track a `needOperand` flag, and a `-` seen when an operand is *expected* (at the start, after `(`, or after another operator) is unary. At that moment I rename it to a distinct internal operator **`~`**, with its own precedence (3, right-associative). From there it flows through the same machinery as everything else, and the value stack negates instead of subtracting. Because `^` (4) outranks `~` (3), `-2 ^ 2` correctly becomes `-(2^2) = -4` for free — no extra logic, just the table doing its job.

### A variadic operator, as a stress test

To probe how flexible the design really is, I added a variadic **average**: `(1, 2, 3)` returns the integer mean. The interesting part is arity. Every other operator has fixed arity, but average takes *n* operands, so the postfix stream itself has to carry that count. I encode it into the token — three operands emit `... 3avg` — and the evaluator reads the count off the token, pops that many values, and pushes the mean. It slots into the same value-stack loop as a binary operator. That an *n*-ary operator drops in without disturbing the rest is the best evidence that postfix-plus-a-stack was the right backbone.

## Why not recursion or a full grammar?

A recursive-descent parser over a precedence-climbing grammar would also be correct, and for a richer language (functions, variables, type rules) I'd reach for one. But for fixed-precedence arithmetic it's heavier than the problem deserves — a function per precedence level, and the call stack doing implicitly what an explicit stack does plainly. Shunting-yard is a **single O(n) pass** with one visible stack, which makes the precedence logic something you can point at rather than infer from the recursion structure. For this scope, simplest-correct wins.

## Complexity

**O(n)** in the number of tokens, for both conversion and evaluation. Each token is read once; each operator is pushed and popped at most once, so the total stack work is amortized constant per token. The variadic average pops *k* values, but those *k* were each pushed exactly once, so it stays linear overall. Auxiliary space is O(n) for the operator and value stacks.

## What this demonstrates

- **Stacks** applied to two distinct jobs (deferring operators; holding operands), the core data-structure focus.
- **Parsing and expression evaluation** without a grammar framework.
- **Operator precedence and associativity** handled by a data-driven table rather than hand-written special cases.
- **Design judgment** — choosing the lighter algorithm for the problem's actual scope, and extending it (unary minus, variadic average) without rework.

## Build & Run

Pure standard library — no dependencies, no build tool. Note the filename (`CaculatorTest.java`) and the class name (`CalculatorTest`) differ in spelling.

```bash
javac CaculatorTest.java
java CalculatorTest
```

**Input:** one expression per line on stdin; whitespace is optional and numbers are non-negative integers (negatives arise via unary minus). Each accepted line prints the generated postfix string, then the result. Bad input prints `ERROR`. Type `q` to quit.

```
$ java CalculatorTest
3 + 4 * 2
3 4 2 * +
11
-(5 - 8) ^ 2
5 8 - ~ 2 ^
9
(10, 20, 30)
10 20 30 3avg
20
q
```
