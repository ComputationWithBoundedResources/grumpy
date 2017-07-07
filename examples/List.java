
import java.util.*;

class List<T> implements Iterable<T>{
  public List next;
  public T elem;	

  @Override
	public Iterator<T> iterator(){ return new LIterator(); }

	class LIterator implements Iterator<T>{
		public boolean hasNext(){ return List.this.next != null; }
		public T next()         { return List.this.elem; }
	}


}

class ListFun {

	// O(n)
	static void iterate(List l){
		while(l != null){
			l = l.next;
		}
	}


	// O(n^2)
	// to obtain expected linear bound it is necessary to pattern match list size
	// ie list = elem + rest
	static void iterate2(List<Integer> l){
		while(l != null){
			int a = l.elem;
			while(a >= 0) --a;
			l = l.next;
		}
	}


}
