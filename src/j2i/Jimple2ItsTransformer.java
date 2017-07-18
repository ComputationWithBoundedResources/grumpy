package j2i;

import java.util.Map;
import soot.Body;
import soot.BodyTransformer;
import soot.G;
import soot.jimple.JimpleBody;


public final class Jimple2ItsTransformer extends BodyTransformer {

  @Override
  protected void internalTransform(Body body, String string, Map map) {
    Grumpy m = new Grumpy((JimpleBody) body);
    G.v().out.println(m.body);
    G.v().out.println(m.jimpleBody2KoAT());
    // Transitions its = m.grimpBody2Its();
    // G.v().out.println(its.ppKoAt());
    // G.v().out.println(m.body);
    // G.v().out.println(its.ppKoAT());
  }

}
