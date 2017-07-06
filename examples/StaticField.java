class StaticField {

  void loop1(){
     for(int i = 0; i < M.MAX; ++i);
	}
  void loop2(){
     for(int i = M.MAX; i >= M.MIN; --i);
	}


}

class M {
	final static int MAX=20;
	static int MIN=10;
}
