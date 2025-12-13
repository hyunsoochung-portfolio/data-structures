# BigInteger Calculator

![Java](https://img.shields.io/badge/Java-007396?logo=openjdk&logoColor=white)

A from-scratch arbitrary-precision integer calculator. It reads `a OP b` expressions from standard input and evaluates `+`, `-`, and `*` on numbers far larger than a machine word — without ever calling `java.math.BigInteger`. I wrote this to prove to myself that I could rebuild big-number arithmetic from first principles: how a number is laid out in memory, how positional arithmetic actually works column by column, and where the algorithmic cost goes.

## The problem I set out to solve

A native `long` is 64 bits. That ceiling is around 9.2 × 10¹⁸ — roughly nineteen decimal digits. The moment a value crosses that boundary, the hardware has nowhere to put the high bits: the result silently wraps around modulo 2⁶⁴ and you get a number that is not just imprecise but *wrong*, with no exception to warn you. Fixed-width integers are a property of the CPU, not of mathematics. So if I want to add two hundred-digit numbers, I can't lean on a primitive type at all — I have to invent my own representation of "a number" and define every arithmetic operation over it myself. That is the entire reason a custom big-integer type has to exist.

## Design decisions and the reasoning behind them

### Represent the number as an array of digits

The first decision was the data structure. I chose to store a number as an array where each slot holds **one decimal digit (base 10)**. The alternative — packing many digits per slot in a higher base like 10⁹ or a power of two — is what a production library does because it uses the full width of each machine word and does far fewer operations. I deliberately went the other way for this project: base 10 makes every intermediate state directly readable and maps one-to-one onto the pencil-and-paper arithmetic I was modeling, which kept the carry/borrow logic honest and easy to reason about. The trade-off I accepted is that I'm using a whole `int` to hold a single 0–9 digit and doing far more iterations than a word-packed design would. For a learning exercise focused on the *algorithms*, that was the right call; for production it would not be.

### Little-endian digit order

I store digits **least-significant-first**: index 0 is the ones place, index 1 the tens, and so on. This is the choice that pays off everywhere downstream. Arithmetic naturally flows from the low-order end — when you add, carries propagate *upward* into higher place values — so storing low digits at low indices means a plain `for (i = 0; i++)` loop walks the columns in exactly the order the math demands. The digit of weight 10^i lives at index `i`, which also makes multiplication's place-value bookkeeping fall out for free (more on that below). The only price is that printing has to reverse the order back to big-endian for human eyes, which happens once, at the very end.

### Keep sign separate from magnitude

I made the sign its own boolean and let the digit array hold only a non-negative magnitude. The obvious alternative is two's-complement, the way hardware represents signed integers. I weighed it and rejected it: two's-complement is elegant for *fixed* widths because the carry-out wraps cleanly, but for a variable-length digit array it forces you to either sign-extend across the whole array or constantly track where the "infinite leading sign bits" begin. Sign-and-magnitude sidesteps all of that. The three operations decompose cleanly — I run the arithmetic purely on magnitudes and decide the result's sign with a separate, small piece of logic. The cost is that the sign rules become an explicit case analysis rather than something the representation handles automatically, but that analysis is short and, more importantly, it's where the interesting reasoning lives.

### Decide add-vs-subtract by comparing magnitudes

Because sign lives apart from magnitude, the operator on the page (`+` or `-`) is not the operation I actually perform. `a + b` with mixed signs is really a subtraction; `a − (−b)` is really an addition. So I first collapse the operator and the two signs into a single question — *do I add the magnitudes or subtract them?* — and only then dispatch.

Subtraction raised the key subtlety: schoolbook subtraction only works cleanly when you take the smaller magnitude away from the larger one. So before subtracting I compare absolute values, always subtract the smaller magnitude from the larger, and then attach the sign of whichever operand "won." This guarantees the subtraction routine never has to produce a negative magnitude, and it cleanly handles the cancellation case (equal magnitudes → exactly zero, never a stray `−0`). Folding all the sign permutations down to "add or subtract, then who's bigger" is the part of the design I'm happiest with, because it turns a messy combinatorial case table into two small, independent decisions.

## The algorithms

All three operate on magnitudes only; the sign is layered on afterward.

**Addition and subtraction** are the schoolbook column algorithms. Addition walks the digits low to high carrying a running `carry`: at each column it sums the two aligned digits plus the incoming carry, keeps the result modulo 10, and passes the tens part up to the next column. Subtraction is the mirror image with a `borrow`: when a column goes negative it borrows 10 from the next column up and sets the borrow flag. Little-endian storage is what makes both loops a single clean upward pass — the carry/borrow always flows in the direction of increasing index.

**Multiplication** is long multiplication. For each digit of the first operand I multiply it across every digit of the second, and the product of the digit at index `i` and the digit at index `j` contributes to place value `i + j` — which, because of the little-endian layout, is *literally the array index* I write to. That single fact is why the shifting in grade-school multiplication needs no special handling here: place value and array index are the same number. Each partial product is accumulated into the running total with the same carry logic as addition. I also skip multiplier digits that are zero, since their entire partial product is zero.

## Complexity, and where I drew the scope line

Let `n` and `m` be the digit counts of the two operands.

| Operation | Cost | Why |
|---|---|---|
| Addition / Subtraction | O(n) | one pass, constant work per column |
| Multiplication | O(n·m) | every digit of one operand times every digit of the other |
| Magnitude compare | O(n) | scan from the top until the first differing digit |

Add and subtract are linear because each output digit depends only on its column plus one carry — constant work, done once per digit. Multiplication is the product of the two lengths because long multiplication forms every pairwise digit product; that quadratic behavior is fundamental to the schoolbook method, not an implementation accident.

I made one deliberate scope decision: the magnitude is a **fixed-width array** with a hard digit cap, rather than a dynamically resized buffer. That kept memory management out of the picture so I could concentrate on the arithmetic, but it's an honest limitation — a result that overflows the cap would drop its top carry, and oversized input won't fit. A production big-integer type would differ in two concrete ways I'm aware of: it would **size the backing store dynamically** (growing to fit the true result and trimming leading zeros), and it would replace schoolbook multiplication with a subquadratic algorithm — **Karatsuba** at roughly O(n^1.585) for medium sizes, and FFT-based multiplication for very large operands. Knowing where the O(n·m) wall is, and what the field does to get past it, is exactly the point.

## What this demonstrates

- **Number representation** — choosing a base, an endianness, and a sign scheme, and understanding why each choice ripples through everything else.
- **Positional arithmetic** — carry and borrow propagation, place-value alignment, and why digit-index equals place-value is such a useful invariant.
- **Algorithmic thinking** — reducing a tangle of sign cases to two clean decisions, and reasoning explicitly about asymptotic cost and the trade-offs that bound it.

## Build and run

Plain Java, standard library only — no dependencies.

```bash
javac BigInteger.java
java BigInteger
```

Then enter one expression per line (whitespace and stacked signs like `--5` are tolerated):

```
123456789012345678901234567890 + 987654321098765432109876543210
-5 * 4
--100 - 250
quit
```

Each result prints on its own line. Type `quit` to exit; anything that isn't a valid expression prints `Wrong Input` and the loop keeps reading.
