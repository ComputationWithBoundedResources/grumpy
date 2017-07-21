package j2i;

import java.io.*;
import java.util.*;

final public class KoAT implements PrettyPrint {

  private Domain domain;
  private Transitions transitions;

  public KoAT(Domain domain, Transitions transitions) {
    this.domain = domain;
    this.transitions = transitions;
  }

  public KoAT compact() {
    this.transitions = Transitions.compact(this.transitions);
    return this;
  }

  public StringBuilder domain2String() {
    StringBuilder b = new StringBuilder();
    b.append("(");
    Iterator<Var> it = this.domain.iterator();
    while (it.hasNext()) {
      Var v = it.next();
      b.append(v.pp());
      if (it.hasNext()) {
        b.append(", ");
      }
    }
    b.append(")");
    return b;
  }

  public StringBuilder postdomain2String(Clause guard) {
    StringBuilder b = new StringBuilder();
    b.append("(");
    Iterator<Var> it = this.domain.iterator();
    while (it.hasNext()) {
      Var v = it.next();
      Var w = Var.newPostVar(v);
      if (guard.hasVar(w)) {
        b.append(w.pp());
      } else {
        b.append(v.pp());
      }
      if (it.hasNext()) {
        b.append(", ");
      }
    }
    b.append(")");
    return b;
  }

  @Override
  public String pp() {
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
      StringBuilder r = new StringBuilder();
      r.append(t.getFrom().toString());
      r.append(lhs);
      r.append(" -> ");
      r.append(t.getTo().toString());
      if (t.getGuard().isEmpty()) {
        b.append(r);
        b.append(domain2String());
        b.append('\n');
      } else {
        for (Clause c : t.getGuard()) {
          b.append(r);
          b.append(postdomain2String(c));
          b.append(" :|: ");
          b.append(c.pp());
          b.append('\n');
        }
      }
    }
    b.append(")\n");
    return b.toString();
  }

  @Override
  public String toString() {
    return "KoAT{" + "domain=" + domain + ", transitions=" + transitions + '}';
  }
}


