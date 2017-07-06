package j2i;

import j2i.label.Label;

import java.util.*;


public class Transitions implements Iterable<Transition> {
	protected List<Transition> transitions = new LinkedList();

	public Transitions()                                    { }
	public Transitions(Transition t)                        { this.transitions.add(t); }
	public Transitions(Label from, Label to)                { this.transitions.add(new Transition(from,to)); }
	public Transitions(Label from, Formula guard, Label to) { this.transitions.add(new Transition(from, guard, to)); }
	public static Transitions empty()     { return new Transitions(); }

	public Transitions add(Transition t)  { this.transitions.add(t); return this;}
	public Transitions add(Transitions t) { this.transitions.addAll(t.transitions); return this; }
	public boolean isEmpty()              { return this.transitions.isEmpty(); }

	@Override
	public Iterator<Transition> iterator(){ return this.transitions.iterator(); }

}

class Transition {
	protected Label from;
	protected Formula guard = Formula.empty();
	protected Label to;
	protected AExpr lower = Val.one();
	protected AExpr upper = Val.one();

	public Transition(Label from, Label to){
		this.from  = from;
		this.to    = to;
	}

	public Transition(Label from, Formula guard, Label to){
		this.from  = from;
		this.guard = guard;
		this.to    = to;
	}

	public Transition(Label from, Formula guard, AExpr lower, AExpr upper, Label to){
		this.from  = from;
		this.guard = guard;
		this.to    = to;
		this.lower = lower;
		this.upper = upper;
	}

}

class KoAT{
  private Domain domain;
	private Transitions transitions;

	public KoAT(Domain domain, Transitions transitions){
		this.domain      = domain;
		this.transitions = transitions;
	}

	public StringBuilder domain2String(){
    StringBuilder b = new StringBuilder();
		b.append("(");
		Iterator<Var> it = this.domain.iterator();
		while(it.hasNext()) {
			Var v = it.next();
			b.append(v);
			if(it.hasNext()) b.append(", ");
		}
		b.append(")");
	  return b;
	}

	public StringBuilder postdomain2String(Formula guard){
    StringBuilder b = new StringBuilder();
		b.append("(");
		Iterator<Var> it = this.domain.iterator();
		while(it.hasNext()) {
			Var v = it.next();
			Var w = v.asPostVar();
			if(guard.hasVar(w)) b.append(w);
			else                b.append(v);
			if(it.hasNext()) b.append(", ");
		}
		b.append(")");
	  return b;
	}

	@Override
	public String toString(){
		StringBuilder b = new StringBuilder();
		b.append("(GOAL COMPLEXITY)\n");
		if(this.transitions.isEmpty()){
			b.append("(STARTTERM (FUNCTIONSYMBOLS start))\n");
			b.append("start(x) -> end(x)\n");
	    return b.toString();
		}
		String s = this.transitions.iterator().next().from.toString();
		b.append("(STARTTERM (FUNCTIONSYMBOLS " + s + "))\n");
		b.append("(VAR )\n");
		b.append("(RULES\n");
		if(this.transitions.isEmpty()) {
			b.append("start(x) -> end(x)\n");
		}

		StringBuilder lhs = this.domain2String();

		for(Transition t : transitions) {
			b.append(t.from.toString());
			b.append(lhs);
			b.append(" -> ");
			b.append(t.to.toString());
			b.append(postdomain2String(t.guard));
			b.append(" :|: ");
			b.append(t.guard.pp());
			b.append('\n');
		}
		b.append(")\n");
		return b.toString();
	}

}
