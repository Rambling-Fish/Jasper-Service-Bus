package org.jasper.jLib.webview.trax.decoder;

import java.io.Serializable;

public class WebViewTraxMessage implements Serializable{

	private static final long serialVersionUID = -863039322963525257L;

	/*
	 * Sample trax response
	 *
	 * T,0,0,trax|ack('trax')
	 * starttrax(3,50031,0,78,1236,2943,2949)
	 * trax(1,236,1236,50031,ZZZZ,ZZZZ,ZZZZ,ZZZZ,-812,1207,0,False,0,False,False,64,False,0,0,1,-1,0,5,0)
	 * trax(1,,2943,50031,ZZZZ,ZZZZ,ZZZZ,ZZZZ,4163,16343,640,False,16,False,False,8,False,0,1,3,262,45,5,0)
	 * trax(1,949,2949,50031,ZZZZ,ZZZZ,ZZZZ,ZZZZ,165,756,0,False,0,False,False,64,False,0,0,1,-1,0,5,0)
	 * endtrax(3,50031)
	 */

   /*
	* Description of trax fields from Searidge
	* 
	* index - data                   -  type
	*-------|------------------------|------------------------
	*   0   - target_type            -  enum = 0:ghost 1:valid 
	*   1   - call_sign_name         -  String
	*   2   - track_num              -  String
	*   3   - time_of_day            -  String
	*   4   - source_airport         -  String
	*   5   - destination_airport    -  String
	*   6   - ac_type                -  String
	*   7   - stand                  -  String
	*   8   - cart_coord_x           -  String
	*   9   - cart_coord_y           -  String
	*  10   - mode_3a                -  String
	*  11   - mode_3a_valid          -  boolean
	*  12   - mode_c_altitude        -  String
	*  13   - mode_c_valid           -  boolean
	*  14   - track_status_fusion    -  boolean
	*  15   - track_status_rad       -  String
	*  16   - track_status_quality   -  boolean
	*  17   - track_info_size        -  enum = 0:? 1:very small, 2:small 3:medium 4:large 5:very large
	*  18   - track_info_type        -  enum = 0:default 1:aircrft 2:vehicle 
	*  19   - track_info_vmi         -  enum = 0:default 1:flat 2:climb 3:descend
	*  20   - direction              -  String in degrees
	*  21   - speed                  -  String in knots
	*  22   - track_info_direction   -  String, but should have been enum = 0:default 1:arr 2:dep 3:local 4:transit, but we ran into a scenario where the value was 5 so not sure if we have the right mapping
	*  23   - unknown                -  excluded
	*   
	*/
	
	public enum TargetType{
		ghost,
		valid
	}
	
	public enum TrackInfoSize{
		unknown,
		very_small,
		small,
		medium,
		large,
		very_large
	}
	
	public enum TrackInfoType{
		defaultValue,
		aircraft,
		vehicle
	}
	
	public enum TrackInfoVmi{
		defaultValue,
		flat,
		climb,
		descend
	}
	
	private TargetType target_type;
	private String call_sign_name;
	private String track_number;
	private String time_of_day;
	private String source_airport;
	private String destination_airport;
	private String ac_type;
	private String stand;
	private String cart_coord_x;
	private String cart_coord_y;
	private String mode_3a;
	private boolean isMode_3a_valid;
	private String mode_c_altitude; // flight level  (100's of feet)
	private boolean isMode_c_valid;
	private boolean isTrack_status_fusion;
	private String track_status_rad;
	private boolean isTrack_status_quality;
	private TrackInfoSize track_info_size;
	private TrackInfoType track_info_type;
	private TrackInfoVmi track_info_vmi;
	private String direction;	//-1 means direction hasn't changed, use previous value
	private String speed;
	private String track_info_direction;
	
	public WebViewTraxMessage(TargetType target_type, String call_sign_name,
			String track_number, String time_of_day, String source_airport,
			String destination_airport, String ac_type, String stand,
			String cart_coord_x, String cart_coord_y, String mode_3a,
			boolean isMode_3a_valid, String mode_c_altitude,
			boolean isMode_c_valid, boolean isTrack_status_fusion,
			String track_status_rad, boolean isTrack_status_quality,
			TrackInfoSize track_info_size, TrackInfoType track_info_type,
			TrackInfoVmi track_info_vmi, String direction, String speed,
			String track_info_direction) {
		super();
		this.target_type = target_type;
		this.call_sign_name = call_sign_name;
		this.track_number = track_number;
		this.time_of_day = time_of_day;
		this.source_airport = source_airport;
		this.destination_airport = destination_airport;
		this.ac_type = ac_type;
		this.stand = stand;
		this.cart_coord_x = cart_coord_x;
		this.cart_coord_y = cart_coord_y;
		this.mode_3a = mode_3a;
		this.isMode_3a_valid = isMode_3a_valid;
		this.mode_c_altitude = mode_c_altitude;
		this.isMode_c_valid = isMode_c_valid;
		this.isTrack_status_fusion = isTrack_status_fusion;
		this.track_status_rad = track_status_rad;
		this.isTrack_status_quality = isTrack_status_quality;
		this.track_info_size = track_info_size;
		this.track_info_type = track_info_type;
		this.track_info_vmi = track_info_vmi;
		this.direction = direction;
		this.speed = speed;
		this.track_info_direction = track_info_direction;
	}

	public TargetType getTarget_type() {
		return target_type;
	}

	public void setTarget_type(TargetType target_type) {
		this.target_type = target_type;
	}

	public String getCall_sign_name() {
		return call_sign_name;
	}

	public void setCall_sign_name(String call_sign_name) {
		this.call_sign_name = call_sign_name;
	}

	public String getTrack_number() {
		return track_number;
	}

	public void setTrack_number(String track_number) {
		this.track_number = track_number;
	}

	public String getTime_of_day() {
		return time_of_day;
	}

	public void setTime_of_day(String time_of_day) {
		this.time_of_day = time_of_day;
	}

	public String getSource_airport() {
		return source_airport;
	}

	public void setSource_airport(String source_airport) {
		this.source_airport = source_airport;
	}

	public String getDestination_airport() {
		return destination_airport;
	}

	public void setDestination_airport(String destination_airport) {
		this.destination_airport = destination_airport;
	}

	public String getAc_type() {
		return ac_type;
	}

	public void setAc_type(String ac_type) {
		this.ac_type = ac_type;
	}

	public String getStand() {
		return stand;
	}

	public void setStand(String stand) {
		this.stand = stand;
	}

	public String getCart_coord_x() {
		return cart_coord_x;
	}

	public void setCart_coord_x(String cart_coord_x) {
		this.cart_coord_x = cart_coord_x;
	}

	public String getCart_coord_y() {
		return cart_coord_y;
	}

	public void setCart_coord_y(String cart_coord_y) {
		this.cart_coord_y = cart_coord_y;
	}

	public String getMode_3a() {
		return mode_3a;
	}

	public void setMode_3a(String mode_3a) {
		this.mode_3a = mode_3a;
	}

	public boolean isMode_3a_valid() {
		return isMode_3a_valid;
	}

	public void setMode_3a_valid(boolean isMode_3a_valid) {
		this.isMode_3a_valid = isMode_3a_valid;
	}

	public String getMode_c_altitude() {
		return mode_c_altitude;
	}

	public void setMode_c_altitude(String mode_c_altitude) {
		this.mode_c_altitude = mode_c_altitude;
	}

	public boolean isMode_c_valid() {
		return isMode_c_valid;
	}

	public void setMode_c_valid(boolean isMode_c_valid) {
		this.isMode_c_valid = isMode_c_valid;
	}

	public boolean isTrack_status_fusion() {
		return isTrack_status_fusion;
	}

	public void setTrack_status_fusion(boolean isTrack_status_fusion) {
		this.isTrack_status_fusion = isTrack_status_fusion;
	}

	public String getTrack_status_rad() {
		return track_status_rad;
	}

	public void setTrack_status_rad(String track_status_rad) {
		this.track_status_rad = track_status_rad;
	}

	public boolean isTrack_status_quality() {
		return isTrack_status_quality;
	}

	public void setTrack_status_quality(boolean isTrack_status_quality) {
		this.isTrack_status_quality = isTrack_status_quality;
	}

	public TrackInfoSize getTrack_info_size() {
		return track_info_size;
	}

	public void setTrack_info_size(TrackInfoSize track_info_size) {
		this.track_info_size = track_info_size;
	}

	public TrackInfoType getTrack_info_type() {
		return track_info_type;
	}

	public void setTrack_info_type(TrackInfoType track_info_type) {
		this.track_info_type = track_info_type;
	}

	public TrackInfoVmi getTrack_info_vmi() {
		return track_info_vmi;
	}

	public void setTrack_info_vmi(TrackInfoVmi track_info_vmi) {
		this.track_info_vmi = track_info_vmi;
	}

	public String getDirection() {
		return direction;
	}

	public void setDirection(String direction) {
		this.direction = direction;
	}

	public String getSpeed() {
		return speed;
	}

	public void setSpeed(String speed) {
		this.speed = speed;
	}

	public String getTrack_info_direction() {
		return track_info_direction;
	}

	public void setTrack_info_direction(String track_info_direction) {
		this.track_info_direction = track_info_direction;
	}
	

}
