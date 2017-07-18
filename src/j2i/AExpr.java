package j2i;

import java.util.*;
import java.util.function.Predicate;
import org.antlr.v4.runtime.*;


abstract class AExpr implements PrettyPrint {

  static Val zero = new Val(0);
  static Val one = new Val(1);

  static AExpr add(AExpr lhs, AExpr rhs) {
    return new Add(lhs, rhs);
  }

  static AExpr mul(AExpr lhs, AExpr rhs) {
    return new Mul(lhs, rhs);
  }

  static AExpr sub(AExpr lhs, AExpr rhs) {
    return new Sub(lhs, rhs);
  }

  static AExpr neg(AExpr e) {
    return new Neg(e);
  }

  static AExpr val(Long v) {
    return new Val(v);
  }

  static AExpr var(String v) {
    return new Var(v);
  }

  static AExpr fromString(String expr) {
    CharStream in = CharStreams.fromString(expr);
    AExprLexer lexer = new AExprLexer(in);
    CommonTokenStream tokens = new CommonTokenStream(lexer);
    AExprParser parser = new AExprParser(tokens);
    AExpr e = parser.eval().value;
    // System.out.println("parsing AEexpr" + expr +  " ~> " + e);
    return e;
  }

  abstract boolean hasVar(Var var);

  abstract void addVariables(Set<Var> vars, Predicate<Var> p);

  abstract AExpr substitute(Map<Var, AExpr> smap);
}

abstract class BinExpr extends AExpr {

  AExpr lhs;
  AExpr rhs;

  @Override
  boolean hasVar(Var var) {
    return this.lhs.hasVar(var) || this.rhs.hasVar(var);
  }

  @Override
  void addVariables(Set<Var> vars, Predicate<Var> p) {
    lhs.addVariables(vars, p);
    rhs.addVariables(vars, p);
  }

  @Override
  AExpr substitute(Map<Var, AExpr> smap) {
    this.lhs = this.lhs.substitute(smap);
    this.rhs = this.rhs.substitute(smap);
    return this;
  }

  protected String ppWith(String op) {
    return "(" + this.lhs.pp() + " " + op + " " + this.rhs.pp() + ")";
  }
}

final class Add extends BinExpr {

  Add(AExpr lhs, AExpr rhs) {
    this.lhs = lhs;
    this.rhs = rhs;
  }

  Add(String lhs, String rhs) {
    this.lhs = new Var(lhs);
    this.rhs = new Var(rhs);
  }

  @Override
  public String pp() {
    return ppWith("+");
  }

  @Override
  public String toString() {
    return "Add{" + "lhs=" + lhs + ", rhs=" + rhs + '}';
  }
}

final class Mul extends BinExpr {

  Mul(AExpr lhs, AExpr rhs) {
    this.lhs = lhs;
    this.rhs = rhs;
  }

  Mul(String lhs, String rhs) {
    this.lhs = new Var(lhs);
    this.rhs = new Var(rhs);
  }

  @Override
  public String pp() {
    return ppWith("*");
  }

  @Override
  public String toString() {
    return "Mul{" + "lhs=" + lhs + ", rhs=" + rhs + '}';
  }
}

final class Sub extends BinExpr {

  Sub(AExpr lhs, AExpr rhs) {
    this.lhs = lhs;
    this.rhs = rhs;
  }

  Sub(String lhs, String rhs) {
    this.lhs = new Var(lhs);
    this.rhs = new Var(rhs);
  }

  @Override
  public String pp() {
    return ppWith("-");
  }

  @Override
  public String toString() {
    return "Sub{" + "lhs=" + lhs + ", rhs=" + rhs + '}';
  }
}

final class Neg extends AExpr {

  AExpr neg;

  Neg(AExpr neg) {
    this.neg = neg;
  }

  @Override
  boolean hasVar(Var var) {
    return this.neg.hasVar(var);
  }

  @Override
  void addVariables(Set<Var> vars, Predicate<Var> p) {
    this.neg.addVariables(vars, p);
  }


  @Override
  AExpr substitute(Map<Var, AExpr> smap) {
    this.neg = this.neg.substitute(smap);
    return this;
  }

  @Override
  public String pp() {
    return "(-" + neg.pp() + ")";
  }

  @Override
  public String toString() {
    return "Neg{" + "neg=" + neg + '}';
  }
}

final class Val extends AExpr {

  long val;

  Val(long val) {
    this.val = val;
  }

  static Val one() {
    return new Val(1);
  }

  static Val zero() {
    return new Val(0);
  }

  @Override
  boolean hasVar(Var var) {
    return false;
  }

  @Override
  void addVariables(Set<Var> vars, Predicate<Var> p) {
  }

  @Override
  AExpr substitute(Map<Var, AExpr> smap) {
    return this;
  }

  @Override
  public String pp() {
    if (this.val >= 0) {
      return Long.toString(val);
    } else {
      return "(" + Long.toString(val) + ")";
    }
  }

  @java.lang.Override
  public java.lang.String toString() {
    return "Val{" + "val=" + val + '}';
  }
}

final class Var extends AExpr {

  String symb;
  boolean post = false;

  Var(String symb) {
    this.symb = symb;
  }

  Var(String symb, boolean post) {
    this.symb = symb;
    this.post = post;
  }

  static Var newPostVar(Var var) {
    return new Var(var.symb, true);
  }

  static Var newPreVar(Var var) {
    return new Var(var.symb);
  }

  boolean isPostVar() {
    return this.post;
  }

  @Override
  AExpr substitute(Map<Var, AExpr> smap) {
    AExpr expr = smap.get(this);
    return expr != null ? expr : this;
  }

  @Override
  boolean hasVar(Var var) {
    return this.equals(var);
  }

  @Override
  void addVariables(Set<Var> vars, Predicate<Var> p) {
    if (p.test(this)) {
      vars.add(this);
    }
  }

  @Override
  public String pp() {
    return this.symb + (this.post ? "'" : "");
  }

  @Override
  public String toString() {
    return "Var{" + "symb='" + symb + '\'' + ", post=" + post + '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Var object = (Var) o;

    if (symb != null ? !symb.equals(object.symb) : object.symb != null) {
      return false;
    }
    return !(post != object.post);
  }

  @Override
  public int hashCode() {
    int result = 17;
    result = 31 * result + (symb != null ? symb.hashCode() : 0);
    result = 31 * result + (post ? 0 : 1);
    return result;
  }

}

