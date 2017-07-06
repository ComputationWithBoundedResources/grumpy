package j2i;

import soot.Body;
import soot.BodyTransformer;
import soot.G;
import soot.jimple.JimpleBody;

import java.io.*;
import java.util.*;


/* This module provides a KoAT wrapper. */
public final class WithKoAT extends BodyTransformer {


	@Override
	protected void internalTransform(Body body, String string, Map map) {
		Grumpy m = new Grumpy((JimpleBody) body);
		KoAT its   = m.jimpleBody2KoAT();

		G.v().out.println(">>> Jimple");
		G.v().out.println(m.body);
		G.v().out.println(">>> Its");
		G.v().out.println(its);

		G.v().out.println("...");
		this.process(its);
		G.v().out.println("---");

	}

	public void process(KoAT its) {
		try {
			File tmp = File.createTempFile("method", ".koat");
			tmp.deleteOnExit();
			try(PrintWriter out = new PrintWriter(tmp.getAbsoluteFile())) {
				out.println(its);
			}
			Process koat = new ProcessBuilder("koat", tmp.getAbsolutePath()).start();
			BufferedReader input = new BufferedReader(new InputStreamReader(koat.getInputStream()));
			BufferedReader error = new BufferedReader(new InputStreamReader(koat.getErrorStream()));

			Thread t1 = new Thread(new Runnable(){
				@Override
				public void run(){
					try {
						String answer = input.readLine();
						input.close();
						G.v().out.println(Optional.ofNullable(answer).orElse("Maybe"));
					} catch(IOException e) {
						e.printStackTrace();
					}
				}
			});

			Thread t2 = new Thread(new Runnable() {
				@Override
				public void run(){
					try {
						String line;
						while((line = error.readLine()) != null) {
							G.v().out.println(line);
						}
						error.close();
					} catch(IOException e) {
						e.printStackTrace();
					}
				}
			});

			t1.start();
			t2.start();
			try {
				t1.join();
				t2.join();
			} catch(InterruptedException e){
						e.printStackTrace();
			}

		} catch(IOException e) {
			e.printStackTrace();
		}
	}

}

