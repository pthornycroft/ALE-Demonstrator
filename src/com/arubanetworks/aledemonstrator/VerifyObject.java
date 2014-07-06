package com.arubanetworks.aledemonstrator;

import java.util.Date;


public class VerifyObject {

	Date trueTimestamp;
	double trueX;
	double trueY;
	Date aleTimestamp;
	double aleX;
	double aleY;
	
	public VerifyObject(Date trueTimestamp, float trueX, float trueY, Date aleTimestamp, float aleX, float aleY){
		this.trueTimestamp = trueTimestamp;
		this.trueX = (double)trueX;
		this.trueY = (double)trueY;
		this.aleTimestamp = aleTimestamp;
		this.aleX = (double)aleX;
		this.aleY = (double)aleY;
	}
	
	public String toCsv(){
		String result = "";
		float timeDiff = (float)(trueTimestamp.getTime() - aleTimestamp.getTime())/(float)1000;
		String trueTime = android.text.format.DateFormat.format("yyyy-MM-dd kk:mm:ss", trueTimestamp).toString();
		String aleTime = android.text.format.DateFormat.format("yyyy-MM-dd kk:mm:ss", aleTimestamp).toString();
		double distDiff = Math.sqrt(((trueX-aleX)*(trueX-aleX)) + ((trueY-aleY)*(trueY-aleY)));
		result = result + trueTime + "," + timeDiff + "," + String.format("%.2f",distDiff) + "," +String.format("%.2f",trueX) + "," + String.format("%.2f",trueY) + ",";
		result = result + aleTime + "," + String.format("%.2f",aleX) + "," + String.format("%.2f",aleY) + "\n";
		return result;
	}
	
}
