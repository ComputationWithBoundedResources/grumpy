package j2i;


import soot.SootMethod;


interface PrettyPrint {

  String pp();
}

final class FreshSupply {

  int id = 0;
  String prefix;

  FreshSupply(String prefix) {
    this.prefix = prefix;
  }

  String fresh() {
    return this.prefix + id++;
  }

  Var freshVar() {
    return new Var(this.fresh());
  }

  @Override
  public java.lang.String toString() {
    return "FreshSupply{" + "id=" + id + ", prefix='" + prefix + '\'' + '}';
  }
}

final class Util {

  // returns signature of a method <Class>:<Method><Descriptor>
  static String getSignature(SootMethod m) {
    String nearlySig = m.getBytecodeSignature();
    nearlySig = nearlySig.substring(1, nearlySig.length() - 1);
    String[] sigSegs = nearlySig.split(":");
    nearlySig = sigSegs[0] + "." + sigSegs[1].substring(1);
    return nearlySig;
  }

  // returns JVM Method descriptor of a method
  static String getMethodDescriptor(SootMethod m) {
    String s = m.getBytecodeSignature();
    s = s.substring(1, s.length() - 1);
    String[] ss = s.split("\\(");
    return "(" + ss[1];
  }
}
