package org.jasper.jLib.webview.trax.decoder;

import java.util.ArrayList;

import org.apache.log4j.Logger;

import org.jasper.jLib.webview.trax.decoder.WebViewTraxMessage.TargetType;
import org.jasper.jLib.webview.trax.decoder.WebViewTraxMessage.TrackInfoSize;
import org.jasper.jLib.webview.trax.decoder.WebViewTraxMessage.TrackInfoType;
import org.jasper.jLib.webview.trax.decoder.WebViewTraxMessage.TrackInfoVmi;

public class WebViewTraxDecoder {

	/*
	 * Sample trax response
	 *
	 * T,0,0,trax|ack('trax')
	 * starttrax(3,50031,0,78,1236,2943,2949)
	 * trax(1,236,1236,50031,ZZZZ,ZZZZ,ZZZZ,ZZZZ,-812,1207,0,False,0,False,False,64,False,0,0,1,-1,0,5,0)
	 * trax(1,,2943,50031,ZZZZ,ZZZZ,ZZZZ,ZZZZ,4163,16343,640,False,16,False,False,8,False,0,1,3,262,45,5,0)
	 * trax(1,949,2949,50031,ZZZZ,ZZZZ,ZZZZ,ZZZZ,165,756,0,False,0,False,False,64,False,0,0,1,-1,0,5,0)
	 * endtrax(3,50031)
	 */

   /*
	* Description of trax fields from Searidge
	* 
	* index - data                   -  type
	*-------|------------------------|------------------------
	*   0   - target_type            -  enum = 0:ghost 1:valid 
	*   1   - call_sign_name         -  String
	*   2   - track_num              -  String
	*   3   - time_of_day            -  String
	*   4   - source_airport         -  String
	*   5   - destination_airport    -  String
	*   6   - ac_type                -  String
	*   7   - stand                  -  String
	*   8   - cart_coord_x           -  String
	*   9   - cart_coord_y           -  String
	*  10   - mode_3a                -  String
	*  11   - mode_3a_valid          -  boolean
	*  12   - mode_c_altitude        -  String
	*  13   - mode_c_valid           -  boolean
	*  14   - track_status_fusion    -  boolean
	*  15   - track_status_rad       -  String
	*  16   - track_status_quality   -  boolean
	*  17   - track_info_size        -  enum = 0:? 1:very small, 2:small 3:medium 4:large 5:very large
	*  18   - track_info_type        -  enum = 0:default 1:aircrft 2:vehicle 
	*  19   - track_info_vmi         -  enum = 0:default 1:flat 2:climb 3:descend
	*  20   - direction              -  String in degrees
	*  21   - speed                  -  String in knots
	*  22   - track_info_direction   -  String, but should have been enum = 0:default 1:arr 2:dep 3:local 4:transit, but we ran into a scenario where the value was 5 so not sure if we have the right mapping
	*  23   - unknown                -  excluded
	*   
	*/	
	
	static ArrayList<String> traxRows = new ArrayList<String>();
	static Boolean isGhostTrax = false;
	static Logger logger = Logger.getLogger("org.jasper");
	
	public static WebViewTrax doDecode(String msg){
		ArrayList<WebViewTraxMessage> trax = new ArrayList<WebViewTraxMessage>();

		if(isValidTraxResponse(msg)){
			ArrayList<String> rows = traxRows;
			for(String row:rows){
				WebViewTraxMessage decodedRow = decodeRow(row);
				if (decodedRow != null) trax.add(decodedRow);
			}
		}

		return new WebViewTrax(trax.toArray(new WebViewTraxMessage[0]));
	}

	private static WebViewTraxMessage decodeRow(String row) {
		String[] line = row.split(",");
		if(! isValidRow(line)) {
			logger.warn("Invalid TRAX row detected: " + row);
			return null;
		}
		
		TargetType target_type       = TargetType.values()[Integer.valueOf(line[0].split("\\(")[1].trim())];
		String call_sign_name        = line[1];
		String track_num             = line[2];
		String time_of_day           = line[3];
		// Ghost Trax rows only have the first 4 fields of a WebviewTrax message
		if(! isGhostTrax) {
			String source_airport        = line[4];
			String destination_airport   = line[5];
			String ac_type               = line[6];
			String stand                 = line[7];
			String cart_coord_x          = line[8];
			String cart_coord_y          = line[9];
			String mode_3a               = line[10];
			boolean mode_3a_valid        = Boolean.parseBoolean(line[11]);
			String mode_c_altitude       = line[12];
			boolean mode_c_valid         = Boolean.parseBoolean(line[13]);
			boolean track_status_fusion  = Boolean.parseBoolean(line[14]);
			String track_status_rad      = line[15];
			boolean track_status_quality = Boolean.parseBoolean(line[16]);
			TrackInfoSize track_info_size = TrackInfoSize.values()[Integer.valueOf(line[17].trim())];
			TrackInfoType track_info_type = TrackInfoType.values()[Integer.valueOf(line[18].trim())];
			TrackInfoVmi track_info_vmi   = TrackInfoVmi.values()[Integer.valueOf(line[19].trim())];
			String direction              = line[20];
			String speed                  = line[21];
			String track_info_direction   = line[22];
			//String unknown                = line[23];
		
		
			return new WebViewTraxMessage(target_type, call_sign_name, track_num, time_of_day, source_airport, destination_airport,
					ac_type, stand, cart_coord_x, cart_coord_y, mode_3a, mode_3a_valid, mode_c_altitude, mode_c_valid, track_status_fusion,
					track_status_rad, track_status_quality, track_info_size, track_info_type, track_info_vmi, direction, speed, track_info_direction);
		}
		else {
			isGhostTrax = false;
			return new WebViewTraxMessage(target_type, call_sign_name, track_num, time_of_day, "", "",
					"", "", "", "", "", false, "", false, false, "", false, null, null, null, "", "", "");
		}
				
	}

	private static ArrayList<String> getTraxRows(String msg) {
		String[] rows = msg.split("\n");
		ArrayList<String> traxRows = new ArrayList<String>(); 
		for(String row : rows){
			if(row.startsWith("trax(") && row.endsWith("")){
				traxRows.add(row);
			}
		}
		return traxRows;
	}
	
	private static ArrayList<String> getTraxHeader(String msg, String search) {
		String[] rows = msg.split("\n");
		ArrayList<String> traxHeader = new ArrayList<String>(); 
		for(String row : rows){
			if(row.startsWith(search)) {
				traxHeader.add(row);
			}
		}
		return traxHeader;
	}

	private static boolean isValidTraxResponse(String msg) {
		if(msg == null) {
			logger.warn("Trax message is null");
			return false;
		}
		ArrayList<String> startTrax = getTraxHeader(msg, "starttrax(");
		ArrayList<String> endTrax = getTraxHeader(msg, "endtrax(");
		
		// message had zero or more than 1 'starttrax' row
		if(startTrax.size() == 0) {
			logger.warn("TRAX msg does not contain a starttrax row\n" + msg);
			return false;
		}
		else {
			if(startTrax.size() > 1) {
				logger.warn("TRAX msg contains more than one starttrax row\n" + msg);
				return false;
			}
		}
			
		
		// message had zero or more than 1 'endtrax' row
		if(endTrax.size() == 0) {
			logger.warn("TRAX msg does not contain an endtrax row\n" + msg);
			return false;
		}
		else {
			if(endTrax.size() > 1) {
				logger.warn("TRAX msg contains more than one endtrax row\n" + msg);
				return false;
			}
		}
		
		String[] tmp = startTrax.get(0).split(",");
		int totalRows;
		
		// ensure that field in the starttrax that indicates the number of trax rows in this msg
		// is present and numeric
		try {
		    totalRows = Integer.parseInt(tmp[0].split("\\(")[1].trim());
		} catch (NumberFormatException e) {
			logger.warn("Total number of Trax rows not valid: " + startTrax.toString());
			return false;
		} catch (ArrayIndexOutOfBoundsException ex) {
			logger.warn("Total number of Trax rows not present: " + startTrax.toString());
			return false;
		}
		
		// validate that the msg has the number of rows indicated in the starttrax row
		traxRows = getTraxRows(msg);
		if(totalRows != traxRows.size()) {
			logger.warn("Invalid Trax message: starttrax indicates " + totalRows + 
					" but there are " + traxRows.size() + " row(s):\n" + msg);
			return false;
		}
		
		return true;
	}
	
	private static Boolean isValidRow(String[] line) {
		int max_target_type      = (TargetType.values().length - 1);
		int max_track_info_size  = (TrackInfoSize.values().length - 1);
		int max_track_info_type  = (TrackInfoType.values().length - 1);
		int max_track_info_vmi   = (TrackInfoVmi.values().length - 1);
		int max_trax_row_len     = 22;
		String value;
		
		try {
			value = line[0].split("\\(")[1].trim();
		} catch (ArrayIndexOutOfBoundsException ex) {
			return false;
		}
		
		// ghost trax records are valid and need to be parsed
		if(value.equals("0")) {
			isGhostTrax = true;
			if(logger.isDebugEnabled()) {
				logger.debug("GHOST record detected");
			}
		}
		
		else if(line.length < max_trax_row_len) return false;
		
		// check all values before creating enums
		if(! validateEnumValue(value, max_target_type)) return false;
		
		// Ghost Trax rows do not contain any of these enums so skip
		if(! isGhostTrax) {
			value = line[17].trim();
			if(! validateEnumValue(value, max_track_info_size)) return false;
		
			value = line[18].trim();
			if(! validateEnumValue(value, max_track_info_type)) return false;
		
			value = line[19].trim();
			if(! validateEnumValue(value, max_track_info_vmi)) return false;
		}
		
		return true;
		
	}
	
	private static Boolean validateEnumValue(String value, int limit) {
		int enumValue;
	
		try {
		    enumValue = Integer.parseInt(value);
		} catch (NumberFormatException e) {
			return false;
		}
		if (enumValue < 0 || enumValue > limit) return false;
		
		return true;


	}
	
	public static void main(String arg[]){
		String record =
		"Content-type: text/plain; charset=ISO-8859-1\n" +
		"\n" +
		"T,0,0,trax|ack('trax')\n" +
		"starttrax(10,74571,0,36,10233,12799,12811,12829,12843,12849,12850,12853)\n" +
		"trax(1,233,10233,74571,ZZZZ,ZZZZ,ZZZZ,ZZZZ,-812,1207,0,False,0,False,False,64,False,0,0,1,-1,0,5,0)\n" +
		"trax(1,,12799,74571,ZZZZ,ZZZZ,ZZZZ,ZZZZ,3092,13719,2713,False,76,False,False,8,False,0,1,3,36,127,5,0)\n" +
		"trax(1,811,12811,74571,ZZZZ,ZZZZ,ZZZZ,ZZZZ,-898,2027,0,False,0,False,False,64,False,0,0,1,-1,0,5,0)\n" +
		"trax(1,,12829,74571,ZZZZ,ZZZZ,ZZZZ,ZZZZ,757,202,2419,False,31,False,False,64,False,0,0,3,307,9,1,0)\n" +
		"trax(1,843,12843,74571,ZZZZ,ZZZZ,ZZZZ,ZZZZ,1878,977,0,False,0,False,False,64,False,0,0,1,57,18,5,0)\n" +
		"trax(1,849,12849,74571,ZZZZ,ZZZZ,ZZZZ,ZZZZ,1424,744,0,False,0,False,False,64,False,0,0,1,-1,0,5,0)\n" +
		"trax(1,,12850,74571,ZZZZ,ZZZZ,ZZZZ,ZZZZ,10200,26661,151,False,59,False,False,8,False,0,1,1,266,169,5,0)\n" +
		"trax(1,853,12853,74571,ZZZZ,ZZZZ,ZZZZ,ZZZZ,1626,815,0,False,0,False,False,64,False,0,0,1,56,21,5,0)\n" +
		"trax(0,973,16973,72614)\n" +
		"trax(0,,13718,58585)\n" +
		"endtrax(8,74571)";
		
		
		WebViewTrax trax = doDecode(record);
		
	} 
	
	
}
