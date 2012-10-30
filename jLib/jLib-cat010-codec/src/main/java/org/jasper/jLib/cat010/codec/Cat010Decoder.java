package org.jasper.jLib.cat010.codec;
import java.util.Arrays;

import org.jasper.jLib.cat010.codec.Cat010SubTrack.Cat010SubTrackType;
import org.jasper.jLib.cat010.codec.Cat010SystemStatus.DIV;
import org.jasper.jLib.cat010.codec.Cat010SystemStatus.NOGO;
import org.jasper.jLib.cat010.codec.Cat010SystemStatus.OVL;
import org.jasper.jLib.cat010.codec.Cat010SystemStatus.TSV;
import org.jasper.jLib.cat010.codec.Cat010SystemStatus.TTF;
import org.jasper.jLib.cat010.codec.Cat010TargetReportDescriptor.CHN;
import org.jasper.jLib.cat010.codec.Cat010TargetReportDescriptor.CRT;
import org.jasper.jLib.cat010.codec.Cat010TargetReportDescriptor.DCR;
import org.jasper.jLib.cat010.codec.Cat010TargetReportDescriptor.GBS;
import org.jasper.jLib.cat010.codec.Cat010TargetReportDescriptor.LOP;
import org.jasper.jLib.cat010.codec.Cat010TargetReportDescriptor.RAB;
import org.jasper.jLib.cat010.codec.Cat010TargetReportDescriptor.SIM;
import org.jasper.jLib.cat010.codec.Cat010TargetReportDescriptor.SPI;
import org.jasper.jLib.cat010.codec.Cat010TargetReportDescriptor.TOT;
import org.jasper.jLib.cat010.codec.Cat010TargetReportDescriptor.TST;
import org.jasper.jLib.cat010.codec.Cat010TargetReportDescriptor.TYP;
import org.jasper.jLib.cat010.codec.Cat010TrackStatus.CNF;
import org.jasper.jLib.cat010.codec.Cat010TrackStatus.CST;
import org.jasper.jLib.cat010.codec.Cat010TrackStatus.DOU;
import org.jasper.jLib.cat010.codec.Cat010TrackStatus.GHO;
import org.jasper.jLib.cat010.codec.Cat010TrackStatus.MAH;
import org.jasper.jLib.cat010.codec.Cat010TrackStatus.MRS;
import org.jasper.jLib.cat010.codec.Cat010TrackStatus.STH;
import org.jasper.jLib.cat010.codec.Cat010TrackStatus.TCC;
import org.jasper.jLib.cat010.codec.Cat010TrackStatus.TOM;
import org.jasper.jLib.cat010.codec.Cat010TrackStatus.TRE;

public class Cat010Decoder {
		
	//First set of FSPEC masks
	private static final byte dataSourceIdentifierMask         = (byte)0x80;
	private static final byte messageTypeMask                  = (byte)0x40;
	private static final byte targetReportDescriptorMask       = (byte)0x20;
	private static final byte timeOfDayMask                    = (byte)0x10;
	private static final byte positionInCartesianCoordMask     = (byte)0x02;
	private static final byte firstFieldExtensionIndicatorMask = (byte)0x01;
	
	//Second set of FSPEC Masks
	private static final byte calTrackVelocityInCartesianCoordMask = (byte)0x40;
	private static final byte trackNumberMask                      = (byte)0x20;
	private static final byte trackStatusMask                      = (byte)0x10;
	private static final byte secondFieldExtensionIndicatorMask    = (byte)0x01;
	
	//Third set of FSPEC Masks
	private static final byte targetSizeAndOrientationMask     = (byte)0x08;
	private static final byte systemStatusMask                 = (byte)0x04;
	private static final byte thirdFieldExtensionIndicatorMask = (byte)0x01; 
	
	//Fourth set of FSPEC Masks
	private static final byte standardDeviationofPositionMask   = (byte)0x80;
	private static final byte subTrackMask                      = (byte)0x04;
	private static final byte fourthFieldExtensionIndicatorMask = (byte)0x01;
	
	
	public static Cat010Message doDecode(byte[] array) throws Exception{
		
		if(array == null) return null;
		
		//Check if message is CAT010
		if (!isMessageCat010(array)) throw new Exception("Message not an CAT010 message. MSG = " + array);
		
		//Check if message is complete, i.e. array length matches the size indicated by the LEN field in message
		if (!isMessageComplete(array)) throw new Exception("CAT010 message not complete or corrupted. MSG = " + array);
		
		//Get FSPEC and verify only supported Data Items are specified
		byte[] fspec = getFSPEC(array);
		if (!isFspecSupported(fspec)) throw new Exception("FSPEC specifies Data Items that aren't currently supported : FSPEC = " + fspec);
		
		//The record is everything after the FSPEC
		byte[] record = Arrays.copyOfRange(array, (3 + fspec.length), array.length);
		
		//Create msg based on type found in Byte 3 of record
		Cat010Message msg;
		switch (record[2]){
		case 1:
			msg = new Cat010TargetReport();
			break;
		case 2:
			throw new Exception("Received Start of Update Cycle Message - currently not supported");
		case 3:
			msg = new Cat010PeriodicStatusMessage();
			break;
		case 4:
			throw new Exception("Received Event-triggered Status Message - currently not supported");
		default:
			throw new Exception("Unknown Message type");
		}
		
		/*
		 * Each line in the following block will compare the FSPEC with the mask
		 * of each supported data field, if present in the FSPEC then we run the
		 * corresponding decode and set method, named set<Data_Field_Name>. These
		 * set of methods all assume that the data field is encoded at the beginning
		 * of the record, they will decode the data field, set the appropriate values
		 * in the Cat010Message, remove the decoded data field from the record and
		 * then return the new record which is ready to be decoded by the next method.
		 * It is assumed that once we hit the field extension indicator bit in the
		 * FSPEC, if it's 0, indicating no more data fields then the record should
		 * be empty, as all data items would have been decoded, if it is not we throw
		 * an exception.
		 */
		
		if((fspec[0] & Cat010Decoder.dataSourceIdentifierMask) != 0)     record = setDataSourceIdentifier(msg,record);
		if((fspec[0] & Cat010Decoder.messageTypeMask) != 0)              record = removeBytesFromArray(1,record); //no need to set as type was already determined, only remove byte
		if((fspec[0] & Cat010Decoder.targetReportDescriptorMask) != 0)   record = setTargetReportDescriptor(msg,record);
		if((fspec[0] & Cat010Decoder.timeOfDayMask) != 0)                record = setTimeOfDay(msg,record);
		if((fspec[0] & Cat010Decoder.positionInCartesianCoordMask) != 0) record = setPositionInCartesianCoord(msg,record);
		if((fspec[0] & Cat010Decoder.firstFieldExtensionIndicatorMask) == 0){
			//If the fieldExtensionIndicator is 0 then we have nothing else to decode and record should be empty
			if (record.length != 0){
				throw new Exception("CAT010 message not properly decoded, first FieldExtensionIndicator set to 0 however there are more bytes to decode");
			}
			return msg;
		}
		
		if((fspec[1] & Cat010Decoder.calTrackVelocityInCartesianCoordMask) != 0) record = setCalTrackVelocityInCartesianCoord(msg,record);
		if((fspec[1] & Cat010Decoder.trackNumberMask) != 0)                      record = setTrackNumber(msg,record);
		if((fspec[1] & Cat010Decoder.trackStatusMask) != 0)                      record = setTrackStatus(msg,record);
		if((fspec[1] & Cat010Decoder.secondFieldExtensionIndicatorMask) == 0){
			//If the fieldExtensionIndicator is 0 then we have nothing else to decode and record should be empty
			if (record.length != 0){
				throw new Exception("CAT010 message not properly decoded, second FieldExtensionIndicator set to 0 however there are more bytes to decode");
			}
			return msg;
		}
		
		if((fspec[2] & Cat010Decoder.targetSizeAndOrientationMask) != 0) record = setTargetSizeAndOrientation(msg,record);
		if((fspec[2] & Cat010Decoder.systemStatusMask) != 0)             record = setSystemStatus(msg,record);
		if((fspec[2] & Cat010Decoder.thirdFieldExtensionIndicatorMask) == 0){
			//If the fieldExtensionIndicator is 0 then we have nothing else to decode and record should be empty
			if (record.length != 0){
				throw new Exception("CAT010 message not properly decoded, third FieldExtensionIndicator set to 0 however there are more bytes to decode");
			}
			return msg;
		}
		
		if((fspec[3] & Cat010Decoder.standardDeviationofPositionMask) != 0) record = setStandardDeviationofPosition(msg,record);
		if((fspec[3] & Cat010Decoder.subTrackMask) != 0)                    record = setSubTrack(msg,record);
		if((fspec[3] & Cat010Decoder.fourthFieldExtensionIndicatorMask) == 0){
			//If the fieldExtensionIndicator is 0 then we have nothing else to decode and record should be empty
			if (record.length != 0){
				throw new Exception("CAT010 message not properly decoded, fourth FieldExtensionIndicator set to 0 however there are more bytes to decode");
			}
			return msg;
		}
		throw new Exception("CAT010 message not properly decoded, the fourth FieldExtensionIndicator was set to 1 indicating more data items then defined in the spec");
	}
	
	private static byte[] setSubTrack(Cat010Message msg, byte[] record) throws Exception {
		Cat010TargetReport message;
		if(msg instanceof Cat010TargetReport){
			message = (Cat010TargetReport) msg;
		}else{
			throw new Exception("Cannot set SubTracks on non-TargetReport message");
		}
		
		int numOfBytes = 1;
		boolean fieldExtensionIndicator = true;
		
		int expectedNumOfBytes = (int)record[0];
		/*
		 * See note regarding casting from byte to int in
		 * setStandardDeviationofPosition(..);
		 */
		expectedNumOfBytes &= 0x00ff;
		
		do{
			int trackId = ((((int)record[numOfBytes]) & 0x00ff) << 8 ) | (((int)record[numOfBytes + 1] & 0x00ff));
			int sensorId = (((int)record[numOfBytes + 2]) & 0x00ff);
			
			int finalByte = (((int)record[numOfBytes + 3]) & 0x00ff);
			Cat010SubTrackType type = Cat010SubTrackType.values()[((finalByte & 0xe0) >>> 5)];
			
			fieldExtensionIndicator = (finalByte & 0x01) == 1;
			
			Cat010SubTrack subTrack = new Cat010SubTrack(trackId, sensorId, type);
			
			message.addSubTrack(trackId, subTrack);
			numOfBytes += 4;
		}while(fieldExtensionIndicator);
	
		if(numOfBytes != expectedNumOfBytes) throw new Exception("SubTrack error, number of actual bytes different than what was specified in LEN field of SubTrack");
		
		return Arrays.copyOfRange(record, numOfBytes, record.length);
	}

	private static byte[] setStandardDeviationofPosition(Cat010Message msg,byte[] record) throws Exception{
		Cat010TargetReport message;
		if(msg instanceof Cat010TargetReport){
			message = (Cat010TargetReport) msg;
		}else{
			throw new Exception("Cannot set staandard deviation on non-TargetReport message");
		}
		
		int stdDevX = (int)record[0];  
		int stdDevY = (int)record[1];
		int convByte1 = (int)record[2];
		int convByte2 = (int)record[3];

		/*
		 * NOTE : Casting from byte to int
		 * Standard deviation is a positive number and so the bytes in
		 * the record are unsigned, however bytes in java are signed,
		 * so we must 0 the higher bits of the integer after casting
		 * from a byte as java will perform a signed cast, i.e.
		 * byte 1110 0001 = 225  will become 
		 * int 1111 1111 1111 1111 1111 1111 1110 0001 =  -31
		 * after & with 0x000000ff the int will be
		 * 0000 0000 0000 0000 0000 0000 1110 0001  = 225
		 */
		stdDevX &= 0x000000ff;
		stdDevY &= 0x000000ff;
		
		/*
		 * NOTE
		 * As convariance can be negative we don't 0 out the higher bits
		 * of byte 1 only of byte 2
		 */
		convByte2 &= 0x00ff;
		
		convByte1 <<= 8;
		convByte1 |= convByte2;

		Double stdDevXDouble = ((double)stdDevX) * 0.25;
		Double stdDevYDouble = ((double)stdDevY) * 0.25;
		Double convDouble = ((double)convByte1) * 0.25;
		
		message.setStandardDeviationOfX(stdDevXDouble);
		message.setStandardDeviationOfY(stdDevYDouble);
		message.setConvariance(convDouble);
		return Arrays.copyOfRange(record, 4, record.length);
	}

	private static byte[] setSystemStatus(Cat010Message msg, byte[] record) throws Exception {
		Cat010SystemStatus status = new Cat010SystemStatus();
		
		Cat010PeriodicStatusMessage message;
		if(msg instanceof Cat010PeriodicStatusMessage){
			message = (Cat010PeriodicStatusMessage) msg;
		}else{
			throw new Exception("Cannot set System Status on non-PeriodicStatus message");
		}
		
		/*
		 * JAVA's shift operator, >>> will cast up to an int to do the shift. To
		 * prevent casting problems we will work with int and cast to a byte
		 * prior to storing the info
		 */
		int statusByte = ((int)record[0]);
		statusByte &= 0x00ff;
		
		status.setNogo( NOGO.values()[((statusByte & 0xc0) >>> 6)] );
		status.setOvl(   OVL.values()[((statusByte & 0x20) >>> 5)] );
		status.setTsv(   TSV.values()[((statusByte & 0x10) >>> 4)] );
		status.setDiv(   DIV.values()[((statusByte & 0x08) >>> 3)] );
		status.setTtf(   TTF.values()[((statusByte & 0x04) >>> 2)] );

		message.setSystemStatus(status);
		return Arrays.copyOfRange(record, 1, record.length);
	}

	private static byte[] setTargetSizeAndOrientation(Cat010Message msg,byte[] record) throws Exception {
		Cat010TargetReport message;
		if(msg instanceof Cat010TargetReport){
			message = (Cat010TargetReport) msg;
		}else{
			throw new Exception("Cannot set Target Size and Orientation on non-TargetReport message");
		}
		
		byte numOfBytes = 1;
		

		int length = ((int)record[0]);
		length &= 0x00ff;
		boolean firstExtendPresent = ((length & 0x0001)==1); 
		
		length >>>= 1;
		message.setTargetLength(length);
		
		if(firstExtendPresent){
			numOfBytes++;
			int orientation = ((int)record[1]);
			orientation &= 0x00ff;
			boolean secondExtendPresent = ((orientation & 0x0001)==1); 
			
			orientation >>>= 1;
			Double orientationDouble = ((double)orientation) * (2.8125);
			message.setTargetOrientation(orientationDouble);
			
			if(secondExtendPresent){
				numOfBytes++;
				int width = ((int)record[2]);
				width &= 0x00ff;
				boolean thirdExtendPresent = ((width & 0x0001) ==1);
				width >>>= 1;
				message.setTargetWidth(width);
				if (thirdExtendPresent) throw new Exception("Field Extension bit set to 1 in third byte of Target Size and Orientaiton, only 3 bytes defined in spec");
			}
		}
		return Arrays.copyOfRange(record, numOfBytes, record.length);
	}

	private static byte[] setTrackStatus(Cat010Message msg, byte[] record) throws Exception{
		Cat010TargetReport message;
		if(msg instanceof Cat010TargetReport){
			message = (Cat010TargetReport) msg;
		}else{
			throw new Exception("Cannot set track status on non-TargetReport message");
		}
		int statusSize = 1;
		
		Cat010TrackStatus status = new Cat010TrackStatus();
		
		/*
		 * JAVA's shift operator, >>> will cast up to an int to do the shift. To
		 * prevent casting problems we will work with int and cast to a byte
		 * prior to storing the info
		 */
		int statusByte = ((int)record[0]);
		statusByte &= 0x00ff;
		
		status.setCnf(CNF.values()[((statusByte & 0x80) >>> 7)]);
		status.setTre(TRE.values()[((statusByte & 0x40) >>> 6)]);
		status.setCst(CST.values()[((statusByte & 0x30) >>> 4)]);
		status.setMah(MAH.values()[((statusByte & 0x08) >>> 3)]);
		status.setTcc(TCC.values()[((statusByte & 0x04) >>> 2)]);
		status.setSth(STH.values()[((statusByte & 0x02) >>> 1)]);
		
		//status has first extent
		if((statusByte & 0x01) == 1){
			statusSize++;
			statusByte = ((int)record[1]);
			statusByte &= 0xff;
			
			status.setTom(TOM.values()[((statusByte & 0xc0) >>> 6)]);
			status.setDou(DOU.values()[((statusByte & 0x38) >>> 3)]);
			status.setMrs(MRS.values()[((statusByte & 0x06) >>> 1)]);
			
			//status has second extent
			if((statusByte & 0x01) == 1){
				statusSize++;
				statusByte = ((int)record[2]);
				statusByte &= 0xff;
				status.setGho(GHO.values()[((statusByte & 0x80) >>> 7)]);
			}
		}

		message.setTrackStatus(status);
		return Arrays.copyOfRange(record, statusSize, record.length);
	}

	private static byte[] setTrackNumber(Cat010Message msg, byte[] record) throws Exception{
		Cat010TargetReport message;
		if(msg instanceof Cat010TargetReport){
			message = (Cat010TargetReport) msg;
		}else{
			throw new Exception("Cannot set track number on non-TargetReport message");
		}
		
		int trackNumByte1 = (int)record[0];  
		int trackNumByte2 = (int)record[1];

		/*
		 * Track number is made of two bytes, where bits 13-16
		 * are spare bits, and should be set to 0, that would be
		 * the first 4 bits of byte 1, so we will set them to 0
		 * with the mask 0x000f before shifting them
		 */
		trackNumByte1 &= 0x000f;
		trackNumByte2 &= 0x00ff;
		
		trackNumByte1 <<= 8;
	
		trackNumByte1 |= trackNumByte2;
		
		message.setTrackNumber(trackNumByte1);
		return Arrays.copyOfRange(record, 2, record.length);
	}

	private static byte[] setCalTrackVelocityInCartesianCoord(Cat010Message msg, byte[] record) throws Exception{
		Cat010TargetReport message;
		if(msg instanceof Cat010TargetReport){
			message = (Cat010TargetReport) msg;
		}else{
			throw new Exception("Cannot set staandard deviation on non-TargetReport message");
		}
		
		int xVelByte1 = (int)record[0];  
		int xVelByte2 = (int)record[1];
		int yVelByte1 = (int)record[2];
		int yVelByte2 = (int)record[3];
		
		xVelByte1 <<= 8;
		yVelByte1 <<= 8;

		/*
		 * Casting to an int preserves the sign, however byte 2
		 * of the 2 byte velocity doesn't contain the sign, byte
		 * 1 does, so we need to 0 out all bits greater than 8 in
		 * byte 2 for both x and y, we do this with the mask 0x00ff
		 */
		xVelByte2 &= 0x00ff;
		yVelByte2 &= 0x00ff;
		
		xVelByte1 |= xVelByte2;
		yVelByte1 |= yVelByte2;
		
		Double xVel = ((double)xVelByte1) * 0.25;
		Double yVel = ((double)yVelByte1) * 0.25;
		
		message.setCalTrackVelocityInCartesianCoordX(xVel);
		message.setCalTrackVelocityInCartesianCoordY(yVel);
		return Arrays.copyOfRange(record, 4, record.length);
	}

	private static byte[] setDataSourceIdentifier(Cat010Message msg,byte[] record) {
		msg.setDataSourceIdSac(record[0]);
		msg.setDataSourceIdSic(record[1]);
		return Arrays.copyOfRange(record, 2, record.length);
	}

	private static byte[] removeBytesFromArray(int num, byte[] record) {
		return Arrays.copyOfRange(record, num, record.length);
	}

	private static byte[] setTargetReportDescriptor(Cat010Message msg,byte[] record) throws Exception{
		Cat010TargetReport message;
		if(msg instanceof Cat010TargetReport){
			message = (Cat010TargetReport) msg;
		}else{
			throw new Exception("Cannot set target report descriptor on non-TargetReport message");
		}
		
		Cat010TargetReportDescriptor descriptor = new Cat010TargetReportDescriptor();
		
		/*
		 * JAVA's shift operator, >>> will cast up to an int to do the shift. To
		 * prevent casting problems we will work with int and cast to a byte
		 * prior to storing the info
		 */
		int targetReportByte = ((int)record[0]);
		targetReportByte &= 0xff;
		
		descriptor.setTyp(TYP.values()[((targetReportByte & 0xe0) >>> 5)]);
		descriptor.setDcr(DCR.values()[((targetReportByte & 0x10) >>> 4)]);
		descriptor.setChn(CHN.values()[((targetReportByte & 0x08) >>> 3)]);
		descriptor.setGbs(GBS.values()[((targetReportByte & 0x04) >>> 2)]);
		descriptor.setCrt(CRT.values()[((targetReportByte & 0x02) >>> 1)]);
		
		int descriptorSize = 1;
		
		//descriptor has first extent
		if((targetReportByte & 0x01) == 1){
			descriptorSize++;
			targetReportByte = ((int)record[1]);
			targetReportByte &= 0xff;
			descriptor.setSim(SIM.values()[((targetReportByte & 0x80) >>> 7)]);
			descriptor.setTst(TST.values()[((targetReportByte & 0x40) >>> 6)]);
			descriptor.setRab(RAB.values()[((targetReportByte & 0x20) >>> 5)]);
			descriptor.setLop(LOP.values()[((targetReportByte & 0x18) >>> 3)]);
			descriptor.setTot(TOT.values()[((targetReportByte & 0x06) >>> 1)]);
			
			//descriptor has second extent
			if((targetReportByte & 0x01) == 1){
				descriptorSize++;
				targetReportByte = ((int)record[2]);
				targetReportByte &= 0xff;
				descriptor.setSpi(SPI.values()[((targetReportByte & 0x80) >>> 7)]);
			}
		}
		
		message.setTargetReportDescriptor(descriptor);
		return Arrays.copyOfRange(record, descriptorSize, record.length);
	}

	private static byte[] setTimeOfDay(Cat010Message msg, byte[] record) {
		int timeOfDayByte1 = (int)record[0];  
		int timeOfDayByte2 = (int)record[1];
		int timeOfDayByte3 = (int)record[2];
		
		timeOfDayByte1 &= 0xff;
		timeOfDayByte2 &= 0xff;
		timeOfDayByte3 &= 0xff;
		
		timeOfDayByte1 <<= 16;
		timeOfDayByte2 <<= 8;
		
		timeOfDayByte1 |= timeOfDayByte2;
		timeOfDayByte1 |= timeOfDayByte3;
		
		double timeOfDayInSec = (double)timeOfDayByte1;
		timeOfDayInSec = timeOfDayInSec / 128;
		
		msg.setTimeOfDay(timeOfDayInSec);
		return Arrays.copyOfRange(record, 3, record.length);
	}

	private static byte[] setPositionInCartesianCoord(Cat010Message msg,byte[] record) throws Exception{
		Cat010TargetReport message;
		if(msg instanceof Cat010TargetReport){
			message = (Cat010TargetReport) msg;
		}else{
			throw new Exception("Cannot set position in cartesian coord on non-TargetReport message");
		}
		
		int xCompByte1 = (int)record[0];  
		int xCompByte2 = (int)record[1];
		int yCompByte1 = (int)record[2];
		int yCompByte2 = (int)record[3];
		
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
		
		xCompByte1 |= xCompByte2;
		yCompByte1 |= yCompByte2;
		
		message.setPositionInCartesianCoordX(xCompByte1);
		message.setPositionInCartesianCoordY(yCompByte1);
		return Arrays.copyOfRange(record, 4, record.length);
	}

	private static boolean isFspecSupported(byte[] fspec) {
		/*
		 * The FSPEC supported bit mask 00001100 10001110 11110010 01111011
		 *                      in hex      0x0C     0x8E     0xF2     0x7B
		 * 
		 * Unsupported data items are set to 1 in the mask and should be 0
		 * in the FSPEC and by ANDing the mask and FSPEC the result should
		 * be 0, if not 0 then there were unsupported data items in the FSPEC.
		 * 
		 * Byte 1
		 * 0 - Data Source Identifier
		 * 0 - Message Type
		 * 0 - Target Report Descriptor
		 * 0 - Time of Day
		 * 1 - Position in WGS-84 Co-ordinates
		 * 1 - Measured Position in Polar Co-ordinates
		 * 0 - Position in Cartesian Co-ordinates
		 * 0 - Field Extension Indicator
		 * 
		 * Byte 2
		 * 1 - Calculated Track Velocity in Polar Co-ordinates
		 * 0 - Calculated Track Velocity in Cartesian Coord.
		 * 0 - Track Number
		 * 0 - Track Status
		 * 1 - Mode-3/A Code in Octal Representation
		 * 1 - Target Address
		 * 1 - Target Identification
		 * 0 - Field Extension Indicator
		 * 
		 * Byte 3
		 * 1 - Mode S MB Data
		 * 1 - Vehicle Fleet Identification
		 * 1 - Flight Level in Binary Representation
		 * 1 - Measured Height
		 * 0 - Target Size & Orientation
		 * 0 - System Status
		 * 1 - Pre-programmed Message
		 * 0 - Field Extension Indicator
		 * 
		 * Byte 4
		 * 0 - Standard Deviation of Position
		 * 1 - Presence
		 * 1 - Amplitude of Primary Plot
		 * 1 - Calculated Acceleration
		 * 1 - Spare unused
		 * 0 - Sub tracks
		 * 1 - Reserved Expansion Field
		 * 1 - Field Extension Indicator
		 * 
		 * 
		 */
		byte[] mask = {(byte)0x0c,(byte)0x8e,(byte)0xf2,(byte)0x7b};
		int value = 0;
		
		for(int index = 0 ; index < fspec.length ; index++){
			value = value + (fspec[index] & mask[index]);
		}
		
		return value == 0;
	}

	private static byte[] getFSPEC(byte[] array) throws Exception {
		/*
		 * The FSPEC begins in byte 4 (index 3) and 
		 * ends with the byte whose least significant
		 * bit (known as the FX, Field Extension Indicator)
		 * is 0. As per the spec the FSPEC can be as short
		 * as 1 byte and as long as 4 bytes, if the FX hasn't
		 * been set to 1 by the 4th byte we throw an exception 
		 */
		if ((array[3] & 0x01) == 0x00){
			return Arrays.copyOfRange(array, 3, 4);
		}else if ((array[4] & 0x01) == 0x00){
			return Arrays.copyOfRange(array, 3, 5);
		}else if ((array[5] & 0x01) == 0x00){
			return Arrays.copyOfRange(array, 3, 6);
		}else if ((array[6] & 0x01) == 0x00){
			return Arrays.copyOfRange(array, 3, 7);
		}else{
			throw new Exception("ASTERIX CAT010 message has invalid FSPEC, Field Extension Indicator (FX bit) found in all 4 bytes");
		}
	}

	/**
	 * Checks that the message is a CAT010 message, as per the
	 * CAT010 spec, first byte should be 10.
	 * 
	 * @param array
	 * @return true if 1st byte value is 10 as per CAT010 spec
	 */
	private static boolean isMessageCat010(byte[] array) {
		return (array.length > 0 && array[0]==0x0a);
	}

	/**
	 * Checks length of byte[] against length value specified in array
	 * which is stored in bytes [1] and [2] as per CAT010 spec
	 * 
	 * @param array
	 * @return true if length of byte[] matches value stored in array
	 */
	private static boolean isMessageComplete(byte[] array) {
		if (array.length < 13) return false;
		
		int length = (int)array[1];  
		length &= 0xff;
	    length <<= 8;  
	    
	    int lengthByte2 = (int)array[2];  
		lengthByte2 &= 0xff;  
	    
	    length = length | lengthByte2;
		return array.length == length;
	}

	public static void main(String args[]) throws Exception{

		byte[] msg = {(byte)0x0a,(byte)0x00,(byte)0x0d,(byte)0xd1,(byte)0x01,
			          (byte)0x04,(byte)0x00,(byte)0x01,(byte)0x03,(byte)0xa3,
			          (byte)0x7d,(byte)0x5c,(byte)0x80};
		
		byte[] msg1 = {(byte)0x0a,(byte)0x00,(byte)0x0d,(byte)0xd1,(byte)0x01,
				       (byte)0x04,(byte)0x00,(byte)0x01,(byte)0x03,(byte)0xa3,
				       (byte)0x7d,(byte)0x5c,(byte)0x80};
		byte[] msg2 = {(byte)0x0a,(byte)0x00,(byte)0x22,(byte)0xf3,(byte)0x31,
				       (byte)0x09,(byte)0x84,(byte)0x00,(byte)0x01,(byte)0x01,
				       (byte)0xe1,(byte)0x0c,(byte)0xa3,(byte)0x83,(byte)0x1c,
				       (byte)0x0d,(byte)0x8a,(byte)0x09,(byte)0x82,(byte)0x00,
				       (byte)0x02,(byte)0x82,(byte)0x0b,(byte)0x01,(byte)0x28,
				       (byte)0x01,(byte)0x01,(byte)0x00,(byte)0x00,(byte)0x05,
				       (byte)0x00,(byte)0x02,(byte)0x01,(byte)0x40};
		byte[] msg3 = {(byte)0x0a,(byte)0x00,(byte)0x26,(byte)0xf3,(byte)0x71,
				       (byte)0x09,(byte)0x84,(byte)0x00,(byte)0x01,(byte)0x01,
				       (byte)0xe1,(byte)0x0c,(byte)0xa3,(byte)0x83,(byte)0x5c,
				       (byte)0x0d,(byte)0x8b,(byte)0x09,(byte)0x80,(byte)0xff,
				       (byte)0xf4,(byte)0x00,(byte)0x05,(byte)0x00,(byte)0x02,
				       (byte)0x82,(byte)0x0b,(byte)0x5f,(byte)0x2a,(byte)0x01,
				       (byte)0x01,(byte)0x00,(byte)0x00,(byte)0x05,(byte)0x00,
				       (byte)0x02,(byte)0x01,(byte)0x00};
		
		System.out.print(doDecode(msg));
		System.out.print(doDecode(msg1));
		System.out.print(doDecode(msg2));
		System.out.print(doDecode(msg3));
	}
	
}
