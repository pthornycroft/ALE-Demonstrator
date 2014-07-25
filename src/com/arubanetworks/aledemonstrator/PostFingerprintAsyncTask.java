package com.arubanetworks.aledemonstrator;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.JSONObject;

import android.os.AsyncTask;
import android.util.Log;

public class PostFingerprintAsyncTask extends AsyncTask < SurveyObject, String, SurveyObject> {
	String TAG = "PostFingerprintAsyncTask";
	HttpURLConnection connection;
	int TIMEOUT_VALUE = 120000;
	String urlString = "/api/v1/survey/fingerprint";
	
	protected SurveyObject doInBackground(SurveyObject... params) {
		SurveyObject result = new SurveyObject(params[0].pho, params[0].action, false, 0);
		PositionHistoryObject pho = params[0].pho;
		JSONObject jsonObject = JsonBuilders.formFingerprintJsonObject( pho.ethAddr, pho.timestamp.getTime(), pho.floorId, pho.touchX, 
    			pho.touchY, pho.units, pho.deviceMfg, pho.deviceModel, pho.compassDegrees, pho.iBeaconJsonArray);
		char[] payload = jsonObject.toString().toCharArray();
		if(params[0].action == "delete") { urlString = ":"+MainActivity.alePort+"/api/v1/survey/delete"; }
		
		try {

			try {
				URL url = new URL("http://" + MainActivity.aleHost +":" + MainActivity.alePort + urlString);
				Log.i(TAG, "posting fingerprint to "+url.toString());
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
	            boolean didSucceed = parseFingerprintPostResponseForResult(builder.toString());
	            int satisfactory = parseFingerprintPostResponseForSatisfactory(builder.toString());
	            result = new SurveyObject(params[0].pho, params[0].action, didSucceed, satisfactory);
	            Log.v(TAG, "ALE Post response to survey post "+builder.toString()+"\n"+result);
	            out.close();
	            in.close();            
	        } finally {
	            connection.disconnect();
	        }

		} catch (Exception e) { Log.e(TAG, "Post Exception "+e); }
		return result;
	}
	
	public void onPreExecute(){
		Log.i(TAG, "PostFingerprintAsyncTask starting");
		MainActivity.postFingerprintAsyncTaskInProgress = true;
		MainActivity.surveyConfirmButton.startAnimation(MainActivity.animAlpha);
	}
	
	public void onPostExecute(SurveyObject result){
		Log.i(TAG, "PostFingerprintAsyncTask finished with "+result.success);
		MainActivity.postFingerprintAsyncTaskInProgress = false;
		if(result.action.equals("add")) {
			if(result.satisfactory > 0 ) { 
				MainActivity.addSurveyPointToList(result.pho, true);
				FingerprintMapPoint newPoint = new FingerprintMapPoint(result.pho.measuredX, result.pho.measuredY, result.satisfactory, null);
				MainActivity.floorList.get(MainActivity.floorListIndex).fingerprintMapList.add(newPoint);
			} 
			else { MainActivity.addSurveyPointToList(result.pho, result.success); } 
		}
		else if(result.action.equals("delete")) { MainActivity.deleteSurveyPointFromList(result.pho, result.success); }
		MainActivity.surveyConfirmButton.clearAnimation();
	}	
	
	private boolean parseFingerprintPostResponseForResult(String in) {
		boolean result = false;
		try {
			JSONObject jObject = new JSONObject(in);
			if(jObject.has("result")) { result = jObject.getBoolean("result"); }
		} catch (Exception e) { Log.e(TAG, "could not parse fingerprint Post response for result "+e); }
		return result;
	}
	
	private int parseFingerprintPostResponseForSatisfactory(String in) {
		int result = 0;
		try {
			JSONObject jObject = new JSONObject(in);
			if(jObject.has("satisfactory")) { result = jObject.getInt("satisfactory"); }
		} catch (Exception e) { Log.e(TAG, "could not parse fingerprint Post response for satisfactory "+e); }
		return result;
	}

	
}
