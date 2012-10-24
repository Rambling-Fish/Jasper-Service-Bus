package org.jasper.jLib.cat010.codec;

import java.io.Serializable;

public class Cat010SystemStatus  implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -2147348607204348207L;


	//Operational Release Status of the System
	public enum NOGO{
		operational,
		degraded,
		NOGO
	}
	
	//Overload indicator
	public enum OVL{
		no_overload,
		overload
	}
	
	//Time Source Validity
	public enum TSV{
		valid,
		invalid
	}
	
	public enum DIV{
		normal_operation,
		diversity_degraded
	}
	
	public enum TTF{
		test_target_operative,
		test_target_failure
	}
	
	private NOGO nogo;
	private OVL ovl;
	private TSV tsv;
	private DIV div;
	private TTF ttf;
	
	
	public NOGO getNogo() {
		return nogo;
	}


	public void setNogo(NOGO nogo) {
		this.nogo = nogo;
	}


	public OVL getOvl() {
		return ovl;
	}


	public void setOvl(OVL ovl) {
		this.ovl = ovl;
	}


	public TSV getTsv() {
		return tsv;
	}


	public void setTsv(TSV tsv) {
		this.tsv = tsv;
	}


	public DIV getDiv() {
		return div;
	}


	public void setDiv(DIV div) {
		this.div = div;
	}


	public TTF getTtf() {
		return ttf;
	}


	public void setTtf(TTF ttf) {
		this.ttf = ttf;
	}


	public String toString(){
		StringBuilder sb = new StringBuilder();

		sb.append("{NOGO:" + nogo);
		sb.append(", ovl:" + ovl);
		sb.append(", tsv:" + tsv);
		sb.append(", div:" + div);
		sb.append(", ttf:" + ttf + "}");

		return sb.toString();
	}

}
