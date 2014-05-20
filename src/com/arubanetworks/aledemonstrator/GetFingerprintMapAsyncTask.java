package com.arubanetworks.aledemonstrator;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;

import android.os.AsyncTask;
import android.util.Log;


public class GetFingerprintMapAsyncTask extends AsyncTask < String, String, String> {
	String TAG = "GetFingerprintMapAsyncTask";
	HttpURLConnection connection;
	int TIMEOUT_VALUE = 15000;
	String urlString = "/api/v1/survey/map";
	String result = "fail";
		
	protected String doInBackground(String... params) {
		Log.d(TAG, "getFingerprintMapAsyncTask starting with "+params[0]);
		JSONObject jsonObject = new JSONObject();
		try {
			jsonObject.put("floorId", params[0]);
		} catch (Exception e) { Log.e(TAG, "Exception forming JSON with "+params[0]); }
		char[] payload = jsonObject.toString().toCharArray();
		try {
			
			try {
				URL url = new URL("http://" + MainActivity.aleHost + ":" +MainActivity.alePort + urlString);
				Log.i(TAG, "getting fingerprint map with "+url.toString());
				Log.i(TAG, "posting this JSON "+params[0]);				
				connection = (HttpURLConnection) url.openConnection();
				connection.setConnectTimeout(TIMEOUT_VALUE);
				connection.setReadTimeout(TIMEOUT_VALUE);
				connection.setRequestMethod("POST");
				connection.setRequestProperty("content-type", "application/json");
				connection.setDoOutput(true);
				connection.setDoInput(true);
				connection.setFixedLengthStreamingMode(payload.length);				
				OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream());
				out.write(payload);
				out.flush();
				
				Log.v(TAG, "ALE Post response status code "+connection.getResponseCode());
	            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
		        StringBuilder builder = new StringBuilder();
		        String line;
	            while ((line = in.readLine()) != null) {
	                builder.append(line);	                
	            }
	            result = builder.toString();
	            
	            out.close();
	            in.close();     
	            
	        } finally {
	        	try { connection.disconnect(); } catch (Exception e) { Log.e(TAG, "Exception disconnecting connection "+e); }
	        }

		} catch (Exception e) { Log.e(TAG, "Post Exception "+e); }
		return result;
	}
	
	public void onPreExecute(){
		Log.i(TAG, "GetFingerprintMapAsyncTask starting");
		MainActivity.getFingerprintMapAsyncTaskInProgress = true;
	}
	
	public void onPostExecute(String result){
		Log.i(TAG, "GetFingerprintMapAsyncTask finished with "+result);
		MainActivity.getFingerprintMapAsyncTaskInProgress = false;
		String floorId = parseResultForFloor(result);
		Log.v(TAG, "floorId "+floorId);
		Log.v(TAG, "parsed"+parseResultForFingerprintMap(result));
		if(MainActivity.floorList != null && MainActivity.floorListIndex != -1 && MainActivity.floorList.size() >= MainActivity.floorListIndex &&
			floorId != null && floorId.equals(MainActivity.floorList.get(MainActivity.floorListIndex).floor_id)) {
			ArrayList<FingerprintMapPoint> fingerprintMap = parseResultForFingerprintMap(result);
			MainActivity.floorList.get(MainActivity.floorListIndex).fingerprintMapList = fingerprintMap;
		}
	}	
	
	
	public String parseResultForFloor(String in) {
		String result = null;
		try{
			JSONObject jObject = new JSONObject(in);
			result = jObject.getString("floorId");
			Log.v(TAG, "fingerprint map floor id "+result);
		} catch (Exception e) { 
			Log.e(TAG, "could not parse json floor object "+e); 
		}
	return result;
	}

	
	public ArrayList<FingerprintMapPoint> parseResultForFingerprintMap(String in) {
		ArrayList<FingerprintMapPoint> fingerprintMap = new ArrayList<FingerprintMapPoint>();
		try{
			JSONObject jObject = new JSONObject(in);
			JSONArray jArray = jObject.getJSONArray("surveyPoint");
			for(int i=0; i<jArray.length(); i++){
				JSONObject surveyPoint = jArray.getJSONObject(i);  // gets the msg object
				Log.v(TAG, "1 "+i+"  "+surveyPoint.toString());
				float locationX = (float)surveyPoint.getDouble("locationX");
				float locationY = (float)surveyPoint.getDouble("locationY");
				int satisfactory = Integer.parseInt(surveyPoint.getString("satisfactory"));
				if(surveyPoint.has("radioCoverage")) {
					JSONArray j2Array = surveyPoint.getJSONArray("radioCoverage");
					ArrayList<RadioCoverPoint> radioCoverList = new ArrayList<RadioCoverPoint>();
					for(int j=0; j<j2Array.length(); j++){
						String apName = null;
						JSONObject radioCoverage = j2Array.getJSONObject(j);
						String apMac = radioCoverage.getString("apMac");
						String radioBssid = radioCoverage.getString("radioBssid");
						if(radioCoverage.has("apName")) { apName = radioCoverage.getString("apName"); }
						int rssi = radioCoverage.getInt("rssi");
						RadioCoverPoint radioCover = new RadioCoverPoint(apMac, radioBssid, apName, rssi);
						radioCoverList.add(radioCover);
					}
				fingerprintMap.add(new FingerprintMapPoint(locationX, locationY, satisfactory, radioCoverList));
				}
			}
		} catch (Exception e) { 
			Log.e(TAG, "could not parse json floor object "+e); 
		}
	return fingerprintMap;
	}

	String testObject = "{" +
			"\"floorId\":\"FEE3EBCE3AA64CBA836DAB1DEB0F8385\",\"surveyPoint\":[{\"locationX\" : \"15.2\",\"locationY\":\"35.3\",\"satisfactory\":\"true\",\"radioCoverage\": ["+
			"{\"apMac\":\"9C:1C:12:89:11:00\",\"radioBssid\":\"9C:1C:12:89:21:13\",\"apName\":\"ap-225-GSW\",\"rssi\":\"-82\"},"+
			"{\"apMac\":\"9C:1C:12:44:11:00\",\"radioBssid\":\"9C:1C:12:44:21:13\",\"apName\":\"ap-225-GSZ\",\"rssi\":\"-67\"}" +
			"]},{\"locationX\":\"15.4\",\"locationY\":\"40.5\",\"satisfactory\":\"true\",\"radioCoverage\":[" +
			"{\"apMac\":\"9C:1C:12:89:11:00\",\"radioBssid\":\"9C:1C:12:89:21:13\",\"apName\":\"ap-225-GSW\",\"rssi\":\"-75\"}," +
			"{\"apMac\":\"9C:1C:12:44:11:00\",\"radioBssid\":\"9C:1C:12:44:21:13\",\"apName\":\"ap-225-GSZ\",\"rssi\":\"-77\"}" +
			"]}]}"
	;
		
}
