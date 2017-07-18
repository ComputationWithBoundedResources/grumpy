package j2i;

import java.io.*;
import java.util.*;

public class KoATExecutor {

  private KoAT problem;
  private String[] options = new String[0];

  public KoATExecutor(KoAT problem) {
    this.problem = problem;
  }

  public KoATExecutor(KoAT problem, String... options) {
    this.problem = problem;
    this.options = options;
  }

  public String execute() {
    try {

      File tmp = File.createTempFile("method", ".koat");
      tmp.deleteOnExit();

      try (PrintWriter out = new PrintWriter(tmp.getAbsoluteFile())) {
        out.println(this.problem);
      }
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
        public void run() {
          try {
            String line;
            while ((line = error.readLine()) != null) {
              System.err.println(line);
            }
            error.close();
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
      });

      t1.start();
      t2.start();
      try {
        t1.join();
        t2.join();
        return h.getAnswer();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }

    } catch (IOException e) {
      e.printStackTrace();
    }
    return "MAYBE";
  }

  class Handler implements Runnable {

    BufferedReader input;
    private String answer;

    public Handler(BufferedReader input) {
      this.input = input;
    }

    public String getAnswer() {
      return this.answer != null ? this.answer : "MAYBE";
    }

    @Override
    public void run() {
      try {
        answer = input.readLine();
        input.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

}
