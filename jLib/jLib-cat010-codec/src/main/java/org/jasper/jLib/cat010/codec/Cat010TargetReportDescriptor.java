package org.jasper.jLib.cat010.codec;

import java.io.Serializable;

public class Cat010TargetReportDescriptor implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4662607631829017326L;


	public enum TYP{
		ssr_multilateration,
		mode_s_multilateration,
		ads_b,
		psr,
		magnetic_loop_system,
		hf_multilateration,
		not_defined,
		other_types,
	}
	
	public enum DCR{
		no_differential_coorection__ads_b,
		differential_coorection__ads_b
	}
	
	public enum CHN{
		chain_1,
		chain_2
	}
	
	public enum GBS{
		transponder_ground_bit_not_set,
		transponder_ground_bit_set
	}
	
	public enum CRT{
		no_corrupted_reply_in_multilateration,
		corrupted_replies_in_multilateration
	}
	
	public enum SIM{
		actual_target_report,
		simulated_target_report
	}
	
	public enum TST{
		defaultTST,
		test_target
	}
	
	public enum RAB{
		report_from_target_transponder,
		report_from_field_monitor__fixed_transponder
	}
	
	public enum LOP{
		undetermined,
		loop_start,
		loop_finish
	}
	
	public enum TOT{
		undetermined,
		aircraft,
		ground_vehicle,
		helicopter
	}
	
	public enum SPI{
		absense_of_SPI,
		special_position_identification
	}
	
	private TYP typ;
	private DCR dcr;
	private CHN chn;
	private GBS gbs;
	private CRT crt;
	
	private SIM sim;
	private TST tst;
	private RAB rab;
	private LOP lop;
	private TOT tot;

	private SPI spi;
	

	public TYP getTyp() {
		return typ;
	}


	public void setTyp(TYP typ) {
		this.typ = typ;
	}


	public DCR getDcr() {
		return dcr;
	}


	public void setDcr(DCR dcr) {
		this.dcr = dcr;
	}


	public CHN getChn() {
		return chn;
	}


	public void setChn(CHN chn) {
		this.chn = chn;
	}


	public GBS getGbs() {
		return gbs;
	}


	public void setGbs(GBS gbs) {
		this.gbs = gbs;
	}


	public CRT getCrt() {
		return crt;
	}


	public void setCrt(CRT crt) {
		this.crt = crt;
	}


	public SIM getSim() {
		return sim;
	}


	public void setSim(SIM sim) {
		this.sim = sim;
	}


	public TST getTst() {
		return tst;
	}


	public void setTst(TST tst) {
		this.tst = tst;
	}


	public RAB getRab() {
		return rab;
	}


	public void setRab(RAB rab) {
		this.rab = rab;
	}


	public LOP getLop() {
		return lop;
	}


	public void setLop(LOP lop) {
		this.lop = lop;
	}


	public TOT getTot() {
		return tot;
	}


	public void setTot(TOT tot) {
		this.tot = tot;
	}


	public SPI getSpi() {
		return spi;
	}


	public void setSpi(SPI spi) {
		this.spi = spi;
	}


	public String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append("{");
		if(typ != null) sb.append("typ = " + typ);
		if(dcr != null) sb.append(", dcr = " + dcr);
		if(chn != null) sb.append(", chn = " + chn);
		if(gbs != null) sb.append(", gbs = " + gbs);
		if(crt != null) sb.append(", crt = " + crt);
		if(sim != null) sb.append(", sim = " + sim);
		if(tst != null) sb.append(", tst = " + tst);
		if(rab != null) sb.append(", rab = " + rab);
		if(lop != null) sb.append(", lop = " + lop);
		if(tot != null) sb.append(", tot = " + tot);
		if(spi != null) sb.append(", spi = " + spi);
		sb.append("}");
		return sb.toString();
	}
	
}
