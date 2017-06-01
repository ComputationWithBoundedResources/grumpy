
import j2i.*;
import soot.PackManager;
import soot.Transform;
import soot.options.Options;


public class Main {

	public static void main(String[] margs) {

		Transform t = new Transform("jtp.Jbc2Its", new Jimple2ItsTransformer());
		// Transform t = new Transform("jtp.Jbc2Its", new WithKoAT());
		PackManager.v().getPack("jtp").add(t);
		// Options.v().set_whole_program(true);
		Options.v().set_output_format(Options.output_format_none);

		//String[] args = {
		//		"-pp",
		//		"-cp", ".",
		//		// "--process-dir"	, "examples",
		//		// "ForLoop"
		//		"M"
		//};
		//String [] args = {
		//		"-pp"
		//		"-cp"

		// };
		soot.Main.main(margs);
	}

}

