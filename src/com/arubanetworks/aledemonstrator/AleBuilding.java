package com.arubanetworks.aledemonstrator;

public class AleBuilding {
	String building_id;
	String building_name;
	String campus_id;
	
	public AleBuilding(String _building_id, String _building_name, String _campus_id){
		building_name = _building_name;
		building_id = _building_id;
		campus_id = _campus_id;
	}
	
	public String toString(){
		String result = "name_"+building_name+"_ building_id_"+building_id+"_ campus_id_"+campus_id+"_";
		return result;
	}
	
}
