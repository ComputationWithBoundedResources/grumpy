// a poor-man's unit test for Grumpy
import java.util.*;

public class Loop {
	static int Max = 10;
	final static int FinalMax = 10;
	
	
	// expected O(1)
	// FinalMax should be inlined
	public static void	staticFinalMax(boolean b){
		if(b){
			int i = FinalMax;
			while(i-->=0);
		} else {
			int j = 0-FinalMax;
			while(j++<=FinalMax);
		}
	}
	
	// expected O(n)
	// non-final fields are not-inlined
	// the prototype behaviour does not read Max but consider it as parameter
	public static void	staticMax(boolean b){
		if(b){
			int i = Max;
			while(i-->=0);
		} else {
			int j = 0-Max;
			while(j++<=Max);
		}
	}

	// expected O(n)
	// actual ?
	// unsound behaviour of static field
	// static fields are considered read-only paramters
	public static void unsoundMax(){
		int i = 0;
		while(i++ < Loop.Max++);
	}
	
	// expected O(n)
	public static void simpleLoop(List<Integer> list, int m, int n){
		list.add(m);
		list.add(n);
		int bound = m + n;
		int j = 0;
		for(int i = 0; i < bound; i++){
			j += i;
			list.add(i);
		}
	}

	public static void conjuctiveLoop(List<Integer> list, int m, int n){
		list.add(m);
		list.add(n);
		int j = 0;
		while(m >= 0 && j <= n){
			m -= 2;
			j += 1;
		}
		while(n-->=0);;
	}

	public static void disjunctiveLoop(List<Integer> list, int m, int n, long o){
		list.add(m);
		list.add(n);
		int j = 0;
		while(m >= 0 || j <= n){
			m -= 2;
			j += 1;
		}
		while(n-->=0);;

		while(o != 0){
		 if(o > 0){
			 o --;
		 } else {
		   o ++;
		 }
		}
	}



	// expected O(n^2)
	public static void sequentLoops(List<Integer> list, int m, int n){
		list.add(m);
		list.add(n);
		int bound = m + n;
		int j = 0;
		for(int i = 0; i < bound; i++){
			j += i;
			list.add(i);
		}
		Integer a = new Integer(3) + new Integer(4);
		do {
			--j;
		} while(j>0);
	}

	// // expected O(n^2)
	public static void nestedLoops(List<Integer> list, int m, int n){
		for(int i = 0; i < m; i++)
			for(int j = 0; j < i; j++);
		for(int i = 0; i < m; i++)
			for(int j = 0; j < n; j++);
	}

}
