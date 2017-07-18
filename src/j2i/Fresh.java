package j2i;

// This module provides global counters to generate fresh variables.
class Fresh {

  private static Fresh instance = null;
  int imm = 0;
  int var = 0;

  private Fresh() {
  }

  private static Fresh f() {
    if (instance == null) {
      instance = new Fresh();
    }
    return instance;
  }

  static Var freshVar() {
    return new Var("fresh#" + f().var++);
  }

  static Var freshImm() {
    return new Var("imm#" + f().imm++);
  }
}
