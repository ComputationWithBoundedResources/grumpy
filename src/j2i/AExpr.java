package j2i;

import org.antlr.v4.runtime.*;


abstract public class AExpr{

	public static AExpr fromString(String expr){
		CharStream in = CharStreams.fromString(expr);
		AExprLexer lexer = new AExprLexer(in);
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		AExprParser parser = new AExprParser(tokens);
		AExpr e = parser.eval().value;
		// System.out.println("parsing AEexpr" + expr +  " ~> " + e);
		return e;
	}

}

abstract class XBinExpr extends AExpr{
	AExpr lhs;
	AExpr rhs;
}

class XAdd extends XBinExpr{
	public XAdd(AExpr lhs, AExpr rhs){
		this.lhs = lhs;
		this.rhs = rhs;
	}

	@Override
	public String toString(){
		return "(+ " + lhs + " " + rhs + ")";
	}
}

class XMul extends XBinExpr{
	public XMul(AExpr lhs, AExpr rhs){
		this.lhs = lhs;
		this.rhs = rhs;
	}

	@Override
	public String toString(){
		return "(* " + lhs + " " + rhs + ")";
	}
}

class XSub extends XBinExpr{
	public XSub(AExpr lhs, AExpr rhs){
		this.lhs = lhs;
		this.rhs = rhs;
	}

	@Override
	public String toString(){
		return "(- " + lhs + " " + rhs + ")";
	}
}

class XNeg extends AExpr{
	AExpr neg;
	public XNeg(AExpr neg){
		this.neg = neg;
	}

	@Override
	public String toString(){
		return "(- " + neg + ")";
	}
}

class XVal extends AExpr{
	long val;

	public XVal(long val){
		this.val = val;
	}

	@Override
	public String toString(){
		return Long.toString(val);
	}
}

class XVar extends AExpr{
	String var;

	public XVar(String var){
		this.var = var;
	}

	@Override
	public String toString(){
		return var;
	}
}


