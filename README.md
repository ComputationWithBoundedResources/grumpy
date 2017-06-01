### grumpy

A simple transformation from _Java Bytecode_ to _Integer Transition Systems_
using the [Soot](https://sable.github.io/soot) (LGPL) framework. The
transformation restricts to arithmetic stack operations and ignores all heap
statements.

The transformation is intended to reflect
  * termination, and
  * runtime complexity of the original program.

Though there are several intentional restrictions making the analysis in general unsound.
  * cast operations of numerical types are ignored
  * assume unbounded integer instead of machine integer
  * exceptional control-flow terminates the program immediately
  * static fields are considered as read-only parameter


#### Example

Following Java program
```java
import java.uitl.*;

class Loop{
  public static void simpleLoop(List<Integer> list, int m, int n){
    list.add(m);
    list.add(n);
    int bound = m + n;
    int j = 0;
    for(int i = 0; i < bound; i++){
      j += i;
      list.add(i);
    }
  }
```
is translated into this transition system
```
(GOAL COMPLEXITY)
(STARTTERM (FUNCTIONSYMBOLS label_0))
(VAR )
(RULES
  label_1(i0, i1, i2, i4) -> label_1(i0, i1, i0 + i1, i4)
  label_0(i0, i1, i2, i4) -> label1(i0, i1, i2, 0)
  label0(i0, i1, i2, i4)  -> label2(i0, i1, i2, i4)       :|: i4 >= i2
  label0(i0, i1, i2, i4)  -> label_2(i0, i1, i2, i4)      :|: i4 < i2
  label_1(i0, i1, i2, i4) -> label_3(i0, i1, i2, i4 + 1)
  label_2(i0, i1, i2, i4) -> label1(i0, i1, i2, i4)
  label1(i0, i1, i2, i4)  -> halt(i0, i1, i2, i4)
)
```
The runtime behaviour of `simpleLoop` can then be analysed with existing tools,
for example [KoAT](https://github.com/s-falke/kittel-koat).


#### Installation

Prerequisites: `JDK 8` and the `Soot` library (shipped in `lib/`). The tool can
be automatically build with `Apache Ant` and the enclosed build file
`build.xml`.
```
ant dist
```
builds `grumpy.jar`


#### Usage

The main-application is just a wrapper for the `Soot` main function.
```bash
java -jar grumpy jar -pp -cp Loop
```

