package com.arubanetworks.aledemonstrator;

import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

public class JsonParsers {
	static String TAG = "JsonParsers";
	
	// when anonymization is applied on ALE, it does not send sta_eth_mac element in JSON any more.  So added a boolean to indicate this is for a myMac or targetMac lookup.
	public static String parseAleJsonLocation(String in, String myLocation) {
		String result = null;
		in = in.replace(" ", "");
		try{
			JSONObject location = new JSONObject(in);
			JSONArray locationArray = location.getJSONArray("Location_result");
			for(int i=0; i<locationArray.length(); i++){
				JSONObject msgObject = locationArray.getJSONObject(i).getJSONObject("msg");  // gets the msg object
				float sta_location_x = (float)msgObject.getDouble("sta_location_x");
				float sta_location_y = (float)msgObject.getDouble("sta_location_y");
				float error = (float)msgObject.getDouble("error_level");
				String floorId = msgObject.getString("floor_id");
				String buildingId = msgObject.getString("building_id");
				String campusId = msgObject.getString("campus_id");
				String sta_eth_mac = "";
				String hashed_sta_eth_mac = msgObject.getString("hashed_sta_eth_mac");
				
				// if it's the mac of this device, set some fields and add a position history object to the alePositionHistoryList
				if(myLocation == "true") {
					MainActivity.site_xAle = (float) msgObject.getDouble("sta_location_x");
					MainActivity.site_yAle = (float) msgObject.getDouble("sta_location_y");
					MainActivity.myHashMac = msgObject.getString("hashed_sta_eth_mac");
					MainActivity.myFloorId = msgObject.getString("floor_id");
					Log.v(TAG, "my mac located hash_mac _"+MainActivity.myHashMac+"_  floor _"+MainActivity.myFloorId+"_");

					PositionHistoryObject newObject = new PositionHistoryObject(new Date(), 0, 0, sta_location_x, sta_location_y, -99, false, error, 
							floorId, buildingId, campusId, sta_eth_mac, hashed_sta_eth_mac, "ft", "XX", "XX", 0, null);
					MainActivity.alePositionHistoryList.add(0, newObject);
					// added to make the verify feature work with zmq disabled
					if(MainActivity.zmqEnabled == false && MainActivity.trackMode == MainActivity.MODE_VERIFY) { MainActivity.aleHttpPositionHistoryList.add(0, newObject); }
				} 
				
				// if it's the mac of a target, not us, add a position history object to a list on the aleAllPositionHistoryMap 
				// so it shows up on the floorplan view immediately before protobufs come in
				if(myLocation == "false") {
					MainActivity.targetHashMac = msgObject.getString("hashed_sta_eth_mac");
					Log.v(TAG, "target mac located hash_mac "+MainActivity.targetHashMac);
					PositionHistoryObject newObject = new PositionHistoryObject(new Date(), 0, 0, sta_location_x, sta_location_y, -99, false, error, 
							floorId, buildingId, campusId, sta_eth_mac, hashed_sta_eth_mac, "ft", "XX", "XX", 0, null);
					Log.v(TAG, "new positionHistoryObject target Mac eth _"+sta_eth_mac+"_ hash _"+hashed_sta_eth_mac+"_  x_"+sta_location_x+"  y_"+sta_location_y+" site "+floorId);
					if(MainActivity.aleAllPositionHistoryMap.containsKey(hashed_sta_eth_mac)){
						MainActivity.aleAllPositionHistoryMap.get(hashed_sta_eth_mac).add(newObject);
						Log.v(TAG, "added new position history object to an existing list in the map");
					} else {
						ArrayList<PositionHistoryObject> newList = new ArrayList<PositionHistoryObject>();
						newList.add(newObject);
						Log.d(TAG, "ale all position history map, adding list for hash "+hashed_sta_eth_mac+" list size "+newList.size());
						MainActivity.aleAllPositionHistoryMap.put(hashed_sta_eth_mac, newList);
						Log.v(TAG, "added new position history object with a new entry in the map");
					}
				}
				
			}
		} catch (Exception e) { 
			Log.e(TAG, "could not parse location object "+e+"  (input was _"+in+")");
		}	
		
		Log.v(TAG, "parseAleJsonLocation returning "+result);
		return result;
	}
	
	public static ArrayList<AleFloor> parseAleJsonFloor(String in) {
		ArrayList<AleFloor> floorList = new ArrayList<AleFloor>();
		try{
			JSONObject jObject = new JSONObject(in);
			JSONArray jArray = jObject.getJSONArray("Floor_result");
			for(int i=0; i<jArray.length(); i++){
				JSONObject floor = jArray.getJSONObject(i).getJSONObject("msg");  // gets the msg object
				//Log.v(TAG, "1 "+i+"  "+floor.toString());
				String floor_id = floor.getString("floor_id");
				String floor_name = floor.getString("floor_name");
				float floor_latitude = Float.valueOf(floor.getString("floor_latitude"));
				float floor_longitude = Float.valueOf(floor.getString("floor_longitude"));
				String floor_img_path = floor.getString("floor_img_path");
				float floor_img_width = Float.valueOf(floor.getString("floor_img_width"));
				float floor_img_length = Float.valueOf(floor.getString("floor_img_length"));
				String building_id = floor.getString("building_id");
				
				float floor_level = 0;
				try{
					floor_level = Float.valueOf(floor.getString("floor_level"));  // not seen in tests?
				} catch (Exception e) { Log.e(TAG, "no floor_level seen in the floor object, using 0 "); }
				
				String units = "ft";
				try{
					units = floor.getString("units");  // not seen in tests?
				} catch (Exception e) { Log.e(TAG, "no units in the floor object, using feet "); }
				
				float grid_size = 5.0f;
				try{
					grid_size = Float.valueOf(floor.getString("grid_size"));  // not seen in tests?
				} catch (Exception e) { Log.e(TAG, "no grid size, using 5.0 "); }
				

				AleFloor newFloor = new AleFloor(floor_id, floor_name, floor_latitude, floor_longitude, floor_img_path,
						floor_img_width, floor_img_length, building_id, floor_level, units, grid_size);
				floorList.add(newFloor);
			}
		} catch (Exception e) { 
			Log.e(TAG, "could not parse json floor object "+e); 
		}
	return floorList;
	}
	
	public static ArrayList<AleBuilding> parseAleJsonBuilding(String in) {
		ArrayList<AleBuilding> buildingList = new ArrayList<AleBuilding>();
		try{
			JSONObject jObject = new JSONObject(in);
			JSONArray jArray = jObject.getJSONArray("Building_result");
			for(int i=0; i<jArray.length(); i++){
				JSONObject building = jArray.getJSONObject(i).getJSONObject("msg");  // gets the msg object
				String building_id = building.getString("building_id");
				String building_name = building.getString("building_name");
				String campus_id = building.getString("campus_id");
				AleBuilding newBuilding = new AleBuilding(building_id, building_name, campus_id);
				buildingList.add(newBuilding);
			}
		} catch (Exception e) { 
			Log.e(TAG, "could not parse json building object "+e); 
		}
		return buildingList;
	}
	
	public static ArrayList<AleCampus> parseAleJsonCampus(String in){
		ArrayList<AleCampus> campusList = new ArrayList<AleCampus>();
		try{
			JSONObject jObject = new JSONObject(in);
			JSONArray jArray = jObject.getJSONArray("Campus_result");
			for(int i=0; i<jArray.length(); i++){
				JSONObject campus = jArray.getJSONObject(i).getJSONObject("msg");  // gets the msg object
				String campus_id = campus.getString("campus_id");
				String campus_name = campus.getString("campus_name");
				AleCampus newCampus = new AleCampus(campus_id, campus_name);
				campusList.add(newCampus);
			}
		} catch (Exception e) { 
			Log.e(TAG, "could not parse json campus object "+e); 
		}
		return campusList;
	}
	

}
