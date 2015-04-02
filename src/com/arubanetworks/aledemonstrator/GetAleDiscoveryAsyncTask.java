package com.arubanetworks.aledemonstrator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.net.ssl.HttpsURLConnection;

import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

public class GetAleDiscoveryAsyncTask extends AsyncTask <String, Integer, ArrayList[]> {
	String TAG = "GetAleDiscoveryAsyncTask";
	int TIMEOUT_VALUE = 20000;
	
	protected ArrayList[] doInBackground(String... params) {
		
		// query for campuses
		ArrayList<AleCampus> campusList = JsonParsers.parseAleJsonCampus(readStreamString(runLookup("/api/v1/campus.json")));
		Log.v(TAG, "finished campus lookup");
		// query for buildings
		ArrayList<AleBuilding> buildingList = JsonParsers.parseAleJsonBuilding(readStreamString(runLookup("/api/v1/building.json")));
		Log.v(TAG, "finished building lookup");
		// query for floors
		ArrayList<AleFloor> floorList = JsonParsers.parseAleJsonFloor(readStreamString(runLookup("/api/v1/floor.json")));
		Log.v(TAG, "finished floor lookup");
		
		ArrayList[] result = {campusList, buildingList, floorList};
		return result;
	}
	
	public void onPreExecute(){
		Log.i(TAG, "getAleDiscoveryAsyncTask starting");
		MainActivity.aleDiscoveryAsyncTaskInProgress = true;
		MainActivity.httpStatusString1 = "Discovering floors";
		MainActivity.httpStatusString2 = "";
	}
	
	public void onPostExecute(ArrayList[] result){
		Log.i(TAG, "getAleDiscoveryAsyncTask finished");
		MainActivity.aleDiscoveryAsyncTaskInProgress = false;
		MainActivity.campusList = result[0];
		MainActivity.buildingList = result[1];
		MainActivity.floorList = result[2];
		for (int i=0; i<result[0].size(); i++){ Log.i(TAG, "Campus List "+i +" "+result[0].get(i).toString()); }
		for (int i=0; i<result[1].size(); i++){ Log.i(TAG, "Building List "+i +" "+result[1].get(i).toString()); }
		for (int i=0; i<result[2].size(); i++){ Log.i(TAG, "Floor List "+i +" "+result[2].get(i).toString()); }
		if(result[2] != null) { MainActivity.httpStatusString1 = "Discovered "+result[2].size()+" floors"; }
		else { MainActivity.httpStatusString1 = "Failed to discover floors"; }
	}
	
	
	// this opens the http connection and returns an input stream, same for json or protobuf
	private InputStream runLookup(String args){
		InputStream result = null;
		try{
			URL url = new URL("https://"+MainActivity.aleHost+":"+MainActivity.alePort+args);
			Log.v(TAG, "URL get protocol "+url.getProtocol()+" host "+url.getHost()+" port "+url.getPort()+" file "+url.getFile());
			HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
			connection.setHostnameVerifier(new CustomVerifier());
			connection.setConnectTimeout(TIMEOUT_VALUE);
			connection.setReadTimeout(TIMEOUT_VALUE);
			connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 4.3; Nexus 7 Build/JWR66D) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/27.0.1453.111 Safari/537.36");
			connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			
			// prints the outgoing headers for troubleshooting
			/*Map<String, List<String>> mapOut = connection.getRequestProperties();
			for(Entry<String, List<String>> entry : mapOut.entrySet()) { 
				for(int i=0; i<entry.getValue().size(); i++){
					Log.v(TAG, "outgoing http discovery header "+entry.getKey()+"  "+entry.getValue().get(i));
				}
			} */
						
			connection.connect();
			
			// prints the incoming headers for troubleshooting
			/*Map<String, List<String>> mapIn = connection.getHeaderFields();
			for(Entry<String, List<String>> entry : mapIn.entrySet()) { 
				for(int i=0; i<entry.getValue().size(); i++){
					Log.v(TAG, "incoming http discovery header "+entry.getKey()+"  "+entry.getValue().get(i));
				}
			}	*/
			
			result = connection.getInputStream();
			if(connection.getResponseCode() != 200) { MainActivity.httpStatusString2 = connection.getResponseMessage(); }
			Log.v(TAG, "AleDiscovery http result code was "+connection.getResponseCode()+"  response message "+connection.getResponseMessage());	
		} catch (java.net.ConnectException e) { 
			Log.e(TAG, "connect exception "+e); 
			MainActivity.httpStatusString2 = e.getMessage();
		} catch (java.net.SocketTimeoutException e) {
			Log.e(TAG, "socket timeout exception "+e); 
			if(e.toString().contains("failed to connect")) { MainActivity.httpStatusString2 = "Failed to connect to "+MainActivity.aleHost+":"+MainActivity.alePort+" after "+(TIMEOUT_VALUE/1000)+"sec "; }
			else { MainActivity.httpStatusString2 = e.getMessage().toString(); }
		}
		catch (Exception e) { Log.e(TAG, "Exception getting location AleDiscovery "+e); }
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
			  //Log.i(TAG, "NEW LINE "+line);
		      sb.append(line);
		    }
		} 
		catch (IOException e) { Log.e(TAG, "IOException reading AleDiscovery inputstream "+e); } 
		catch (Exception e1) { Log.e(TAG, "Exception reading AleDiscovery inputstream "+e1); } 
		finally { if (br != null) { try { br.close(); } catch (IOException e) { Log.e(TAG, "Exception closing AleDiscovery reader "+e); } } }
		return sb.toString();
	}
	
	
}

