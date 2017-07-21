package j2i;

import java.util.*;
import soot.*;


final class Transitions implements Iterable<Transition>, PrettyPrint {

  protected List<Transition> transitions = new LinkedList();

  Transitions() {
  }

  Transitions(Transition t) {
    this.transitions.add(t);
  }

  Transitions(Label from, Label to) {
    this.transitions.add(new Transition(from, to));
  }

  Transitions(Label from, Constraint guard, Label to) {
    this.transitions.add(new Transition(from, Formula.atom(guard), to));
  }

  Transitions(Label from, Formula guard, Label to) {
    this.transitions.add(new Transition(from, guard, to));
  }

  static Transitions empty() {
    return new Transitions();
  }

  //
  static Transitions compact(Transitions transitions) {
    Transitions result = new Transitions();
    if (transitions.isEmpty()) {
      return result;
    }

    Iterator<Transition> it = transitions.iterator();
    Transition cur = it.next();

    while (it.hasNext()) {
      Transition nxt = it.next();
      Optional<Transition> tmp = Optional.empty();
      G.v().out.println(cur + "*" + nxt);
      if (cur.getTo().isDefined() || nxt.getFrom().isDefined()) {
        // if(cur.getTo().isDefined() || nxt.getFrom().isDefined() || !(tmp = Transition.compose(cur,nxt)).isPresent()){
        result.add(cur);
        cur = nxt;
      } else {
        tmp = Transition.compose(cur, nxt);
        cur = tmp.get();
      }
    }
    result.add(cur);

    G.v().out.println("*** compact");
    for (Transition t : transitions) {
      G.v().out.println(t.pp());
    }
    G.v().out.println(">>>");
    for (Transition t : result) {
      G.v().out.println(t.pp());
    }

    return result;
  }

  Transitions add(Transition t) {
    this.transitions.add(t);
    return this;
  }

  Transitions add(Transitions t) {
    this.transitions.addAll(t.transitions);
    return this;
  }

  boolean isEmpty() {
    return this.transitions.isEmpty();
  }

  @Override
  public Iterator<Transition> iterator() {
    return this.transitions.iterator();
  }

  @Override
  public String toString() {
    return "Transitions{" +
        "transitions=" + transitions +
        '}';
  }

  @Override
  public String pp() {
    StringBuilder b = new StringBuilder();
    for (Transition t : this) {
      b.append(t.pp());
      b.append('\n');
    }
    return b.toString();

  }
}

final class Transition implements PrettyPrint {

  private Label from;
  private Label to;

  private Formula guard = Formula.empty();
  private AExpr lower = Val.one();
  private AExpr upper = Val.one();

  Transition(Label from, Label to) {
    this.from = from;
    this.to = to;
  }

  Transition(Label from, Formula guard, Label to) {
    this.from = from;
    this.guard = guard;
    this.to = to;
  }

  Transition(Label from, Formula guard, AExpr lower, AExpr upper, Label to) {
    this.from = from;
    this.guard = guard;
    this.to = to;
    this.lower = lower;
    this.upper = upper;
  }

  static Optional<Transition> compose(Transition t1, Transition t2) {
    if (t1.to != t2.from) {
      return Optional.empty();
    }
    return Optional.of(new Transition
        (t1.from
            , Formula.compose(t1.guard, t2.guard)
            , new Add(t1.lower, t2.lower)
            , new Add(t1.upper, t2.upper)
            , t2.to));
  }

  Label getFrom() {
    return from;
  }

  Label getTo() {
    return to;
  }

  Formula getGuard() {
    return guard;
  }

  @Override
  public String pp() {
    return "<" + this.from + "," + this.to + "," + this.guard.pp() + ">";
  }

  @Override
  public String toString() {
    return "Transition{" +
        "from=" + from +
        ", to=" + to +
        ", guard=" + guard +
        ", lower=" + lower +
        ", upper=" + upper +
        '}';
  }
}

