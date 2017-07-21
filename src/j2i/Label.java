package j2i;

import java.util.*;
import soot.*;
import soot.jimple.JimpleBody;

// Labels for Transitions.
//
// In Grimp targets of jump instructions are labelled, here called 'defined' labels.
// We keep track of defined labels, and use them as fixed control-flow points.
final class Label {

  protected String name;
  protected boolean defined = false;


  protected Label(String name) {
    this.name = name;
  }

  protected Label(String name, boolean defined) {
    this.name = name;
    this.defined = defined;
  }

  @Override
  public String toString() {
    return this.name;
  }

  @Override
  public int hashCode() {
    int result = 17;
    result = 31 * result + (name != null ? name.hashCode() : 0);
    result = 31 * result + (defined ? 0 : 1);
    return result;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Label object = (Label) o;

    if (name != null ? !name.equals(object.name) : object.name != null) {
      return false;
    }
    return !(defined != object.defined);
  }

  public boolean isDefined() {
    return this.defined;
  }
}

// To generate labels for the ITS we introduce an 'instruction' counter.

// Following properties hold:
// label1 = nextLabel(); label2 = currentLabel(undefinedStmt); label1 == label2;
// label1 = nextLabel(); label2 = currentLabel(definedStmt); label1 == label2;
// These properties are used to correctly mimic control-flow of defined and undefined statements.
final class LabelMaker {

  private Map<Unit, String> definedLabels;
  private Label label = new Label(fromInt(0));
  private int ic = 0;


  public LabelMaker(JimpleBody body) {
    this.definedLabels = new NormalUnitPrinter(body).labels();
  }

  private static String fromInt(int i) {
    return "marke" + i;
  }

  private Label fresh() {
    return new Label(fromInt(++ic));
  }

  public boolean hasDefinedLabel(Unit stmt) {
    return this.definedLabels.containsKey(stmt);
  }

  public Label currentLabel(Unit stmt) {
    String label = this.definedLabels.get(stmt);
    if (label != null) {
      this.label.name = label;
      this.label.defined = true;
    }
    return this.label;
  }

  public Label targetLabel(Unit stmt) {
    String label = this.definedLabels.get(stmt);
    if (label != null) {
      this.label = fresh();
      return new Label(label, true);
    } else {
      throw new RuntimeException("targetLabel: unlabelled target: " + stmt);
    }
  }

  public Label fallthroughLabel() {
    this.label = fresh();
    return this.label;
  }
}

