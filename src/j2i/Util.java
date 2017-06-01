package j2i;


import soot.SootMethod;


public class Util {

	public static String getDescriptor(SootMethod m) {
		String nearlySig = m.getBytecodeSignature();
		nearlySig = nearlySig.substring(1, nearlySig.length() - 1);
		String[] sigSegs = nearlySig.split(":");
		nearlySig = sigSegs[0] + "." + sigSegs[1].substring(1);
		return nearlySig;
	}

}


