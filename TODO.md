
### Tasks

  - [ ] provide valid KoAT output
    - [ ] transform DNF formula into multiple transitions
    - [ ] compute domain
      - [ ] use predicate for locals to filter assignment and invoke statement
      - [ ] reduce Jimple stack variables \*
  - [ ] sensible built-in default summaries for standard libraries
    - [ ] special default summary for <specialinvoke init>


\*Note: A simple (but possible incomplete) heuristic is to assume that stack
variables are only alive in sequence blocks. Thus we can ignore them if we
compose sequences wrt. to jump instructions. Alternatively, Soot provides a
live-ness analysis.

### Features & Optimisations

  - [ ] cheap and sound type based reachability and dag analysis
  - [ ] provide logging information for things that are not handled
  - [ ] configuration
    - [ ] path-length and nodes-abstraction
    - [ ] only numerical abstraction
    - [ ] binop for long
    - [ ] binop (shift,...)
  - [ ] Strings and env variable

