package org.jasper.core.exceptions;

import org.jasper.core.constants.JasperConstants;
import org.jasper.core.constants.JasperConstants.ResponseCodes;

public class JasperRequestException extends Throwable {

	private static final long	serialVersionUID	= 1205500343877261466L;
	private ResponseCodes	responseCode;
	private String	details;

	public JasperRequestException(JasperConstants.ResponseCodes responseCode, String details){
		super();
		this.setResponseCode(responseCode);
		this.setDetails(details);
	}

	public ResponseCodes getResponseCode() {
		return responseCode;
	}

	public void setResponseCode(ResponseCodes responseCode) {
		this.responseCode = responseCode;
	}

	public String getDetails() {
		return details;
	}

	public void setDetails(String details) {
		this.details = details;
	}
	
	
}
