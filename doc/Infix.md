## Infix expressions in Lisp

Lisp is, in general, a poor language for expressing maths; For example, a polynomial which could be written in Julia as

  3x^2 + 2x + 1

must be written in Lisp as

  (+ (* 3 (^ x 2)) (* 2 x) 1)

This is pretty uncontroversial.

However, while many believe that any modification to Lisp's syntax - mathematically-oriented or not - while destroy all of Lisp's power, simplicity and flexibility, I disagree. I think that infix notation can be implemented in Clojure:

 * As a macro, so that is convenient to use.
 * Apart from an edge case, entirely backwards-compatible with (i.e. a superset of) vanilla Clojure.
 * Fully supporting prefix notation infix operators where it is appropriate.

The `infix` macro walks over the code it is supplied, looking for lists where every second element is an infix operator. An infix operator is a symbol which either contains no alphanumeric characters or begins with an apostrophe (i.e. is quoted) - e.g. `+`, `-`, `'mod`. For example, this would be a valid infix form:

```clj
(3 + 2 * x + y 'mod 2)
```

The macro then simply turns this into prefix notation, taking into account operator precedence and treating repeat operators (like the `+` here) as additional arguments to the same function call:

```clj
(+ 3 (* 2 x) (mod y 2))
```

This does, however, present a problem when using operators as first-class functions. Consider, for example, two expressions which are syntactically equivalent but have very different meanings:

```clj
(a + b)
(reduce + xs)
```

Under the rules I've specified above, the latter would be transformed into `(+ reduce xs)`, which is of course incorrect. My proposed solution to this is that lists containing only an infix operator will be replaced with the operator itself, without infixing it - so that the above reduction would be written as

```clj
(reduce (+) xs)
```

Unfortunately, this does mean that the infix macro would be incompatible with vanilla Lisp in a very small number of cases, as well as this change arguably bringing a small increase in complexity. However, I think that's worthwhile for the gain in convenience for mathematical notation. Plus, you'd only have to use the macro in the particular namespace or area of code which actually contained formulas.

## Convenient constant multiplication syntax

Julia's syntax for constant multiplication is also pretty useful.

```clj
3x => (* x 3)
3(x + 2) => (* 3 (+ x 2))
```

Both of these could potentially be implemented via a reader macro. The first is invalid in vanilla Clojure, but the second case would be treated as the two seperate forms `3` and `(+ x 2)` - so whether or not changing this behaviour is a good idea is still up for debate.

## Examples

With the above changes implemented, the polynomial above could be written (using Chiara) as:

```clj
infix

  defn ** [x y]
    Math/pow x y

  defn f [x]
    3 * x ** 2 + 2x + 1
```

Or, using the arguably nicer, though less familiar, mix of prefix and infix:

```clj
defn f [x]
  + 3(x ** 2) 2x 1
```

## Implementation

For various reasons (such as lack of operator overloading), Clojure is not the best language for mathematical/scientific programming, nor is it generally used as such. Therefore, this is more of an idea - unfortunately, the actual implementation is a low priority for me, and I have no plans to do it in the immediate future. So the project is basically up for grabs for anyone who might be interested in doing it.

Nevertheless, the infix macro does exist in a primitive state; what it does so far is it checks the second element of each list for an infix operator, and if it finds one prefixes it. So basic forms like `(x + y)`, or even `(x + y z)`, will be recognised, but that's it - it's not really useful at this stage.
