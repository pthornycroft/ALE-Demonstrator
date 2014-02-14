package com.arubanetworks.aledemonstrator;

import java.util.ArrayList;
import java.util.Date;

public class PositionHistoryObject {
	Date timestamp;  // use System.currentTimeMillis()
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
	
	public PositionHistoryObject(Date ts, float touch_X, float touch_Y, float meas_X, float meas_Y, int level, boolean from, float err, 
			String floor, String bldg, String campus, String eth, String hashed){
		timestamp = ts;
		touchX = touch_X;
		touchY = touch_Y;
		measuredX = meas_X;
		measuredY = meas_Y;
		maxRssiLevel = level;
		fromALE = from;
		floorId = floor;
		buildingId = bldg;
		campusId = campus;
		ethAddr = eth;
		hashedEth = hashed;
	}
	
	public PositionHistoryObject(Date ts, float touch_X, float touch_Y, float meas_X, float meas_Y){
		timestamp = ts;
		touchX = touch_X;
		touchY = touch_Y;
		measuredX = meas_X;
		measuredY = meas_Y;
	}
	
	public PositionHistoryObject(Date ts, float touch_X, float touch_Y, float meas_X, float meas_Y, int numYel, int numRed, int level){
		timestamp = ts;
		measuredX = meas_X;
		measuredY = meas_Y;
		touchX = touch_X;
		touchY = touch_Y;
		numberApsAboveYellowRssiThreshold = numYel;
		numberApsAboveRedRssiThreshold = numRed;
		maxRssiLevel = level;
	}
	
	public String toString(){
		String s = "";
		s = "\nTimestamp " + timestamp + "\nTouched x,y " + touchX + " , " + touchY + "\nMeasured x,y "+measuredX+" , "+measuredY+"\nMax rssi level " + maxRssiLevel +
				"\nfromALE " + fromALE + "\nerror " + error + "\nfloorId " + floorId + "\nbuildingId " + buildingId +
				"\ncampusId " + campusId + "\nethAddr " + ethAddr + "\nhashedEth " + hashedEth;
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
