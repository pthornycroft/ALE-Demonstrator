package com.arubanetworks.aledemonstrator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import android.os.AsyncTask;
import android.util.Log;

public class AleDiscoveryAsyncTask extends AsyncTask <String, Integer, ArrayList[]> {
	String TAG = "AleDiscoveryAsyncTask";
	
	protected ArrayList[] doInBackground(String... params) {
		
		// query for campuses
		ArrayList<AleCampus> campusList = JsonParsers.parseAleJsonCampus(readStreamString(runLookup("/api/v1/campus.json")));
		// query for buildings
		ArrayList<AleBuilding> buildingList = JsonParsers.parseAleJsonBuilding(readStreamString(runLookup("/api/v1/building.json")));
		// query for floors
		ArrayList<AleFloor> floorList = JsonParsers.parseAleJsonFloor(readStreamString(runLookup("/api/v1/floor.json")));
		
		ArrayList[] result = {campusList, buildingList, floorList};
		return result;
	}
	
	public void onPreExecute(){
		Log.i(TAG, "aleDiscoveryAsyncTask starting");
		MainActivity.aleDiscoveryAsyncTaskInProgress = true;
		MainActivity.httpStatusString = "Discovering campus, building, floor data";
	}
	
	public void onPostExecute(ArrayList[] result){
		Log.i(TAG, "aleDiscoveryAsyncTask finished");
		MainActivity.aleDiscoveryAsyncTaskInProgress = false;
		MainActivity.campusList = result[0];
		MainActivity.buildingList = result[1];
		MainActivity.floorList = result[2];
		for (int i=0; i<result[0].size(); i++){ Log.i(TAG, "Campus List "+i +" "+result[0].get(i).toString()); }
		for (int i=0; i<result[1].size(); i++){ Log.i(TAG, "Building List "+i +" "+result[1].get(i).toString()); }
		for (int i=0; i<result[2].size(); i++){ Log.i(TAG, "Floor List "+i +" "+result[2].get(i).toString()); }
		if(result[2] != null) { MainActivity.httpStatusString = "Discovered campus, building, floor data"; }
		else { MainActivity.httpStatusString = "Failed to discover campus, building, floor"; }
	}
	
	
	// this opens the http connection and returns an input stream, same for json or protobuf
	private InputStream runLookup(String args){
		InputStream result = null;
		try{
			URL url = new URL("http://"+MainActivity.aleHost+":8080"+args);
			Log.v(TAG, "URL get protocol "+url.getProtocol()+" host "+url.getHost()+" port "+url.getPort()+" file "+url.getFile());
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			
			/*Map<String, List<String>> map = connection.getHeaderFields();
			for(Entry<String, List<String>> entry : map.entrySet()) { 
				for(int i=0; i<entry.getValue().size(); i++){
					Log.v(TAG, "header "+entry.getKey()+"  "+entry.getValue().get(i));
				}
			} */
	
			result = connection.getInputStream();
			Log.v(TAG, "AleDiscovery http result code was "+connection.getResponseCode()+"  response message "+connection.getResponseMessage());	
		} catch (Exception e) { Log.e(TAG, "Exception getting location AleDiscovery "+e); }
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
			  //Log.v(TAG, "NEW LINE "+line);
		      sb.append(line);
		    }
		} 
		catch (IOException e) { Log.e(TAG, "IOException reading AleDiscovery inputstream "+e); } 
		catch (Exception e1) { Log.e(TAG, "Exception reading AleDiscovery inputstream "+e1); } 
		finally { if (br != null) { try { br.close(); } catch (IOException e) { Log.e(TAG, "Exception closing AleDiscovery reader "+e); } } }
		return sb.toString();
	}
	
	
}

