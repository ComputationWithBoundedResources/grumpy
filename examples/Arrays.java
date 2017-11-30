public class Arrays {
	
	static int[] reverse(int[] arr){
    int l			= arr.length;
		int[] rev = new int[l];

		for (int i=l; i > 0; i--) rev[l-i] = arr[i-1];
		return rev;
	}

}
