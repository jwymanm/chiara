# Chiara

```clj
[chiara-0.2.0]
```

Chiara is a package for Clojure which includes a powerful library for creating DSLs (complete with reader macros and other goodies), as well as an indentation-based syntax for S-Expressions.

## The Syntax

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

Unlike machines, us humans don't find a purely parenthesis-based notatation for S-Expressions at all readable. Instead, we rely on other visual hints (i.e. indentation and newlines) to determine the structure of the code we're looking at.

The basic premise of Chiara is that the way we use these visual hints is actually very regular and simple, which means that we can easily and reliably parse code using indentation instead of brackets - so we only have to write `(` and `)` when they are actually necessary (just like semicolons in Python, for example). This brings several benefits; code is quicker to write, more readable, less scary to non-lispers, and easier to make structural edits to without relying on external tools like ParEdit.

For example, take this function, straight from `clojure.core`:

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

The beauty of Chiara is that it is strictly a superset of vanilla Clojure - it doesn't force anything on you, and you don't have to change anything about the way you write your code. But, if and when you choose to, you can remove some of the redundant outer parens, and your code will continue to be perfectly valid - like so:

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

To read more about Chiara's rules and features please see [Chiara.md](doc/Chiara.md).

## The DSL library

### Syntax Macros

Chiara introduces the concept of "syntax macros". These are much like regular macros in that they receive and return code, except that they receive code as a string. For example:

```clj
(ns user
  (use chiara.syntax))

(defsyntax r [s]
  `(println ~(.toUpperCase s)))

(use-syntax r)

(defn foo []
  (r The program says (loudly) "Hello, World!"))

;; Then, at a repl:

(foo)
;=> THE PROGRAM SAYS (LOUDLY) "HELLO, WORLD!"
```

Note that syntax macros are namespace-safe - each reader must be explicitly enabled in each namespace it is used via the `use-syntax` function.

Although this is a trivial example, the ability to embed arbitrary syntax in Clojure is powerful - I see it being particularly useful for areas like defining pattern matching rules, which are cumbersome without a dedicated syntax.

`defsyntax` also supports the `^:stream` tag, e.g.

```clj
(defsyntax ^:stream r [rdr] ...)
```

When `:stream` is applied the syntax macro will be passed the Clojure reader's `java.io.PushbackReader` directly.

### Reader macros

The namespaces under `chiara.reader` contain all the hackery necessary to make this stuff work. You can define your own reader macros using `chiara.reader.macros/use-reader-macros`, which will enable its arguments in the current namespace. Each argument is a map containing `:char`, the character that will trigger the macro, and `:reader`, a function of two args (the clojure reader and the dispatch character) which returns a value. Examples can be found in the `chiara` namespace - `colon` and `raw-string`. However, I recommend avoiding going this low-level unless you really know what you're doing (or really want to).
