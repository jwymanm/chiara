# Clarity

If you've written or read any substantial amount of Clojure, you'll have noticed that forms take on a *very* regular visual structure. For example, from `clojure.core`:

```clj
(defn take-while
  "Returns a lazy sequence of successive items from coll while
  (pred item) returns true. pred must be free of side-effects."
  [pred coll]
  (lazy-seq
   (when-let [s (seq coll)]
       (when (pred (first s))
         (cons (first s) (take-while pred (rest s)))))))
```

Look at virtually any Clojure function and you'll see that its structure, and thus its meaning, is made clear through layout - lists start on new lines, with the list's elements continuing on the same line or continuing on new lines with an indent.

This formatting is regular enough, in fact, that the a lot of the parentheses are redundant; if we get rid of them the structure of the expression remains clear and unambiguous.

```clj
defn take-while
  "Returns a lazy sequence of successive items from coll while
  (pred item) returns true. pred must be free of side-effects."
  [pred coll]
  lazy-seq
   when-let [s (seq coll)]
       when (pred (first s))
         cons (first s) (take-while pred (rest s))
```

If you're anything like me looking at this for the first time, you might just be thinking that this actually looks pretty readable; nicer and less noisy than the original, even. So if it's easier on the eyes, and a computer can read it, why not write our code like this?

Clarity is, in a nutshell, a way to write Clojure with a reduced number of parentheses. It's actually very simple - where brackets are redundant, as in the previous example, it will allow you to omit them completely, and will then interpret the expression as if they were present. This is always optional, though - Clarity doesn't force anything on you.

Readability means reducing the amount your brain has to process to infer meaning; getting rid of a few redundant parens may not seem like a big deal, but it can make a big difference.

### Using Clarity

Clarity is easy to use. Just add `[one_more_minute/clarity "0.1.1"]` to your project, call `(clarity.core/use-clarity)` and then write code within the `clarity` macro, for example:

```clj
;; user.clj
(ns user
  (use clarity.core))

(use-clarity)
(clarity

defn foo [x]
  map inc (range x)

)
```

Files like this can then be `use/require`d from a repl or other namespaces as usual.

## Other Functionality

### Reader macros

Clarity isn't meant as a reader macro library, but they were necessary for the project, so are included. `clarity.syntax` enables a novel form of reader macros, called syntax macros, which are essentially like normal macros except that they receive their body as a string instead of structured data.

```clj
(ns user
  (use clarity.syntax))

(defsyntax r [s]
  `(println ~(.toUpperCase s)))

(use-syntax r)

(r say "hi")

;=> SAY "HI"
```

Clarity also enables more familiar character-dispatch + stream reader macros in `clarity.reader.macros`. See `clarity.utils` below for an example.

### clarity.utils

```clj
  (keys (ns-publics 'clarity.utils))
;=> (symbol-extract i-str defnrecord queued inner-namespace quote* defntype queue colon use-raw-strings λ infix)
```

`clarity.utils` contains a few useful and irrelevant things. Their docstrings are the best reference, but a couple are worth mentioning.

`literal-string` is a reader macro that provides raw triple-quoted strings, e.g.

```clj
  (use-raw-strings)

  (do """say "hi" \to me""")
;=> "say \"hi\" \\to me"

  (println *1)
;=> say "hi" \to me
```

It is included by default when you `(use-clarity)`, but note that this reader macro (or indeed any other) can be happily used in regular Clojure, as shown is above.

### Clarity's rules, more formally.

Clarity's structure is simple and intuitive - remember, the idea isn't to rewrite your code with a new syntax, but simply to remove a few outer brackets from what you've already written.

The indentation rules are the same used implicity in idiomatic clojure - formally, a list begins with an element (e.g. `defn` below) on a new line. The elements of the list are the elements on that line, followed by any elements directly below which have a higher indentation. This works recursively, until a regular bracketed form is encountered, which will be read in by the regular clojure read - so Clarity will have no effect inside parentheses.

The only exception to this is that elements on their own are treated as themselves (as opposed to lists of one item).

If it sounds complicated, it's best to see it in action; the following definitions are all equivalent, all idiomatic Clojure layouts (minus parens), and all valid Clarity.

```clj
defn foo [x]
  println x
  * x 2

defn foo [x] (println x) (* x 2)

defn foo [x]
  (println x)
  let [y (* x 2)]
    y

(defn foo [x]
  (println x)
  (* x 2))
```
