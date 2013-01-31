package org.jasper.jtaDemo.util;

import java.util.HashMap;
import java.util.Map;

import org.jasper.jLib.cat010.codec.Cat010SubTrack;
import org.jasper.jLib.cat010.codec.Cat010TargetReport;
import org.jasper.jLib.cat010.codec.Cat010TargetReportDescriptor;
import org.jasper.jLib.cat010.codec.Cat010TrackStatus;

public class Cat010TargetReportGenerator {
	
	enum State{up,down,accross};

	
	private static double[] velCurrent = {0.0,0.0,0.0,0.0};
	
	private static int[] x1 = {-1000,-2000,-3000,-4000};
	private static int[] y1 = {500,1000,1500,2000};
	private static int[] x = {-1000,-2000,-3000,-4000};
	private static int[] y = {500,1000,1500,2000};

	private static int[] delta = {1,4,5,2};
	
	private static final int cart_coord_x_offset = 2680; //TODO make configurable
	private static final int cart_coord_y_offset = 2001; //TODO make configurable

	private static State[] state = {State.up,State.up,State.up,State.up};
	
	private static int count = 0;
	
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
		trd.setTot(Cat010TargetReportDescriptor.TOT.undetermined);
		trd.setTst(Cat010TargetReportDescriptor.TST.test_target);
		trd.setTyp(Cat010TargetReportDescriptor.TYP.not_defined);
		tr.setTargetReportDescriptor(trd);
		
		long millisSinceGMTMidnight = System.currentTimeMillis() % (24L * 60*60*1000);
		tr.setTimeOfDay( ((double)millisSinceGMTMidnight) / 1000);
		

		if(state[count].equals(State.up)){
			x[count] += delta[count];
			y[count] -= delta[count];
			if(y[count] <= ((-1)*y1[count])) state[count] = State.down;
		}else if(state[count].equals(State.down)){
			x[count] += delta[count];
			y[count] += delta[count];
			if(y[count] >= y1[count]) state[count] = State.accross;
		}else if(state[count].equals(State.accross)){
			x[count] = x[count]- delta[count]*2;
			if(x[count] <= x1[count]) state[count] = State.up;
		}
		
		tr.setPositionInCartesianCoordX(x[count] + cart_coord_x_offset);
		tr.setPositionInCartesianCoordY(y[count] + cart_coord_y_offset);
		
		tr.setCalTrackVelocityInCartesianCoordX(velCurrent[count]);
		tr.setCalTrackVelocityInCartesianCoordY(velCurrent[count]);

		tr.setTrackNumber(count+8);
	
		count++;
		count %= 4;
		
		
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