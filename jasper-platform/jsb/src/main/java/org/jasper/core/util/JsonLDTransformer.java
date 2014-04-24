package org.jasper.core.util;

import java.util.Map.Entry;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class JsonLDTransformer {
	private JsonObject output;
	private JsonObject globalContext;

	public JsonLDTransformer() {}
	
	private String parseUri(String uri, JsonObject context){
		String[] tmp = uri.split("/");
		String element = tmp[tmp.length-1];
		if(context.has(element)){
			element = tmp[tmp.length-2].concat("_").concat(tmp[tmp.length-1]);
		}
		
		return element;
	}
	
	public JsonElement parseResponse(JsonElement response){
		output = new JsonObject();
		globalContext = new JsonObject();
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
		else if(response.isJsonPrimitive()){
			return response;
		}
		else if(response.isJsonObject()){
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
			else if(!entry.getValue().isJsonArray()){
				output.add(entry.getKey(), entry.getValue());
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
	
	private JsonElement parseArray(String arrayName, JsonArray jsonArr, boolean useGlobalCtx){
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
					tmpObj.add(parsedUri, arrayEntry.getValue());
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

}
