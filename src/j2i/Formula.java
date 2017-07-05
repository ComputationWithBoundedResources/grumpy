package j2i;

import j2i.*;

import java.util.*;
import java.util.stream.*;

abstract public class Formula implements PrettyPrint {

	abstract public Formula substitute(Var var, AExpr aexpr);
	abstract public Formula remove(Var var);
	abstract boolean hasVar(Var var);

	public static Formula empty() { return new Empty(); }
	public static Atom ge(AExpr lhs, AExpr rhs) { return new Atom(new GeConstraint(lhs,rhs)); }
	public static Atom gt(AExpr lhs, AExpr rhs) { return new Atom(new GtConstraint(lhs,rhs)); }
	public static Atom le(AExpr lhs, AExpr rhs) { return new Atom(new LeConstraint(lhs,rhs)); }
	public static Atom lt(AExpr lhs, AExpr rhs) { return new Atom(new LtConstraint(lhs,rhs)); }
	public static Atom eq(AExpr lhs, AExpr rhs) { return new Atom(new EqConstraint(lhs,rhs)); }
	public And and(Formula a){ return new And(this, a); }

	// Composing expressions:
	// when working with input and output variables the idea is to substitute output variables of the first expression
	// and input variables of the second expression with a common fresh variable; then combining the two constraints
	//
	// we only produce output variables for updates; eg equality is implicitly handled in the resulting ITS; then
	// composing is a bit more difficult:
	// used conventions:
	//	 v	... input
	//	 v' ... output
	//	 v^ ... fresh variable
	// wlog we consider a single variable
	// case v' \not\in [[exp_1]]:
	//	 then v hasn't changed and composition is just the conjunction
	// case v' \in [[exp_1]] and v' \not\in [[exp_2]]
	//	 then v is v' after exp_1; we set [[exp_2]][v |-> v']
	// case v' \in [[exp_1]] and v' \in [exp_2]]
	//	 then we introduce a immediate var, ie [[exp_1]][v' |-> v^] and [[exp_2]][v |-> v^]
	public Formula compose(Formula fm2) { 

		Set<Var> vars1 = new HashSet<>();
		List<Var> pvars1 = vars1.stream().filter(v -> v.isPostVar()).collect(Collectors.toList());

		Set<Var> vars2 = new HashSet<>();
		Set<Var> pvars2 = vars1.stream().filter(v -> v.isPostVar()).collect(Collectors.toSet());
		// List<Var> pvars2 = vars1.stream().filter(v -> v.isPostVar()).collect(Collectors.toList());

		
		
		return new And(this,fm2); 
	
	}

}

class Empty extends Formula{

	public Formula substitute(Var var, AExpr aexpr) { return this; }
	public Formula remove(Var var)                  { return this; }
	public boolean hasVar(Var var)                  { return false; }

	public String pp(){ return "1 > 0"; }

}

class Atom extends Formula {
  Constraint constraint;

  public Atom(Constraint constraint){ this.constraint = constraint; }

	public Formula substitute(Var var, AExpr aexpr){
		this.constraint = this.constraint.substitute(var, aexpr);
		return this;
	}

	public Formula remove(Var var){
		if(this.constraint.hasVar(var)) return new Empty();
		else return this;
	}

	public boolean hasVar(Var var) { return this.constraint.hasVar(var); }

	public String pp(){ return this.constraint.pp(); }
}

abstract class BiFormula extends Formula {
	Formula lhs;
	Formula rhs;

  public BiFormula(Formula lhs, Formula rhs){
		this.lhs = lhs;
		this.rhs = rhs;
	}

	public Formula substitute(Var var, AExpr aexpr){
		this.lhs = this.lhs.substitute(var, aexpr);
		this.rhs = this.rhs.substitute(var, aexpr);
		return this;
	}

	public Formula remove(Var var){
		this.lhs = this.lhs.remove(var);
		this.rhs = this.rhs.remove(var);
		return this;
	}

	public boolean hasVar(Var var){
	  return this.lhs.hasVar(var) || this.rhs.hasVar(var);
	}

}

class And extends BiFormula {
	
  public And(Formula lhs, Formula rhs){ super(lhs,rhs); }

	public String pp(){ return this.lhs.pp() + " && " + this.rhs.pp(); }
}

