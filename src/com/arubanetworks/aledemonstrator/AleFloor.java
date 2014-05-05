package com.arubanetworks.aledemonstrator;

import java.util.ArrayList;

public class AleFloor {
	String floor_id;
	String floor_name;
	float floor_latitude;
	float floor_longitude;
	String floor_img_path;
	float floor_img_width;
	float floor_img_length;
	String building_id;
	float floor_level;
	String units;
	float grid_size;
	
	String building_name = "not found";
	String campus_id = "not found";
	String campus_name = "not found";
	
	ArrayList<FingerprintMapPoint> fingerprintMapList = new ArrayList<FingerprintMapPoint>();
	
	public AleFloor(String _floor_id, String _floor_name, float _floor_latitude, float _floor_longitude, String _floor_img_path,
			float _floor_img_width, float _floor_img_length, String _building_id, float _floor_level, String _units, float _grid_size){
		floor_id = _floor_id;
		floor_name = _floor_name;
		floor_latitude = _floor_latitude;
		floor_longitude = _floor_longitude;
		floor_img_path = _floor_img_path;
		floor_img_width = _floor_img_width;
		floor_img_length = _floor_img_length;
		building_id = _building_id;
		floor_level = _floor_level;
		units = _units;
		grid_size = _grid_size;
	}
	
	public String toString(){
		String result = "floor_id_"+floor_id+"_ floor_name_"+floor_name+"_ floor_latitude_"+floor_latitude+"_ floor_longitude_"+floor_longitude+
				"_ floor_img_path_"+floor_img_path+"_ floor_img_width_"+floor_img_width+"_ floor_img_length_"+floor_img_length+"_ building_id_"+building_id+
				"_ floor_level_"+floor_level+"_ units_"+units+"_"+"_ building_name_"+building_name+"_ campus_id_"+campus_id+"_ campus_name_"+campus_name+
				"_ grid_size_"+grid_size;
		return result;
	}
}
