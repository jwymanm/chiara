# Clarity

Right now, s-expressions have too much syntax. Like semicolons, parentheses are great for parsers but often amount to line noise for us humans, who often rely instead on indentation to specify structure. When s-expressions are indented, the brackets are redundant - so why not get rid of them?

Clarity is a Clojure project which lets you write this:

    (defn foo [x]
      (map inc
        (range x)))

like this:

    defn foo [x]
      map inc
        range x

Which looks a lot cleaner, to me at least. Clarity is based very on simple rules which are already used in standard Clojure code - so using it is almost always a simple matter of removing outer parens. It is also fully compatible with regular, bracketed s-exps, so if needed you can drop back to them whenever you want.

## Using Clarity

## Clarity's rules

e.g.:

    a b
      c d
      e
        f g
      h

is equivalent to:

    (a b
      (c d)
      (e
        f g)
      h)
