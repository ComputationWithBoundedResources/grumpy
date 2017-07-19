package j2i;

import j2i.*;
import java.util.*;
import java.util.function.Predicate;


abstract class Constraint implements PrettyPrint {

  AExpr lhs;
  AExpr rhs;

  Constraint(AExpr lhs, AExpr rhs) {
    this.lhs = lhs;
    this.rhs = rhs;
  }

  static Constraint ge(AExpr lhs, AExpr rhs) {
    return new GeConstraint(lhs, rhs);
  }

  static Constraint gt(AExpr lhs, AExpr rhs) {
    return new GtConstraint(lhs, rhs);
  }

  static Constraint le(AExpr lhs, AExpr rhs) {
    return new LeConstraint(lhs, rhs);
  }

  static Constraint lt(AExpr lhs, AExpr rhs) {
    return new LtConstraint(lhs, rhs);
  }

  static Constraint eq(AExpr lhs, AExpr rhs) {
    return new EqConstraint(lhs, rhs);
  }

  static Constraint as(AExpr lhs, AExpr rhs) {
    return new AsConstraint(lhs, rhs);
  }

  static Constraint positive(AExpr e) {
    return new GtConstraint(e, Val.zero);
  }

  static Constraint nonnegative(AExpr e) {
    return new GeConstraint(e, Val.zero);
  }

  static Constraint negative(AExpr e) {
    return new LtConstraint(e, Val.zero);
  }

  boolean hasVar(Var var) {
    return this.lhs.hasVar(var) || this.rhs.hasVar(var);
  }

  void addVariables(Set<Var> vars, Predicate<Var> p) {
    lhs.addVariables(vars, p);
    rhs.addVariables(vars, p);
  }

  void substitute(Map<Var, AExpr> smap) {
    this.lhs = this.lhs.substitute(smap);
    this.rhs = this.rhs.substitute(smap);
  }

  protected String ppWithOp(String op) {
    return lhs.pp() + " " + op + " " + rhs.pp();
  }
}

final class GeConstraint extends Constraint {

  GeConstraint(AExpr lhs, AExpr rhs) {
    super(lhs, rhs);
  }

  @Override
  public String pp() {
    return ppWithOp(">=");
  }

  @Override
  public String toString() {
    return "GeConstraint{" + "lhs=" + lhs + ", rhs=" + rhs + '}';
  }
}

final class GtConstraint extends Constraint {

  GtConstraint(AExpr lhs, AExpr rhs) {
    super(lhs, rhs);
  }

  @Override
  public String pp() {
    return ppWithOp(">");
  }

  @Override
  public String toString() {
    return "GtConstraint{" + "lhs=" + lhs + ", rhs=" + rhs + '}';
  }
}

final class LeConstraint extends Constraint {

  LeConstraint(AExpr lhs, AExpr rhs) {
    super(lhs, rhs);
  }

  @Override
  public String pp() {
    return ppWithOp("<=");
  }

  @Override
  public String toString() {
    return "LeConstraint{" + "lhs=" + lhs + ", rhs=" + rhs + '}';
  }
}

final class LtConstraint extends Constraint {

  LtConstraint(AExpr lhs, AExpr rhs) {
    super(lhs, rhs);
  }

  public String pp() {
    return ppWithOp("<");
  }

  @Override
  public String toString() {
    return "LtConstraint{" + "lhs=" + lhs + ", rhs=" + rhs + '}';
  }
}

final class EqConstraint extends Constraint {

  EqConstraint(AExpr lhs, AExpr rhs) {
    super(lhs, rhs);
  }

  @Override
  public String pp() {
    return ppWithOp("=");
  }

  @Override
  public String toString() {
    return "EqConstraint{" + "lhs=" + lhs + ", rhs=" + rhs + '}';
  }
}

// assignment ~ as oriented equation
final class AsConstraint extends Constraint {

  AsConstraint(AExpr lhs, AExpr rhs) {
    super(lhs, rhs);
  }

  @Override
  public String pp() {
    return ppWithOp("=");
  }

  @Override
  public String toString() {
    return "AsConstraint{" + "lhs=" + lhs + ", rhs=" + rhs + '}';
  }
}
