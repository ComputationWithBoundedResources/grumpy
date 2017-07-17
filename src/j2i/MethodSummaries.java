package j2i;

import static j2i.Formula.*;
import static j2i.Constraint.*;

import org.json.simple.*;
import org.json.simple.parser.*;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;


class Complexity {
	Optional<AExpr> upperTime;
	Optional<AExpr> lowerTime;
	Optional<AExpr> upperSpace;
	Optional<AExpr> lowerSpace;

	Complexity(){
		this.upperTime  = Optional.empty();
		this.lowerTime  = Optional.empty();
		this.upperSpace = Optional.empty();
		this.lowerSpace = Optional.empty();
	}

	Complexity(String upperTime, String lowerTime, String upperSpace, String lowerSpace){
		this.upperTime  = fromString(upperTime);
		this.lowerTime  = fromString(lowerTime);
		this.upperSpace = fromString(upperSpace);
		this.lowerSpace = fromString(lowerSpace);
	}
	
	static Optional<AExpr> fromString(String bound){
		return bound != null ? Optional.of( AExpr.fromString(bound) ) : Optional.empty();
	}

	@Override
	public String toString() {
		return "Complexity" +
				"{ upperTime=" + upperTime +
				", lowerTime=" + lowerTime +
				", upperSpace=" + upperSpace +
				", lowerSpace=" + lowerSpace +
				"}";
	}
}

class Declaration implements Comparable<Declaration>{
  String className;
  String methodName;
  String descriptor;

	public Declaration(String className, String methodName, String descriptor) {
		this.className = className;
		this.methodName = methodName;
		this.descriptor = descriptor;
	}

	@Override
	public int compareTo(Declaration other){
		int result = this.className.compareTo(other.className);
		result = result == 0 ? this.methodName.compareTo(other.methodName) : result;
		result = result == 0 ? this.descriptor.compareTo(other.descriptor) : result;
		return result;
	}

	@Override
	public int hashCode() {
		int result = 17;
		result = 31 * result + (className != null ? className.hashCode() : 0);
		result = 31 * result + (methodName != null ? methodName.hashCode() : 0);
		result = 31 * result + (descriptor != null ? descriptor.hashCode() : 0);
		return result;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		Declaration object = (Declaration) o;

		if (className != null ? !className.equals(object.className) : object.className != null) return false;
		if (methodName != null ? !methodName.equals(object.methodName) : object.methodName != null) return false;
		return !(descriptor != null ? !descriptor.equals(object.descriptor) : object.descriptor != null);
	}

}



public class MethodSummaries {
	TreeMap<Declaration, MethodSummary> summaries;

	Optional<MethodSummary> get(Declaration decl){ return Optional.ofNullable(this.summaries.get(decl)); }
	Optional<MethodSummary> get(String className, String methodName, String descriptor)
		{ return get(new Declaration(className, methodName, descriptor)); }
	Optional<MethodSummary> get(String className, String methodName){
		Map.Entry<Declaration,MethodSummary> entry = this.summaries.higherEntry( new Declaration(className, methodName, "") );
		return
			entry != null
			&& entry.getKey().className.equals(className)
			&& entry.getKey().methodName.equals(methodName)
				? Optional.of( entry.getValue() )
				: Optional.empty();
	}


	public static MethodSummaries fromFile(String filePath){
		JSONParser parser = new JSONParser();
		try {
			Object obj = parser.parse(new FileReader(filePath));
			JSONObject jsonObject = (JSONObject) obj;
			return MethodSummaries.fromJSON(jsonObject);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}

		throw new RuntimeException("could not parse method summaries");
	}

	public static MethodSummaries fromJSON(JSONObject jsonObject){
		TreeMap<Declaration, MethodSummary> summaries = new TreeMap<>();
		JSONArray _clazzes = (JSONArray) jsonObject.get("summaries");
		for(Object _clazz : _clazzes){
			JSONObject clazz = (JSONObject) _clazz;
			String cname = (String) clazz.get("class");
			for(Object _method : (JSONArray) clazz.get("methods")){
				JSONObject method = (JSONObject) _method;
				String mname = (String) method.get("name");
				String descr = (String) method.get("descriptor");

				Declaration key = new Declaration(cname,mname,descr);
				MethodSummary value = MethodSummary.fromJSON(method);
				summaries.put(key, value);
			}
		}
		MethodSummaries ms = new MethodSummaries();
		ms.summaries = summaries;
		return ms;
	}

	@Override
	public String toString() {
		return "MethodSummaries{" + "summaries=" + summaries + "}";
	}
}



class MethodSummary {
	String name;
	String descriptor;
	boolean isStatic;
	Complexity complexity;
	Map<String, AExpr> lowerSize;
	Map<String, AExpr> upperSize;
	List<String> modifies;

	private MethodSummary(){ }

	MethodSummary(String name, String descriptor, boolean isStatic, Complexity complexity, Map<String, AExpr> lowerSize, Map<String, AExpr> upperSize, List<String> modifies){
		this.name = name;
		this.descriptor = descriptor;
		this.isStatic = isStatic;
		this.complexity = complexity;
		this.lowerSize = lowerSize;
		this.upperSize = upperSize;
		this.modifies = modifies;
	}

	public static MethodSummary fromJSON(JSONObject jsonObject){

		String name       = (String) jsonObject.get("name");
		String descriptor = (String) jsonObject.get("descriptor");
		boolean isStatic  = (boolean) jsonObject.get("static");

		// complexity
		Complexity complexity;
		JSONObject _complexity = (JSONObject) jsonObject.get("complexity");
		if(_complexity != null)
			complexity = new Complexity
				( (String) _complexity.get("lowerTime")
					, (String) _complexity.get("upperTime")
					, (String) _complexity.get("upperSpace")
					, (String) _complexity.get("lowerSpace"));
		else
			complexity = new Complexity();


		// lowerSize
		Map<String, AExpr> lowerSize = new HashMap<>();
		JSONArray _lowerSizes = (JSONArray) jsonObject.get("lowerSize");
		if(_lowerSizes != null){
			for(Object _obj: _lowerSizes){
				JSONObject _lowerSize = (JSONObject) _obj;
				String pos   = (String) _lowerSize.get("pos");
				String bound = (String) _lowerSize.get("bound");
				lowerSize.put(pos, AExpr.fromString(bound));
			}
		}
		// upperSize
		Map<String, AExpr> upperSize = new HashMap<>();
		JSONArray _upperSizes = (JSONArray) jsonObject.get("upperSize");
		if(_upperSizes != null){
			for(Object _obj: _upperSizes){
				JSONObject _upperSize = (JSONObject) _obj;
				String pos   = (String) _upperSize.get("pos");
				String bound = (String) _upperSize.get("bound");
				upperSize.put(pos, AExpr.fromString(bound));
			}
		}

		// modifies
		List<String> _modifies = (List<String>) jsonObject.get("modifies");
		List<String> modifies = _modifies != null ? _modifies : new ArrayList<>();

		return new MethodSummary
			( name
				, descriptor
				, isStatic
				, complexity
				, lowerSize
				, upperSize
				, modifies );

	}

	public static MethodSummary defaultSummary(){
		MethodSummary m = new MethodSummary();
		m.name          = "default";
		m.descriptor    = "?";
		m.isStatic      = false;
		m.complexity    = new Complexity();
		m.lowerSize     = new HashMap<>();
		m.upperSize     = new HashMap<>();
		m.modifies      = new ArrayList<>();
		return m;
	}

	public AExpr getUpperTimeWithDefault(){ return this.complexity.upperTime.orElse( Val.one() ); }

	public AExpr getLowerTimeWithDefault(){ return this.complexity.lowerTime.orElse( Val.one() ); }
	
	public Formula getEffect() {
		int vid = 0;

		Formula fm = Formula.empty();
		for(Map.Entry<String,AExpr> entry : this.lowerSize.entrySet())
			fm = fm.and( ge(new Var(entry.getKey()), entry.getValue()) );
		for(Map.Entry<String,AExpr> entry : this.upperSize.entrySet())
			fm = fm.and( le(new Var(entry.getKey()), entry.getValue()) );
		for(String entry : this.modifies)
			fm = fm.and( eq(new Var(entry, true), new Var("imm_"+ ++vid)) );
		return fm;
	} 

			


	@java.lang.Override
	public java.lang.String toString() {
		return "MethodSummary{" +
			"name='" + name + '\'' +
			", descriptor='" + descriptor + '\'' +
			", isStatic=" + isStatic +
			", complexity=" + complexity +
			", lowerSize=" + lowerSize +
			", upperSize=" + upperSize +
			", modifies=" + modifies +
			'}';
}

}


