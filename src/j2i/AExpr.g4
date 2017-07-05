grammar AExpr;

@header{
		package j2i;

		import j2i.AExpr;
}

eval returns [AExpr value]
    :    exp=add EOF     {$value = $exp.value;}
    ;

add returns [AExpr value]
    :    e1=mul          {$value = $e1.value;}
         ( '+' e2=mul    {$value = new Add($e1.value, $e2.value);}
         | '-' e2=mul    {$value = new Sub($e1.value, $e2.value);}
         )* 
    ;

mul returns [AExpr value]
    :    e1=uexpr        {$value = $e1.value;}
         ( '*' e2=uexpr  {$value = new Mul($e1.value, $e2.value);}
         )* 
    ;

uexpr returns [AExpr value]
    :    '-' a=atom      {$value = new Neg($a.value);}
    |    a=atom          {$value = $a.value;}
		;

atom returns [AExpr value]
    :    n=Number        {$value = new Val(Long.parseLong($n.text));}
    |    i=Identifier    {$value = new Var($i.text);}
    |    '(' e=add ')'   {$value = $e.value;}
    ;

Number
    :    ('0'..'9')+ ('.' ('0'..'9')+)?
    ;

Identifier
    :  ('a'..'z' | 'A'..'Z' | '_') ('a'..'z' | 'A'..'Z' | '_' | '0'..'9')*
    ;

WS : [ \t\r\n] -> skip; 
