// variants of simple loop construct
class SimpleLoop {
 
	void forLoop1(int m){ 
		for(int i = 0; i < m; ++i); 
	}
	void forLoop2(int m){ 
		for(int i = m; i >= 0; --i); 
	}
	void whileLoop(int m){
		int i = 0;
		while(i < m) i++;
	}
  void doWhileLoop(int m){
		int i = m;
		do
			--i;
		while(i >= 0);
	}

}
