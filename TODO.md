
### Tasks

  - [ ] provide valid KoAT output
    - [ ] transform DNF formula into multiple transitions
    - [ ] compute domain from transition system

Note: we can just take the locals provided by soot and collect the accessed
static fields. But Soot introduces a lot of stack variables, even for simple
programs. A simple (but possible incomplete) heuristic is to assume that stack
variables are only alive in sequence blocks. Thus if we just can ignore them if
we compose all sequences wrt to jump instructions. Alternatively, we could try
Soot's also provides live-ness analysis.

  - [ ] sensible default summaries for standard libraries


### Features & Optimisations

  - [ ] provide different configurations
    - [ ] path-length and nodes-abstraction
    - [ ] only numerical abstraction
    - [ ] exact arithmetic

