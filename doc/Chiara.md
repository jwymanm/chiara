```clj
  (use-raw-strings)

  (do """say "hi" \to me""")
;=> "say \"hi\" \\to me"

  (println *1)
;=> say "hi" \to me
```

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
