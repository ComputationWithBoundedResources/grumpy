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

