package org.jasper.jtaDemo.util;

import java.util.HashMap;
import java.util.Map;

import org.jasper.jLib.cat010.codec.Cat010SubTrack;
import org.jasper.jLib.cat010.codec.Cat010TargetReport;
import org.jasper.jLib.cat010.codec.Cat010TargetReportDescriptor;
import org.jasper.jLib.cat010.codec.Cat010TrackStatus;

public class Cat010TargetReportGenerator {
		
	private static int[] xMax = {180,325};
	private static int[] xMin = {-265,-47};
	private static int[] xArray = {0,0};
	private static int[] xChange = {-1,2};

	
	private static final int cart_coord_x_offset = 2680; //TODO make configurable
	private static final int cart_coord_y_offset = 2001; //TODO make configurable

	private static boolean x1 = true;
	
	/*
	 * we ignore the string parameter, as our mule flow will
	 * look for a method with a String parameter, we simply 
	 * call the parameter-less version of this method
	 */
	public static Cat010TargetReport getTargetReport(String str){
		return getTargetReport();
	}

	private static Cat010TargetReport getTargetReport() {
		Cat010TargetReport tr = new Cat010TargetReport();
		
		tr.setDataSourceIdSac((byte) 0);
		tr.setDataSourceIdSic((byte) 1);
		
		Cat010TargetReportDescriptor trd = new Cat010TargetReportDescriptor();
		trd.setChn(Cat010TargetReportDescriptor.CHN.chain_1);
		trd.setCrt(Cat010TargetReportDescriptor.CRT.no_corrupted_reply_in_multilateration);
		trd.setDcr(Cat010TargetReportDescriptor.DCR.no_differential_coorection__ads_b);
		trd.setGbs(Cat010TargetReportDescriptor.GBS.transponder_ground_bit_set);
		trd.setLop(Cat010TargetReportDescriptor.LOP.undetermined);
		trd.setRab(Cat010TargetReportDescriptor.RAB.report_from_field_monitor__fixed_transponder);
		trd.setSim(Cat010TargetReportDescriptor.SIM.simulated_target_report);
		trd.setSpi(Cat010TargetReportDescriptor.SPI.absense_of_SPI);
		trd.setTot(Cat010TargetReportDescriptor.TOT.ground_vehicle);
		trd.setTst(Cat010TargetReportDescriptor.TST.test_target);
		trd.setTyp(Cat010TargetReportDescriptor.TYP.not_defined);
		tr.setTargetReportDescriptor(trd);
		
		long millisSinceGMTMidnight = System.currentTimeMillis() % (24L * 60*60*1000);
		tr.setTimeOfDay( ((double)millisSinceGMTMidnight) / 1000);
		
		int y = 0;
		int x = (x1)?xArray[0]:xArray[1];
		if(x1){
			y=x*73483/100000+78;
			xArray[0] += xChange[0];
			if(xArray[0] > xMax[0] || xArray[0] < xMin[0]){
				xChange[0] *= (-1);
			}
		}else{
			y=x*(-63709)/100000+130;
			xArray[1] += xChange[1];
			if(xArray[1] > xMax[1] || xArray[1] < xMin[1]){
				xChange[1] *= (-1);
			}
		}
		
		tr.setPositionInCartesianCoordX(x*10 + cart_coord_x_offset);
		tr.setPositionInCartesianCoordY(y*10 + cart_coord_y_offset);
		
		tr.setCalTrackVelocityInCartesianCoordX(0.0);
		tr.setCalTrackVelocityInCartesianCoordY(0.0);
		
		int id = (x1)?0:1; 
		tr.setTrackNumber(id);
		x1=!x1;
		
		
		Cat010TrackStatus ts = new Cat010TrackStatus();
		ts.setCnf(Cat010TrackStatus.CNF.confirmed_track);
		ts.setCst(Cat010TrackStatus.CST.no_extrapolation);
		ts.setDou(Cat010TrackStatus.DOU.no_doubt);
		ts.setGho(Cat010TrackStatus.GHO.defaultGHO);
		ts.setMah(Cat010TrackStatus.MAH.defaultMAH);
		ts.setMrs(Cat010TrackStatus.MRS.merge_or_split_indication_undetermined);
		ts.setSth(Cat010TrackStatus.STH.measured_position);
		ts.setTcc(Cat010TrackStatus.TCC.tracking_performed_in_sensor_plane);
		ts.setTom(Cat010TrackStatus.TOM.unknown_type_of_movement);
		ts.setTre(Cat010TrackStatus.TRE.defaultTRE);
		tr.setTrackStatus(ts);
		
		tr.setTargetLength(15);
		tr.setTargetOrientation(0.0);
		tr.setTargetWidth(5);
		
		tr.setStandardDeviationOfX(1.0);
		tr.setStandardDeviationOfY(1.0);
		tr.setConvariance(1.0);
		
		Map<Integer, Cat010SubTrack> subTracks = new HashMap<Integer, Cat010SubTrack>();
		Cat010SubTrack subtrack = new Cat010SubTrack();
		subtrack.setTrackId(0);
		subtrack.setSensorId(0);
		subtrack.setType(Cat010SubTrack.Cat010SubTrackType.unknown);
		subTracks.put(0, subtrack);
		tr.setSubTracks(subTracks);
		
		return tr;
	}
	
}