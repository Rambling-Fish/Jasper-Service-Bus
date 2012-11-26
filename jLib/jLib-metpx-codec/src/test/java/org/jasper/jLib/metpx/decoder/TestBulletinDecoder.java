package org.jasper.jLib.metpx.decoder;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.junit.Test;
import org.jasper.jLib.metpx.transformer.*;


public class TestBulletinDecoder {
	
	@Test
	/*
	 * This test case sends a valid bulletin byteArray message to be decoded.
	 * The bulletin object should not be null for the test to pass
	 */
	public void testValidMetpxMessage() {
		BulletinDecoder bulletinDecoder = new BulletinDecoder();
		String str = "WAUS41 KKCI 261901 AAA\n" +
			"WA1S\n" + 
			"BOSS WA 261901 AMD\n" + 
			"AIRMET SIERRA UPDT 3 FOR IFR AND MTN OBSCN VALID UNTIL 262100\n" +
			"AIRMET IFR...CT NY NJ PA WV MD DC DE VA AND CSTL WTRS...UPDT\n" +
			"FROM 50SSW ALB TO HTO TO 40NE ORF TO CSN TO HAR TO 50SSW ALB\n" +
			"CIG BLW 010/VIS BLW 3SM PCPN/BR. CONDS CONTG BYD 21Z THRU 03Z.\n" +
			"...NEW AIRMET...\n" +
			"AIRMET IFR...PA OH LE WV...UPDT\n" +
			"FROM 50W ERI TO 30SSE ERI TO HNN TO ROD TO 50W ERI\n" +
			"CIG BLW 010/VIS BLW 3SM PCPN/BR. CONDS ENDG 18-21Z.\n" +
			"...NEW AIRMET...\n" +
			"AIRMET MTN OBSCN...NY PA WV MD VA NC\n" +
			"FROM 20ENE SYR TO 30S PSB TO 20S HAR TO CSN TO 20N GSO TO 30NNE\n" +
			"SPA TO HMV TO HNN TO JHW TO 20ENE SYR\n" + 
			"MTNS OBSC BY CLDS/PCPN. CONDS CONTG BYD 21Z THRU 03Z.";
		
		byte[] msg = str.getBytes();
		
		Bulletin bulletin = bulletinDecoder.doDecode(msg);
		Assert.assertNotNull(bulletin);
	}
	
	@Test
	/*
	 * This test case sends an invalid byteArray message to be decoded as there
	 * are not enough rows (i.e. < 3). The bulletin object should be null
	 * for the test to pass
	 */
	public void testNotEnoughRows() {
		BulletinDecoder bulletinDecoder = new BulletinDecoder();
		String str = "WAUS41 KKCI 261901 AAA\n" +
			"WA1S\n" ;
		
		byte[] msg = str.getBytes();
		
		Bulletin bulletin = bulletinDecoder.doDecode(msg);
		Assert.assertNull(bulletin);
	}
	
	@Test
	/*
	 * This test case sends null to the decoder.  The bulletin object should
	 * be null and no exceptions for this test to pass
	 */
	public void testNullMessage() {
		BulletinDecoder bulletinDecoder = new BulletinDecoder();
		
		Bulletin bulletin = bulletinDecoder.doDecode(null);
		Assert.assertNull(bulletin);
	}
	
	@Test
	/*
	 * This test case invokes the getters on a Bulletin object as most setters are invoked
	 * by the decoder. The bulletin object should not be null for the test to pass
	 */
	public void testMetPxMessageClasses() {
		BulletinDecoder bulletinDecoder = new BulletinDecoder();
		String str = "WAUS41 KKCI 261901 AAA\n" +
			"WA1S\n" + 
			"BOSS WA 261901 AMD\n" + 
			"AIRMET SIERRA UPDT 3 FOR IFR AND MTN OBSCN VALID UNTIL 262100\n" +
			"AIRMET IFR...CT NY NJ PA WV MD DC DE VA AND CSTL WTRS...UPDT\n" +
			"FROM 50SSW ALB TO HTO TO 40NE ORF TO CSN TO HAR TO 50SSW ALB\n" +
			"CIG BLW 010/VIS BLW 3SM PCPN/BR. CONDS CONTG BYD 21Z THRU 03Z.\n" +
			"...NEW AIRMET...\n" +
			"AIRMET IFR...PA OH LE WV...UPDT\n" +
			"FROM 50W ERI TO 30SSE ERI TO HNN TO ROD TO 50W ERI\n" +
			"CIG BLW 010/VIS BLW 3SM PCPN/BR. CONDS ENDG 18-21Z.\n" +
			"...NEW AIRMET...\n" +
			"AIRMET MTN OBSCN...NY PA WV MD VA NC\n" +
			"FROM 20ENE SYR TO 30S PSB TO 20S HAR TO CSN TO 20N GSO TO 30NNE\n" +
			"SPA TO HMV TO HNN TO JHW TO 20ENE SYR\n" + 
			"MTNS OBSC BY CLDS/PCPN. CONDS CONTG BYD 21Z THRU 03Z.";
		
		byte[] msg = str.getBytes();
		Bulletin[] bulletins = new Bulletin[3];
		for(int i = 0; i < 2; i++) {
			Bulletin bulletin = bulletinDecoder.doDecode(msg);
			Assert.assertNotNull(bulletin);
			bulletins[i] = bulletin;
		}
		MetPxBoard board = new MetPxBoard(bulletins);
		board.getBoard();
		board.setBoard(bulletins);
		bulletins[0].getHeader();
		bulletins[0].getContent();
		bulletins[0].getCountry();
		bulletins[0].getFrom();
		bulletins[0].getDestination();
		bulletins[0].getScope();
		bulletins[0].getTime();
		bulletins[0].getOrigin();
	}
	
	@Test
	/*
	 * This test case invokes the methods on the ByteArrayToBulletin class
	 */
	public void testByteArrayToBulletin() {
		ByteArrayToBulletin transformer = new ByteArrayToBulletin();
		String str = "WAUS41 KKCI 261901 AAA\n" +
		"WA1S\n" + 
		"BOSS WA 261901 AMD\n" + 
		"AIRMET SIERRA UPDT 3 FOR IFR AND MTN OBSCN VALID UNTIL 262100\n" +
		"AIRMET IFR...CT NY NJ PA WV MD DC DE VA AND CSTL WTRS...UPDT\n" +
		"FROM 50SSW ALB TO HTO TO 40NE ORF TO CSN TO HAR TO 50SSW ALB\n" +
		"CIG BLW 010/VIS BLW 3SM PCPN/BR. CONDS CONTG BYD 21Z THRU 03Z.\n" +
		"...NEW AIRMET...\n" +
		"AIRMET IFR...PA OH LE WV...UPDT\n" +
		"FROM 50W ERI TO 30SSE ERI TO HNN TO ROD TO 50W ERI\n" +
		"CIG BLW 010/VIS BLW 3SM PCPN/BR. CONDS ENDG 18-21Z.\n" +
		"...NEW AIRMET...\n" +
		"AIRMET MTN OBSCN...NY PA WV MD VA NC\n" +
		"FROM 20ENE SYR TO 30S PSB TO 20S HAR TO CSN TO 20N GSO TO 30NNE\n" +
		"SPA TO HMV TO HNN TO JHW TO 20ENE SYR\n" + 
		"MTNS OBSC BY CLDS/PCPN. CONDS CONTG BYD 21Z THRU 03Z.";
	
		byte[] src = str.getBytes();
		try {
			Object myObj = transformer.transform(src);
			Assert.assertNotNull(myObj);
			myObj = transformer.transform("hello");
		} catch (Exception ex) {
			TestCase.fail("Exception caught");
		}
		
	}


}
