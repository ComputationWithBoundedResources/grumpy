package j2i;

import java.io.*;
import java.util.*;

public class KoAT{
  private Domain domain;
	private Transitions transitions;

	public KoAT(Domain domain, Transitions transitions){
		this.domain      = domain;
		this.transitions = transitions;
	}

	public StringBuilder domain2String(){
    StringBuilder b = new StringBuilder();
		b.append("(");
		Iterator<Var> it = this.domain.iterator();
		while(it.hasNext()) {
			Var v = it.next();
			b.append(v);
			if(it.hasNext()) b.append(", ");
		}
		b.append(")");
	  return b;
	}

	public StringBuilder postdomain2String(Formula guard){
    StringBuilder b = new StringBuilder();
		b.append("(");
		Iterator<Var> it = this.domain.iterator();
		while(it.hasNext()) {
			Var v = it.next();
			Var w = Var.newPostVar(v);
			if(guard.hasVar(w)) b.append(w);
			else                b.append(v);
			if(it.hasNext()) b.append(", ");
		}
		b.append(")");
	  return b;
	}

	@Override
	public String toString(){
		StringBuilder b = new StringBuilder();
		b.append("(GOAL COMPLEXITY)\n");
		if(this.transitions.isEmpty()){
			b.append("(STARTTERM (FUNCTIONSYMBOLS start))\n");
			b.append("start(x) -> end(x)\n");
	    return b.toString();
		}
		String s = this.transitions.iterator().next().getFrom().toString();
		b.append("(STARTTERM (FUNCTIONSYMBOLS " + s + "))\n");
		b.append("(VAR )\n");
		b.append("(RULES\n");
		if(this.transitions.isEmpty()) {
			b.append("start(x) -> end(x)\n");
		}

		StringBuilder lhs = this.domain2String();

		for(Transition t : transitions) {
			b.append(t.getFrom().toString());
			b.append(lhs);
			b.append(" -> ");
			b.append(t.getTo().toString());
			b.append(postdomain2String(t.getGuard()));
			b.append(" :|: ");
			b.append(t.getGuard().pp());
			b.append('\n');
		}
		b.append(")\n");
		return b.toString();
	}
}

class KoATExecutor {
	private KoAT problem;
	private String [] options = new String[0];

	public KoATExecutor(KoAT problem){ this.problem = problem; }
	public KoATExecutor(KoAT problem, String ... options) {
		this.problem = problem;
		this.options = options;
	}

	class Handler implements Runnable{
		BufferedReader input;
		private String answer;
		public String getAnswer(){ return this.answer != null ? this.answer : "MAYBE"; }

		public Handler(BufferedReader input){ this.input = input; }

		@Override
		public void run(){
			try {
				answer = input.readLine();
				input.close();
			} catch(IOException e) { e.printStackTrace(); }
		}
	}

	public String execute() {
		try {

			File tmp = File.createTempFile("method", ".koat");
			tmp.deleteOnExit();

			try(PrintWriter out = new PrintWriter(tmp.getAbsoluteFile())) { out.println(this.problem); }
			String[] command = new String[this.options.length + 2];
			command[0] = "koat";
			command[command.length - 1] = tmp.getAbsolutePath();
			System.arraycopy(this.options, 0, command, 1, this.options.length);
			Process koat = new ProcessBuilder(command).start();
			BufferedReader input = new BufferedReader(new InputStreamReader(koat.getInputStream()));
			BufferedReader error = new BufferedReader(new InputStreamReader(koat.getErrorStream()));

			Handler h = new Handler(input);
			Thread t1 = new Thread(h);

			Thread t2 = new Thread(new Runnable() {
				@Override
				public void run(){
					try {
						String line;
						while((line = error.readLine()) != null) {
							System.err.println(line);
						}
						error.close();
					} catch(IOException e) { e.printStackTrace(); }
				}
			});

			t1.start();
			t2.start();
			try {
				t1.join();
				t2.join();
				return h.getAnswer();
			} catch(InterruptedException e){ e.printStackTrace(); }

		} catch(IOException e) { e.printStackTrace(); }
		return "MAYBE";
	}

}

