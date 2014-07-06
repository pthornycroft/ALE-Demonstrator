package com.arubanetworks.aledemonstrator;

import java.util.Date;

import org.json.JSONArray;
import org.json.JSONException;

import android.util.Log;

public class PositionHistoryObject {
	String TAG = "PositionHistoryObject";
	
	Date timestamp;  // use "new Date()"  from System.currentTimeMillis()
	float touchX = 0;  // this is as indicated on touchscreen in 'site survey' mode
	float touchY = 0;
	float measuredX;  // this is as reported by visualRF or ALE
	float measuredY;
	int numberApsAboveYellowRssiThreshold = 0;
	int numberApsAboveRedRssiThreshold = 0;
	int maxRssiLevel = -99;
	boolean fromALE = false;
	float error;
	String floorId = "XXX";
	String buildingId = "XXX";
	String campusId = "XXX";
	String ethAddr = "XX:XX:XX:XX:XX:XX";
	String hashedEth = "XX";
	String units = "ft";
	String deviceMfg = "XX";
	String deviceModel = "XX";
	float compassDegrees = 0;
	JSONArray iBeaconJsonArray;
	
	public PositionHistoryObject(Date ts, float touch_X, float touch_Y, float meas_X, float meas_Y, int level, boolean from, float err, 
			String floor, String bldg, String campus, String eth, String hashed, String units, String deviceMfg, String deviceModel, 
			float compassDegrees, JSONArray ibeaconJsonArray){
		this.timestamp = ts;
		this.touchX = touch_X;
		this.touchY = touch_Y;
		this.measuredX = meas_X;
		this.measuredY = meas_Y;
		this.maxRssiLevel = level;
		this.fromALE = from;
		this.floorId = floor;
		this.buildingId = bldg;
		this.campusId = campus;
		this.ethAddr = eth;
		this.hashedEth = hashed;
		this.units = units;
		this.deviceMfg = deviceMfg;
		this.deviceModel = deviceModel;
		this.compassDegrees = compassDegrees;
		this.iBeaconJsonArray = ibeaconJsonArray;
	}
	
	public PositionHistoryObject(Date ts, float touch_X, float touch_Y, float meas_X, float meas_Y){
		this.timestamp = ts;
		this.touchX = touch_X;
		this.touchY = touch_Y;
		this.measuredX = meas_X;
		this.measuredY = meas_Y;
	}
	
	public PositionHistoryObject(Date ts, float touch_X, float touch_Y, float meas_X, float meas_Y, int numYel, int numRed, int level){
		this.timestamp = ts;
		this.measuredX = meas_X;
		this.measuredY = meas_Y;
		this.touchX = touch_X;
		this.touchY = touch_Y;
		this.numberApsAboveYellowRssiThreshold = numYel;
		this.numberApsAboveRedRssiThreshold = numRed;
		this.maxRssiLevel = level;
	}
	
	public String toString(){
		String s = "";
		s = "\nTimestamp " + timestamp + "\nTouched x,y " + touchX + " , " + touchY + "\nMeasured x,y "+measuredX+" , "+measuredY+"\nMax rssi level " + maxRssiLevel +
				"\nfromALE " + fromALE + "\nerror " + error + "\nfloorId " + floorId + "\nbuildingId " + buildingId +
				"\ncampusId " + campusId + "\nethAddr " + ethAddr + "\nhashedEth " + hashedEth +
				"\nunits "+ units + "\ndeviceMfg "+deviceMfg+"\ndeviceModel "+deviceModel+"\ncompassDegrees "+compassDegrees;
		return s;
	}
	
	public String toAirWaveString(){
		String s = "";
		s = "\nTimestamp " + timestamp + "\nTouched x,y          " + touchX + " , " + touchY + 
				"\nAirWave Measured x,y "+measuredX+" , "+measuredY+
				"\nMax rssi level " + maxRssiLevel;
		return s;
	}
	
	public String toAleString(){
		String s = "";
		s = "\nTimestamp " + timestamp + "\nTouched x,y      " + touchX + " , " + touchY + 
				"\nALE Measured x,y "+measuredX+" , "+measuredY;
		return s;
	}
}
