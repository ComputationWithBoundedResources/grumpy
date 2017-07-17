package j2i;

import j2i.*;
import java.util.*;
import java.util.function.Predicate;


abstract public class Constraint implements PrettyPrint {
  AExpr lhs;
  AExpr rhs;

  public Constraint(AExpr lhs, AExpr rhs){ this.lhs = lhs; this.rhs = rhs; }
	
	public static Constraint ge(AExpr lhs, AExpr rhs) { return new GeConstraint(lhs,rhs); }
	public static Constraint gt(AExpr lhs, AExpr rhs) { return new GtConstraint(lhs,rhs); }
	public static Constraint le(AExpr lhs, AExpr rhs) { return new LeConstraint(lhs,rhs); }
	public static Constraint lt(AExpr lhs, AExpr rhs) { return new LtConstraint(lhs,rhs); }
	public static Constraint eq(AExpr lhs, AExpr rhs) { return new EqConstraint(lhs,rhs); }
	public static Constraint as(AExpr lhs, AExpr rhs) { return new AsConstraint(lhs,rhs); }

	boolean hasVar(Var var){ return this.lhs.hasVar(var) || this.rhs.hasVar(var); }

	public void addVariables(Set<Var> vars, Predicate<Var> p){
		lhs.addVariables(vars,p);
		rhs.addVariables(vars,p);
	}

	public void substitute(Map<Var,AExpr> smap){
    this.lhs = this.lhs.substitute(smap);
    this.rhs = this.rhs.substitute(smap);
	}

	protected String ppWith(String op){ return lhs.pp() + " "	+ op + " " + rhs.pp(); }
}

class GeConstraint extends Constraint {
  public GeConstraint(AExpr lhs , AExpr rhs) { super(lhs,rhs); }
	public String pp()                         { return ppWith(">="); }
}
class GtConstraint extends Constraint {
  public GtConstraint(AExpr lhs , AExpr rhs) { super(lhs,rhs); }
	public String pp()                         { return ppWith(">"); }
}
class LeConstraint extends Constraint {
  public LeConstraint(AExpr lhs , AExpr rhs) { super(lhs,rhs); }
	public String pp()                         { return ppWith("<="); }
}
class LtConstraint extends Constraint {
  public LtConstraint(AExpr lhs , AExpr rhs) { super(lhs,rhs); }
	public String pp()                         { return ppWith("<"); }
}
class EqConstraint extends Constraint {
  public EqConstraint(AExpr lhs , AExpr rhs) { super(lhs,rhs); }
	public String pp()                         { return ppWith("="); }
}
// assignment ~ as oriented equation
class AsConstraint extends Constraint {
  public AsConstraint(AExpr lhs , AExpr rhs) { super(lhs,rhs); }
	public String pp()                         { return ppWith("="); }
}
