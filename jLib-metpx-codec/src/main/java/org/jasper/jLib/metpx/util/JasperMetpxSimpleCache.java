package org.jasper.jLib.metpx.util;

import org.jasper.jLib.metpx.decoder.Bulletin;

import java.util.ArrayList;

public class JasperMetpxSimpleCache {
	private static ArrayList<Bulletin> board = new ArrayList<Bulletin>();
	private static Bulletin bulletin = new Bulletin();
	private static int count = 0;

	public Object getBulletinList(String msg){
		if(msg.equals("/jasper/metpx/1.0/board")) return board.toArray();
		if(msg.equals("/jasper/metpx/1.0/latest")) return bulletin;
		if(msg.equals("/jasper/metpx/1.0/count")) return count;
		return "JASPER ASB --- Resource Not Found.";
	}
	public Bulletin setObjects(Bulletin bulletin){

		JasperMetpxSimpleCache.board.add(bulletin);
		JasperMetpxSimpleCache.bulletin = bulletin;
		JasperMetpxSimpleCache.count++;
		return bulletin;
	}

}
