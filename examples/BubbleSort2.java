// Bubble Sort
// Abano et al. Formal Verification with Dafny
class BubbleSort2 {

  static void sort(int[] arr){

    int m = 1;
    while(m < arr.length){
      int i = arr.length - 1;
      while(i >= 0){
        if(arr[i-1] > arr[i]){
          int tmp = arr[i-1];
          arr[i-1] = arr[i];
          arr[i] = tmp;
        }
        i = i -1;
      }
      m = m + 1;
    }
  }
}
