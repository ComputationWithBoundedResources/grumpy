package j2i;


import soot.SootMethod;


public class Util {

  // returns signature of a method <Class>:<Method><Descriptor>
  public static String getSignature(SootMethod m) {
    String nearlySig = m.getBytecodeSignature();
    nearlySig = nearlySig.substring(1, nearlySig.length() - 1);
    String[] sigSegs = nearlySig.split(":");
    nearlySig = sigSegs[0] + "." + sigSegs[1].substring(1);
    return nearlySig;
  }

  // retunrs JVM Method descriptor of a method
  public static String getMethodDescriptor(SootMethod m) {
    String s = m.getBytecodeSignature();
    s = s.substring(1, s.length() - 1);
    String[] ss = s.split("\\(");
    return "(" + ss[1];
  }

}


