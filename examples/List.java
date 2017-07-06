
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

	static void iterate(List l){
		while(l != null){
			l = l.next;
		}
	}

	static void iterate2(List<Integer> l){
		while(l != null){
			int a = l.elem;
			while(a >= 0){
				--a;
			}
			l = l.next;
			// l = elem + next /\ a' = elem
			// a >= 0 /\ a' = a-1
			// l = elem + next /\ tmp' = next
			// l = elem + next /\ l' >= 0 /\ l' < 1+tmp'
		}
	}


}
