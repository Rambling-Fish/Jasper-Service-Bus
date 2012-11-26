package org.jasper.jLib.webview.notam.decoder;

import java.util.ArrayList;

import org.apache.log4j.Logger;

public class WebViewNotamDecoder {

	/*
	 * Sample notam response
	 *
	 *	T,0,0,notam|ack('notam')
	 *	notam(startnotam)
	 *	notam(110688,110688 NOTAMN CYOW OTTAWA/MACDONALD-CARTIER INTL@CYOW AMEND LOC<BC> RWY 25 APCH: PLAN VIEW: NO PT ALT 1500 AUTH FM@TEFLY WPT ONLY)
	 *	notam(120073,120073 NOTAMN CYOW OTTAWA/MACDONALD-CARTIER INTL@CYOW WEF 1202090901 AMEND RIVER SEVEN ARR:@THURO: ALT RESTRICTION TO READ:@RWY 25 AT OR BLW 10000 RWY 07 AT OR BLW 16000@RWY 32 AT OR BLW 12000)
	 *	notam(120173,120173 NOTAMN CYOW OTTAWA/MACDONALD-CARTIER INTL@CYOW RWY 07/25 SLIPPERY WHEN WET DUE TO RUBBER BUILDUP@1205011315 TIL 1205160400)
	 *	notam(120178,120178 NOTAMN CYOW OTTAWA/MACDONALD-CARTIER INTL@CYOW PURSUANT TO SECTION 5.1 OF THE AERONAUTICS ACT; THE AIRSPACE@WITHIN THE AREA BOUNDED BY 453241N 752451W TO 453924N 752709W@TO 454453N 750833W TO 453702N 750101W THEN FOLLOWING ROAD@148 TO POINT OF ORIGIN <CENTRED APRX 25 NM NE AD>; 4000 TO@7000 FT MSL; IS RESTRICTED <FORMATION FLT TRAINING>.@NO PERSON SHALL OPR AN ACFT WITHIN THE AREA DESCRIBED EXC@FOR ACFT AUTH BY ATC.@MAY 11 1700-2200;@MAY 12 1400-2200;@MAY 13 1400-1600.@1205111700 TIL 1205131600)
	 *	notam(120179,120179 NOTAMN CYOW OTTAWA/MACDONALD-CARTIER INTL@CYOW RWY 14/32 AVBL 30 MIN PN 613-248-2013; DUE MAINT@1205071315 TIL 1205071730)
	 *	notam(endnotam)
	 */
	
	static Logger logger = Logger.getLogger("org.jasper");

	public static WebViewNotam doDecode(String msg){		
		ArrayList<WebViewNotamMessage> notam = new ArrayList<WebViewNotamMessage>();
		
		if(isValidNotamResponse(msg)){
			ArrayList<String> rows = getNotamRows(msg);
			for(String row:rows){
				WebViewNotamMessage decodedRow = decodeRow(row);
				if (decodedRow != null) notam.add(decodedRow);
			}
		}
		else {
			logger.warn("Invalid Notam msg: problem with startnotam and/or endnotam\n" + msg);
		}
		
		return new WebViewNotam(notam.toArray(new WebViewNotamMessage[0]));
	}

	private static WebViewNotamMessage decodeRow(String row) {
		String[] line = row.split(",");
		if(line.length == 2){
			String id = line[0].split("\\(")[1];
			String description = line[1].split("\\)")[0];
			return new WebViewNotamMessage(id, description);
		}
		return null;
	}

	private static ArrayList<String> getNotamRows(String msg) {
		String[] rows = msg.split("\n");
		ArrayList<String> notamRows = new ArrayList<String>(); 
		for(String row : rows){
			if(row.startsWith("notam(") && row.endsWith(")") && 
			!((row.equals("notam(startnotam)") || row.equals("notam(endnotam)")))){
					notamRows.add(row);
			}
		}
		return notamRows;
	}

	private static boolean isValidNotamResponse(String msg) {
		if(msg == null) return false;
		String[] rows = msg.split("\n");
		int numStart = 0;
		int numEnd   = 0;
		for(String row : rows) {
		    if(row.equals("notam(startnotam)")) {
				numStart++;
			}
			else if(row.equals("notam(endnotam)")) {
				numEnd++;
			}
		}
		
		if(numStart != 1 || numEnd != 1) return false;
		
		return true;
	}


	
	public static void main(String arg[]){
		String record = 
			"T,0,0,notam|ack('notam')\n" +
			"notam(startnotam)\n" +
			"notam(110688,110688 NOTAMN CYOW OTTAWA/MACDONALD-CARTIER INTL@CYOW AMEND LOC<BC> RWY 25 APCH: PLAN VIEW: NO PT ALT 1500 AUTH FM@TEFLY WPT ONLY)\n" +
			"notam(120073,120073 NOTAMN CYOW OTTAWA/MACDONALD-CARTIER INTL@CYOW WEF 1202090901 AMEND RIVER SEVEN ARR:@THURO: ALT RESTRICTION TO READ:@RWY 25 AT OR BLW 10000 RWY 07 AT OR BLW 16000@RWY 32 AT OR BLW 12000)\n" +
			"notam(120173,120173 NOTAMN CYOW OTTAWA/MACDONALD-CARTIER INTL@CYOW RWY 07/25 SLIPPERY WHEN WET DUE TO RUBBER BUILDUP@1205011315 TIL 1205160400)\n" +
			"notam(120178,120178 NOTAMN CYOW OTTAWA/MACDONALD-CARTIER INTL@CYOW PURSUANT TO SECTION 5.1 OF THE AERONAUTICS ACT; THE AIRSPACE@WITHIN THE AREA BOUNDED BY 453241N 752451W TO 453924N 752709W@TO 454453N 750833W TO 453702N 750101W THEN FOLLOWING ROAD@148 TO POINT OF ORIGIN <CENTRED APRX 25 NM NE AD>; 4000 TO@7000 FT MSL; IS RESTRICTED <FORMATION FLT TRAINING>.@NO PERSON SHALL OPR AN ACFT WITHIN THE AREA DESCRIBED EXC@FOR ACFT AUTH BY ATC.@MAY 11 1700-2200;@MAY 12 1400-2200;@MAY 13 1400-1600.@1205111700 TIL 1205131600)\n" +
			"notam(120179,120179 NOTAMN CYOW OTTAWA/MACDONALD-CARTIER INTL@CYOW RWY 14/32 AVBL 30 MIN PN 613-248-2013; DUE MAINT@1205071315 TIL 1205071730)\n" +
		    "notam(endnotam)";
		
		
		WebViewNotam notam = doDecode(record);		
	}
	
	
}
