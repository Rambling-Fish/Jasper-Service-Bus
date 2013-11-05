package org.jasper.core.notification.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.jena.atlas.json.JsonArray;

public class JsonResponseParser{

	
	public List<Integer> parse(JsonArray array, String ruri){
		
		if(array.isEmpty()) return null;
		
		List<String> matchList = new ArrayList<String>();
		List<Integer> list = new ArrayList<Integer>();
		Pattern regex = Pattern.compile(
		    "\\{           # Match an opening brace.                              \n" +
		    "(?:           # Match either...                                      \n" +
		    " \"           #  a quoted string,                                    \n" +
		    " (?:          #  which may contain either...                         \n" +
		    "  \\\\.       #   escaped characters                                 \n" +
		    " |            #  or                                                  \n" +
		    "  [^\"\\\\]   #   any other characters except quotes and backslashes \n" +
		    " )*           #  any number of times,                                \n" +
		    " \"           #  and ends with a quote.                              \n" +
		    "|             # Or match...                                          \n" +
		    " [^\"{}]*     #  any number of characters besides quotes and braces. \n" +
		    ")*            # Repeat as needed.                                    \n" +
		    "\\}           # Then match a closing brace.", 
		    Pattern.COMMENTS);
		Matcher regexMatcher = regex.matcher(array.toString());

		while (regexMatcher.find()) {
		    matchList.add(regexMatcher.group());
		} 
	//TODO right now we expect int value but that may not be the case. Need a
    // way to determine the data we are parsing
		for(int i=0;i<matchList.size();i++){
			String[] tmp = matchList.get(i).split(",");
			for(int p=0;p<tmp.length;p++){
				if(tmp[p].contains(ruri)){
					String value = tmp[p].substring(tmp[p].lastIndexOf(":"));
					value = value.replaceFirst(":", "").trim();
					try{
						list.add(Integer.parseInt(value));
					} catch(NumberFormatException e){
						// do nothing we won't add to the list if not a valid number
					}
				
				}
			}
		}
		
		return list;
	}
	
}
