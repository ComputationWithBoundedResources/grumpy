package j2i;

import j2i.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

// A Clause is a conjunction of constraints.
class Clause implements PrettyPrint {

  private List<Constraint> constraints;

  Clause(Constraint... cs) {
    this.constraints = new LinkedList<>();
    for (Constraint c : cs) {
      constraints.add(c);
    }
  }

  // We assume that only post-variables are considered to be modified.
  // wlog we consider a single variable, v ... input  v' ... output
  // case 1) v' not in [[exp_1]]: input v is the same for exp_1 and exp_2
  // case 2) v' in [[exp_1]]:     input v is modifed
  //   case 2.a) v' not in [[exp_2]]: v is v' after exp_1; we set [[exp_2]][v |-> v']
  //   case 2.b) v' in     [[exp_2]]: then we introduce a fresh var v^, ie [[exp_1]][v' |-> v^] and [[exp_2]][v |-> v^]
  static Clause compose(Clause lhs, Clause rhs) {
    Predicate<Var> isPost = v -> v.isPostVar();
    Set<Var> lvars = lhs.variables(isPost);
    Set<Var> rvars = rhs.variables(isPost);
    Map<Var, AExpr> lmap = new HashMap<>();
    Map<Var, AExpr> rmap = new HashMap<>();
    for (Var v : lvars) {
      if (!rvars.contains(v)) { // case v'  in [[exp_1]] and v' not in [exp_2]]
        rmap.put(Var.newPreVar(v), v);
      } else {                // case v'  in [[exp_1]] and v' in [exp_2]]
        Var imm = Fresh.freshImm();
        lmap.put(v, imm);
        rmap.put(Var.newPreVar(v), imm);
      }
    }

    for (Constraint c : lhs.constraints) {
      c.substitute(lmap);
    }
    for (Constraint c : rhs.constraints) {
      c.substitute(rmap);
    }

    Clause clause = new Clause();
    clause.constraints.addAll(rhs.constraints);
    clause.constraints.addAll(lhs.constraints);

    return clause;
  }

  void addAll(Constraint... cs) {
    for (Constraint c : cs) {
      constraints.add(c);
    }
  }

  boolean hasVar(Var var) {
    for (Constraint c : constraints) {
      if (c.hasVar(var)) {
        return true;
      }
    }
    return false;
  }

  Set<Var> variables(Predicate<Var> p) {
    Set<Var> vars = new HashSet<>();
    this.addVariables(vars, p);
    return vars;
  }

  void addVariables(Set<Var> vars, Predicate<Var> p) {
    for (Constraint c : constraints) {
      c.addVariables(vars, p);
    }
  }

  void substitute(Map<Var, AExpr> smap) {
    for (Constraint c : constraints) {
      c.substitute(smap);
    }
  }

  @Override
  public String pp() {
    StringBuilder b = new StringBuilder();
    Iterator<Constraint> cs = constraints.iterator();
    while (cs.hasNext()) {
      Constraint constraint = cs.next();
      b.append(constraint.pp());
      if (cs.hasNext()) {
        b.append(" && ");
      }
    }
    return b.toString();
  }

  @Override
  public String toString() {
    return this.pp();
  }

}

// A Formula is a disjunction of clauses.
class Formula implements PrettyPrint {

  List<Clause> dnf;

  Formula() {
    this.dnf = new LinkedList<>();
  }

  Formula(Constraint... cs) {
    this.dnf = new LinkedList<>();
    this.dnf.add(new Clause(cs));
  }

  static Formula atom(Constraint... cs) {
    return new Formula(cs);
  }

  static Formula empty() {
    return new Formula();
  }

  static Formula compose(Formula lhs, Formula rhs) {
    Formula f = new Formula();

    if (lhs.dnf.isEmpty()) {
      f.dnf.addAll(rhs.dnf);
      return f;
    }
    if (rhs.dnf.isEmpty()) {
      f.dnf.addAll(lhs.dnf);
      return f;
    }
    for (Clause lclause : lhs.dnf) {
      for (Clause rclause : rhs.dnf) {
        Clause c3 = Clause.compose(lclause, rclause);
        f.dnf.add(c3);
      }
    }
    return f;
  }

  Formula and(Constraint... cs) {
    for (Clause clause : this.dnf) {
      clause.addAll(cs);
    }
    return this;
  }

  Formula or(Constraint... cs) {
    this.dnf.add(new Clause(cs));
    return this;
  }

  boolean hasVar(Var var) {
    for (Clause clause : this.dnf) {
      if (clause.hasVar(var)) {
        return true;
      }
    }
    return false;
  }

  Set<Var> variables() {
    Set<Var> vars = new HashSet<>();
    this.addVariables(vars, v -> true);
    return vars;
  }

  void addVariables(Set<Var> vars, Predicate<Var> p) {
    for (Clause clause : this.dnf) {
      clause.addVariables(vars, p);
    }
  }

  Formula substitute(Var var, AExpr aexpr) {
    Map<Var, AExpr> smap = new HashMap<>();
    smap.put(var, aexpr);
    return this.substitute(smap);
  }

  Formula substitute(Map<Var, AExpr> smap) {
    for (Clause clause : this.dnf) {
      clause.substitute(smap);
    }
    return this;
  }

  public String pp() {
    StringBuilder b = new StringBuilder();
    Iterator<Clause> clauses = this.dnf.iterator();
    while (clauses.hasNext()) {
      Clause clause = clauses.next();
      b.append(clause.pp());
      if (clauses.hasNext()) {
        b.append(" || ");
      }
    }
    return b.toString();
  }

}


