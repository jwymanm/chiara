## Usage

```clj
(ns my-ns
  (use chiara))

(use-chiara) (chiara

defn foo [x]
  map inc (range x)

def my-list
  foo 10

)
```

Unfortunately, using Chiara does currently come with a little boilerplate. You must first call `(chiara/use-chiara)` to enable it, then wrap your code in the `(chiara ...)` syntax macro.

On the plus side, this design does make it very convenient to use - you can embed Chiara in any Clojure file, and then `use/require` that file just as normal.

## Reader Macros

Chiara comes with the `literal-string` reader macro enabled by default. If you wrap a string in triple quotes, e.g.

```clj
"""I said, "hi there"."""
"""C:\Users\Mike"""
```

The string will be interpreted literally, i.e. with no escaping performed, until the next block of three double-quotes.

## Chiara's rules, more formally.

Chiara's structure is simple and intuitive - remember, the idea isn't to rewrite your code with a new syntax, but simply to remove a few outer brackets from what you've already written.

The indentation rules are the same used implicity in idiomatic clojure - formally, a list begins with an element (e.g. `defn` below) on a new line. The elements of the list are the elements on that line, followed by any elements directly below which have a higher indentation. This works recursively, until a regular bracketed form is encountered, which will be read in by the regular clojure read - so Chiara will have no effect inside parentheses.

The only exception to this is that elements on their own are treated as themselves (as opposed to lists of one item).

If it sounds complicated, it's best to see it in action; the following definitions are all equivalent, all idiomatic Clojure layouts (minus parens), and all valid Chiara.

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
