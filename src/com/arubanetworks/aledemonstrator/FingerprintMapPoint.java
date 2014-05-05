package com.arubanetworks.aledemonstrator;

import java.util.ArrayList;

public class FingerprintMapPoint {
	float locationX;
	float locationY;
	boolean satisfactory;
	ArrayList<RadioCoverPoint> radioCoverList;
	
	public FingerprintMapPoint (float locationX, float locationY, boolean satisfactory, ArrayList<RadioCoverPoint> radioCoverList){
		this.locationX = locationX;
		this.locationY = locationY;
		this.satisfactory = satisfactory;
		this.radioCoverList = radioCoverList;
	}
	
}
