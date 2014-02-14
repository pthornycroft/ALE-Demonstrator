package com.arubanetworks.aledemonstrator;

public class AleCampus {
	String campus_id;
	String campus_name;
	
	public AleCampus(String _campus_id, String _campus_name){
		campus_id = _campus_id;
		campus_name = _campus_name;
	}
	
	public String toString(){
		String result = "campus_id_"+campus_id+"_ campus_name_"+campus_name+"_";
		return result;
	}
	
}
