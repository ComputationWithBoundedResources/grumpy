package j2i;

import java.util.*;
import soot.*;
import soot.jimple.*;

final class Domain implements Iterable<Var> {

  Set<Var> elements = new LinkedHashSet<>();

  public void addLocals(JimpleBody body) {
    for (Local local : body.getLocals()) {
      this.elements.add(Grumpy.var(local));
    }
    // if(Grumpy.isPrimitive(local)) this.elements.add( Grumpy.var(local) );
  }

  public void add(Var v) {
    this.elements.add(v);
  }

  public void addFields(JimpleBody body) {
    for (SootField field : body.getMethod().getDeclaringClass().getFields()) {
      if (Grumpy.isPrimitive(field)) {
        this.elements.add(Grumpy.var(field));
      }
    }
  }

  public boolean hasElem(Var var) {
    return this.elements.contains(var);
  }

  @Override
  public Iterator<Var> iterator() {
    return this.elements.iterator();
  }

  @Override
  public String toString() {
    return "Domain{" + "elements = " + elements + "}";
  }
}
