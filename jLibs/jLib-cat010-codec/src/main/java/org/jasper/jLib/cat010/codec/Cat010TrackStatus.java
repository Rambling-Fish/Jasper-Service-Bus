package org.jasper.jLib.cat010.codec;

import java.io.Serializable;

public class Cat010TrackStatus implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 7090038344224603807L;

	public enum CNF{
		confirmed_track,
		track_in_initialisation_phase
	}
		                                      
	public enum TRE{
		defaultTRE,
		last_report_for_a_track
	}

	public enum CST{
		no_extrapolation,
		predictable_extrapolation_due_to_sensor_refresh_period,
		predictable_extrapolation_in_masked_area,
		extrapolation_due_to_unpredictable_absence_of_detection
	}

	public enum MAH{
		defaultMAH,
		horizontal_manoeuvre
	}

	public enum TCC{
		tracking_performed_in_sensor_plane,
		slant_range_correction_and_a_suitable_projection_technique_are_used_to_track_in_a_2D
	}

	public enum STH{
		measured_position,
		smoothed_position
	}

	public enum TOM{
		unknown_type_of_movement,
		taking_off,
		landing,
		other_types_of_movement
	}

	public enum DOU{
		no_doubt,
		doubtful_correlation_undetermined_reason,
		doubtful_correlation_in_clutter,
		loss_of_accuracy,
		loss_of_accuracy_in_clutter,
		unstable_track,
		previously_coasted
	}

	public enum MRS{
		merge_or_split_indication_undetermined,
		track_merged_by_association_to_plot,
		track_merged_by_non_association_to_plot,
		split_track
	}

	public enum GHO{
		defaultGHO,
		ghost_track
	}
	
	private CNF cnf;
	private TRE tre;
	private CST cst;
	private MAH mah;
	private TCC tcc;
	private STH sth;

	private TOM tom;
	private DOU dou;
	private MRS mrs;
	
	private GHO gho;
	
	public CNF getCnf() {
		return cnf;
	}

	public void setCnf(CNF cnf) {
		this.cnf = cnf;
	}

	public TRE getTre() {
		return tre;
	}

	public void setTre(TRE tre) {
		this.tre = tre;
	}

	public CST getCst() {
		return cst;
	}

	public void setCst(CST cst) {
		this.cst = cst;
	}

	public MAH getMah() {
		return mah;
	}

	public void setMah(MAH mah) {
		this.mah = mah;
	}

	public TCC getTcc() {
		return tcc;
	}

	public void setTcc(TCC tcc) {
		this.tcc = tcc;
	}

	public STH getSth() {
		return sth;
	}

	public void setSth(STH sth) {
		this.sth = sth;
	}

	public TOM getTom() {
		return tom;
	}

	public void setTom(TOM tom) {
		this.tom = tom;
	}

	public DOU getDou() {
		return dou;
	}

	public void setDou(DOU dou) {
		this.dou = dou;
	}

	public MRS getMrs() {
		return mrs;
	}

	public void setMrs(MRS mrs) {
		this.mrs = mrs;
	}

	public GHO getGho() {
		return gho;
	}

	public void setGho(GHO gho) {
		this.gho = gho;
	}

	public String toString(){
		StringBuilder sb = new StringBuilder();

		if(cnf != null) sb.append("cnf:" + cnf);
		if(tre != null) sb.append(", tre:" + tre);
		if(cst != null) sb.append(", cst:" + cst);
		if(mah != null) sb.append(", mah:" + mah);
		if(tcc != null) sb.append(", tcc:" + tcc);
		if(sth != null) sb.append(", sth:" + sth);
		if(tom != null) sb.append(", tom:" + tom);
		if(dou != null) sb.append(", dou:" + dou);
		if(mrs != null) sb.append(", mrs:" + mrs);
		if(gho != null) sb.append(", gho:" + gho);
		
		return sb.toString();
	}
	

}
