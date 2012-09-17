package org.jasper.jLib.cat010.codec;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;


public class Cat010TargetReport extends AbstractCat010Message implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = -8809530615440546947L;
	/*
	 * Note: We are not using primitives because we need to keep track of
	 * values that are not set, i.e. they need to be null by default.
	 */
	private Cat010TargetReportDescriptor targetReportDescriptor;
	private Integer positionInCartesianCoordX;
	private Integer positionInCartesianCoordY;
	private Double calTrackVelocityInCartesianCoordX;
	private Double calTrackVelocityInCartesianCoordY;
	private Integer trackNumber;
	private Cat010TrackStatus trackStatus;
	private Integer targetLength;
	private Double targetOrientation;
	private Integer targetWidth;
	private Double standardDeviationOfX;
	private Double standardDeviationOfY;
	private Double convariance;
	private Map<Integer, Cat010SubTrack> subTracks;
	
	 public Cat010TargetReport(){
		this.msgType = Cat010MessageType.target_report;
		this.subTracks = new HashMap<Integer, Cat010SubTrack>();
	}
	

	public void setTargetReportDescriptor(Cat010TargetReportDescriptor descriptor) {
		targetReportDescriptor = descriptor;
	}
	
	public Cat010TargetReportDescriptor getTargetReportDescriptor(){
		return targetReportDescriptor;
	}

	public void setPositionInCartesianCoordX(Integer positionInCartesianCoordX) {
		this.positionInCartesianCoordX = positionInCartesianCoordX;
	}

	public Integer getPositionInCartesianCoordX() {
		return positionInCartesianCoordX;
	}

	public void setPositionInCartesianCoordY(Integer positionInCartesianCoordY) {
		this.positionInCartesianCoordY = positionInCartesianCoordY;
	}

	public Integer getPositionInCartesianCoordY() {
		return positionInCartesianCoordY;
	}

	public void setCalTrackVelocityInCartesianCoordX(Double vX) {
		this.calTrackVelocityInCartesianCoordX = vX;;
	}

	public Double getCalTrackVelocityInCartesianCoordX() {
		return calTrackVelocityInCartesianCoordX;
	}

	public void setCalTrackVelocityInCartesianCoordY(Double vY) {
		this.calTrackVelocityInCartesianCoordY = vY;
	}

	public Double getCalTrackVelocityInCartesianCoordY() {
		return calTrackVelocityInCartesianCoordY;
	}

	public void setTrackNumber(Integer trackNumber) {
		this.trackNumber = trackNumber;
	}

	public Integer getTrackNumber() {
		return trackNumber;
	}

	public void setTrackStatus(Cat010TrackStatus trackStatus) {
		this.trackStatus = trackStatus;
	}

	public Cat010TrackStatus getTrackStatus() {
		return trackStatus;
	}

	public void setTargetLength(Integer targetLength) {
		this.targetLength = targetLength;
	}

	public Integer getTargetLength() {
		return targetLength;
	}

	public void setTargetOrientation(Double targetOrientation) {
		this.targetOrientation = targetOrientation;
	}

	public Double getTargetOrientation() {
		return targetOrientation;
	}

	public void setTargetWidth(Integer targetWidth) {
		this.targetWidth = targetWidth;
	}

	public Integer getTargetWidth() {
		return targetWidth;
	}

	public void setStandardDeviationOfX(Double standardDeviationOfX) {
		this.standardDeviationOfX = standardDeviationOfX;
	}

	public Double getStandardDeviationOfX() {
		return standardDeviationOfX;
	}

	public void setStandardDeviationOfY(Double standardDeviationOfY) {
		this.standardDeviationOfY = standardDeviationOfY;
	}

	public Double getStandardDeviationOfY() {
		return standardDeviationOfY;
	}

	public void setConvariance(Double convariance) {
		this.convariance = convariance;
	}

	public Double getConvariance() {
		return convariance;
	}

	public void setSubTracks(Map<Integer, Cat010SubTrack> subTracks) {
		this.subTracks = subTracks;
	}

	public Map<Integer, Cat010SubTrack> getSubTracks() {
		return subTracks;
	}
	
	public void addSubTrack(Integer trackId, Cat010SubTrack subTrack){
		subTracks.put(new Integer(trackId), subTrack);
	}
	
	public Cat010SubTrack getSubTrack(Integer trackId){
		return subTracks.get(new Integer(trackId));
	}
	
	public Integer getNumOfSubTracks(){
		return subTracks.size();
	}
	
	public String toString(){
		StringBuilder sb = new StringBuilder();

		sb.append("-----------------------------------------------------------------------------------------------------------------------------------------------\n");
	
		if(dataSourceIdSac != null && dataSourceIdSic != null) sb.append("Data Source ID  = " + dataSourceIdSac + "," + dataSourceIdSic + "\n");
		if(msgType != null)                                    sb.append("  Message Type  = " + msgType + "\n");
		if(targetReportDescriptor != null)                     sb.append(" TR descriptor  = " + targetReportDescriptor + "\n");
		
		if(timeOfDay != null){
			int hrs = ((timeOfDay.intValue()) / 60) / 60;
			int min = ((timeOfDay.intValue()) / 60) % 60;
			double sec = timeOfDay - (3600*hrs + 60*min);
			String timeOfDayFormated = hrs + ":" + min + ":" + sec + " UTC";
			sb.append("   Time of Day  = " + timeOfDayFormated + "\n");
		}
		
		if(positionInCartesianCoordX != null && positionInCartesianCoordY !=null)                sb.append("  Cartesian X,Y = " + positionInCartesianCoordX + "m," + positionInCartesianCoordY + "m\n");
		if(calTrackVelocityInCartesianCoordX !=null && calTrackVelocityInCartesianCoordY !=null) sb.append("Cartesian vX,Vy = " + calTrackVelocityInCartesianCoordX + "m/s," + calTrackVelocityInCartesianCoordY + "m/s\n");
		
		if(trackNumber != null)       sb.append("   Track Number = " + trackNumber + "\n");
		if(trackStatus != null)       sb.append("   Track Status = " + trackStatus + "\n");
		if(targetLength != null)      sb.append("  Target length = " + targetLength + "m\n");
		if(targetOrientation != null) sb.append("  T orientation = " + targetOrientation + "°\n");
		if(targetWidth != null)       sb.append("   Target width = " + targetWidth + "m\n");
		
		if(standardDeviationOfX != null && standardDeviationOfY !=null && convariance != null) sb.append(" stdD dX,dY,cXY = " + standardDeviationOfX + "m," + standardDeviationOfY + "m," + convariance + "m²\n");
		if(subTracks != null){
			sb.append("     Sub Tracks = " + subTracks.size() + " numOfSubTracks :");
			for(Cat010SubTrack subT : subTracks.values()){
			sb.append("{" + subT + "}\n");	
			}
		}
		sb.append("-----------------------------------------------------------------------------------------------------------------------------------------------\n");
		return sb.toString();	
	}	
}
