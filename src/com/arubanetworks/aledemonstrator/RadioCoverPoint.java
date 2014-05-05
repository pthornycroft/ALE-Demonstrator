package com.arubanetworks.aledemonstrator;

public class RadioCoverPoint {
	String apMac;
	String radioBssid;
	String apName;
	int rssi;
	
	public RadioCoverPoint (String apMac, String radioBssid, String apName, int rssi){
		this.apMac = apMac;
		this.radioBssid = radioBssid;
		this.apName = apName;
		this.rssi = rssi;
	}
	
}
