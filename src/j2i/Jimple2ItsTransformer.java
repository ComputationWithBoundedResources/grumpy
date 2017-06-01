package j2i;

import soot.Body;
import soot.BodyTransformer;
import soot.G;
import soot.jimple.JimpleBody;

import java.util.Map;


public final class Jimple2ItsTransformer extends BodyTransformer {

	@Override
	protected void internalTransform(Body body, String string, Map map) {
		GrimpBody2Its m = new GrimpBody2Its((JimpleBody) body);
		Its its = m.grimpBody2Its();
		G.v().out.println(m.body);
		G.v().out.println(its.ppKoAT());
	}

}
