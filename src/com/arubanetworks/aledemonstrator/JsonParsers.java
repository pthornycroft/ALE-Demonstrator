package com.arubanetworks.aledemonstrator;

import java.util.ArrayList;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

public class JsonParsers {
	static String TAG = "JsonParsers";
	
	
	public static String parseAleJsonLocation(String in) {
		String result = null;
		in = in.replace(" ", "");
		try{
			JSONObject location = new JSONObject(in);
			JSONArray locationArray = location.getJSONArray("Location_result");
			for(int i=0; i<locationArray.length(); i++){
				JSONObject msgObject = locationArray.getJSONObject(i).getJSONObject("msg");  // gets the msg object
				String sta_eth_mac = msgObject.getJSONObject("sta_eth_mac").getString("addr");
				Log.v(TAG, "parseAleJsonLocation staEthMac is "+sta_eth_mac+" myMac "+MainActivity.myMac);
				if(MainActivity.myMac != null && sta_eth_mac.equalsIgnoreCase(MainActivity.myMac.replace(":", "").toUpperCase(Locale.US))){
					MainActivity.site_xAle = (float) msgObject.getDouble("sta_location_x");
					MainActivity.site_yAle = (float) msgObject.getDouble("sta_location_y");
					MainActivity.myHashMac = msgObject.getString("hashed_sta_eth_mac");
					MainActivity.myFloorId = msgObject.getString("floor_id");
					Log.v(TAG, "location lookup eth _"+sta_eth_mac+"_  hash _"+MainActivity.myHashMac+"_  floor _"+MainActivity.myFloorId+"_");
					//MainActivity.campusIdAle = msgObject.getString("campus_id");
					//MainActivity.buildingIdAle = msgObject.getString("building_id");
					//Log.i(TAG, "That was our LOCATION x "+MainActivity.site_xAle+"  y "+MainActivity.site_yAle+" site "+MainActivity.floorIdAle+" building "+MainActivity.buildingIdAle); 
					//MainActivity.hashedStaEthMacAle = msgObject.getString("hashed_sta_eth_mac");
					//if(!MainActivity.floorIdAle.equals("not found") && !MainActivity.floorImgIdAle.equals("not found") && !MainActivity.floorIdAle.equals(MainActivity.floorImgIdAle)){
					//	Log.w(TAG, "LOCATION floorIdAle does not match floorImgIdAle, set to not found "+MainActivity.floorIdAle+"  imgId was "+MainActivity.floorImgIdAle);
					//	MainActivity.floorImgIdAle = "not found";
					//}
				} 
				if(MainActivity.targetHashMac != null && sta_eth_mac.equalsIgnoreCase(MainActivity.targetHashMac.replace(":", "").toUpperCase(Locale.US))){
					MainActivity.targetHashMac = msgObject.getString("hashed_sta_eth_mac");
				}
			}
		} catch (Exception e) { 
			Log.e(TAG, "could not parse location object "+e+"  input was _\n"+in);
			//result = parseAleJsonLocationOld(in);
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
				//float floor_level = Float.valueOf(floor.getString("floor_level"));  // not seen in tests?
				//String units = floor.getString("units");  // not seen in tests?

				AleFloor newFloor = new AleFloor(floor_id, floor_name, floor_latitude, floor_longitude, floor_img_path,
						floor_img_width, floor_img_length, building_id, 0, "not found");
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
