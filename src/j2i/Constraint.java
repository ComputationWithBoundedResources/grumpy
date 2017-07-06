package j2i;

import j2i.*;

abstract public class Constraint implements PrettyPrint {
  AExpr lhs;
  AExpr rhs;

  public Constraint(AExpr lhs, AExpr rhs){ this.lhs = lhs; this.rhs = rhs; }

	boolean hasVar(Var var){ return this.lhs.hasVar(var) || this.rhs.hasVar(var); }

	public Constraint substitute(Var var, AExpr expr){
    this.lhs = this.lhs.substitute(var, expr);
    this.rhs = this.rhs.substitute(var, expr);
		return this;
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
