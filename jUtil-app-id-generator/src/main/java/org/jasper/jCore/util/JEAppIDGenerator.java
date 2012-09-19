package org.jasper.jCore.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.activemq.broker.Connector;
import org.apache.commons.codec.digest.DigestUtils;

public class JEAppIDGenerator {
	
	private static final String salt = "aAN:TKcacqi]@yO0xm?d";

	public static void main(String[] args) throws Exception {
	    
		try {
		    BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		    String appUsername = "";
		    while (appUsername != null) {
		        System.out.print("Enter jApp username, usually in the format <vendor>:<app_name>:<version>:<deployment_id> = ");
		        appUsername = in.readLine();
		        //vendor="jasper" appName="webview" version="1.0" deploymentId="CYOW_2012.07.10" jAppAuthKey="b562090611e6118edf10cb9761bddf88d880bee9"
		        
				String[] appDetails = appUsername.split(":");
				if(appDetails.length != 4){
					System.out.println("appID incorrectly formatted");
					break;
				}
				
				StringBuffer sb = new StringBuffer();
				sb.append("vendor=\"");
				sb.append(appDetails[0]);
				sb.append("\" ");
				sb.append("appName=\"");
				sb.append(appDetails[1]);
				sb.append("\" ");
				sb.append("version=\"");
				sb.append(appDetails[2]);
				sb.append("\" ");
				sb.append("deploymentId=\"");
				sb.append(appDetails[3]);
				sb.append("\" ");
				sb.append("jAppAuthKey=\"");
				sb.append(DigestUtils.shaHex(appUsername + salt));
				sb.append("\" ");

				System.out.println(sb + "\n");
		    }
		} catch (IOException e) {
		}
	}

}
