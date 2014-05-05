package com.arubanetworks.aledemonstrator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;

public class FindMyLocationAsyncTask extends AsyncTask < String, String, String> {
	String TAG = "FindMyLocationAsyncTask";
	int TIMEOUT_VALUE = 10000;
	
	protected String doInBackground(String... params) {
		
		// query for location of my mac or target mac
		String result = JsonParsers.parseAleJsonLocation(readStreamString(runLookup("/api/v1/location.json?sta_eth_mac="+params[0])), params[1]);
		//result = JsonParsers.parseAleJsonLocation(readStreamString(runLookup("/api/v1/location.json?sta_eth_mac="+"00:23:14:D4:C8:48")));
		return result;
	}
	
	public void onPreExecute(){
		Log.i(TAG, "FindMyLocationAsyncTask starting");
		MainActivity.findMyLocationAsyncTaskInProgress = true;
		if(MainActivity.floorList != null){
			MainActivity.httpStatusString1 = MainActivity.floorList.size()+" floors.  Discovering my floor";
		}
	}
	
	public void onPostExecute(String result){
		Log.i(TAG, "FindMyLocationAsyncTask finished with _"+result+"_");
		MainActivity.findMyLocationAsyncTaskInProgress = false;
	}
	
	
	// this opens the http connection and returns an input stream, same for json or protobuf
	private InputStream runLookup(String args){
		InputStream result = null;
		try{
			URL url = new URL("http://"+MainActivity.aleHost+":8080"+args);
			Log.v(TAG, "URL get protocol "+url.getProtocol()+" host "+url.getHost()+" port "+url.getPort()+" file "+url.getFile());
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setConnectTimeout(TIMEOUT_VALUE);
			connection.setReadTimeout(TIMEOUT_VALUE);
			/*Map<String, List<String>> map = connection.getHeaderFields();
			for(Entry<String, List<String>> entry : map.entrySet()) { 
				for(int i=0; i<entry.getValue().size(); i++){
					Log.v(TAG, "header "+entry.getKey()+"  "+entry.getValue().get(i));
				}
			} */ 
	
			result = connection.getInputStream();
			Log.v(TAG, "http result code was "+connection.getResponseCode()+"  response message "+connection.getResponseMessage());	
		} catch (Exception e) { Log.e(TAG, "Exception getting location API query "+e); }
	return result;
	}
	
	
	// use this one for json queries
	private String readStreamString(InputStream in) {
		StringBuilder sb = new StringBuilder("");
		BufferedReader br = null;
		try {
			br = new BufferedReader(new InputStreamReader(in));
		    String line = "";
		    while ((line = br.readLine()) != null) {
			  Log.v(TAG, "NEW LINE "+line);
		      sb.append(line);
		    }
		} 
		catch (IOException e) { Log.e(TAG, "IOException reading inputstream "+e); } 
		catch (Exception e1) { Log.e(TAG, "Exception reading inputstream "+e1); } 
		finally { if (br != null) { try { br.close(); } catch (IOException e) { Log.e(TAG, "Exception closing reader "+e); } } }
		return sb.toString();
	}
	
	
}
