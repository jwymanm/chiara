# Clarity

Right now, s-expressions have too much syntax. Like semicolons, parentheses are great for parsers but often amount to line noise for us humans, who often rely instead on indentation to understand structure. When s-expressions are indented, the brackets are redundant - so why not get rid of them?

Clarity is a Clojure project which lets you write this:

```clj
(defn foo [x]
  (map inc
    (range x)))
```

like this:

```clj
defn foo [x]
  map inc
    range x
```

Which looks a lot cleaner, to me at least. Clarity is based very on simple rules which are already used in standard Clojure code - so using it is almost always a simple matter of removing a few outer parens. It is also fully compatible with regular, bracketed s-exps, so if needed you can drop back to them whenever you want. (See below for more on Clarity's rules)

### Using Clarity

Clarity is easy to use. Just add `[clarity "0.1.0"]` to your project, call `(clarity.core/use-clarity)` and then write code within the `clarity` macro, for example:

```clj
;; user.clj
(ns user
  (use clarity.core))

(use-clarity)
(clarity

defn foo [x]
  map inc
    range x

)
```

Files like this can then be `use/require`d from a repl or other namespaces as usual.

### Project status

Clarity is pretty complete and has worked well for me so far. However, given that it relies on (read: relentlessly mangles) Clojure implementation details, it should really be considered experimental and not used in mission-critical code.

There may be (small) api changes before Clarity reaches 1.0.

## Other Functionality

### Reader macros

Clarity isn't meant as a reader macro library, but they were necessary for the project, so are included. Clarity enables with a novel form of reader macros, called syntax macros (in `clarity.syntax`), which are essentially like normal macros except that they recieve their body as a string instead of structured data.

```clj
  (ns user
    (use clarity.syntax))

  (defsyntax r [s]
    `(println ~(.toUpperCase s)))

  (use-syntax r)

  (r hello @you)

;=> HELLO @YOU
```

Clarity also enables more familiar character-dispatch + stream reader macros in `clarity.reader.macros`. See `clarity.utils` below for an example.

### clarity.utils

```clj
  (keys (ns-publics 'clarity.utils))
;=> (symbol-extract i-str defnrecord queued inner-namespace quote* defntype queue colon literal-string λ infix)
```

`clarity.utils` contains a few useful and irrelevant things. Their docstrings are the best reference, but a couple are worth mentioning.

TODO

### Clarity's rules

Clarity's structure is simple and intuitive - it's already used in idiomatic clojure. Formally, a list begins with an element (e.g. `defn` below) on a new line. The elements of the list are the elements on that line, followed by any elements directly below which have a higher indentation.

The only exception to this is that elements on their own are treated as themselves (as opposed to lists of one item).

If it sounds complicated, it's best to see it in action; the following definitions are all equivalent:

```clj
defn foo [x]
  println x
  * x 2

defn foo x (println x) (* x 2)

defn foo [x]
  (println x)
  let [y (* x 2)]
    x

(defn foo x
  (println x)
  (* x 2))
```
