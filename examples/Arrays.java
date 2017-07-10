public class Arrays {
  
	// public static int binarySearch(int[] t, int v, int l, int u){
	// 	int m = 0;
	// 	while(l <= u){
	// 		// m = (l+u)/2;
	// 		m = ()
	// 		if (t[m] == v) return m;
	// 		if (t[m] >  v) u = m -1;
	// 		else           l = m+ 1;
	// 	}
	// 	return -1;
	// }
	
	static int[] reverse(int[] arr){
		int l     = arr.length;
		int[] rev = new int[l];

		for (int i=l; i > 0; i--) rev[l-i] = arr[i-1];
		return rev;
	}

}
