
import j2i.*;

import java.util.*;

import soot.*;
import soot.jimple.*;
import soot.PackManager;
import soot.Transform;
import soot.options.Options;


// Example Usage of the 'Grumpy' library using a Soot BodyTransformer
// java -jar grumpy.jar -pp -cp examples/ Array
public class Main {

	public static void main(String[] margs) {
		Transform t = new Transform("jtp.grumpy", new RunGrumpy());
    t.setDeclaredOptions("enabled summaries-file");
		PackManager.v().getPack("jtp").add(t);

		Options.v().set_output_format(Options.output_format_none);
		soot.Main.main(margs);
	}

}

// Run Grumpy+Koat on Methodbody.
//
// Example Output:
// [Grumpy] >>> fi.iki.elonen.NanoHTTPD$Method.lookup(Ljava/lang/String;)Lfi/iki/elonen/NanoHTTPD$Method;  WORST_CASE(?, O(n^1))
final class RunGrumpy extends BodyTransformer {

  @Override
	protected void internalTransform(Body body, String string, Map opts) {

    Grumpy m = new Grumpy((JimpleBody) body, opts);
    String[] args =
      { "-timeout", "30"
      , "--smt-solver", "z3-internal"
      , "--use-its-parser"
      , "--use-termcomp-format"
      , "--no-print-proof" };

    KoAT its = m.jimpleBody2KoAT();
		String answer = new KoATExecutor(its,args).execute();

    // G.v().out.println("[Grumpy] " + Util.getSignature(body.getMethod()) + " >>> " + answer);

    G.v().out.println("[Grumpy] >>> " + Util.getSignature(body.getMethod()));
		// G.v().out.println("[Grumpy] \n" + its.pp());
    G.v().out.println("[Grumpy] <<< " + answer);
  }

}

// Print Its Only
final class Jimple2ItsTransformer extends BodyTransformer {

	@Override
	protected void internalTransform(Body body, String string, Map map) {
		Grumpy m = new Grumpy((JimpleBody) body);
		// G.v().out.println(body);
		G.v().out.println(m.jimpleBody2KoAT().pp());
	}
}

//
final class WithKoAT extends BodyTransformer {

	@Override
	protected void internalTransform(Body body, String string, Map map) {
		Grumpy m = new Grumpy((JimpleBody) body);
		KoAT its = m.jimpleBody2KoAT();

    String answer;
    String[] args = {"-timeout", "30", "--use-its-parser", "--use-termcomp-format","--no-print-proof"};

		G.v().out.println("*** Jimple");
		G.v().out.println(body);
		G.v().out.println("*** Its");
		G.v().out.println(its.pp());
		G.v().out.println("...");
		answer = new KoATExecutor(its,args).execute();
		G.v().out.println(">>> " + answer + "\n");
		G.v().out.println("*** Its (compact)");
    its = its.compact();
		G.v().out.println(its.pp());
		G.v().out.println("...");
		answer = new KoATExecutor(its,args).execute();
		G.v().out.println(">>> " + answer + "\n");
		G.v().out.println("---");
	}
}

