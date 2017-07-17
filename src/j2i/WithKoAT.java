package j2i;

import soot.Body;
import soot.BodyTransformer;
import soot.G;
import soot.jimple.JimpleBody;

import java.io.*;
import java.util.*;


// This module provides a SOOT BodyTransformer invoking KoAT on the generated Problem
public final class WithKoAT extends BodyTransformer {

	@Override
	protected void internalTransform(Body body, String string, Map map) {
		Grumpy m = new Grumpy((JimpleBody) body);
		KoAT its   = m.jimpleBody2KoAT();
		KoAT its1   = m.jimpleBody2KoAT();
		KoATExecutor exe = new KoATExecutor(its, "-timeout", "30" , "-use-its-parser", "-use-termcomp-format");

		G.v().out.println(">>> Jimple");
		G.v().out.println(m.body);
		G.v().out.println(">>> Its");
		// G.v().out.println(its);

		KoAT its2   = m.jimpleBody2KoAT2();
		G.v().out.println(its1 + "\n>>>\n" + its2);

		G.v().out.println("...");
		String answer = exe.execute();
		G.v().out.println(answer);
		G.v().out.println("---");


	}

}

