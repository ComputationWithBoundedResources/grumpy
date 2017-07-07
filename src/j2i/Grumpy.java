package j2i;

import java.util.*;
import java.util.function.*;

import soot.*;
import soot.jimple.*;

import j2i.label.*;

import static j2i.Formula.*;



// domain {{{ //
final class Domain implements Iterable<Var> {
	Set<Var> elements = new LinkedHashSet<>();

	public void addLocals(JimpleBody body){
		for(Local local : body.getLocals()) this.elements.add( Grumpy.var(local) );
		// if(Grumpy.isPrimitive(local)) this.elements.add( Grumpy.var(local) );
	}

	public void add(Var v){ this.elements.add(v); }

	public void addFields(JimpleBody body){
		for(SootField field : body.getMethod().getDeclaringClass().getFields())
			if(Grumpy.isPrimitive(field)) this.elements.add( Grumpy.var(field) );
	}

	public boolean hasElem(Var var){ return this.elements.contains(var); }

	@Override
	public Iterator<Var> iterator() { return this.elements.iterator(); }

	@Override
	public String toString() { return "Domain{" + "elements = " + elements + "}"; }
	}

// }}} domain //

/* This package provides a simple transformation from Java bytecode methods to integer transition systems.
 *


*/
public final class Grumpy {

	protected JimpleBody body;
	protected Domain domain;
	protected LabelMaker labelMaker;
	protected MethodSummaries summaries;

	private int varId = 0;
	// fresh variables
	private Var freshVar(){ return new Var("fresh_" + varId++); }
	// immediate variables
	// like fresh variables; but used for temporary results
	private Var imm(){ return new Var("imm_" + varId++); }

	final private Var rez  = new Var("ret");
	final private Var thiz = new Var("this");


	// initialisation {{{ //

	public Grumpy(JimpleBody body){
		this.body = body;
		this.labelMaker = new LabelMaker(body);
		this.domain = new Domain();
		this.domain.addLocals(body);
		this.domain.addFields(body);
		this.summaries = MethodSummaries.fromFile("summaries.json");
	}

	// }}} initialisation //


	public Transitions jimpleBody2Its() {
		Transitions ts    = new Transitions();
		for(Unit unit : body.getUnits()){
			Stmt stmt       = (Stmt) unit;
			Transitions now = this.transformStatement(stmt);
			ts              = ts.add(now);
		}
		return ts;
	}

	public KoAT jimpleBody2KoAT(){ return new KoAT(this.domain, jimpleBody2Its()); }

	// helper functions {{{ //

	protected static boolean isPrimitive(final SootField field){
		return Modifier.isStatic(field.getModifiers()) && isPrimitive(field.getType());
	}

	protected static boolean isPrimitive(final Type type){
		return type == ShortType.v() || type == IntType.v() || type == LongType.v();
	}

	protected static boolean isPrimitive(final Value val){
		return isPrimitive(val.getType());
	}

	private boolean hasDefinedLabel(Unit stmt) { return this.labelMaker.hasDefinedLabel(stmt); }
	private Label currentLabel(Unit stmt)      { return this.labelMaker.currentLabel(stmt); }
	private Label targetLabel(Unit stmt)       { return this.labelMaker.targetLabel(stmt); }
	private Label fallthrougLabel()            { return this.labelMaker.fallthrougLabel(); }

	final static Var res	= new Var("res");

	static String getSymbol(Value value)        { return value.toString(); }
	static Var var(Value value)                 { return new Var(getSymbol(value)); }
	static Var pvar(Value value)                { return new Var(getSymbol(value),true); }
	static String getSymbol(SootField field)    { return field.getDeclaringClass().getName() + "." + field.getName(); }
	static String getSymbol(StaticFieldRef ref) { return getSymbol(ref.getField()); }
	static Var var(SootField field)             { return new Var(getSymbol(field)); }
	static Var var(StaticFieldRef ref)          { return new Var(getSymbol(ref)); }
	static Var var(InstanceFieldRef ref)        { return new Var(getSymbol(ref.getBase())); }
	static Var pvar(StaticFieldRef ref)         { return new Var(getSymbol(ref),true); }
	static Var pvar(InstanceFieldRef ref)        { return new Var(getSymbol(ref.getBase()),true); }

	// public static Atom ne(AExpr lhs, AExpr rhs) { return new Atom(new NeConstraint(lhs,rhs)); }
	// }}} helper functions //


	// Jimple Statements {{{ //
	//
	// For the cases consider
	// (i) the stmt/value class hierarchy
	// (ii) Jimple Grammar
	private Transitions transformStatement(Stmt stmt){

		if( stmt instanceof AssignStmt )        return transformAssignStmt((AssignStmt) stmt);

		if( stmt instanceof GotoStmt )          return transformGotoStmt((GotoStmt) stmt);
		if( stmt instanceof IfStmt )            return transformIfStmt((IfStmt) stmt);

		if ( stmt instanceof InvokeStmt )       return transformInvokeStmt((InvokeStmt) stmt);

		if ( stmt instanceof SwitchStmt )       return transformSwitchStmt((SwitchStmt) stmt);

		if( stmt instanceof RetStmt
				|| stmt instanceof ReturnStmt
				|| stmt instanceof ReturnVoidStmt
				|| stmt instanceof ThrowStmt )       return transformReturnStmt(stmt);

		if( stmt instanceof IdentityStmt
				|| stmt instanceof NopStmt
				|| stmt instanceof EnterMonitorStmt
				|| stmt instanceof ExitMonitorStmt
				|| stmt instanceof BreakpointStmt)  return transformIdentityStmt(stmt);


		throw new RuntimeException("transformStatement: unexpected statement: " + stmt + "@" + stmt.getClass());
	}


	private Transitions transformAssignStmt(AssignStmt stmt){
		Value op1 = stmt.getLeftOp();
		Value op2 = stmt.getRightOp();

		Formula guard;
		if( op1 instanceof Local )                 guard = assignRValue((Local) op1, op2);
		else if( op1 instanceof StaticFieldRef )   guard = assignImmediate((StaticFieldRef) op1, (Immediate) op2);
		else if( op1 instanceof InstanceFieldRef ) guard = assignImmediate((InstanceFieldRef) op1, (Immediate) op2);
		else if( op1 instanceof ArrayRef )         guard = assignImmediate((ArrayRef) op1, (Immediate) op2);
		else
			throw new RuntimeException("transformAssignStmt: unexpected stmt: "
					+ op1 + "@" + op1.getClass() + " := " + op2 + "@" + op2.getClass());

		return new Transitions( currentLabel(stmt), guard, fallthrougLabel() );
	}

	private Formula assignUndefined(Local local){ return as( pvar(local), transformUndefinedValue() ); }

	private Formula assignIdentity() { return Formula.empty(); }

	private Formula assignRValue(Local local, Value rvalue){
		if( rvalue instanceof Immediate ) return assignImmediate(local, (Immediate) rvalue);
		if( rvalue instanceof Ref )       return assignRef(local, (Ref) rvalue);
		if( rvalue instanceof Expr )      return assignExpr(local, (Expr) rvalue);
		throw new RuntimeException("assignRValue: unexpected rvalue: "
				+ local + "@" + local.getClass() + " := " + rvalue + "@" + rvalue.getClass());
	}

	private Formula assignImmediate(Local local, Immediate imm){ return as( pvar(local),var(imm) ); }

	private Formula assignImmediate(StaticFieldRef ref, Immediate imm){ return as( pvar(ref),var(imm) ); }

	private Formula assignImmediate(InstanceFieldRef ref, Immediate imm){
		Local base = (Local) ref.getBase();
		Var ivar = var(base); Var ovar = pvar(base);
		return gt(ovar, Val.zero()).and( le(ovar, new Add(ivar,var(imm))) );
	}

	private Formula assignImmediate(ArrayRef ref, Immediate imm){ return assignIdentity(); }


	private Formula assignRef(Local local, Ref ref){
		Var lhs = pvar(local);
		if( ref instanceof StaticFieldRef )   return as( lhs, var((StaticFieldRef) ref) );
		if( ref instanceof InstanceFieldRef ) return ge( lhs, Val.zero() ).and( lt(lhs, var((InstanceFieldRef) ref)) );
		if( ref instanceof ArrayRef )         return as( lhs, var((ArrayRef) ref) );
		throw new RuntimeException("assignRef: unexpected ref: "
				+ local + "@" + local.getClass() + " := " + ref + "@" + ref.getClass());
	}

	private Formula assignExpr(Local local, Expr expr){

		if( expr instanceof BinopExpr )      return assignBinopExpr(local, (BinopExpr) expr);
		if( expr instanceof CastExpr )       return assignCastExpr(local, (CastExpr) expr);
		if( expr instanceof InstanceOfExpr ) return assignInstanceOfExpr(local, (InstanceOfExpr) expr);
		if( expr instanceof InvokeExpr )     return assignInvokeExpr(local, (InvokeExpr) expr);
		if( expr instanceof AnyNewExpr )     return assignAnyNewExpr(local, (AnyNewExpr) expr);
		if( expr instanceof LengthExpr )     return assignLengthExpr(local, (LengthExpr) expr);
		if( expr instanceof NegExpr )        return assignNegExpr(local, (NegExpr) expr);

		throw new RuntimeException("transformRValue: unexpected stmt: "
				+ local + "@" + local.getClass() + " := " + expr + "@" + expr.getClass());
	}


	// Conditional operations are currently only addressed in ifgoto. The operations are 'native', the result is
	// either 0 or 1. If we want to support them in general we need support for DNF formula and specialize the guards
	// respectively, eg eg (a CmpExpr b) := a > b && res 0 || a <= b && res 1; or just return two transitions.
	public Formula assignBinopExpr(Local local, BinopExpr expr){

		Var lhs = pvar(local);
		AExpr imm1 = transformImmediate( (Immediate) expr.getOp1() );
		AExpr imm2 = transformImmediate( (Immediate) expr.getOp2() );

		if( expr instanceof AddExpr )	return as( lhs,new Add(imm1,imm2) );
		if( expr instanceof MulExpr ) return as( lhs,new Mul(imm1,imm2) );
		if( expr instanceof SubExpr ) return as( lhs,new Sub(imm1,imm2) );

		// CmpExpr, CmpgExpr, CmplExpr  - comparison for long, float
		// ConditionExpr                - occur in ifgoto statements
		// DivExpr, RemExpr             - unsupported arith operations
		// AndExpr, OrExpr, XorExpr, ShlExpr, ShrExpr, UShrExpr - shiftoperations are undefined
		return assignUndefined(local);
	}

	// We ignore casts and conversions (unsound for narrowing conversions).
	// The JVM checkcast instruction may throw an ClassCastException - we ignore exceptional control flow. Widening
	// conversions do not modify the value. Narrowing conversions may modify the value, eg lont -> int
	public Formula assignCastExpr(Local local, CastExpr expr){ return assignRValue(local, expr.getOp()); }

	// JVM instanceof returns 0 if expr is null, 1 if expr is instanced of resolved class, 0 otherwise.
	// Ignore, as the abstraction can not be refined wrt to runtime instance.
	public Formula assignInstanceOfExpr(Local local, InstanceOfExpr expr){ return assignUndefined(local); }

	public Formula assignAnyNewExpr(Local local, AnyNewExpr expr){
		if ( expr instanceof NewExpr ) return as( pvar(local),Val.one() );
		return assignUndefined(local); // NewArrayExpr, NewMultiArrayExpr
	}

	public Formula assignLengthExpr(Local local, LengthExpr expr){ return assignUndefined(local); }

	public Formula assignNegExpr(Local local, NegExpr expr){ return as( pvar(local), new Sub(Val.zero(),var(local)) ); }

	public Formula assignInvokeExpr(Local local, InvokeExpr expr){
		return  evalInvokeExpr(expr).and( as(pvar(local), this.rez) );
	}


	public Formula evalInvokeExpr(InvokeExpr expr){

		if( expr instanceof DynamicInvokeExpr )
			throw new RuntimeException("unsupported expr: " + expr + "@" + expr.getClass());
		if( expr instanceof InstanceInvokeExpr
				|| expr instanceof StaticInvokeExpr ) {
			SootMethodRef ref = expr.getMethodRef();
			Optional<MethodSummary> msumM = resolve(ref);
			MethodSummary msum;
			if(msumM.isPresent()){
				msum = msumM.get();
			} else {
				System.out.println("DEF: use default summary for" + expr);
				msum = MethodSummary.defaultSummary();
			}
			return evalMethodSummary(expr, msum);
		}
		throw new RuntimeException("evalInvokeExpr: unexpected expr: " + expr + "@" + expr.getClass() );
	}


	public Formula evalMethodSummary(InvokeExpr expr, MethodSummary msum){
		// obtain necessary information of summary
		AExpr upper = msum.getUpperTimeWithDefault();
		AExpr lower = msum.getLowerTimeWithDefault();
		Formula effect = msum.getEffect();

		// substitute arg1,...,arg_n with imm_1,...,imm_n
		int i = 1;
		for(Value val : expr.getArgs()){
			if(val instanceof Local) effect = effect.substitute( new Var("arg" + i), var( (Local) val ) );
			i++;
		}

		// substitute this with base
		if(expr instanceof InstanceInvokeExpr){
			Local base = (Local) ((InstanceInvokeExpr) expr).getBase();
			effect = effect.substitute( this.thiz, var(base) );
		}

		return effect;
	}


	// Soot provides JVM method references for InvokeExpr.
	// The declaring class is a super type of the runtime instance and we assume that the corresponding summary is
	// a representation of all possible calls.
	// We ignore descriptors for now (unsound).
	private Optional<MethodSummary> resolve(SootMethodRef ref){
		String cname = ref.declaringClass().getName();
		String mname = ref.name();
		String descr = Util.getMethodDescriptor(ref.resolve());
		System.out.println("trying:<" + cname + "," + mname +"," + descr +">");
		return this.summaries.get(cname,mname,descr);
	}


	private Transitions transformGotoStmt(GotoStmt stmt){
		Stmt target = (Stmt) stmt.getTarget();
		return new Transitions(currentLabel(stmt), targetLabel(target));
	}

	// We have  imm1 condop imm2
	private Transitions transformIfStmt(IfStmt stmt){
		ConditionExpr condition = (ConditionExpr) stmt.getCondition();
		Stmt target = stmt.getTarget();

		Immediate op1 = (Immediate) condition.getOp1();
		Immediate op2 = (Immediate) condition.getOp2();
		AExpr imm1 = transformImmediate( op1 );
		AExpr imm2 = transformImmediate( op2 );

		Transitions ts = new Transitions();

		Label from = currentLabel(stmt);
		Label to   = targetLabel(target);
		Label next = fallthrougLabel();

		if( condition instanceof GtExpr )
			return ts
				.add(new Transition( from, gt(imm1,imm2), to ))
				.add(new Transition( from, le(imm1,imm2), next ));
		if( condition instanceof GeExpr )
			return ts
				.add(new Transition( from, ge(imm1,imm2), to ))
				.add(new Transition( from, lt(imm1,imm2), next ));
		if( condition instanceof LeExpr )
			return ts
				.add(new Transition( from, le(imm1,imm2), to ))
				.add(new Transition( from, gt(imm1,imm2), next ));
		if( condition instanceof LtExpr )
			return ts
				.add(new Transition( from, lt(imm1,imm2), to ))
				.add(new Transition( from, ge(imm1,imm2), next ));
		if( condition instanceof EqExpr && op1 instanceof NullConstant )
			return ts
				.add(new Transition( from, eq(imm1,imm2), to ))
				.add(new Transition( from, lt(imm1,imm2), next ));
		if( condition instanceof EqExpr && op2 instanceof NullConstant )
			return ts
				.add(new Transition( from, eq(imm1,imm2), to ))
				.add(new Transition( from, gt(imm1,imm2), next ));
		if( condition instanceof EqExpr )
			return ts
				.add(new Transition( from, eq(imm1,imm2), to ))
				.add(new Transition( from, gt(imm1,imm2), next ))
				.add(new Transition( from, lt(imm1,imm2), next ));
		if( condition instanceof NeExpr && op1 instanceof NullConstant )
			return ts
				.add(new Transition( from, lt(imm1,imm2), to ))
				.add(new Transition( from, eq(imm1,imm2), next ));
		if( condition instanceof NeExpr && op2 instanceof NullConstant )
			return ts
				.add(new Transition( from, gt(imm1,imm2), to ))
				.add(new Transition( from, eq(imm1,imm2), nextLabel()         ));

		throw new RuntimeException("transformIfStmt: unexpected stmt: " + stmt + "@" + stmt.getClass() + ":" + condition + "@" + condition.getClass());

	}

	private Transitions transformInvokeStmt(InvokeStmt stmt){
		Formula guard = evalInvokeExpr(stmt.getInvokeExpr());
		return new Transitions( currentLabel(stmt), guard, nextLabel() );
	}

	// Refinement on immediate variables are not really helpful, thus we model switch as non-deterministic jumps.
	// We model switch statements as non-deterministic jumps.
	private Transitions transformSwitchStmt(SwitchStmt stmt){
		Unit def = stmt.getDefaultTarget();
		Transitions ts = new Transitions( currentLabel(stmt), targetLabel(def));
		for(Unit target : stmt.getTargets())
			ts.add( new Transition(currentLabel(stmt),targetLabel(target)) );
		return ts;
	}

	private Transitions transformReturnStmt(Stmt stmt){ return Transitions.empty(); }

	private Transitions transformIdentityStmt(Stmt stmt){
		if (hasDefinedLabel(stmt)) return new Transitions(currentLabel(stmt), nextLabel());
		else return Transitions.empty();
	}

	// }}} Jimple Statements //

	// Jimple Values {{{ //



	private AExpr transformImmediate(Immediate imm){
		if( imm instanceof Local )    return transformLocal((Local) imm);
		if( imm instanceof Constant ) return transformConstant((Constant) imm);

		throw new RuntimeException("transformImmediate: unexpected imm: " + imm + "@" + imm.getClass());
	}

	public AExpr transformLocal(Local local){ return var(local); }

	public AExpr transformConstant(Constant con){
		if ( con instanceof NullConstant )   return Val.zero();
		if ( con instanceof IntConstant )    return new Val(((IntConstant)  con).value);
		if ( con instanceof LongConstant )   return new Val(((LongConstant) con).value);
		// ClassConstant, MethodHandle, StringConstant, RealConstant
		return transformUndefinedValue();
	}

	public AExpr transformUndefinedValue(){ return freshVar(); }

	// }}} Jimple Values //


}

// vi:set noet sw=2 ts=2 tw=118 fdm=marker:

