package j2i;

import soot.Body;
import soot.BodyTransformer;
import soot.G;
import soot.jimple.JimpleBody;

import java.io.*;
import java.util.Map;


/* This module provides a KoAT */
public final class WithKoAT extends BodyTransformer {


	@Override
	protected void internalTransform(Body body, String string, Map map) {
		GrimpBody2Its m = new GrimpBody2Its((JimpleBody) body);
		Its its = m.grimpBody2Its();
		this.process(its);
	}

	public void process(Its its) {
		try {
			File tmp = File.createTempFile("method", ".koat");
			tmp.deleteOnExit();
			G.v().out.println(its.ppKoAT());
			try(PrintWriter out = new PrintWriter(tmp.getAbsoluteFile())) {
				out.println(its.ppKoAT());
			}
			Process koat = new ProcessBuilder("koat", tmp.getAbsolutePath()).start();
			BufferedReader input = new BufferedReader(new InputStreamReader(koat.getInputStream()));
			BufferedReader error = new BufferedReader(new InputStreamReader(koat.getErrorStream()));

			new Thread(() -> {
				try {
					String answer = input.readLine();
					if(answer != null) G.v().out.println(answer);
					input.close();
				} catch(IOException e) {
					e.printStackTrace();
				}
			}).start();

			new Thread(() -> {
				try {
					String line;
					while((line = error.readLine()) != null) {
						G.v().out.println(line);
					}
					error.close();
				} catch(IOException e) {
					e.printStackTrace();
				}
			}).start();

		} catch(IOException e) {
			e.printStackTrace();
		}
	}

}

