package j2i;


import soot.*;
import soot.grimp.Grimp;
import soot.grimp.GrimpBody;
import soot.grimp.toolkits.base.ConstructorFolder;
import soot.jimple.*;
import soot.jimple.toolkits.base.Aggregator;
import soot.toolkits.scalar.UnusedLocalEliminator;

import java.util.*;


class LStmt {
	String label;
	Stmt stmt;

	public LStmt(String label, Stmt stmt) {
		this.label = label;
		this.stmt = stmt;
	}
}

/* This package provides a simple transformation from Java bytecode methods to integer transition systems.
 *
 */
public class GrimpBody2Its {
	private final Its its = new Its();
	public GrimpBody body;
	private Map<Unit, String> definedLabels;
	private List<Value> activeLocals;
	private List<SootField> activeFields;
	private List<LStmt> activeLStmts;
	private int varId = 0;
	private int labelId = 0;


	// initialisaion {{{ //
	public GrimpBody2Its(JimpleBody jimple) {
		this.body = fromJimpleBody(jimple);

		this.definedLabels = initDefinedLabels(body);
		this.activeLocals = initActiveLocals(body);
		this.activeFields = initActiveFields(body);
		this.activeLStmts = initActiveLStmts(body);
		// G.v().out.println(this.definedLabels);
		// G.v().out.println(this.activeLocals);
		// G.v().out.println(this.activeFields);
	}

	/*
	Grimp optimisation eliminate intermediary stack operations and aggregate three-address-code to
	expressions. Per default Grimp specific phases are only applied if the output format is Grimp.
	Hence we apply the (default) transformations and optimisations explicitly.
	*/
	public static GrimpBody fromJimpleBody(JimpleBody body) {
		GrimpBody m = Grimp.v().newBody(body, null);
		Map<String, String> opts = new HashMap<>();
		opts.put("enabled", "true");
		opts.put("only-stack-locals", "false");
		Aggregator.v().transform(body, null);
		ConstructorFolder.v().transform(m, null, opts);
		Aggregator.v().transform(m, null);
		UnusedLocalEliminator.v().transform(m, null, opts);
		return m;
	}

	private static boolean isPrimitive(final SootField field) {
		return Modifier.isStatic(field.getModifiers()) && isPrimitive(field.getType());
	}

	private static boolean isPrimitive(final Type type) {
		return
				type == ShortType.v()
						|| type == IntType.v()
						|| type == LongType.v();
	}

	private static boolean isPrimitive(final Value val) {
		return isPrimitive(val.getType());
	}


	// private Chain<SootField> getFields(GrimpBody boyd){
	// 	return body.getMethod().getDeclaringClass().getFields();
	// }

	private Map<Unit, String> initDefinedLabels(GrimpBody body) {
		return new NormalUnitPrinter(body).labels();
	}
	// }}} initialisaion //

	// restrict to int-like local variables
	private List<Value> initActiveLocals(GrimpBody body) {
		List<Value> locals = new ArrayList<>();
		for(Local local : body.getLocals()) {
			if(isPrimitive(local)) locals.add(local);
		}
		return locals;
	}

	private List<SootField> initActiveFields(GrimpBody body) {
		List<SootField> fields = new ArrayList<>();
		for(SootField field : body.getMethod().getDeclaringClass().getFields()) {
			if(isPrimitive(field)) fields.add(field);
		}
		return fields;
	}

	private List<LStmt> initActiveLStmts(GrimpBody body) {
		List<LStmt> stmts = new ArrayList<>();
		for(Unit stmt : body.getUnits()) {
			if(this.isActiveStmt(stmt)) {
				String label;
				if(definedLabels.containsKey(stmt))
					label = definedLabels.get(stmt);
				else
					label = this.freshLabel();
				stmts.add(new LStmt(label, (Stmt) stmt));
			}
		}
		stmts.add(new LStmt("halt", Grimp.v().newNopStmt())); // dedicated halting state
		return stmts;
	}

	private boolean isActiveLocal(final Value val) {
		return this.activeLocals.contains(val);
	}

	private boolean isActiveField(final Value val) {
		if(val instanceof StaticFieldRef) {
			return this.activeFields.contains(((StaticFieldRef) val).getField());
		} else {
			return false;
		}
	}

	private Local freshLocal() {
		return Grimp.v().newLocal("fresh_" + varId++, IntType.v());
	}

	private String freshLabel() {
		return "label_" + labelId++;
	}

	private boolean isActiveStmt(final Unit stmt) {
		return
				(stmt instanceof AssignStmt && this.isActiveLocal(((AssignStmt) stmt).getLeftOp()))
						// || ( stmt instanceof AssignStmt && this.isActiveField( ((AssignStmt) stmt).getLeftOp() ) )
						|| stmt instanceof GotoStmt
						|| stmt instanceof IfStmt
						|| stmt instanceof LookupSwitchStmt
						|| stmt instanceof TableSwitchStmt
						|| stmt instanceof ThrowStmt
						|| this.definedLabels.containsKey(stmt);
	}


	// transformation {{{ //
	public Its grimpBody2Its() {
		for(int i = 0, j = 1; j < this.activeLStmts.size(); ++i, ++j) {
			LStmt stmt = this.activeLStmts.get(i);
			LStmt next = this.activeLStmts.get(j);
			List<Rule> rules = this.transformStmt(stmt, next);
			if(rules.isEmpty()) throw new RuntimeException("*** No rules !! ***\n" + stmt + "->" + next);
			this.its.addAll(rules);
		}
		return this.its;
	}


	/* we transform each statement individually

    Immediate -> Local | Constant <br>
    RValue -> Local | Constant | ConcreteRef | Expr<br>
    Variable -> Local | ArrayRef | InstanceFieldRef | StaticFieldRef <br>
	*/
	private List<Rule> transformStmt(final LStmt lstmt, final LStmt next) {
		// the cases to consider; see isActiveStmt
		final Stmt stmt = lstmt.stmt;
		if(stmt instanceof AssignStmt && this.isActiveLocal(((AssignStmt) stmt).getLeftOp())) {
			// Variable = RValue
			Value var = ((AssignStmt) stmt).getLeftOp();
			Value val = ((AssignStmt) stmt).getRightOp();
			return
					Arrays.asList(newAssignment(lstmt, next, var, val));
		} else if(stmt instanceof AssignStmt && this.isActiveField(((AssignStmt) stmt).getLeftOp())) {
			Value var = ((AssignStmt) stmt).getLeftOp();
			Value val = ((AssignStmt) stmt).getRightOp();
			return
					Arrays.asList(newAssignment(lstmt, next, var, val));

		} else if(stmt instanceof GotoStmt) {
			Stmt target = (Stmt) ((GotoStmt) stmt).getTarget();
			return Arrays.asList
					(newRule(lstmt, labelStatement(target)));
		} else if(stmt instanceof IfStmt) {
			ConditionExpr condition = (ConditionExpr) ((IfStmt) stmt).getCondition();
			Stmt target = ((IfStmt) stmt).getTarget();

			Value op1 = condition.getOp1();
			Value op2 = condition.getOp2();
			Expr expr1 = transformExpression(op1);
			Expr expr2 = transformExpression(op2);

			if(condition instanceof EqExpr) {
				return Arrays.asList
						(newRule(lstmt, labelStatement(target), expr1, BRel.Eq, expr2)
								, newRule(lstmt, next, expr1, BRel.Gt, expr2)
								, newRule(lstmt, next, expr1, BRel.Lt, expr2));
			} else if(condition instanceof NeExpr) {
				return Arrays.asList
						(newRule(lstmt, labelStatement(target), expr1, BRel.Gt, expr2)
								, newRule(lstmt, labelStatement(target), expr1, BRel.Lt, expr2)
								, newRule(lstmt, next, expr1, BRel.Eq, expr2));
			} else if(condition instanceof GeExpr) {
				return Arrays.asList
						(newRule(lstmt, labelStatement(target), expr1, BRel.Ge, expr2)
								, newRule(lstmt, next, expr1, BRel.Lt, expr2));
			} else if(condition instanceof GtExpr) {
				return Arrays.asList
						(newRule(lstmt, labelStatement(target), expr1, BRel.Gt, expr2)
								, newRule(lstmt, next, expr1, BRel.Le, expr2));
			} else if(condition instanceof LeExpr) {
				return Arrays.asList
						(newRule(lstmt, labelStatement(target), expr1, BRel.Le, expr2)
								, newRule(lstmt, next, expr1, BRel.Gt, expr2));
			} else if(condition instanceof LtExpr) {
				return Arrays.asList
						(newRule(lstmt, labelStatement(target), expr1, BRel.Lt, expr2)
								, newRule(lstmt, next, expr1, BRel.Ge, expr2));
			} else {
				throw new RuntimeException("Unexpected ConditonExpr: " + stmt);
			}
		} else if(stmt instanceof LookupSwitchStmt) {
			return Arrays.asList
					(newRule(lstmt, lstmt)); // TODO:
		} else if(stmt instanceof TableSwitchStmt) {
			return Arrays.asList
					(newRule(lstmt, lstmt)); // TODO
		} else if(stmt instanceof ThrowStmt) {
			// exceptional control-flow is considered to be constant
			return Arrays.asList
					(newRule(lstmt, new LStmt("error", Grimp.v().newNopStmt())));
		} else {
			if(this.definedLabels.containsKey(stmt)) {
				// labeled rules that are not covered by the above cases are considered as no-op
				return Arrays.asList
						(newRule(lstmt, next));
			} else {
				throw new RuntimeException("Unexpected Statement: " + stmt);
			}

		}
	}


	// transforms (supported) arithmetic expressions
	// other expressions return a fresh variable
	// conditional expressions of ifgoto stmts are treated separately
	private Expr transformExpression(final Value expr) {

		if(expr instanceof Local && this.isActiveLocal(expr)) {
			return newVar((Local) expr);
		} else if(expr instanceof StaticFieldRef && this.isActiveField(expr)) {
			return newVar((StaticFieldRef) expr);
		} else if(expr instanceof IntConstant) {
			return newVal((IntConstant) expr);
		} else if(expr instanceof LongConstant) {
			return newVal((LongConstant) expr);
		} else if(expr instanceof BinopExpr) {
			Value op1 = ((BinopExpr) expr).getOp1();
			Value op2 = ((BinopExpr) expr).getOp2();
			Expr expr1 = transformExpression(op1);
			Expr expr2 = transformExpression(op2);
			if(expr instanceof AddExpr) {
				return new AExpr(expr1, BAOp.Add, expr2);
			} else if(expr instanceof MulExpr) {
				return new AExpr(expr1, BAOp.Mul, expr2);
			} else if(expr instanceof SubExpr) {
				return new AExpr(expr1, BAOp.Add, new UnaryMinus(expr2));
			}
		}
		return newVar(freshLocal());
	}

	private Var newVar(final Value local) {
		if(local instanceof Local)
			return new Var(local.toString());
		else if(local instanceof StaticFieldRef)
			return newVar((StaticFieldRef) local);
		else
			throw new RuntimeException("Unexpected variable: " + local);
	}

	private Var newVar(final Local local) {
		return new Var(local.toString());
	}

	private Var newVar(final StaticFieldRef ref) {
		return newVar(ref.getField());
	}

	private Var newVar(final SootField field) {
		return new Var(field.getDeclaringClass().getName() + "x" + field.getName());
	}

	private LStmt labelStatement(Stmt stmt) {
		return new LStmt(this.definedLabels.get(stmt), stmt);
	}

	private Expr newVal(LongConstant val) {
		return new Val(val.value);
	}

	private Expr newVal(IntConstant val) {
		return new Val(val.value);
	}

	private Term newTerm(final LStmt stmt) {
		List<Expr> args = new ArrayList<>();
		for(Value l : this.activeLocals) {
			args.add(newVar(l));
		}
		for(SootField l : this.activeFields) {
			args.add(newVar(l));
		}
		return new Term(new Symbol(stmt.label), args);
	}

	private Rule newAssignment(final LStmt stmt, final LStmt next, final Value local, final Value expr) {
		Term lhs = newTerm(stmt);
		Term rhs = newTerm(next);
		Expr v = newVar(local);
		Expr aexpr = transformExpression(expr);
		List<Expr> args = rhs.getArgs();
		args.replaceAll(e -> e.equals(v) ? aexpr : e);
		return new Rule(lhs, rhs);
	}

	private Rule newRule(final LStmt stmt, final LStmt next) {
		Term lhs = newTerm(stmt);
		Term rhs = newTerm(next);
		return new Rule(lhs, rhs);
	}

	private Rule newRule(final LStmt stmt, final LStmt next, Expr expr1, BRel rel, Expr expr2) {
		Term lhs = newTerm(stmt);
		Term rhs = newTerm(next);
		return new Rule(lhs, rhs, new Constraint(expr1, rel, expr2));
	}
	// }}} transformation //

}

