# Clarity

Right now, s-expressions have too much syntax. Like semicolons, parentheses are great for parsers but often amount to line noise for us humans, who often rely instead on indentation to specify structure. When s-expressions are indented, the brackets are redundant - so why are they there?

Clarity is a Clojure project which lets you write this:

    (defn foo [x]
      (map inc (range x)))

like this:

    defn foo [x]
      map inc
        range x

  Which looks a lot cleaner, to me at least. Clarity is also fully compatible with regular, bracketed s-exps, so you can drop back to them whenever you want.