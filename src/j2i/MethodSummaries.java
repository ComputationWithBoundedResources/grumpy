package j2i;

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

public class MethodSummaries {
	Map<String, Map<String, MethodSummary>> summaries; // ClassName.MethodName -> Summary

	MethodSummaries(Map<String, Map<String, MethodSummary>> summaries){
   this.summaries = summaries;	
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
		Map<String, Map<String, MethodSummary>> summaries = new HashMap<>();
		JSONArray _clazzes = (JSONArray) jsonObject.get("summaries");
		for(Object _clazz : _clazzes){
			JSONObject clazz = (JSONObject) _clazz;
			String cname = (String) clazz.get("class");
			System.out.println("trying class: " + cname);
			Map<String, MethodSummary> xap = summaries.get(cname);
			Map<String, MethodSummary> map = xap != null ? xap : new HashMap<>();
			for(Object _method : (JSONArray) clazz.get("methods")){
				JSONObject method = (JSONObject) _method;
				String mname = (String) method.get("name");
				System.out.println("trying method: " + mname);
				MethodSummary msumm = MethodSummary.fromJSON(method);
			  map.put(mname,msumm);
			}
			summaries.put(cname,map);
		}
		System.out.println(summaries.isEmpty());
		return new MethodSummaries(summaries);
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


