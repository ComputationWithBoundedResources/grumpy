### grumpy (Under Development)

A simple transformation from _Java Bytecode_ to _Integer Transition Systems_
using the [Soot](https://sable.github.io/soot) (LGPL) framework for 
  * termination, and
  * runtime complexity analysis of _Java Bytecode_ programs.

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

The main-application is just a wrapper for the `Soot` main function.
```bash
java -jar grumpy jar -pp -cp Loop
```

