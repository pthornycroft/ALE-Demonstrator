package com.arubanetworks.aledemonstrator;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.JSONObject;

import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

public class PostFingerprintAsyncTask extends AsyncTask < SurveyObject, String, SurveyObject> {
	String TAG = "PostFingerprintAsyncTask";
	HttpURLConnection connection;
	int TIMEOUT_VALUE = 60000;
	String urlString = ":8080/api/v1/survey/fingerprint";
	
	protected SurveyObject doInBackground(SurveyObject... params) {
		SurveyObject result = new SurveyObject(params[0].pho, params[0].action, false);
		PositionHistoryObject pho = params[0].pho;
		JSONObject jsonObject = JsonBuilders.formFingerprintJsonObject( pho.ethAddr, pho.timestamp.getTime(), pho.floorId, pho.touchX, 
    			pho.touchY, pho.units, pho.deviceMfg, pho.deviceModel, pho.compassDegrees, pho.iBeaconJsonArray);
		char[] payload = jsonObject.toString().toCharArray();
		if(params[0].action == "delete") { urlString = ":8080/api/v1/survey/delete"; }
		try {
			
			try {
				URL url = new URL("http://" + MainActivity.aleHost + urlString);
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
	            boolean didSucceed = parseFingerprintPostResponse(builder.toString());
	            result = new SurveyObject(params[0].pho, params[0].action, didSucceed);
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
	}
	
	public void onPostExecute(SurveyObject result){
		Log.i(TAG, "PostFingerprintAsyncTask finished with "+result.success);
		MainActivity.postFingerprintAsyncTaskInProgress = false;
		if(result.action.equals("add") && result.success == true) { MainActivity.addSurveyPointToList(result.pho, result.success); }
		if(result.action.equals("delete") && result.success == true) { MainActivity.deleteSurveyPointFromList(result.pho, result.success); }
	}	
	
	private boolean parseFingerprintPostResponse(String in) {
		boolean result = false;
		try {
			JSONObject jObject = new JSONObject(in);
			result = jObject.getBoolean("result");
		} catch (Exception e) { Log.e(TAG, "could not parse fingerprint Post response "+e); }
		return result;
	}

	
}
