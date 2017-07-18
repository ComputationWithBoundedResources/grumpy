package j2i.label;

// Labels for Transitions.
//
// In Grimp targets of jump instructions are labelled, here called 'defined' labels.
// We keep track of defined labels.
final public class Label {

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

