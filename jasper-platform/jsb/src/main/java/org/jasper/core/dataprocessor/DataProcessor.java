package org.jasper.core.dataprocessor;

import javax.jms.JMSException;

import org.apache.log4j.Logger;
import org.jasper.core.constants.JasperConstants;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

public class DataProcessor{

    private String scheme;
	static Logger logger = Logger.getLogger(DataProcessor.class.getName()); 
	private JsonArray arrayOutput = new JsonArray();


    public DataProcessor(String scheme) throws JMSException{
    	this.scheme = scheme;
    }
    
    public DataProcessor(){
    	this.scheme = JasperConstants.DEFAULT_PROCESSING_SCHEME;
    }
    
    /**
     * Adds a string which must be a valid JSON object. Later releases
     * will support xml as an input. Processing varies based on the
     * processing scheme that was passed in the constructor.
     *
     * @param  input  a string representation of a JSON object
     * @throws Exception 
     */
    public void add(String input) throws Exception{
    	switch (scheme.toLowerCase()) {
		case JasperConstants.AGGREGATE_SCHEME :
    		aggregateInput(input);
    		break;
		case JasperConstants.COALESCE_SCHEME :
			coalesceInput(input);
			break;
		default :
			logger.error("Unknown processing scheme: " + scheme);
    	}
    }
    
    /**
     * Returns the output based on processing scheme. Currently only
     * aggregate and coalesce schemes are supported.
     * Aggregate returns an array with one or more elements
     * Coalesce returns a single entry from one or more inputs and only
     * if each input is identical. For example if three email addresses
     * have been accumulated, then coalesce scheme will return only one
     * if all three are the same address otherwise it returns null on error
     *
     * @return returns a string representation of the output
     */
    public String process(){
    	String response;
    	switch (scheme.toLowerCase()) {
		case JasperConstants.AGGREGATE_SCHEME :
			Gson gson = new Gson();
			response = gson.toJson(arrayOutput);
			return response;
		case JasperConstants.COALESCE_SCHEME :
			response = coalesceOutput();
			return response;
		default :
			logger.error("Unknown processing scheme " + scheme);
			return null;
    	}
    }
    
    private void aggregateInput(String input){
    	if(isJsonInput(input)){
    		try{
    			JsonElement jelement = new JsonParser().parse(input);
    			arrayOutput.add(jelement);
    		} catch(Exception ex){
    			logger.error("Exception occured in aggregateInput() while adding: " + input);
    			throw ex;
    		}
    	}
    	else{
    		if(isXMLInput(input)){
    		//TODO support xml
    		}
    	}
    }
    
    private void coalesceInput(String input) throws Exception{
    	if(isJsonInput(input)){
    		try{
    			JsonElement jelement = new JsonParser().parse(input);
    			arrayOutput.add(jelement);
    		} catch(Exception ex){
    			logger.error("Exception occured in coalesceInput() while adding: " + input);
    			throw ex;
    		}
    	}
    	else{
    		if(isXMLInput(input)){
    			//TODO support XML
    		}
    	}
    }
    
    private String coalesceOutput(){
    	if(isAllSame()){
    		return arrayOutput.get(0).toString(); 
    	}
    	
    	return null;
    }
    
    private boolean isAllSame(){
    	 for(int i=1; i<arrayOutput.size(); i++){
             if(!arrayOutput.get(0).equals(arrayOutput.get(i))){
                 return false;
             }
         }

         return true;
    }
    
    private boolean isJsonInput(String input){
        input = input.trim();
        return input.startsWith("{") && input.endsWith("}") 
               || input.startsWith("[") && input.endsWith("]");
    }
    
    private boolean isXMLInput(String input){
        input = input.trim();
        return input.startsWith("<") && input.endsWith(">");
    }
    
}
