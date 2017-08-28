// BubbleSort Example from
// H.R. Nielson, A Proof Systems for Analysing Computation Time
class BubbleSort{

  public static void main(String[] args) {
    int[] arr = {6,1,43,67,7,8,89,3,34,6,7};
    sort(arr);
    for(int i:arr){
      System.out.print(i+",");
    }
  }

  static void sort(int[] arr){
    int m = arr.length;
    int i = m;
    while(i > 0){
      i = 0;
      int j = 1;
      while(j < m){
        if(arr[j-1] > arr[j]){
          int tmp = arr[j-1];
          arr[j-1] = arr[j];
          arr[j] = tmp;
          i = j;
          j = j +1;
        } else {
          j = j + 1;
        }
      }
    }
  }

}
