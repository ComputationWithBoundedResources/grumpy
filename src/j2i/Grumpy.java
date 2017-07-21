// This package provides a simple transformation from Java Bytecode methods to Tnteger Transtions Systems
package j2i;

import static j2i.AExpr.*;
import static j2i.Constraint.*;
import static j2i.Formula.*;

import java.util.*;
import java.util.function.*;
import java.util.logging.*;
import soot.*;
import soot.Type.*;
import soot.jimple.*;

/*
  The following transformation uses Soot's Jimple IR. Jimple is a typed 3-address representation of byte code. Most
	notably, putfield, getfield and method invocation occur only as specific statements. The main downside of using
	Jimple is that it introduces a lot of temporary variables. To obtain a compact transition system, some
	optimizations, namely chaining and argument filtering, are necessary.

	Alternative approaches:
		- Use Grimp IR: Grimp aggregates expressions and removes unused local variables, in particular stack variables.
		Thus the Grimp IR is usually more compact and uses less local variables. Though the transformation is more
		complex, in particular for complicated object expressions. Furthermore Soot provides annotations for Jimple
		providing aliasing and purity information. These could be used to improve precision and implement a sound (but
		cheap) shape and sharing analysis.
		- It would be possible to build the abstraction as a Jimple to Jimple transformation, then using Grimp to obtain a
		compact representation. But due to the abstraction we often introduce non-determinism which have to be represented
		as additional control-flow constructs in Jimple.

  Notes on Jimple:
		- In Soot everything is practically a Unit or a Value and the Jimple class hierarchy allows to construct invalid
		Jimple IR. For the transformation we expect code conform to the Jimple Grammar (see [1]).
		- Soot constructs unique variable names for local variables. Prefix $ is used for stack variables. The
		implementation relies on a couple of reserved variable names. Postfix ' is used for output variables, arg1, arg2,
		... this, ret are used in method summaries, env denotes a environment variable and fresh1, ... imm1,... are used
		for fresh variables. The Soot option -use-original-names should not be used to avoid variable capturing.


[1] Vallée-Rai R. et al., Soot: A Java Bytecode Optimization Framework, 2010
 */


public final class Grumpy {

  public static final Logger log = Logger.getLogger("Grumpy");
  final static Var res = new Var("res");
  final private Var rez = new Var("ret");
  final private Var thiz = new Var("this");
  protected JimpleBody body;
  protected Domain domain;
  protected LabelMaker labelMaker;
  protected MethodSummaries summaries;
  protected SizeAbstraction sizeAbstraction = new NodeFieldsAbstraction();
  private int varId = 0;

  public Grumpy(JimpleBody body) {
    this.body = body;
    this.labelMaker = new LabelMaker(body);
    this.domain = new Domain();
    this.domain.addLocals(body);
    this.domain.addFields(body);
    this.summaries = MethodSummaries.fromFile("summaries.json");
  }

  protected static boolean isPrimitive(final SootField field) {
    return Modifier.isStatic(field.getModifiers()) && isPrimitive(field.getType());
  }

  protected static boolean isPrimitive(final Type type) {
    return type == ShortType.v() || type == IntType.v() || type == LongType.v();
  }

  protected static boolean isPrimitive(final Value val) {
    return isPrimitive(val.getType());
  }


  static String getSymbol(Value value) {
    return value.toString();
  }

  static Var var(Value value) {
    return new Var(getSymbol(value));
  }

  static Var pvar(Value value) {
    return new Var(getSymbol(value), true);
  }

  static String getSymbol(SootField field) {
    return field.getDeclaringClass().getName() + "." + field.getName();
  }

  static String getSymbol(StaticFieldRef ref) {
    return getSymbol(ref.getField());
  }

  static Var var(SootField field) {
    return new Var(getSymbol(field));
  }

  static Var var(StaticFieldRef ref) {
    return new Var(getSymbol(ref));
  }

  static Var var(InstanceFieldRef ref) {
    return new Var(getSymbol(ref.getBase()));
  }

  static Var pvar(StaticFieldRef ref) {
    return new Var(getSymbol(ref), true);
  }

  static Var pvar(InstanceFieldRef ref) {
    return new Var(getSymbol(ref.getBase()), true);
  }

  // we treat long as int
  private static boolean hasIntType(Type type) {
    return Type.toMachineType(type) instanceof IntType || type instanceof LongType;
  }

  private static boolean hasRefType(Type type) {
    return type instanceof RefLikeType;
  }

  private static boolean hasIntType(Value val) {
    return hasIntType(val.getType());
  }

  private static boolean hasRefType(Value val) {
    return hasRefType(val.getType());
  }

  private static boolean hasIntType(SootField field) {
    return hasIntType(field.getType());
  }

  private static boolean hasRefType(SootField field) {
    return hasRefType(field.getType());
  }

  protected Var freshVar() {
    return new Var("fresh_" + varId++);
  }

  public Transitions jimpleBody2Its() {
    Transitions ts = new Transitions();
    for (Unit unit : body.getUnits()) {
      Stmt stmt = (Stmt) unit;
      Transitions now = this.transformStatement(stmt);
      ts = ts.add(now);
    }
    return ts;
  }

  public KoAT jimpleBody2KoAT() {
    return new KoAT(this.domain, jimpleBody2Its());
  }

  public KoAT jimpleBody2KoAT2() {
    return new KoAT(this.domain, Transitions.compact(jimpleBody2Its()));
  }

  private boolean hasDefinedLabel(Unit stmt) {
    return this.labelMaker.hasDefinedLabel(stmt);
  }

  private Label currentLabel(Unit stmt) {
    return this.labelMaker.currentLabel(stmt);
  }

  private Label targetLabel(Unit stmt) {
    return this.labelMaker.targetLabel(stmt);
  }

  private Label fallthrougLabel() {
    return this.labelMaker.fallthroughLabel();
  }


  //
  // To implement chaining of blocks we want to provide only one transition for (non-labelled and non-jumping)
  // statements. To mimic non-determinism use disjunctive guards.
  private Transitions transformStatement(Stmt stmt) {

    if (stmt instanceof AssignStmt) {
      return transformAssignStmt((AssignStmt) stmt);
    }

    if (stmt instanceof GotoStmt) {
      return transformGotoStmt((GotoStmt) stmt);
    }
    if (stmt instanceof IfStmt) {
      return transformIfStmt((IfStmt) stmt);
    }

    if (stmt instanceof InvokeStmt) {
      return transformInvokeStmt((InvokeStmt) stmt);
    }

    if (stmt instanceof SwitchStmt) {
      return transformSwitchStmt((SwitchStmt) stmt);
    }

    if (stmt instanceof RetStmt
        || stmt instanceof ReturnStmt
        || stmt instanceof ReturnVoidStmt
        || stmt instanceof ThrowStmt) {
      return transformReturnStmt(stmt);
    }

    if (stmt instanceof IdentityStmt
        || stmt instanceof NopStmt
        || stmt instanceof EnterMonitorStmt
        || stmt instanceof ExitMonitorStmt
        || stmt instanceof BreakpointStmt) {
      return transformIdentityStmt(stmt);
    }

    throw new RuntimeException(
        "transformStatement: unexpected statement: " + stmt + "@" + stmt.getClass());
  }

  private Transitions transformAssignStmt(AssignStmt stmt) {
    Value op1 = stmt.getLeftOp();
    Value op2 = stmt.getRightOp();

    Formula guard;
    if (op1 instanceof Local) {
      guard = assignRValue((Local) op1, op2);
    } else if (op1 instanceof StaticFieldRef) {
      guard = sizeAbstraction.putStaticField((StaticFieldRef) op1, (Immediate) op2);
    } else if (op1 instanceof InstanceFieldRef) {
      guard = sizeAbstraction.putInstanceField((InstanceFieldRef) op1, (Immediate) op2);
    } else if (op1 instanceof ArrayRef) {
      guard = sizeAbstraction.putArrayField((ArrayRef) op1, (Immediate) op2);
    } else {
      throw new RuntimeException("transformAssignStmt: unexpected stmt: "
          + op1 + "@" + op1.getClass() + " := " + op2 + "@" + op2.getClass());
    }

    return new Transitions(currentLabel(stmt), guard, fallthrougLabel());
  }

  private Formula assignUndefined(Local local) {
    if (hasRefType(local)) {
      return atom(ge(pvar(local), Val.zero));
    }
    return atom(as(pvar(local), transformUndefinedValue()));
  }

  private Formula assignRValue(Local local, Value rvalue) {
    if (rvalue instanceof Immediate) {
      return assignImmediate(local, (Immediate) rvalue);
    }
    if (rvalue instanceof Ref) {
      return assignRef(local, (Ref) rvalue);
    }
    if (rvalue instanceof Expr) {
      return assignExpr(local, (Expr) rvalue);
    }
    throw new RuntimeException("assignRValue: unexpected rvalue: "
        + local + "@" + local.getClass() + " := " + rvalue + "@" + rvalue.getClass());
  }

  private Formula assignImmediate(Local local, Immediate imm) {
    return atom(as(pvar(local), transformImmediate(imm)));
  }

  private Formula assignRef(Local local, Ref ref) {
    Var lhs = pvar(local);
    if (ref instanceof StaticFieldRef) {
      return sizeAbstraction.getStaticField(local, (StaticFieldRef) ref);
    }
    if (ref instanceof InstanceFieldRef) {
      return sizeAbstraction.getInstanceField(local, (InstanceFieldRef) ref);
    }
    if (ref instanceof ArrayRef) {
      return sizeAbstraction.getArrayField(local, (ArrayRef) ref);
    }
    throw new RuntimeException("assignRef: unexpected ref: "
        + local + "@" + local.getClass() + " := " + ref + "@" + ref.getClass());
  }

  private Formula assignExpr(Local local, Expr expr) {

    if (expr instanceof BinopExpr) {
      return assignBinopExpr(local, (BinopExpr) expr);
    }
    if (expr instanceof CastExpr) {
      return assignCastExpr(local, (CastExpr) expr);
    }
    if (expr instanceof InstanceOfExpr) {
      return assignInstanceOfExpr(local, (InstanceOfExpr) expr);
    }
    if (expr instanceof InvokeExpr) {
      return assignInvokeExpr(local, (InvokeExpr) expr);
    }
    if (expr instanceof AnyNewExpr) {
      return assignAnyNewExpr(local, (AnyNewExpr) expr);
    }
    if (expr instanceof LengthExpr) {
      return assignLengthExpr(local, (LengthExpr) expr);
    }
    if (expr instanceof NegExpr) {
      return assignNegExpr(local, (NegExpr) expr);
    }

    throw new RuntimeException("transformRValue: unexpected stmt: "
        + local + "@" + local.getClass() + " := " + expr + "@" + expr.getClass());
  }

  // Conditional operations are currently only addressed in ifgoto. The operations are 'native', the result is
  // either 0 or 1. If we want to support them in general we need support for DNF formula and specialize the guards
  // respectively, eg eg (a CmpExpr b) := a > b && res 0 || a <= b && res 1; or just return two transitions.
  public Formula assignBinopExpr(Local local, BinopExpr expr) {

    Var lhs = pvar(local);
    AExpr imm1 = transformImmediate((Immediate) expr.getOp1());
    AExpr imm2 = transformImmediate((Immediate) expr.getOp2());

    if (expr instanceof AddExpr) {
      return atom(as(lhs, new Add(imm1, imm2)));
    }
    if (expr instanceof MulExpr) {
      return atom(as(lhs, new Mul(imm1, imm2)));
    }
    if (expr instanceof SubExpr) {
      return atom(as(lhs, new Sub(imm1, imm2)));
    }

    // CmpExpr, CmpgExpr, CmplExpr  - comparison for long, float
    // ConditionExpr                - occur in ifgoto statements
    // DivExpr, RemExpr             - unsupported arith operations
    // AndExpr, OrExpr, XorExpr, ShlExpr, ShrExpr, UShrExpr - shiftoperations are undefined
    return assignUndefined(local);
  }

  // We ignore casts and conversions (unsound for narrowing conversions).
  // The JVM checkcast instruction may throw an ClassCastException - we ignore exceptional control flow. Widening
  // conversions do not modify the value. Narrowing conversions may modify the value, eg lont -> int
  public Formula assignCastExpr(Local local, CastExpr expr) {
    return assignRValue(local, expr.getOp());
  }

  // JVM instanceof returns 0 if expr is null, 1 if expr is instanced of resolved class, 0 otherwise.
  // Ignore, as the abstraction can not be refined wrt to runtime instance.
  public Formula assignInstanceOfExpr(Local local, InstanceOfExpr expr) {
    return assignUndefined(local);
  }

  public Formula assignAnyNewExpr(Local local, AnyNewExpr expr) {
    if (expr instanceof NewExpr) {
      return atom(as(pvar(local), Val.one()));
    }
    if (expr instanceof NewArrayExpr) {
      return atom(as(pvar(local), transformImmediate((Immediate) ((NewArrayExpr) expr).getSize())));
    }
    return assignUndefined(local); // NewMultiArrayExpr
  }

  public Formula assignLengthExpr(Local local, LengthExpr expr) {
    return atom(as(pvar(local), transformImmediate((Immediate) expr.getOp())),
        ge(pvar(local), Val.zero()));
  }

  public Formula assignNegExpr(Local local, NegExpr expr) {
    return atom(as(pvar(local), new Sub(Val.zero(), var(local))));
  }

  public Formula assignInvokeExpr(Local local, InvokeExpr expr) {
    return evalInvokeExpr(expr).and(as(pvar(local), this.rez));
  }

  public Formula evalInvokeExpr(InvokeExpr expr) {

    if (expr instanceof DynamicInvokeExpr) {
      throw new RuntimeException("unsupported expr: " + expr + "@" + expr.getClass());
    }
    if (expr instanceof InstanceInvokeExpr
        || expr instanceof StaticInvokeExpr) {
      SootMethodRef ref = expr.getMethodRef();
      Optional<MethodSummary> msumM = resolve(ref);
      MethodSummary msum;
      if (msumM.isPresent()) {
        msum = msumM.get();
      } else {
        msum = MethodSummary.defaultSummary();
      }
      return evalMethodSummary(expr, msum);
    }
    throw new RuntimeException("evalInvokeExpr: unexpected expr: " + expr + "@" + expr.getClass());
  }

  public Formula evalMethodSummary(InvokeExpr expr, MethodSummary msum) {
    // obtain necessary information of summary
    AExpr upper = msum.getUpperTimeWithDefault();
    AExpr lower = msum.getLowerTimeWithDefault();
    Formula effect = msum.getEffect();

    // substitute arg1,...,arg_n with imm_1,...,imm_n
    int i = 1;
    for (Value val : expr.getArgs()) {
      if (val instanceof Local) {
        effect = effect.substitute(new Var("arg" + i), var((Local) val));
      }
      i++;
    }

    // substitute this with base
    if (expr instanceof InstanceInvokeExpr) {
      Local base = (Local) ((InstanceInvokeExpr) expr).getBase();
      effect = effect.substitute(this.thiz, var(base));
    }

    return effect;
  }

  // Soot provides JVM method references for InvokeExpr.
  // The declaring class is a super type of the runtime instance and we assume that the corresponding summary is
  // a representation of all possible calls.
  // We ignore descriptors for now (unsound).
  private Optional<MethodSummary> resolve(SootMethodRef ref) {
    String cname = ref.declaringClass().getName();
    String mname = ref.name();
    String descr = Util.getMethodDescriptor(ref.resolve());
    return this.summaries.get(cname, mname, descr);
  }

  private Transitions transformGotoStmt(GotoStmt stmt) {
    Stmt target = (Stmt) stmt.getTarget();
    return new Transitions(currentLabel(stmt), targetLabel(target));
  }

  // We have  imm1 condop imm2
  private Transitions transformIfStmt(IfStmt stmt) {
    ConditionExpr condition = (ConditionExpr) stmt.getCondition();
    Stmt target = stmt.getTarget();

    Immediate op1 = (Immediate) condition.getOp1();
    Immediate op2 = (Immediate) condition.getOp2();
    AExpr imm1 = transformImmediate(op1);
    AExpr imm2 = transformImmediate(op2);

    Transitions ts = new Transitions();

    Label from = currentLabel(stmt);
    Label to = targetLabel(target);
    Label next = fallthrougLabel();

    // guards for arithmetic operations over integers as atomic constraint
    if (hasIntType(op1)) {
      if (condition instanceof GtExpr) {
        return ts
            .add(new Transition(from, atom(gt(imm1, imm2)), to))
            .add(new Transition(from, atom(le(imm1, imm2)), next));
      }
      if (condition instanceof GeExpr) {
        return ts
            .add(new Transition(from, atom(ge(imm1, imm2)), to))
            .add(new Transition(from, atom(lt(imm1, imm2)), next));
      }
      if (condition instanceof LeExpr) {
        return ts
            .add(new Transition(from, atom(le(imm1, imm2)), to))
            .add(new Transition(from, atom(gt(imm1, imm2)), next));
      }
      if (condition instanceof LtExpr) {
        return ts
            .add(new Transition(from, atom(lt(imm1, imm2)), to))
            .add(new Transition(from, atom(ge(imm1, imm2)), next));
      }
      if (condition instanceof EqExpr) {
        return ts
            .add(new Transition(from, atom(eq(imm1, imm2)), to))
            .add(new Transition(from, atom(gt(imm1, imm2))
                .or(lt(imm1, imm2)), next));
      }
      if (condition instanceof NeExpr) {
        return ts
            .add(new Transition(from, atom(gt(imm1, imm2))
                .or(lt(imm1, imm2)), to))
            .add(new Transition(from, atom(eq(imm1, imm2)), next));
      }
    }

    // guards for equality tests of references and null
    if (hasRefType(op1)) {
      if (condition instanceof EqExpr && op1 instanceof NullConstant) // null == ref
      {
        return ts
            .add(new Transition(from, atom(eq(imm1, imm2)), to))
            .add(new Transition(from, atom(lt(imm1, imm2)), next));
      }
      if (condition instanceof EqExpr && op2 instanceof NullConstant) // ref == null
      {
        return ts
            .add(new Transition(from, atom(eq(imm1, imm2)), to))
            .add(new Transition(from, atom(gt(imm1, imm2)), next));
      }
      if (condition instanceof EqExpr)                                // ref == ref
      {
        return ts
            .add(new Transition(from, atom(eq(imm1, imm2)),
                to))              // ref == ref => size(ref) == size(ref)
            .add(new Transition(from, atom(), next));         // ref != ref => undefined
      }

      if (condition instanceof NeExpr && op1 instanceof NullConstant) {
        return ts
            .add(new Transition(from, atom(lt(imm1, imm2)), to))
            .add(new Transition(from, atom(eq(imm1, imm2)), next));
      }
      if (condition instanceof NeExpr && op2 instanceof NullConstant) {
        return ts
            .add(new Transition(from, atom(gt(imm1, imm2)), to))
            .add(new Transition(from, atom(eq(imm1, imm2)), next));
      }
      if (condition instanceof NeExpr && op2 instanceof NullConstant) {
        return ts
            .add(new Transition(from, atom(), to))
            .add(new Transition(from, atom(eq(imm1, imm2)), next));
      }
    }

    throw new RuntimeException(
        "transformIfStmt: unexpected stmt: " + stmt + "@" + stmt.getClass() + ":" + condition + "@"
            + condition.getClass());

  }

  private Transitions transformInvokeStmt(InvokeStmt stmt) {
    Formula guard = evalInvokeExpr(stmt.getInvokeExpr());
    return new Transitions(currentLabel(stmt), guard, fallthrougLabel());
  }

  // Refinement on immediate variables are not really helpful, thus we model switch as non-deterministic jumps.
  // We model switch statements as non-deterministic jumps.
  private Transitions transformSwitchStmt(SwitchStmt stmt) {
    Unit def = stmt.getDefaultTarget();
    Transitions ts = new Transitions(currentLabel(stmt), targetLabel(def));
    for (Unit target : stmt.getTargets()) {
      ts.add(new Transition(currentLabel(stmt), targetLabel(target)));
    }
    return ts;
  }

  private Transitions transformReturnStmt(Stmt stmt) {
    return Transitions.empty();
  }

  private Transitions transformIdentityStmt(Stmt stmt) {
    if (hasDefinedLabel(stmt)) {
      return new Transitions(currentLabel(stmt), fallthrougLabel());
    } else {
      return Transitions.empty();
    }
  }

  private AExpr transformImmediate(Immediate imm) {
    if (imm instanceof Local) {
      return transformLocal((Local) imm);
    }
    if (imm instanceof Constant) {
      return transformConstant((Constant) imm);
    }

    throw new RuntimeException("transformImmediate: unexpected imm: " + imm + "@" + imm.getClass());
  }


  public AExpr transformLocal(Local local) {
    return var(local);
  }

  public AExpr transformConstant(Constant con) {
    if (con instanceof NullConstant) {
      return Val.zero();
    }
    if (con instanceof IntConstant) {
      return new Val(((IntConstant) con).value);
    }
    if (con instanceof LongConstant) {
      return new Val(((LongConstant) con).value);
    }
    // ClassConstant, MethodHandle, StringConstant, RealConstant
    return transformUndefinedValue();
  }

  public AExpr transformUndefinedValue() {
    return freshVar();
  }

// * Size Abstraction ------------------------------------------------------------------------------------------------

  // interface for size abstraction
  // * in Jimple getfield/putfield occur only in limited form (see Jimple grammar)
  // * for method calls we use summaries
  // * at the moment sharing and cyclicity is ignored
  // * general used assumptions
  //   a == null => size(a) = 0
  //   a != null => size(a) > 0
  //   a == b => size(a) == size(b)
  //   size(new A) = 1
  //   size(new A[imm]) = imm
  abstract class SizeAbstraction {

    abstract Formula putInstanceField(InstanceFieldRef ref, Immediate imm);

    abstract Formula getInstanceField(Local local, InstanceFieldRef ref);

    // static fields are like local variables

    Formula putStaticField(StaticFieldRef ref, Immediate imm) {
      return atom(as(pvar(ref), transformImmediate(imm)));
    }

    Formula getStaticField(Local local, StaticFieldRef ref) {
      return atom(as(pvar(local), var(ref)));
    }

    // length abstraction for array operations

    // x[imm1] = imm2; x is not modified
    Formula putArrayField(ArrayRef ref, Immediate imm) {
      return atom();
    }

    // x = y[imm]; x' is undefined
    Formula getArrayField(Local local, ArrayRef ref) {
      return assignUndefined(local);
    }
  }

  // see Frohn et al, WST 2013
  // reachable nodes + absolute value of integer fields
  final class NodeFieldsAbstraction extends SizeAbstraction {

    // x = y.f
    // case1: y.f is of type integer, then x' > -y /\ x' < y
    // case2: x.f is of type ref    , then x' >= 0 /\ x' < y
    // otherwise: x' is undefined
    Formula getInstanceField(Local local, InstanceFieldRef ref) {
      AExpr x = pvar(local);
      AExpr y = var(ref.getBase());
      return hasIntType(ref.getField())
          ? atom(gt(x, neg(y)), lt(x, y))
          : hasRefType(ref.getField())
              ? atom(nonnegative(x), lt(x, y))
              : assignUndefined(local);
    }

    // x.f = y
    // case1: y is of type integer, then x' > 0 /\ if y >= 0 then x' <= x + y else x' <= x + (-y)
    // case2: y is of type ref    , then x' > 0 /\ x' <= x + y
    // otherwise: x is not modified
    Formula putInstanceField(InstanceFieldRef ref, Immediate imm) {
      Local base = (Local) ref.getBase();
      Var ivar = var(base);
      Var ovar = pvar(base);
      AExpr val = transformImmediate(imm);
      return
          hasIntType(imm)
              ? atom(positive(ovar), positive(val), le(ovar, add(ivar, val)))
              .or(positive(ovar), negative(val), le(ovar, add(ivar, neg(val))))
              : hasRefType(imm)
                  ? atom(positive(ovar), le(ovar, add(ivar, val)))
                  : atom();
    }
  }

  // see F. Spoto et al, A Termination Analyzer for Java Bytecode Based on Path-Length, 2010
  // max (simple) path; fields are ignored
  final class SimplePathLengthAbstraction extends SizeAbstraction {

    // x.f = y
    // case1: y is of ref, then x' >= 0 /\ x' < y
    // otherwise: x' is undefined
    Formula getInstanceField(Local local, InstanceFieldRef ref) {
      AExpr x = pvar(local);
      AExpr y = var(ref.getBase());
      return hasRefType(ref.getField())
          ? atom(ge(x, Val.zero), lt(x, y))
          : assignUndefined(local);
    }

    // x.f = y
    // case`: y is of type ref    , then x' > 0 /\ x' <= x + y
    // otherwise: x is not modified
    Formula putInstanceField(InstanceFieldRef ref, Immediate imm) {
      Local base = (Local) ref.getBase();
      Var ivar = var(base);
      Var ovar = pvar(base);
      return hasRefType(imm)
          ? atom(gt(ovar, Val.zero), le(ovar, new Add(ivar, transformImmediate(imm))))
          : atom(gt(ovar, Val.zero), as(ovar, ivar));
    }
  }

}


