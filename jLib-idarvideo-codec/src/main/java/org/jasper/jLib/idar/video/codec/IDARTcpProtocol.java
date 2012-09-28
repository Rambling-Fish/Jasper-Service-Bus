package org.jasper.jLib.idar.video.codec;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import org.mule.transport.tcp.protocols.AbstractByteProtocol;


public class IDARTcpProtocol extends AbstractByteProtocol
{
    
    private static final int READ_BUFFER_SIZE = 4096;
    private static final int PUSHBACK_BUFFER_SIZE = READ_BUFFER_SIZE * 2;
    
    /*
     * NOTE: According to the IntelliDAR ICD the start token is 0xAA55AA55 and
     * the end token is 0x55AA55AA, however the binary portion of the IDAR message
     * which includes the start token, end token, length, sequence number, timestamp
     * and checksum are all encoded in little-endian, so to simplify our search for the
     * start and end token we have encoded them in the byte array in little-endian,
     * that is, least significant byte first. This causes the start and end token to
     * appear reversed.
     */
    private static final byte[] START_TOKEN = {(byte)0x55,(byte)0xAA,(byte)0x55,(byte)0xAA};
    private static final byte[] END_TOKEN   = {(byte)0xAA,(byte)0x55,(byte)0xAA,(byte)0x55};
    
    private static final String START_TOKEN_STRING = new String(START_TOKEN);
    private static final String END_TOKEN_STRING = new String(END_TOKEN);
    
	/*
	 * Each message has an extra 28 bytes, which is broken down as follows:
	 *   - Start token - 4 bytes
	 *   - Body Length - 4 bytes
	 *   - Sequence #  - 4 bytes
	 *   - TimeStamp   - 8 bytes
	 *   - Checksum    - 4 bytes
	 *   - End token   - 4 bytes
	 */  
    private static final int NUMBER_OF_EXTRA_BYTES = 28;

    private ConcurrentMap<InputStream, PushbackInputStream> pbMap = new ConcurrentHashMap<InputStream, PushbackInputStream>();

    public IDARTcpProtocol(){
        super(STREAM_OK);
    }

    public Object read(InputStream is) throws IOException
    {
    	PushbackInputStream pbis = (PushbackInputStream) pbMap.get(is);
        if (null == pbis) {
            pbis = new PushbackInputStream(is, PUSHBACK_BUFFER_SIZE);
            PushbackInputStream prev = (PushbackInputStream) pbMap.putIfAbsent(is, pbis);
            pbis = null == prev ? pbis : prev;
        }

        int len = -1;
        try
        {
            byte[] buffer = new byte[READ_BUFFER_SIZE];
            String result = "";
            boolean repeat;
            do
            {
            	//This copies data from the pushback buffer to our temporary buffer
                len = safeRead(pbis, buffer);
                if (len >= 0)
                {
                	/*
                	 * We build up the byte array in a string, this allows us to efficiently
                	 * check for start and end tokens, we will convert the string back to a
                	 * byte array before returning from this method
                	 */
                    result += new String(buffer);
                    repeat = !( (result.contains(START_TOKEN_STRING) && result.contains(END_TOKEN_STRING)) );
                }
                else
                {
                    // never repeat on closed stream (and avoid calling available)
                    repeat = false;
                }

            }
            while (repeat);


            byte[] bytesToPushBack = {};
            
            /*
             * following block will strip out the message only and create a byte array
             * with any additional bytes after the end token to be pushed back to the
             * pushback buffer, this prevents us from loosing data from the next message
             */
            if(result.contains(START_TOKEN_STRING) && result.contains(END_TOKEN_STRING)){
            	int start = result.indexOf(START_TOKEN_STRING);
            	int end = result.indexOf(END_TOKEN_STRING);
            	bytesToPushBack = result.substring(end + END_TOKEN_STRING.length()).getBytes();
            	result = result.substring(start, end + END_TOKEN_STRING.length());
            }
            
            pbis.unread(bytesToPushBack);
            
            /*
             * If the message is valid we return the byte array, if not we return
             * an empty array and drop the data we as this message is most likely 
             * corrupted
             */
            boolean debug = false;
            
            
            if(isValidMessage(result.getBytes())){
            	/*
            	 * The following debug code will convert the string into a readable
            	 * hex format and return that so we can easily debug the byte stream
            	 */
            	if (debug){
	            	System.out.println("VALID STRING");
	    			StringBuilder sb = new StringBuilder();
	    		    for (byte b : result.getBytes()) {
	    		        sb.append(String.format("%02X ", b));
	    		    }
	    		    return nullEmptyArray(("VALID STRING : " + sb.toString()).getBytes());
            	} else {
            		
	    		    return nullEmptyArray(result.getBytes());
            	}    
            }else{
            	/*
            	 * The following debug code will convert the string into a readable
            	 * hex format and return that so we can easily debug the byte stream
            	 */
            	if (debug){
	            	System.out.println("INVALID STRING");
	    			StringBuilder sb = new StringBuilder();
	    		    for (byte b : result.getBytes()) {
	    		        sb.append(String.format("%02X ", b));
	    		    }
	    		    return nullEmptyArray(("INVALID STRING : " + sb.toString()).getBytes());
            	} else {
            		
            		System.out.println("INVALID STRING");
            		System.out.println(" ");
            		return nullEmptyArray(new byte[0]);
            	}	
            }
            
        }
        finally {
            // clear from map if stream has ended
        	if (len < 0){
                pbMap.remove(is);
            }
        }
    }
    
    /*
     * This method will check that the message has the start and end token at the
     * beginning and end respectively. It will also check that the message body
     * length corresponds to what len value found in the message.
     * 
     */
	private boolean isValidMessage(byte[] message) {
		if(message.length < NUMBER_OF_EXTRA_BYTES) return false;
		
		boolean startTokenAtStart = ((message[0] == START_TOKEN[0]) &&
                                     (message[1] == START_TOKEN[1]) &&
                                     (message[2] == START_TOKEN[2]) &&
                                     (message[3] == START_TOKEN[3]));
		
		boolean endTokenAtEnd =  ((message[message.length - 1] == END_TOKEN[END_TOKEN.length - 1]) &&
				                  (message[message.length - 2] == END_TOKEN[END_TOKEN.length - 2]) &&
				                  (message[message.length - 3] == END_TOKEN[END_TOKEN.length - 3]) &&
			                      (message[message.length - 4] == END_TOKEN[END_TOKEN.length - 4]));
		
		
		
		int len = 0;
		for (int i = 3 ; i >= 0 ;  i--){
			len <<= 8;
			len |= (message[START_TOKEN.length + i] & 0xff);
		}
		
		long chcksum = 0;
		// TODO create a loop for checksum
		int fourbytes = 0;
			fourbytes |= (message[message.length - END_TOKEN.length -1] & 0xff);
			fourbytes <<=8;
			fourbytes |= (message[message.length - END_TOKEN.length -2] & 0xff);
		chcksum |= (fourbytes & 0xffff);
			fourbytes = 0;
			fourbytes |= (message[message.length - END_TOKEN.length -3] & 0xff);
			fourbytes <<=8;
			fourbytes |= (message[message.length - END_TOKEN.length -4] & 0xff);
		chcksum <<=16;
		chcksum |= (fourbytes & 0xffff);
		
		

		String messageString = new String(message);
		String startString = "<JP";
		String endString   = "</IntelliDAR>";
		
		int messageBodyLength = messageString.indexOf(endString) + endString.length() - messageString.indexOf(startString);
		long messageBodyChecksum = crc32( messageString.substring( messageString.indexOf(startString), endString.length() + messageString.indexOf(endString) ) );
		
		boolean messageBodyLengthSameAsLenValue = len == messageBodyLength;
		if (!messageBodyLengthSameAsLenValue){
			System.out.println("Invalid Message: Checksum does not match");
			System.out.println("Msg body Checksum: " + chcksum + "Msg calc Checksum: " + messageBodyChecksum);
		}
		//TODO compare CRC-32 Checksum of the body of the message
		
		//return startTokenAtStart && endTokenAtEnd && messageBodyLengthSameAsLenValue;
		return startTokenAtStart && endTokenAtEnd;
	}
	
	private static long crc32(String str){

        byte bytes[] = str.getBytes();
       
        Checksum checksum = new CRC32();         
        checksum.update(bytes,0,bytes.length);

        return checksum.getValue();
	}

}