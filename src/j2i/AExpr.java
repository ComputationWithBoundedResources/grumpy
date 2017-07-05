package j2i;

import org.antlr.v4.runtime.*;


abstract public class AExpr implements PrettyPrint{

	public static AExpr fromString(String expr){
		CharStream in = CharStreams.fromString(expr);
		AExprLexer lexer = new AExprLexer(in);
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		AExprParser parser = new AExprParser(tokens);
		AExpr e = parser.eval().value;
		// System.out.println("parsing AEexpr" + expr +  " ~> " + e);
		return e;
	}

	abstract public boolean hasVar(Var var);
	abstract AExpr substitute(Var var, AExpr expr);

}

abstract class BinExpr extends AExpr{
	AExpr lhs;
	AExpr rhs;

	@Override
	public boolean hasVar(Var var){
		return this.lhs.hasVar(var) || this.rhs.hasVar(var);
	}

  @Override
	public AExpr substitute(Var var, AExpr expr){
		this.lhs = this.lhs.substitute(var, expr);
		this.rhs = this.rhs.substitute(var, expr);
		return this;
	}

	protected String ppWith(String op){ return "(" + this.lhs.pp() + " " + op + " " + this.rhs.pp() + ")"; }
}

class Add extends BinExpr{

	public Add(AExpr lhs, AExpr rhs){ this.lhs = lhs; this.rhs = rhs; }
	public Add(String lhs, String rhs){ this.lhs = new Var(lhs); this.rhs = new Var(rhs); }

	@Override
	public String toString(){
		return "(+ " + lhs + " " + rhs + ")";
	}

	public String pp(){ return ppWith("+"); }

}

class Mul extends BinExpr{

	public Mul(AExpr lhs, AExpr rhs){ this.lhs = lhs; this.rhs = rhs; }
	public Mul(String lhs, String rhs){ this.lhs = new Var(lhs); this.rhs = new Var(rhs); }

	@Override
	public String toString(){
		return "(* " + lhs + " " + rhs + ")";
	}

	public String pp(){ return ppWith("*"); }

}

class Sub extends BinExpr{

	public Sub(AExpr lhs, AExpr rhs){ this.lhs = lhs; this.rhs = rhs; }
	public Sub(String lhs, String rhs){ this.lhs = new Var(lhs); this.rhs = new Var(rhs); }

	@Override
	public String toString(){
		return "(- " + lhs + " " + rhs + ")";
	}
	public String pp(){ return ppWith("-"); }

}

class Neg extends AExpr{
	AExpr neg;

	public Neg(AExpr neg){
		this.neg = neg;
	}

  @Override
	public boolean hasVar(Var var){
		return this.neg.hasVar(var);
	}


	@Override
	public AExpr substitute(Var var, AExpr expr){
		this.neg = this.neg.substitute(var, expr);
		return this;
	}

	@Override
	public String toString(){
		return "(- " + neg + ")";
	}

	public String pp(){ return "(-" + neg.pp() + ")"; }
	
}


class Val extends AExpr{
	long val;

	public Val(long val){ this.val = val; }
	public static Val one() { return new Val(1); }
	public static Val zero(){ return new Val(0); }

	@Override
	public boolean hasVar(Var var){ return false; }

	@Override
	public AExpr substitute(Var var, AExpr expr){ return this; }

	@Override
	public String toString(){ return Long.toString(val); }

	public String pp(){
		if(this.val >= 0) return this.toString();
		else              return "(" + this.toString() + ")";
	}

}


class Var extends AExpr{
	String symb;
	boolean post = false;

	public Var(String symb){ this.symb = symb; }
	public Var(String symb, boolean post){ this.symb = symb; this.post = post; }

  public void toPostVar() { this.post = true; }
	public Var asPostVar()  { return new Var(this.symb, true); }
	public boolean isPostVar() { return this.post; }

	@Override
	public AExpr substitute(Var var, AExpr expr){ return this.equals(var) ? expr : this; }

  @Override
	public boolean hasVar(Var var){ return this.equals(var); }

	@Override
	public String toString(){ return this.symb + (this.post ? "'" : ""); }

	@Override
	public String pp(){ return this.toString(); }


	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		Var object = (Var) o;

		if (symb != null ? !symb.equals(object.symb) : object.symb != null) return false;
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


