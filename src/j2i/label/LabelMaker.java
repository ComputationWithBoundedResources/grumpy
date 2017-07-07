package j2i.label;

import java.util.*;

import soot.*;
import soot.jimple.JimpleBody;


// In Grimp only targets of jump instructions are labelled, here referred to as 'defined' labels. To generate labels
// for the ITS we introduce an 'instruction'. LabelMaker should be unique per transformation.

// Following properties hold:
// label1 = nextLabel(); label2 = currentLabel(undefinedStmt); label1 == label2;
// label1 = nextLabel(); label2 = currentLabel(definedStmt); label1 == label2;
// These properties are used to correctly mimic control-flow of defined and undefined statements.
final public class LabelMaker {
	private Map<Unit, String> definedLabels;
	private Label label = new Label(fromInt(0));
	private int ic = 0;


	private static String fromInt(int i){ return "marke" + i; }
	private Label fresh()               { return new Label(fromInt(ic++)); }


	public LabelMaker(JimpleBody body)  { this.definedLabels =  new NormalUnitPrinter(body).labels(); }

	public boolean hasDefinedLabel(Unit stmt){ return this.definedLabels.containsKey(stmt); }

	public Label currentLabel(Unit stmt){
		String label = this.definedLabels.get(stmt);
		if(label != null) {
			this.label.name    = label;
			this.label.defined = true;
		}
		return this.label;
	}

	public Label targetLabel(Unit stmt){
		String label = this.definedLabels.get(stmt);
		if(label != null) {
		  this.label = fresh();
			return new Label(label, true);
		} else throw new RuntimeException("targetLabel: unlabelled target: " + stmt);
	}

	public Label fallthroughLabel(){
		this.label = fresh();
		return this.label;
	}
}
