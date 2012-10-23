package org.jasper.jLib.cat010.transformer;

import org.jasper.jLib.cat010.codec.Cat010TargetReport;
import org.jasper.jLib.webview.trax.decoder.WebViewTraxMessage;
import org.jasper.jLib.webview.trax.decoder.WebViewTraxMessage.TargetType;
import org.jasper.jLib.webview.trax.decoder.WebViewTraxMessage.TrackInfoSize;
import org.jasper.jLib.webview.trax.decoder.WebViewTraxMessage.TrackInfoType;
import org.jasper.jLib.webview.trax.decoder.WebViewTraxMessage.TrackInfoVmi;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractTransformer;

public class Cat010TargetReportToWebViewTrax extends AbstractTransformer {

	private static final String defaultStringString = "ZZZZ";
	private static final String defaultIntString    = "0";
	private static final boolean defaultBoolean     = false;
	private static final String defaultDirectionString = "-1";
	private static final int cart_coord_x_offset = 2680; //TODO make configurable
	private static final int cart_coord_y_offset = 2001; //TODO make configurable
	
	@Override
	protected Object doTransform(Object src, String encoding)throws TransformerException {
		if(src instanceof Cat010TargetReport){
			try {
				Cat010TargetReport tr = (Cat010TargetReport)src;
				TargetType target_type       = TargetType.valid;
				String call_sign_name        = tr.getTrackNumber().toString();
				String track_num             = tr.getTrackNumber().toString();
				String time_of_day           = tr.getTimeOfDay().toString();
				String source_airport        = defaultStringString;
				String destination_airport   = defaultStringString;
				String ac_type               = defaultStringString;
				String stand                 = defaultStringString;
				String cart_coord_x          = "";
				if(tr.getPositionInCartesianCoordX() !=null){
					cart_coord_x = "" + (tr.getPositionInCartesianCoordX().intValue() - cart_coord_x_offset);
				}
				String cart_coord_y          = "";
				if(tr.getPositionInCartesianCoordY() !=null){
					cart_coord_y = "" + (tr.getPositionInCartesianCoordY().intValue() - cart_coord_y_offset);
				}
				String mode_3a               = defaultIntString;
				boolean mode_3a_valid        = defaultBoolean;
				String mode_c_altitude       = defaultIntString;
				boolean mode_c_valid         = defaultBoolean;
				boolean track_status_fusion  = defaultBoolean;
				String track_status_rad      = defaultIntString;
				boolean track_status_quality = defaultBoolean;
				TrackInfoSize track_info_size = TrackInfoSize.unknown;
				TrackInfoType track_info_type = TrackInfoType.defaultValue;
				TrackInfoVmi track_info_vmi   = TrackInfoVmi.defaultValue;
				String direction              = defaultDirectionString;
				/*
				 * So if you have vX = 1.5, vY=1.25
				 * s = sqrt(1.5^2 + 1.25^2)
				 * speed = s * 1.94384
				 * speed = 3.795
				 */
				String speed = "";
				if(tr.getCalTrackVelocityInCartesianCoordX() == null || tr.getCalTrackVelocityInCartesianCoordY() == null){
					speed = "unknown";
				}else{
					double speedInMetersPerSec = Math.sqrt( (Math.pow(tr.getCalTrackVelocityInCartesianCoordX(), 2)) +
						                                (Math.pow(tr.getCalTrackVelocityInCartesianCoordY(), 2)));
					double speedInKnots = speedInMetersPerSec * 1.94384;
					speed = "" + speedInKnots;
				}
				String track_info_direction   = defaultIntString;
//				String unknown                = line[23];
				
				return new WebViewTraxMessage(target_type, call_sign_name, track_num, time_of_day, source_airport, destination_airport,
						ac_type, stand, cart_coord_x, cart_coord_y, mode_3a, mode_3a_valid, mode_c_altitude, mode_c_valid, track_status_fusion,
						track_status_rad, track_status_quality, track_info_size, track_info_type, track_info_vmi, direction, speed, track_info_direction);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return "Error deocding Message";
			}
		}
		return src;
	}

}
