package j2i;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;


enum BAOp implements PPKoAT {
	Add, Mul;

	@Override
	public String toString() {
		switch(this) {
			case Add:
				return "+";
			case Mul:
				return "*";
			default:
				throw new RuntimeException("Unexpected BAOp: " + this);
		}
	}

	@Override
	public String ppKoAT() {
		return this.toString();
	}
}

enum BRel implements PPKoAT {
	Lt, Le, Eq, Ge, Gt;

	@Override
	public String toString() {

		switch(this) {
			case Lt:
				return "<";
			case Le:
				return "<=";
			case Eq:
				return "=";
			case Ge:
				return ">=";
			case Gt:
				return ">";
			default:
				throw new RuntimeException("Unexpected BRel: " + this);
		}
	}

	@Override
	public String ppKoAT() {
		return this.toString();
	}
}

interface PPKoAT {
	String ppKoAT();
}

public class Its implements PPKoAT {
	List<Rule> rules = new ArrayList<>();

	public void addAll(Rule rule) {
		this.rules.add(rule);
	}

	public void addAll(Collection<Rule> rules) {
		this.rules.addAll(rules);
	}

	public List<Rule> getRules() {
		return this.rules;
	}

	@Override
	public String toString() {
		return this.rules.toString();
	}

	@Override
	public String ppKoAT() {
		StringBuilder b = new StringBuilder();
		b.append("(GOAL COMPLEXITY)\n");
		String s = this.rules.isEmpty() ? "start" : this.rules.get(0).lhs.label.toString();
		b.append("(STARTTERM (FUNCTIONSYMBOLS " + s + "))\n");
		b.append("(VAR )\n");
		b.append("(RULES\n");
		if(this.rules.isEmpty()) {
			b.append("start(x) -> end(x)\n");
		}
		for(Rule r : rules) {
			b.append(r.ppKoAT());
			b.append('\n');
		}
		b.append(")\n");
		return b.toString();
	}
}

class Symbol implements PPKoAT {
	String symbol;

	public Symbol(String symbol) {
		this.symbol = symbol;
	}

	@Override
	public String toString() {
		return this.symbol;
	}

	@Override
	public String ppKoAT() {
		return this.symbol;
	}

	@Override
	public boolean equals(Object o) {
		if(this == o) return true;
		if(o == null || getClass() != o.getClass()) return false;

		Symbol symbol1 = (Symbol) o;

		return symbol != null ? symbol.equals(symbol1.symbol) : symbol1.symbol == null;
	}

	@Override
	public int hashCode() {
		return symbol != null ? symbol.hashCode() : 0;
	}
}

class Rule implements PPKoAT {
	Term lhs;
	Term rhs;
	List<Constraint> constraints = new ArrayList<>();

	public Rule(Term lhs, Term rhs) {
		this.lhs = lhs;
		this.rhs = rhs;
	}

	public Rule(Term lhs, Term rhs, Constraint constraint) {
		this.lhs = lhs;
		this.rhs = rhs;
		this.constraints.add(constraint);
	}

	@Override
	public String toString() {
		return this.lhs + " -> " + this.rhs + " | " + this.constraints;
	}

	@Override
	public String ppKoAT() {
		StringBuilder b = new StringBuilder();
		Iterator<Constraint> it = this.constraints.iterator();
		while(it.hasNext()) {
			Constraint c = it.next();
			b.append(c.ppKoAT());
			if(it.hasNext())
				b.append(" && ");
		}

		return this.lhs.ppKoAT() + " -> " + this.rhs.ppKoAT() + (this.constraints.isEmpty() ? "" : " :|: " + b.toString());
	}
}

class Term implements PPKoAT {
	Symbol label;
	List<AExpr> args;

	public Term(Symbol label, List<AExpr> args) {
		this.label = label;
		this.args = args;
	}

	public List<AExpr> getArgs() {
		return this.args;
	}

	public void setLabel(Symbol label) {
		this.label = label;
	}

	@Override
	public String toString() {
		return this.label + "(" + this.args + ")";
	}

	@Override
	public String ppKoAT() {
		StringBuilder b = new StringBuilder();
		Iterator<AExpr> it = this.args.iterator();
		while(it.hasNext()) {
			AExpr e = it.next();
			b.append(e.toNormalform().ppKoAT());
			if(it.hasNext())
				b.append(", ");
		}
		return this.label.ppKoAT() + "(" + b.toString() + ")";
	}
}

// abstract class Expr implements PPKoAT {

// 	// KoAT's expression parser is a bit picky, especially with parentheses. It looks (unnecessary) complicated and I do
// 	// not want to modify it. Thus we bring expressions in normalform; that is we distribute multiplication over
// 	// addition. Parentheses are only used for unary minus.
// 	public abstract Expr toNormalform();

// 	@Override
// 	protected Object clone() throws CloneNotSupportedException {
// 		return super.clone();
// 	}
// }

// class Var extends Expr implements PPKoAT {
// 	Symbol var;

// 	public Var(Symbol var) {
// 		this.var = var;
// 	}

// 	public Var(String var) {
// 		this.var = new Symbol(var);
// 	}

// 	@Override
// 	public Expr toNormalform() {
// 		return new Var(this.var);
// 	}

// 	@Override
// 	public String toString() {
// 		return this.var.toString();
// 	}

// 	@Override
// 	public String ppKoAT() {
// 		return this.var.toString();
// 	}

// 	@Override
// 	public boolean equals(Object o) {
// 		if(this == o) return true;
// 		if(o == null || getClass() != o.getClass()) return false;

// 		Var var1 = (Var) o;

// 		return var != null ? var.equals(var1.var) : var1.var == null;
// 	}

// 	@Override
// 	public int hashCode() {
// 		return var != null ? var.hashCode() : 0;
// 	}
// }

// class Val extends Expr {
// 	long val;

// 	public Val(long val) {
// 		this.val = val;
// 	}

// 	public long getVal() {
// 		return this.val;
// 	}

// 	@Override
// 	public Expr toNormalform() {
// 		if(this.val < 0) {
// 			return new UnaryMinus(new Val(-this.val));
// 		} else {
// 			return new Val(this.val);
// 		}
// 	}

// 	@Override
// 	public String toString() {
// 		return Long.toString(this.val);
// 	}

// 	@Override
// 	public String ppKoAT() {
// 		return Long.toString(this.val);
// 	}
// }

// class UnaryMinus extends Expr implements PPKoAT {
// 	Expr neg;

// 	public UnaryMinus(Expr e) {
// 		this.neg = e;
// 	}

// 	@Override
// 	public Expr toNormalform() {
// 		Expr e1 = this.neg.toNormalform();
// 		if(e1 instanceof UnaryMinus) {
// 			return ((UnaryMinus) e1).neg;
// 		}
// 		return new UnaryMinus(e1);
// 	}

// 	@Override
// 	public String toString() {
// 		// return "(0 - " + this.neg + ")";
// 		return "(-" + this.neg + ")";
// 	}

// 	@Override
// 	public String ppKoAT() {
// 		return this.toString();
// 	}
// }

// class AAExpr extends Expr implements PPKoAT {
// 	Expr lhs;
// 	BAOp bop;
// 	Expr rhs;

// 	public AAExpr(Expr lhs, BAOp bop, Expr rhs) {
// 		this.lhs = lhs;
// 		this.bop = bop;
// 		this.rhs = rhs;
// 	}

// 	// KoAT is bad with arbitrary expressions containing parentheses
// 	@Override
// 	public Expr toNormalform() {
// 		Expr lhs1 = lhs.toNormalform();
// 		Expr rhs1 = rhs.toNormalform();
// 		if(lhs1 instanceof Val && rhs1 instanceof Val) {
// 			long v1 = ((Val) lhs1).getVal();
// 			long v2 = ((Val) rhs1).getVal();
// 			switch(this.bop) {
// 				case Add:
// 					return new Val(v1 + v2);
// 				case Mul:
// 					return new Val(v1 * v2);
// 			}
// 		}
// 		if(this.bop == BAOp.Mul) {
// 			if(lhs1 instanceof AAExpr &&
// 					((AAExpr) lhs1).bop == BAOp.Add) {
// 				// (a.b)*c = a*c . b*c
// 				Expr a = ((AAExpr) lhs1).lhs;
// 				Expr b = ((AAExpr) lhs1).rhs;
// 				Expr c = this.rhs;
// 				AAExpr e1 = new AAExpr(a, BAOp.Mul, c);
// 				AAExpr e2 = new AAExpr(b, BAOp.Mul, c);
// 				return new AAExpr(e1, BAOp.Add, e2);
// 			} else if(rhs1 instanceof AAExpr &&
// 					((AAExpr) rhs1).bop == BAOp.Add) {
// 				// c*(a.b) = a*c . b*c
// 				Expr a = ((AAExpr) rhs1).lhs;
// 				Expr b = ((AAExpr) rhs1).rhs;
// 				Expr c = this.lhs;
// 				AAExpr e1 = new AAExpr(a, BAOp.Mul, c);
// 				AAExpr e2 = new AAExpr(b, BAOp.Mul, c);
// 				return new AAExpr(e1, BAOp.Add, e2);
// 			}
// 		}

// 		return new AAExpr(this.lhs, this.bop, this.rhs);
// 	}

// 	@Override
// 	public String toString() {
// 		return this.lhs + " " + this.bop + " " + this.rhs;
// 	}

// 	@Override
// 	public String ppKoAT() {
// 		return
// 				this.lhs.ppKoAT()
// 						+ " "
// 						+ this.bop.ppKoAT()
// 						+ " "
// 						+ this.rhs.ppKoAT();
// 	}


// }

class Constraint implements PPKoAT {
	AExpr lhs;
	BRel rel;
	AExpr rhs;

	public Constraint(AExpr lhs, BRel rel, AExpr rhs) {
		this.lhs = lhs;
		this.rel = rel;
		this.rhs = rhs;
	}

	public void setConstraint(AExpr lhs, BRel rel, AExpr rhs) {
		this.lhs = lhs;
		this.rel = rel;
		this.rhs = rhs;
	}

	@Override
	public String toString() {
		return this.lhs + " " + this.rel + " " + this.rhs;
	}

	@Override
	public String ppKoAT() {
		return this.lhs.ppKoAT() + " " + this.rel.ppKoAT() + " " + this.rhs.ppKoAT();
	}
}


