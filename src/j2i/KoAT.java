package j2i;

import java.io.*;
import java.util.*;

public class KoAT {

  private Domain domain;
  private Transitions transitions;

  public KoAT(Domain domain, Transitions transitions) {
    this.domain = domain;
    this.transitions = transitions;
  }

  public StringBuilder domain2String() {
    StringBuilder b = new StringBuilder();
    b.append("(");
    Iterator<Var> it = this.domain.iterator();
    while (it.hasNext()) {
      Var v = it.next();
      b.append(v);
      if (it.hasNext()) {
        b.append(", ");
      }
    }
    b.append(")");
    return b;
  }

  public StringBuilder postdomain2String(Formula guard) {
    StringBuilder b = new StringBuilder();
    b.append("(");
    Iterator<Var> it = this.domain.iterator();
    while (it.hasNext()) {
      Var v = it.next();
      Var w = Var.newPostVar(v);
      if (guard.hasVar(w)) {
        b.append(w);
      } else {
        b.append(v);
      }
      if (it.hasNext()) {
        b.append(", ");
      }
    }
    b.append(")");
    return b;
  }

  @Override
  public String toString() {
    StringBuilder b = new StringBuilder();
    b.append("(GOAL COMPLEXITY)\n");
    if (this.transitions.isEmpty()) {
      b.append("(STARTTERM (FUNCTIONSYMBOLS start))\n");
      b.append("start(x) -> end(x)\n");
      return b.toString();
    }
    String s = this.transitions.iterator().next().getFrom().toString();
    b.append("(STARTTERM (FUNCTIONSYMBOLS " + s + "))\n");
    b.append("(VAR )\n");
    b.append("(RULES\n");
    if (this.transitions.isEmpty()) {
      b.append("start(x) -> end(x)\n");
    }

    StringBuilder lhs = this.domain2String();

    for (Transition t : transitions) {
      b.append(t.getFrom().toString());
      b.append(lhs);
      b.append(" -> ");
      b.append(t.getTo().toString());
      b.append(postdomain2String(t.getGuard()));
      b.append(" :|: ");
      b.append(t.getGuard().pp());
      b.append('\n');
    }
    b.append(")\n");
    return b.toString();
  }


}


