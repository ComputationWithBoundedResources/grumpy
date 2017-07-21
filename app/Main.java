

import j2i.*;

import java.util.*;

import soot.*;
import soot.jimple.*;
import soot.PackManager;
import soot.Transform;
import soot.options.Options;


public class Main {

	public static void main(String[] margs) {

		// Transform t = new Transform("jtp.Jbc2Its", new Jimple2ItsTransformer());
		Transform t = new Transform("jtp.Jbc2Its", new WithKoAT());
		PackManager.v().getPack("jtp").add(t);
		Options.v().set_output_format(Options.output_format_none);
		//
		//// Options.v().set_whole_program(true);

		////String[] args = {
		////		"-pp",
		////		"-cp", ".",
		////		// "--process-dir"	, "examples",
		////		// "ForLoop"
		////		"M"
		////};
		////String [] args = {
		////		"-pp"
		////		"-cp"

		//// };
		soot.Main.main(margs);

  // MethodSummaries summaries = MethodSummaries.fromFile("/tmp/summaries.json");
	// System.out.println(summaries);

	}

}



final class Jimple2ItsTransformer extends BodyTransformer {

	@Override
	protected void internalTransform(Body body, String string, Map map) {
		Grumpy m = new Grumpy((JimpleBody) body);
		G.v().out.println(body);
		G.v().out.println(m.jimpleBody2KoAT());
		// Transitions its = m.grimpBody2Its();
		// G.v().out.println(its.ppKoAt());
		// G.v().out.println(m.body);
		// G.v().out.println(its.ppKoAT());
	}

}


final class WithKoAT extends BodyTransformer {

	@Override
	protected void internalTransform(Body body, String string, Map map) {
		Grumpy m = new Grumpy((JimpleBody) body);
		KoAT its = m.jimpleBody2KoAT();
    KoATExecutor exe = new KoATExecutor(its, "-timeout", "30", "-use-its-parser", "-use-termcomp-format");

		G.v().out.println("*** Jimple");
		G.v().out.println(body);
		G.v().out.println("*** Its");
		G.v().out.println(its.pp());
		G.v().out.println("...");
		String answer = exe.execute();
		G.v().out.println(answer);
		G.v().out.println("*** Its (compact)");
    its = its.compact();
		G.v().out.println(its.pp());
    exe = new KoATExecutor(its, "-timeout", "30", "-use-its-parser", "-use-termcomp-format");
		answer = exe.execute();
		G.v().out.println(answer);
		G.v().out.println("---");
	}
}
