package org.jasper.jTestApp.util;

/*
 * Class will generate a byte array of Asterix Category 010
 * Target Reports updating the time and position of the target 
 */
public class Cat010ByteArrayGenerator {
	
	//Initial Target Report encoded as byte array
	private static byte[] msg = {(byte)0x0a,(byte)0x00,(byte)0x26,(byte)0xf3,(byte)0x71,
		       (byte)0x09,(byte)0x84,(byte)0x00,(byte)0x01,(byte)0x01,
		       (byte)0xe1,(byte)0x0c,(byte)0xa3,(byte)0x83,(byte)0x5c,
		       (byte)0x0d,(byte)0x8b,(byte)0x09,(byte)0x80,(byte)0xff,
		       (byte)0xf4,(byte)0x00,(byte)0x05,(byte)0x00,(byte)0x02,
		       (byte)0x82,(byte)0x0b,(byte)0x5f,(byte)0x2a,(byte)0x01,
		       (byte)0x01,(byte)0x00,(byte)0x00,(byte)0x05,(byte)0x00,
		       (byte)0x02,(byte)0x01,(byte)0x00};
	
	//static method to update time and position and return array
	public static byte[] getByteArray(){
		updateTimeOfDay();
		updatePosition();
		return msg;
	}
	
	/*
	 * we ignore the string parameter, as our mule flow will
	 * look for a method with a String parameter, we simply 
	 * call the parameter-less version of this method
	 */
	public static byte[] getByteArry(String str){
		return getByteArray();
	}

	/*
	 * We directly update the x and y component in the byte array
	 */
	private static void updatePosition() {
		/*
		 * The position bytes in the target report are always bytes
		 * 16-19 (indices 15-18). The x component in 15-16 and y in
		 * 17-18. The position is a 2 byte 2's complement signed
		 * integer
		 */
		int xCompByte1 = (int)msg[15];  
		int xCompByte2 = (int)msg[16];
		int yCompByte1 = (int)msg[17];
		int yCompByte2 = (int)msg[18];
		
		/*
		 * We shift the first byte of x and y by 8 so we can OR them
		 * with the second byte, shifting left will preserve the sign
		 */
		xCompByte1 <<= 8;
		yCompByte1 <<= 8;

		/*
		 * Casting to an int preserves the sign, however byte 2
		 * of the 2 byte coordinate doesn't contain the sign, byte
		 * 1 does, so we need to 0 out all bits greater then 8 in
		 * byte 2 for both x and y, we do this with the mask 0x00ff
		 */
		xCompByte2 &= 0x00ff;
		yCompByte2 &= 0x00ff;
		
		//We combine the first and second bytes of x and y by OR'ing
		xCompByte1 |= xCompByte2;
		yCompByte1 |= yCompByte2;
		
		//Increment each x and y
		xCompByte1++;
		yCompByte1++;
		
		/*
		 * We now update the array with the updated values for 
		 * x and y.
		 */
		msg[15] = (byte)((xCompByte1 & 0xff00) >> 8);
		msg[16] = (byte)(xCompByte1 & 0x00ff);
		msg[17] = (byte)((yCompByte1 & 0xff00) >> 8);
		msg[18] = (byte)(yCompByte1 & 0x00ff);
		
	}

	/*
	 * we directly update the time bytes in the array
	 */
	private static void updateTimeOfDay() {
		/*
		 * The time bytes in the target report are always bytes
		 * 13-15 (indices 12-14). Time is stored as a 3 byte
		 * unsigned integer
		 */
		int timeOfDayByte1 = (int)msg[12];  
		int timeOfDayByte2 = (int)msg[13];
		int timeOfDayByte3 = (int)msg[14];
		
		/*
		 * as each byte was cast into an int (4 bytes)
		 * we want to ensure that all bits other than
		 * the first 8 are zeroed so we AND with 0xff
		 */
		timeOfDayByte1 &= 0xff;
		timeOfDayByte2 &= 0xff;
		timeOfDayByte3 &= 0xff;
		
		/*
		 * We shift the first byte by 16 bits
		 * and the second byte by 8 bits before
		 * OR'ing them to get our 3 byte integer
		 */
		timeOfDayByte1 <<= 16;
		timeOfDayByte2 <<= 8;
		
		//OR all bytes into the first
		timeOfDayByte1 |= timeOfDayByte2;
		timeOfDayByte1 |= timeOfDayByte3;
		
		//Increment the time by 1
		timeOfDayByte1 ++;
		
		/*
		 * We need to break up the 4 byte int into 3 bytes and store
		 * them respectively, we'll AND with the appropriate mask and
		 * then shift the bits of the first and second bytes
		 */
		msg[12] = (byte)((timeOfDayByte1 & 0x00ff0000) >> 16);
		msg[13] = (byte)((timeOfDayByte1 & 0x0000ff00) >> 8);
		msg[14] = (byte)((timeOfDayByte1 & 0x000000ff));		
	}
	
}
