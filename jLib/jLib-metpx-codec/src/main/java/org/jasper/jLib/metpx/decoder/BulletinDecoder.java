package org.jasper.jLib.metpx.decoder;

import java.util.ArrayList;

import org.jasper.jLib.metpx.decoder.Bulletin;

public class BulletinDecoder {
	public Bulletin doDecode(byte[] byteArray) {
		if(byteArray == null) return null;
		ArrayList<String> rows;
		String messageString = new String(byteArray);
		int rowCount = 0;
		String header = "";
		String content = "";
		
		rows = getRows(messageString, "\n");
		if (rows.size() >=3 ){
			if (rows != null){
				Bulletin bulletin = new Bulletin();
				
				for(String row:rows){
					if (rowCount == 0) {
						bulletin.setHeader(row);
						header = row;
					}
					if (rowCount == 1) bulletin.setDestination(row);
					if (rowCount > 1) content = row + "\n" + content;
					rowCount++;
				}
				bulletin.setContent(content);
				rowCount = 0;
				if (header != ""){
					rows = getRows(header, "\\s");
					for(String row:rows){
						if (rowCount == 0) {
							// From
							bulletin.setFrom(row.substring(0, 2));
							// Country
							bulletin.setCountry(row.substring(2, 4));
							// Scope
							bulletin.setScope(row.substring(4, 6));
						}
						// Origin
						if (rowCount == 1) bulletin.setOrigin(row);
						// Time
						if (rowCount == 2) bulletin.setTime(row);
						rowCount++;
					}
		
				}
				return bulletin;
			}
			
		} else {
			System.out.println("Message no valid");
		}
		
		return null;
		
	}
	
	private static ArrayList<String> getRows(String messageString, String regex) {
		
		String[] rows = messageString.split(regex);
		ArrayList<String> bulletinRows = new ArrayList<String>();
		int counter = 0;
		for (String row:rows){
			bulletinRows.add(row);
			counter++;
		}
		return bulletinRows;
	}
	
}
