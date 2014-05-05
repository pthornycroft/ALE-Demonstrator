package com.arubanetworks.aledemonstrator;

import org.json.JSONArray;
import org.json.JSONObject;

import android.util.Log;

public class JsonBuilders {
	static String TAG = "JsonBuilders";
	
	public static JSONObject formFingerprintJsonObject(
		String mac,
		long timestamp,
		String floorId,
		float locationX,
		float locationY,
		String units,
		String deviceMfg,
		String deviceModel,
		float compassDegrees,
		JSONArray iBeacon
		){
			JSONObject fingerprint = new JSONObject();
			try{
				fingerprint.put("mac", mac);
				fingerprint.put("timestamp", timestamp);
				fingerprint.put("floorId", floorId);
				fingerprint.put("locationX", locationX);
				fingerprint.put("locationY", locationY);
				fingerprint.put("units", units);
				fingerprint.put("deviceMfg", deviceMfg);
				fingerprint.put("deviceModel", deviceModel);
				if(compassDegrees != 0.0) { fingerprint.put("compassDegrees", compassDegrees); }  // test for validity of compass
				if(iBeacon != null) { fingerprint.put("iBeacon", iBeacon); }
			} catch (Exception e) { Log.e(TAG, "Exception forming fingerprint object "+e); }
			return fingerprint;
	}
	
	public static JSONObject formIBeaconJsonObject(
		String mac,
		String uuid,
		int major,
		int minor,
		int txPwr,
		int rssi
		){
			JSONObject iBeacon = new JSONObject();
			try{
				iBeacon.put("mac",  mac);
				iBeacon.put("uuid", uuid);
				iBeacon.put("major", major);
				iBeacon.put("minor", minor);
				iBeacon.put("txPwr", txPwr);
				iBeacon.put("rssi", rssi);
			} catch (Exception e) { Log.e(TAG, "Exception forming iBeacon object "+e); }
			return iBeacon;
		
	}
	
}
