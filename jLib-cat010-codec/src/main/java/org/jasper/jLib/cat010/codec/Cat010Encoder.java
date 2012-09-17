package org.jasper.jLib.cat010.codec;
import java.util.Arrays;

public class Cat010Encoder {
		
	public static byte[] doEncode(Cat010Message msg) throws Exception{
		
		//Check if message to encode is supported cat010 message type
		if(!(msg instanceof Cat010PeriodicStatusMessage || msg instanceof Cat010TargetReport)){
			throw new Exception("Unable to encode msg, not a supported cat010 message type, msg : " + msg);
		}
		
		//The following fields are standard on all Cat010 messages
		byte[] category = {0x0a};     //category for asterix message, will always be 10 for this encoder
		byte[] length = {0,0};        //length of full message is stored in two bytes and will be updated at the end
		byte[] FSPEC = getFSPEC(msg);

		/*
		 * Below are all the Cat010 fields we support
		 */
		byte[] dataId = {msg.getDataSourceIdSac(),msg.getDataSourceIdSic()};
		byte[] msgType = {(byte)(msg.getMessageType().ordinal()+1)};
		byte[] targetReportDescriptor = getTargetReportDescriptor(msg);
		byte[] timeOfDay = getTimeOfDay(msg);
		byte[] posInCartCoord = getPositionInCartesianCoord(msg);
		byte[] calTrackVelInCartCoord = getCalulatedTrackVelInCartCoord(msg);
		byte[] trackNum = getTrackNumber(msg);
		byte[] trackStatus = getTrackStatus(msg);
		byte[] targetSizeAndOrientation = getTargetSizeAndOrientation(msg);
		byte[] systemStatus = getSystemStatus(msg);
		byte[] standardDeviationOfPosition = getStandardDeviationOfPosition(msg);
		byte[] subTracks = getSubTracks(msg);
		
		//We now combine all arrays into one single array
		byte[] result = mergeArrays(category,length,FSPEC,dataId,msgType,targetReportDescriptor,timeOfDay,posInCartCoord,calTrackVelInCartCoord,trackNum,trackStatus,targetSizeAndOrientation,systemStatus,standardDeviationOfPosition,subTracks);	
		
		//Update length of encoded message
		result[1] = (byte)((result.length & 0x0000ff00) >> 8);
		result[2] = (byte)((result.length & 0x000000ff));
		
		return result;
	}

	/*
	 * this method will merge all given byte arrays into a single array,
	 * it will ignore null arrays
	 */
	private static byte[] mergeArrays(byte[]...arrays){
	
		if (arrays.length < 1) return new byte[0];  // if no byte arrays are returned, we return an empty array
		if (arrays.length == 1) return arrays[0];  // if only one array is passed we return it
		
		byte[] mergedArray = {};
		
		/*
		 * we'll iterate over all arrays and merge them one at a time, we ignore null arrays
		 */
		for(int i = 0 ; i < arrays.length ; i++){
			if(arrays[i] == null)continue;
			byte[] tmp = new byte[mergedArray.length + arrays[i].length];
			System.arraycopy(mergedArray, 0, tmp, 0, mergedArray.length);
			System.arraycopy(arrays[i], 0, tmp, mergedArray.length, arrays[i].length);
			mergedArray = tmp;
		}
		return mergedArray;
	}
	
	private static byte[] getFSPEC(Cat010Message msg) throws Exception {	
		/*
		 * We support 2 types of Cat010 messages, Periodic Status Messages and
		 * Target Reports. Both will have a minimal FSPEC of 3 bytes, but may
		 * have a fourth. The last bit of the each byte is the FX (Field Extension
		 * Indicator) bit which indicates there is another byte of the FSPEC,
		 * since we have at least 3 bytes, the FX of byte 1 and 2 is by default
		 * set to 1.
		 */
		byte[] fspec = {1,1,0};
		
		/*
		 * for each supported field we will check if the value is not null, if
		 * not null than we will OR with the appropriate mask to create the
		 * FSPEC.
		 */
		
		// The following 3 fields are common to both types of supported Cat010 messages
		if(msg.getDataSourceIdSic() !=null && msg.getDataSourceIdSac() !=null) fspec[0] |= 0x80;
		if(msg.getMessageType() != null) fspec[0] |= 0x40;
		if(msg.getTimeOfDay() != null) fspec[0] |= 0x10;
		
		if(msg instanceof Cat010PeriodicStatusMessage){
			Cat010PeriodicStatusMessage psm = (Cat010PeriodicStatusMessage)msg;
			if(psm.getSystemStatus() != null) fspec[2] |= 0x04;
		}else if(msg instanceof Cat010TargetReport){
			Cat010TargetReport tr = (Cat010TargetReport)msg;
			/*
			 * all target reports have 4 byte FSPECs and so we grow the array and
			 * add the FX bit to the 3 byte
			 */
			fspec = Arrays.copyOf(fspec, 4);
			fspec[2] |= 0x01;
			
			if(tr.getTargetReportDescriptor() != null) fspec[0] |= 0x20;
			if(tr.getPositionInCartesianCoordX() != null && tr.getPositionInCartesianCoordY() != null) fspec[0] |= 0x02;

			if(tr.getCalTrackVelocityInCartesianCoordX() != null && tr.getCalTrackVelocityInCartesianCoordY() != null) fspec[1] |= 0x40;
			if(tr.getTrackNumber() != null) fspec[1] |= 0x20;
			if(tr.getTrackStatus() != null) fspec[1] |= 0x10;
			
			if(tr.getTargetLength() != null) fspec[2] |= 0x08;

			if(tr.getStandardDeviationOfX() != null && tr.getStandardDeviationOfY() != null && tr.getConvariance() != null) fspec[3] |= 0x80;
			if(tr.getSubTracks() != null) fspec[3] |= 0x04;
		}
	
		return fspec;
	}
	
	private static byte[] getTargetReportDescriptor(Cat010Message msg) {
		/*
		 * method returns null if message isn't supported or if value we are retrieving
		 * isn't set
		 */
		if(!(msg instanceof Cat010TargetReport)) return null;
		Cat010TargetReport tr = (Cat010TargetReport)msg;
		if(tr.getTargetReportDescriptor() == null) return null;
		
		Cat010TargetReportDescriptor trd = tr.getTargetReportDescriptor();
		byte[] desc = {0};
		
		//Refer to Cat010 spec for bit layout
		desc[0] |= (byte)(trd.getTyp().ordinal() << 5);
		desc[0] |= (byte)(trd.getDcr().ordinal() << 4);
		desc[0] |= (byte)(trd.getChn().ordinal() << 3);
		desc[0] |= (byte)(trd.getGbs().ordinal() << 2);
		desc[0] |= (byte)(trd.getCrt().ordinal() << 1);
		
		//if not null than we have second byte
		if(trd.getSim() != null){
			desc = Arrays.copyOf(desc, 2);
			desc[0] |= 0x01; //set FX bit
			desc[1] = 0; // ensure we start with all 0s
			desc[1] |= (byte)(trd.getSim().ordinal() << 7);
			desc[1] |= (byte)(trd.getTst().ordinal() << 6);
			desc[1] |= (byte)(trd.getRab().ordinal() << 5);
			desc[1] |= (byte)(trd.getLop().ordinal() << 3);
			desc[1] |= (byte)(trd.getTot().ordinal() << 1);
			
			//if not null than we have 3rd byte
			if(trd.getSpi() != null){
				desc = Arrays.copyOf(desc, 3);
				desc[1] |= 0x01; //set FX bit
				desc[2] = 0; // ensure we start with all 0s
				desc[2] |= (byte)(trd.getSpi().ordinal() << 7);
			}
		}
		
		return desc;
	}
		
	private static byte[] getTimeOfDay(Cat010Message msg){
		/*
		 * method returns null if value we are retrieving isn't set
		 */
		if(msg.getTimeOfDay() == null) return null;
		
		/*
		 * as per spec, time is encoded in 1/128 of a second
		 */
		double time = msg.getTimeOfDay();
		time *= 128;
		int timeOfDayInSec = (int)time;
		byte[] timeOfDay = {0,0,0};
		timeOfDay[0] = (byte)((timeOfDayInSec & 0x00ff0000) >> 16);
		timeOfDay[1] = (byte)((timeOfDayInSec & 0x0000ff00) >> 8);
		timeOfDay[2] = (byte)((timeOfDayInSec & 0x000000ff));
		return timeOfDay;
	}
	
	private static byte[] getPositionInCartesianCoord(Cat010Message msg) {
		/*
		 * method returns null if message isn't supported or if value we are retrieving isn't set
		 */
		if(!(msg instanceof Cat010TargetReport)) return null;
		Cat010TargetReport tr = (Cat010TargetReport)msg;
		if(tr.getPositionInCartesianCoordX() == null || tr.getPositionInCartesianCoordY() == null) return null;
		
		byte[] pos = new byte[4];
		int x = tr.getPositionInCartesianCoordX();
		int y = tr.getPositionInCartesianCoordY();
		
		pos[0] = ((byte)(x >> 8));
		pos[1] = ((byte)(x & 0xff));
		pos[2] = ((byte)(y >> 8));
		pos[3] = ((byte)(y & 0xff));
		
		return pos;
	}
	
	private static byte[] getCalulatedTrackVelInCartCoord(Cat010Message msg) {
		/*
		 * method returns null if message isn't supported or if value we are retrieving isn't set
		 */
		if(!(msg instanceof Cat010TargetReport)) return null;
		Cat010TargetReport tr = (Cat010TargetReport)msg;
		if(tr.getCalTrackVelocityInCartesianCoordX() == null || tr.getCalTrackVelocityInCartesianCoordY() == null) return null;
		
		byte[] vel = new byte[4];
		double vX = tr.getCalTrackVelocityInCartesianCoordX();
		double vY = tr.getCalTrackVelocityInCartesianCoordY();
		
		int x = (int)(vX * 4);
		int y = (int)(vY * 4);
		
		vel[0] = ((byte)(x >> 8));
		vel[1] = ((byte)(x & 0xff));
		vel[2] = ((byte)(y >> 8));
		vel[3] = ((byte)(y & 0xff));
		
		return vel;
	}
	
	private static byte[] getTrackNumber(Cat010Message msg){
		/*
		 * method returns null if message isn't supported or if value we are retrieving isn't set
		 */
		if(!(msg instanceof Cat010TargetReport)) return null;
		Cat010TargetReport tr = (Cat010TargetReport)msg;
		if(tr.getTrackNumber() == null) return null;
		
		byte[] trackNumber = new byte[2];
		
		int tNum = tr.getTrackNumber();
		trackNumber[0] = (byte)((tNum >> 8) & 0x0f);
		trackNumber[1] = (byte)(tNum & 0xff);
		
		return trackNumber;
	}
	
	private static byte[] getTrackStatus(Cat010Message msg){
		/*
		 * method returns null if message isn't supported or if value we are retrieving isn't set
		 */
		if(!(msg instanceof Cat010TargetReport)) return null;
		Cat010TargetReport tr = (Cat010TargetReport)msg;
		if(tr.getTrackStatus() == null) return null;
		
		Cat010TrackStatus status = tr.getTrackStatus();
		byte[] trackStatus = {0};
		
		trackStatus[0] |= (byte)(status.getCnf().ordinal() << 7);
		trackStatus[0] |= (byte)(status.getTre().ordinal() << 6);
		trackStatus[0] |= (byte)(status.getCst().ordinal() << 4);
		trackStatus[0] |= (byte)(status.getMah().ordinal() << 3);
		trackStatus[0] |= (byte)(status.getTcc().ordinal() << 2);
		trackStatus[0] |= (byte)(status.getSth().ordinal() << 1);
		
		//if not null than we have second byte
		if(status.getTom() != null){
			trackStatus = Arrays.copyOf(trackStatus, 2);
			trackStatus[0] |= 0x01; //set FX bit
			trackStatus[1] = 0; // ensure we start with all 0s
			trackStatus[1] |= (byte)(status.getTom().ordinal() << 6);
			trackStatus[1] |= (byte)(status.getDou().ordinal() << 3);
			trackStatus[1] |= (byte)(status.getMrs().ordinal() << 1);
			
			//if not null than we have 3rd byte
			if(status.getGho() != null){
				trackStatus = Arrays.copyOf(trackStatus, 3);
				trackStatus[1] |= 0x01; //set FX bit
				trackStatus[2] = 0; // ensure we start with all 0s
				trackStatus[2] |= (byte)(status.getGho().ordinal() << 7);
			}
		}
		
		return trackStatus;
	}
	
	private static byte[] getTargetSizeAndOrientation(Cat010Message msg){
		/*
		 * method returns null if message isn't supported or if value we are retrieving isn't set
		 */
		if(!(msg instanceof Cat010TargetReport)) return null;
		Cat010TargetReport tr = (Cat010TargetReport)msg;
		if(tr.getTargetLength() == null) return null;
		
		byte[] sizeAndOrientation = {0};
		
		sizeAndOrientation[0] |= (byte)(tr.getTargetLength() << 1);
		
		//if not null than we have second byte
		if(tr.getTargetOrientation() != null){
			sizeAndOrientation = Arrays.copyOf(sizeAndOrientation, 2);
			sizeAndOrientation[0] |= 0x01; //set FX bit
			sizeAndOrientation[1] = 0; // ensure we start with all 0s
			
			double orientation = tr.getTargetOrientation();
			
			int targetOrientation = (int)(orientation / (2.8125));

			sizeAndOrientation[1] |= (byte)(targetOrientation << 1);
			
			//if not null than we have 3rd byte
			if(tr.getTargetWidth() != null){
				sizeAndOrientation = Arrays.copyOf(sizeAndOrientation, 3);
				sizeAndOrientation[1] |= 0x01; //set FX bit
				sizeAndOrientation[2] = 0; // ensure we start with all 0s
				sizeAndOrientation[2] |= (byte)(tr.getTargetWidth() << 1);
			}
		}
		
		return sizeAndOrientation;
	}
	
	private static byte[] getSystemStatus(Cat010Message msg) {
		/*
		 * method returns null if message isn't supported or if value we are retrieving isn't set
		 */
		if(!(msg instanceof Cat010PeriodicStatusMessage)) return null;
		Cat010PeriodicStatusMessage psm = (Cat010PeriodicStatusMessage)msg;
		Cat010SystemStatus sStatus = psm.getSystemStatus();
		
		byte[] status = {0};
		status[0] |= ((byte) (sStatus.getNogo().ordinal() << 6));
		status[0] |= ((byte) (sStatus.getOvl().ordinal() << 5));
		status[0] |= ((byte) (sStatus.getTsv().ordinal() << 4));
		status[0] |= ((byte) (sStatus.getDiv().ordinal() << 3));
		status[0] |= ((byte) (sStatus.getTtf().ordinal() << 2));
		
		return status;
	}
	
	private static byte[] getStandardDeviationOfPosition(Cat010Message msg){
		/*
		 * method returns null if message isn't supported or if value we are retrieving isn't set
		 */
		if(!(msg instanceof Cat010TargetReport)) return null;
		Cat010TargetReport tr = (Cat010TargetReport)msg;
		if(tr.getStandardDeviationOfX() == null || tr.getStandardDeviationOfY() == null || tr.getConvariance() == null) return null;
		
		byte[] std = new byte[4];
		double stdX = tr.getStandardDeviationOfX();
		double stdY = tr.getStandardDeviationOfY();
		double conv = tr.getConvariance();
		
		int x = (int)(stdX * 4);
		int y = (int)(stdY * 4);
		int c = (int)(conv * 4);
		std[0] = (byte)x;
		std[1] = (byte)y;
		std[2] = ((byte)(c >> 8));
		std[3] = ((byte)(c & 0xff));
		
		return std;
	}
	
	private static byte[] getSubTracks(Cat010Message msg){
		/*
		 * method returns null if message isn't supported or if value we are retrieving isn't set
		 */
		if(!(msg instanceof Cat010TargetReport)) return null;
		Cat010TargetReport tr = (Cat010TargetReport)msg;
		if(tr.getSubTracks() == null) return null;
		
		byte[] subTracks = {0};
		
		for(Integer trackId:tr.getSubTracks().keySet()){
			byte[] trackArray = {0,0,0,0};
			Cat010SubTrack track = tr.getSubTrack(trackId);
			
			int id = track.getTrackId();
			trackArray[0] = (byte)(id >> 8);
			trackArray[1] = (byte)(id & 0xff);
			
			int sensorId = track.getSensorId();
			trackArray[2] = (byte)sensorId;
			trackArray[3] = (byte)(track.getType().ordinal() << 5);
			
			//always set the fx bit and unset it at the end
			trackArray[3] |= 0x01;
			
			subTracks = mergeArrays(subTracks,trackArray);
		}
		
		//set final fx bit to 0
		subTracks[subTracks.length-1] &= 0xfffe;
		
		//set length
		subTracks[0] = (byte)(subTracks.length);
		return subTracks;
	}
	

	public static void main(String args[]) throws Exception{

		byte[] msg = {(byte)0x0a,(byte)0x00,(byte)0x0d,(byte)0xd1,(byte)0x01,
			          (byte)0x04,(byte)0x00,(byte)0x01,(byte)0x03,(byte)0xa3,
			          (byte)0x7d,(byte)0x5c,(byte)0x80};
		
		byte[] msg_decode = doEncode(Cat010Decoder.doDecode(msg));
		
		byte[] msg1 = {(byte)0x0a,(byte)0x00,(byte)0x0d,(byte)0xd1,(byte)0x01,
				       (byte)0x04,(byte)0x00,(byte)0x01,(byte)0x03,(byte)0xa3,
				       (byte)0x7d,(byte)0x5c,(byte)0x80};
		
		byte[] msg1_decode = doEncode(Cat010Decoder.doDecode(msg1));
		
		byte[] msg2 = {(byte)0x0a,(byte)0x00,(byte)0x22,(byte)0xf3,(byte)0x31,
				       (byte)0x09,(byte)0x84,(byte)0x00,(byte)0x01,(byte)0x01,
				       (byte)0xe1,(byte)0x0c,(byte)0xa3,(byte)0x83,(byte)0x1c,
				       (byte)0x0d,(byte)0x8a,(byte)0x09,(byte)0x82,(byte)0x00,
				       (byte)0x02,(byte)0x82,(byte)0x0b,(byte)0x01,(byte)0x28,
				       (byte)0x01,(byte)0x01,(byte)0x00,(byte)0x00,(byte)0x05,
				       (byte)0x00,(byte)0x02,(byte)0x01,(byte)0x40};
		
		byte[] msg2_decode = doEncode(Cat010Decoder.doDecode(msg2));
		
		byte[] msg3 = {(byte)0x0a,(byte)0x00,(byte)0x26,(byte)0xf3,(byte)0x71,
				       (byte)0x09,(byte)0x84,(byte)0x00,(byte)0x01,(byte)0x01,
				       (byte)0xe1,(byte)0x0c,(byte)0xa3,(byte)0x83,(byte)0x5c,
				       (byte)0x0d,(byte)0x8b,(byte)0x09,(byte)0x80,(byte)0xff,
				       (byte)0xf4,(byte)0x00,(byte)0x05,(byte)0x00,(byte)0x02,
				       (byte)0x82,(byte)0x0b,(byte)0x5f,(byte)0x2a,(byte)0x01,
				       (byte)0x01,(byte)0x00,(byte)0x00,(byte)0x05,(byte)0x00,
				       (byte)0x02,(byte)0x01,(byte)0x00};
		
		byte[] msg3_decode = doEncode(Cat010Decoder.doDecode(msg3));
		
		
		System.out.println(Arrays.equals(msg, msg_decode));
		System.out.println(Arrays.equals(msg1, msg1_decode));
		System.out.println(Arrays.equals(msg2, msg2_decode));
		System.out.println(Arrays.equals(msg3, msg3_decode));



	}
	
}
