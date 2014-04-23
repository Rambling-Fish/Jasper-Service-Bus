package org.jasper.core.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class JsonLDTransformer {
	private static JsonObject output= new JsonObject();
	private static JsonObject globalContext = new JsonObject();

	public JsonLDTransformer() {}
	
	private static String parseUri(String uri, JsonObject context){
		String[] tmp = uri.split("/");
		String element = tmp[tmp.length-1];
		if(context.has(element)){
			element = tmp[tmp.length-2].concat("_").concat(tmp[tmp.length-1]);
		}
		
		return element;
	}
	
	public JsonElement parseResponse(JsonElement response){
		JsonObject jsonObj = new JsonObject();
		String arrayName = null;
		if(response.isJsonArray()){
			if(response.getAsJsonArray().get(0).isJsonPrimitive()){
				return response;
			}
			else{
				return parseArray(null, response.getAsJsonArray(), false);
			}
		}
		if(response.isJsonObject()){
			jsonObj = response.getAsJsonObject();
			output.add("@context",  globalContext);
		}
		String parsedUri = null;
	
		for(Entry<String, JsonElement> entry : jsonObj.entrySet()){
			if(entry.getKey().contains("http://")){
				parsedUri = parseUri(entry.getKey(), globalContext);
			}
			else{
				parsedUri = null;
			}
			
			if(!entry.getValue().isJsonArray() && (parsedUri != null)){
				globalContext.addProperty(parsedUri,entry.getKey());
				output.add(parsedUri, entry.getValue());
			}
			else{
				if(entry.getValue().getAsJsonArray().get(0).isJsonPrimitive()){
					globalContext.addProperty(parsedUri,entry.getKey());
					output.add(parsedUri, entry.getValue());
				}
				else{
					arrayName = parsedUri;
					globalContext.addProperty(parsedUri, entry.getKey());
					parseArray(arrayName, entry.getValue().getAsJsonArray(), true);
				}
			}
		}
			
		return output;	
	}
	
	private static JsonElement parseArray(String arrayName, JsonArray jsonArr, boolean useGlobalCtx){
		JsonArray newArray = new JsonArray();
		String parsedUri = null;
		for(JsonElement item:jsonArr){
			JsonObject localContext = new JsonObject();
			JsonObject tmpObj = new JsonObject();
			if(!useGlobalCtx){
				tmpObj.add("@context", localContext);
			}

			for(Entry<String,JsonElement> arrayEntry : item.getAsJsonObject().entrySet()){
				if(arrayEntry.getKey().contains("http://")){
					parsedUri = parseUri(arrayEntry.getKey(), localContext);
				}
				else{
					parsedUri = null;
				}
				if((useGlobalCtx) && (parsedUri != null)){
					globalContext.addProperty(parsedUri,arrayEntry.getKey());
				}
				else if(parsedUri != null){
					localContext.addProperty(parsedUri,arrayEntry.getKey());
					tmpObj.add(parsedUri, arrayEntry.getValue());
				}
				else{
					tmpObj.add(arrayEntry.getKey(), arrayEntry.getValue());
				}
			}
		    
			newArray.add(tmpObj);
		}
		
		if(useGlobalCtx){
			output.add(arrayName, newArray);
			return output;
		}
		else{
			return newArray;
		}
	}


	public static void main(String[] args) {
		JsonLDTransformer ldTransformer = new JsonLDTransformer();
		JsonParser jParser = new JsonParser();
		JsonObject jsonObj = new JsonObject();
		JsonArray jsonArr  = new JsonArray();
		JsonArray mixedArr = new JsonArray();
		JsonArray primitiveArr  = new JsonArray();
		JsonElement prim1 = jParser.parse("one");
		JsonElement prim2 = jParser.parse("two");
		primitiveArr.add(prim1);
		primitiveArr.add(prim2);
		Map<String,String> map = new HashMap<String,String>();
		Map<String,String> map2 = new HashMap<String,String>();
		Gson gson = new Gson();
		map.put("http://coralcea.ca/demo/bpm", "52");
		map.put("http://coralcea.ca/demo/timestamp", "2014-04-16");
		map.put("http://coralcea.ca/demo/patient/id", "http://pid01");
		map.put("http://coralcea.ca/demo/sensor/id", "srID02");
		
		JsonElement jsonTree = gson.toJsonTree(map, Map.class);
		jsonArr.add(jsonTree);
		mixedArr.add(jsonTree);
		map.put("http://coralcea.ca/demo/bpm", "99");
		map.put("http://coralcea.ca/demo/timestamp", "2014-04-17");
		map.put("http://coralcea.ca/demo/patient/id", "http://pid99");
		map.put("http://coralcea.ca/demo/sensor/id", "srID03");
		jsonTree = gson.toJsonTree(map, Map.class);
		jsonArr.add(jsonTree);
		
		map2.put("http://coralcea.ca/demo/diastolic", "120");
		map2.put("http://coralcea.ca/demo/timestamp", "2014-05-12");
		map2.put("http://coralcea.ca/demo/systolic", "85");
		jsonTree = gson.toJsonTree(map2, Map.class);
		mixedArr.add(jsonTree);

		
		jsonObj.addProperty("http://coralcea.ca/demo/wardId", "Wing-5-Floor-3-Ward-4");
		jsonObj.addProperty("http://coralcea.ca/demo/data", "some-data");
		jsonObj.addProperty("http://coralcea.ca/demo/more/data", "more-data");
		jsonObj.add("http://coralcea.ca/demo/patient/data", jsonArr);
		jsonObj.add("http://coralcea.ca/demo/patient/primitive/data", primitiveArr);
		
		System.out.println("Input - simple primitive array:\n" + primitiveArr.toString());
		System.out.println("Parsed response:\n" + ldTransformer.parseResponse(primitiveArr));
		System.out.println("\nInput - object with embedded non-primitive and primitive arrays:\n" + jsonObj);
		System.out.println("Parsed response:\n" + ldTransformer.parseResponse(jsonObj));
		System.out.println("\nInput - non-primitive array:\n" + jsonArr.toString());
		System.out.println("Parsed response:\n" + ldTransformer.parseResponse(jsonArr));
		System.out.println("\nInput - mixed non-primitive array:\n" + mixedArr.toString());
		System.out.println("Parsed response :\n" + ldTransformer.parseResponse(mixedArr));
	}

}
