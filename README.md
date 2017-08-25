### grumpy

provides a simple transformation from _Java Bytecode_ to _Integer Transition Systems_
using the [Soot](https://sable.github.io/soot) (LGPL) framework for the analysis of
  * termination, and
  * runtime complexity of _Java Bytecode_ programs.

The transformation features
  * support for basic arithmetic operations, and
  * a size based abstraction for arrays and objects.

The library is _not_ intended to handle full-fledged Java Bytecode programs.
But some (yet undefined) subset that supports _while_ programs and textbook algorithms with objects and arrays.
  * integer-like types are treated as unbounded integer
  * (narrowing) type conversions are ignored
  * method calls are treated via dedicated method summaries
  * exceptional control-flow is ignored
  * simplified unsound object abstraction

#### Installation

Prerequisites:
  * `JDK 8`
  * [Soot](https://sable.github.io/soot) (LGPL)
  * [ANTLR](https://www.antlr.org) (BSD)

The tool can
be automatically build with `Apache Ant` and the enclosed build file
`build.xml`.
```
ant dist
```
builds `grumpy.jar`


#### Usage

This repository provides a library and an example application.

##### Library

The `Grumpy` library provides the transformation from `Soot's` internal
representation `Jimple` to (weighted) transitions. A transition is a triple
`<from,guard,to>`, where `from` and `to` are labels and `guard` is
a conjunction of constraints over arithmetic expressions.


##### Application

The main-application is just a wrapper for the `Soot's` function.

```bash
$ cat Arrays.java
public class Arrays {

 static int[] reverse(int[] arr){
  int l     = arr.length;
  int[] rev = new int[l];

  for (int i=l; i > 0; i--) rev[l-i] = arr[i-1];
  return rev;
  }

}
$ javac Arrays.java
$ java -jar grumpy.jar -pp -cp . Arrays
Soot started on Fri Aug 25 09:11:57 CEST 2017
Transforming Arrays...

(GOAL COMPLEXITY)
(STARTTERM (FUNCTIONSYMBOLS marke0))
(VAR )
(RULES
marke0(r0, i0, r1, $i1, $i2, $i3, i4) -> marke1(r0, i0', r1, $i1, $i2, $i3, i4) :|: i0' = r0 && i0' >= 0
marke1(r0, i0, r1, $i1, $i2, $i3, i4) -> marke2(r0, i0, r1', $i1, $i2, $i3, i4) :|: r1' = i0
marke2(r0, i0, r1, $i1, $i2, $i3, i4) -> label1(r0, i0, r1, $i1, $i2, $i3, i4') :|: i4' = i0
label1(r0, i0, r1, $i1, $i2, $i3, i4) -> label2(r0, i0, r1, $i1, $i2, $i3, i4) :|: i4 <= 0
label1(r0, i0, r1, $i1, $i2, $i3, i4) -> marke5(r0, i0, r1, $i1, $i2, $i3, i4) :|: i4 > 0
marke5(r0, i0, r1, $i1, $i2, $i3, i4) -> marke6(r0, i0, r1, $i1, $i2, $i3', i4) :|: $i3' = (i0 - i4)
marke6(r0, i0, r1, $i1, $i2, $i3, i4) -> marke7(r0, i0, r1, $i1', $i2, $i3, i4) :|: $i1' = (i4 - 1)
marke7(r0, i0, r1, $i1, $i2, $i3, i4) -> marke8(r0, i0, r1, $i1, $i2', $i3, i4) :|: $i2' = fresh_0
marke8(r0, i0, r1, $i1, $i2, $i3, i4) -> marke9(r0, i0, r1, $i1, $i2, $i3, i4)
marke9(r0, i0, r1, $i1, $i2, $i3, i4) -> marke10(r0, i0, r1, $i1, $i2, $i3, i4') :|: i4' = (i4 + (-1))
marke10(r0, i0, r1, $i1, $i2, $i3, i4) -> label1(r0, i0, r1, $i1, $i2, $i3, i4)
)

Soot finished on Fri Aug 25 09:11:58 CEST 2017
Soot has run for 0 min. 0 sec.
```

